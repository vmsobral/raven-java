package com.getsentry.raven.jpos;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.SimpleLogListener;

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
public class SentryListener extends SimpleLogListener 
	implements Configurable {
    /**
     * Name of the {@link Event#extra} property containing NDC details.
     */
    public static final String JPOS_NDC = "jpos-NDC";
    /**
     * Name of the {@link Event#extra} property containing the Thread name.
     */
    public static final String THREAD_NAME = "Raven-Threadname";
    /**
     * Current instance of {@link Raven}.
     *
     * @see #initRaven()
     */
    protected Raven raven;
    /**
     * DSN property of the appender.
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
     * Transforms the location info of a log into a stacktrace element (stackframe).
     *
     * @param location details on the location of the log.
     * @return a stackframe.
     */
//    protected static StackTraceElement asStackTraceElement(LocationInfo location) {
//        String fileName = (LocationInfo.NA.equals(location.getFileName())) ? null : location.getFileName();
//        int line = (LocationInfo.NA.equals(location.getLineNumber())) ? -1 : Integer.parseInt(location.getLineNumber());
//        return new StackTraceElement(location.getClassName(), location.getMethodName(), fileName, line);
//    }

    /**
     * Initialises the Raven instance.
     */
    protected void initRaven() {
        try {
            if (dsn == null)
                dsn = Dsn.dsnLookup();

            raven = RavenFactory.ravenInstance(new Dsn(dsn), ravenFactory);
        } catch (InvalidDsnException e) {
        	logDebugEx("An exception occurred during the retrieval of the DSN for Raven", e);
        } catch (Exception e) {
        	logDebugEx("An exception occurred during the creation of a Raven instance", e);
        }
    }
    
	@Override
	public LogEvent log(LogEvent logEvent) {
		// Do not log the event if the current thread is managed by raven
        if (RavenEnvironment.isManagingThread())
            return null;

        RavenEnvironment.startManagingThread();
        try {
            Event event = buildEvent(logEvent);
            raven.sendEvent(event);
            
            return logEvent;
        } catch (Exception e) {
        	logDebugEx("An exception occurred while creating a new event in Raven", e);
        } finally {
            RavenEnvironment.stopManagingThread();
        }
        
        return null;
	}

    /**
     * Builds an Event based on the logging event.
     *
     * @param loggingEvent Log generated.
     * @return Event containing details provided by the logging system.
     */
    protected Event buildEvent(LogEvent logEvent) {
    	
    	List<Object> payLoad = logEvent.getPayLoad();
    	
        EventBuilder eventBuilder = new EventBuilder()
                .withTimestamp(new Date())
                .withMessage(logEvent.toString())
                .withLogger(logEvent.getSource().getLogger().getName())
                .withLevel(formatLevel(logEvent.getTag()))
                .withExtra(THREAD_NAME, logEvent.getRealm());

        if (!Util.isNullOrEmpty(serverName)) {
            eventBuilder.withServerName(serverName.trim());
        }

        if (!Util.isNullOrEmpty(release)) {
            eventBuilder.withRelease(release.trim());
        }
        
        synchronized (payLoad) {
            for (Object o : payLoad) {
            	if (o instanceof Throwable) {
                    eventBuilder.withSentryInterface(new ExceptionInterface((Throwable) o));
            	}
            	else if (o != null) {
            		eventBuilder.withMessage(o.toString());
            	}
            }
        }
        
//        if (loggingEvent.getLocationInformation().fullInfo != null) {
//            LocationInfo location = loggingEvent.getLocationInformation();
//            if (!LocationInfo.NA.equals(location.getFileName()) && !LocationInfo.NA.equals(location.getLineNumber())) {
//                StackTraceElement[] stackTrace = {asStackTraceElement(location)};
//                eventBuilder.withSentryInterface(new StackTraceInterface(stackTrace));
//            }
//        }

        // Set culprit
//        if (loggingEvent.getLocationInformation().fullInfo != null) {
//            eventBuilder.withCulprit(asStackTraceElement(loggingEvent.getLocationInformation()));
//        } else {
//            eventBuilder.withCulprit(loggingEvent.getLoggerName());
//        }
//
//        if (loggingEvent.getNDC() != null)
//            eventBuilder.withExtra(LOG4J_NDC, loggingEvent.getNDC());

//        @SuppressWarnings("unchecked")
//        Map<String, Object> properties = (Map<String, Object>) loggingEvent.getProperties();
//        for (Map.Entry<String, Object> mdcEntry : properties.entrySet()) {
//            if (extraTags.contains(mdcEntry.getKey())) {
//                eventBuilder.withTag(mdcEntry.getKey(), mdcEntry.getValue().toString());
//            } else {
//                eventBuilder.withExtra(mdcEntry.getKey(), mdcEntry.getValue());
//            }
//        }

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

    @Override
    public void close() {
        RavenEnvironment.startManagingThread();
        try {
            this.close();
            if (raven != null)
                raven.closeConnection();
        } catch (Exception e) {
        	logDebugEx("An exception occurred while closing the Raven connection", e);
        } finally {
            RavenEnvironment.stopManagingThread();
        }
    }
    
    protected void logDebugEx(String msg, Throwable e){
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        ps.println(msg);
        e.printStackTrace(ps);
        ps.close();
//        logDebug(os.toString());
    }

	@Override
	public void setConfiguration(Configuration cfg) throws ConfigurationException {

		this.dsn = cfg.get("dsn");
		
		if (raven == null)
            initRaven();
	}
    
//    protected synchronized void logDebug (String msg) {
//        if (p != null) {
//            p.println ("<log realm=\"rotate-log-listener\" at=\""+new Date().toString() +"\">");
//            p.println ("   "+msg);
//            p.println ("</log>");
//        }
//    }
}
