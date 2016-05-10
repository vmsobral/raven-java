package com.getsentry.raven.jpos;

import org.jpos.core.Configuration;
import org.testng.annotations.Test;

import com.getsentry.raven.Raven;
import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.dsn.Dsn;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

public class SentryListenerCloseTest {
    @Injectable
    private Raven mockRaven = null;
    @Injectable
    private Configuration cfg = null;
    @Mocked("ravenInstance")
    private RavenFactory mockRavenFactory = null;
    @Mocked("dsnLookup")
    private Dsn mockDsn = null;

    @Test
    public void testConnectionClosedWhenAppenderClosed() throws Exception {
        final SentryListener sentryListener = new SentryListener(mockRaven);
        
        new Expectations(sentryListener) {{
            sentryListener.handleError(anyString, (Throwable) any); times = 0;
        }};
        sentryListener.setConfiguration(cfg);
        
        sentryListener.close();

        new Verifications() {{
            mockRaven.closeConnection();
        }};
    }

    @Test
    public void testClosedIfRavenInstanceNotProvided() throws Exception {
        final String dsnUri = "protocol://public:private@host/1";
        final SentryListener sentryListener = new SentryListener();
        
        new Expectations(sentryListener) {{
            Dsn.dsnLookup();
            result = dsnUri;
            RavenFactory.ravenInstance(withEqual(new Dsn(dsnUri)), anyString);
            result = mockRaven;
            sentryListener.handleError(anyString, (Throwable) any); times = 0;
        }};
        sentryListener.setConfiguration(cfg);

        sentryListener.close();

        new Verifications() {{
            mockRaven.closeConnection();
        }};
    }

    @Test
    public void testCloseDoNotFailIfInitFailed() throws Exception {
        // This checks that even if sentry wasn't setup correctly its listener can still be closed.
        final SentryListener sentryListener = new SentryListener();
        
        new NonStrictExpectations(sentryListener) {{
            RavenFactory.ravenInstance((Dsn) any, anyString);
            result = new UnsupportedOperationException();
            sentryListener.handleError(anyString, (Throwable) any); times = 1;
        }};
        sentryListener.setConfiguration(cfg);

        sentryListener.close();
    }

    @Test
    public void testCloseDoNotFailIfNoInit() throws Exception {
        final SentryListener sentryListener = new SentryListener();

        new Expectations(sentryListener) {{
            sentryListener.handleError(anyString, (Throwable) any); times = 0;
        }};
        
        sentryListener.close();
    }

    @Test
    public void testCloseDoNotFailWhenMultipleCalls() throws Exception {
        final SentryListener sentryListener = new SentryListener(mockRaven);
        
        new Expectations(sentryListener) {{
            sentryListener.handleError(anyString, (Throwable) any); times = 0;
        }};
        sentryListener.setConfiguration(cfg);

        sentryListener.close();
        sentryListener.close();

        new Verifications() {{
            mockRaven.closeConnection();
            times = 1;
        }};
    }
}
