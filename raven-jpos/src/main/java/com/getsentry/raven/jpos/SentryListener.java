package com.getsentry.raven.jpos;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;

import com.getsentry.raven.Raven;
import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.dsn.Dsn;
import com.getsentry.raven.dsn.InvalidDsnException;
import com.getsentry.raven.environment.RavenEnvironment;
import com.getsentry.raven.event.Event;
import com.getsentry.raven.event.EventBuilder;
import com.getsentry.raven.event.interfaces.ExceptionInterface;
import com.getsentry.raven.util.Util;

/**
 * Listener for JPOS in charge of sending the logged events to a Sentry server.
 */
public class SentryListener implements Configurable, LogListener {
	/**
	 * States is the listener is started or not
	 */
	protected boolean started = false;
    /**
     * Current instance of {@link Raven}.
     *
     * @see #initRaven()
     */
    protected Raven raven;
    /**
     * DSN property of the listener.
     * <p>
     * Might be null in which case the DSN should be detected automatically.
     */
    protected String dsn;
    /**
     * Name of the {@link RavenFactory} being used.
     * <p>
     * Might be null in which case the factory should be defined automatically.
     */
    protected String ravenFactory;
    /**
     * Release to be sent to sentry.
     * <p>
     * Might be null in which case no release is sent.
     */
    protected String release;
    /**
     * Server name to be sent to sentry.
     * <p>
     * Might be null in which case the hostname is found via a reverse DNS lookup.
     */
    protected String serverName;
    /**
     * Additional tags to be sent to sentry.
     * <p>
     * Might be empty in which case no tags are sent.
     */
    protected Map<String, String> tags = Collections.emptyMap();
    /**
     * List of tags to look for in the MDC. These will be added as tags to be sent to sentry.
     * <p>
     * Might be empty in which case no mapped tags are set.
     */
    protected Set<String> extraTags = Collections.emptySet();
    /**
     * Priority tag which restricts the level of messages sent to Raven
     */
    private String priority = Log.WARN;
    /**
     * Hashtable representing the possible levels of logging and its ordering
     */
	private static Hashtable<String, Integer> levels;

    static{
            levels = new Hashtable<String, Integer>(6);
            levels.put(Log.TRACE, 1);
            levels.put(Log.DEBUG, 2);
            levels.put(Log.INFO, 3);
            levels.put(Log.WARN, 4);
            levels.put(Log.ERROR, 5);
            levels.put(Log.FATAL, 6);
    }

    /**
     * Creates an instance of SentryAppender.
     */
    public SentryListener() {
    	
    }

    /**
     * Creates an instance of SentryAppender.
     *
     * @param raven instance of Raven to use with this appender.
     */
    public SentryListener(Raven raven) {
        this.raven = raven;
    }

    /**
     * Transforms a JPOS level string into an {@link Event.Level}.
     *
     * @param level original level as defined in JPOS.
     * @return log level used within raven.
     */
    protected static Event.Level formatLevel(String level) {
        if (level.equalsIgnoreCase(Log.FATAL)) {
            return Event.Level.FATAL;
        } else if (level.equalsIgnoreCase(Log.ERROR)) {
            return Event.Level.ERROR;
        } else if (level.equalsIgnoreCase(Log.WARN)) {
            return Event.Level.WARNING;
        } else if (level.equalsIgnoreCase(Log.INFO)) {
            return Event.Level.INFO;
        } else if (level.equalsIgnoreCase(Log.DEBUG) ||
        		level.equalsIgnoreCase(Log.TRACE)) {
            return Event.Level.DEBUG;
        } else return null;
    }

    /**
     * Initialises the Raven instance.
     * @throws Exception 
     */
    protected void initRaven() throws Exception {
        try {
            if (dsn == null)
                dsn = Dsn.dsnLookup();

            raven = RavenFactory.ravenInstance(new Dsn(dsn), ravenFactory);
        } catch (InvalidDsnException e) {
        	handleError("An exception occurred during the retrieval of the DSN for Raven", e);
        } catch (Exception e) {
        	handleError("An exception occurred during the creation of a Raven instance", e);
        }
    }
    
	@Override
	public synchronized LogEvent log(LogEvent logEvent) {
		// Do not log the event if the current thread is managed by raven
        if (!RavenEnvironment.isManagingThread()) {
        
	        RavenEnvironment.startManagingThread();
	        try {
	        	if (permitLogging(logEvent.getTag())) {
	                
		            Event event = buildEvent(logEvent);
		            raven.sendEvent(event);
	        	}
	        } catch (Exception e) {
	        	handleError("An exception occurred while creating a new event in Raven", e);
	        	
	        } finally {
	            RavenEnvironment.stopManagingThread();
	        }
        }
        
        return logEvent;
	}

    /**
     * Builds an Event based on the log event.
     *
     * @param logEvent Log generated.
     * @return Event containing details provided by the logging system.
     */
    protected Event buildEvent(LogEvent logEvent) {
    	
    	String message = new String();
    	
        EventBuilder eventBuilder = new EventBuilder()
                .withTimestamp(new Date())
                .withLogger(logEvent.getSource().getLogger().getName())
                .withLevel(formatLevel(logEvent.getTag()))
                .withExtra("Realm", logEvent.getRealm());

        if (!Util.isNullOrEmpty(serverName)) {
            eventBuilder.withServerName(serverName.trim());
        }

        if (!Util.isNullOrEmpty(release)) {
            eventBuilder.withRelease(release.trim());
        }
        
        if (logEvent.getPayLoad().isEmpty()) {
        	message = logEvent.getPayLoad().toString();
        }
        else {
	        for (Object o : logEvent.getPayLoad()) {
	        	if (o instanceof SQLException) {
	                SQLException e = (SQLException) o;
	                eventBuilder.withSentryInterface(new ExceptionInterface(e));
	                eventBuilder.withExtra("SQLState", e.getSQLState());
	                eventBuilder.withCulprit(e.getStackTrace()[0]);
	                if (message.isEmpty())
	                	message = e.getMessage();
	            }
	        	else if (o instanceof Throwable) {
	        		Throwable t = (Throwable) o;
	                eventBuilder.withSentryInterface(new ExceptionInterface(t));
	                eventBuilder.withCulprit(t.getStackTrace()[0]);
	                if (message.isEmpty())
	                	message = t.getMessage();
	        	}
	        	else if (o != null) {
	        		message = o.toString();
	        		eventBuilder.withCulprit(logEvent.getRealm());
	        	}
	        	else
	        		if (message.isEmpty()) message = "null";
	        }
        }
        
        eventBuilder.withMessage(message);
        
        for (Map.Entry<String, String> tagEntry : tags.entrySet())
            eventBuilder.withTag(tagEntry.getKey(), tagEntry.getValue());

        raven.runBuilderHelpers(eventBuilder);
        return eventBuilder.build();
    }

    public void setRavenFactory(String ravenFactory) {
        this.ravenFactory = ravenFactory;
    }

    public void setDsn(String dsn) {
        this.dsn = dsn;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Set the tags that should be sent along with the events.
     *
     * @param tags A String of tags. key/values are separated by colon(:) and tags are separated by commas(,).
     */
    public void setTags(String tags) {
        this.tags = Util.parseTags(tags);
    }

    /**
     * Set the mapped extras that will be used to search MDC and upgrade key pair to a tag sent along with the events.
     *
     * @param extraTags A String of extraTags. extraTags are separated by commas(,).
     */
    public void setExtraTags(String extraTags) {
        this.extraTags = new HashSet<>(Arrays.asList(extraTags.split(",")));
    }

    public void close() throws Exception {
        RavenEnvironment.startManagingThread();
        try {
        	if (!started)
        		return;
            if (raven != null)
                raven.closeConnection();
            this.started = false;
        } catch (Exception e) {
        	throw new Exception("An exception occurred while closing the Raven connection", e);
        } finally {
            RavenEnvironment.stopManagingThread();
        }
    }

	@Override
	public void setConfiguration(Configuration cfg) throws ConfigurationException {

		try {
            
			this.dsn = cfg.get("dsn");
			setTags(cfg.get("tags"));
			this.ravenFactory = cfg.get("ravenFactory", null);
			String log_priority = cfg.get("priority", Log.WARN);
			
	        if ( (log_priority != null) && (!log_priority.trim().equals("")) )
	        {
	        	if (levels.containsKey(log_priority))
	        		priority = log_priority;
	        }
	        if (raven == null)
	            initRaven();
	        
	        this.started = true;
	        
		} catch (Exception e) {
			throw new ConfigurationException (e);
		}
	}
	
	public boolean permitLogging(String tagLevel) {
		Integer I = (Integer)levels.get(tagLevel);

	    if (I == null) {
	    	return false;
	    } else {
	    	Integer J = (Integer)levels.get(priority);
	    	return (I >= J);
	    }
	}
	
	protected void handleError(String msg, Throwable e) {
        PrintStream ps = new PrintStream(new ByteArrayOutputStream());
        ps.println(msg);
        if (e != null) e.printStackTrace(ps);
        ps.close();
    }
}
