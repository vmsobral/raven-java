package com.getsentry.raven.jpos;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.getsentry.raven.Raven;
import com.getsentry.raven.RavenFactory;
import com.getsentry.raven.dsn.Dsn;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;

public class SentryListenerDsnTest {
    @Tested
    private SentryListener sentryListener = null;
    @Injectable
    private Raven mockRaven = null;
    @Mocked("ravenInstance")
    private RavenFactory mockRavenFactory = null;
    @Mocked("dsnLookup")
    private Dsn mockDsn = null;

    @BeforeMethod
    public void setUp() throws Exception {
    	sentryListener = new SentryListener();
    }

    @Test
    public void testDsnDetected() throws Exception {
        final String dsnUri = "protocol://public:private@host/1";
        new Expectations(sentryListener) {{
        	Dsn.dsnLookup();
            result = dsnUri;
            RavenFactory.ravenInstance(withEqual(new Dsn(dsnUri)), anyString);
            result = mockRaven;
            sentryListener.handleError(anyString, (Throwable) any); times = 0;
        }};

        sentryListener.initRaven();
    }

    @Test
    public void testDsnProvided() throws Exception {
        final String dsnUri = "protocol://public:private@host/2";
        sentryListener.setDsn(dsnUri);
        new Expectations(sentryListener) {{
            RavenFactory.ravenInstance(withEqual(new Dsn(dsnUri)), anyString);
            result = mockRaven;
            sentryListener.handleError(anyString, (Throwable) any); times = 0;
        }};

        sentryListener.initRaven();
    }
}
