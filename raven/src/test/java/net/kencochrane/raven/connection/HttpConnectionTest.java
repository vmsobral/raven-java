package net.kencochrane.raven.connection;

import net.kencochrane.raven.Dsn;
import net.kencochrane.raven.event.Event;
import net.kencochrane.raven.event.EventBuilder;
import net.kencochrane.raven.marshaller.Marshaller;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectionTest {
    private HttpConnection httpConnection;
    private String publicKey = UUID.randomUUID().toString();
    private String secretKey = UUID.randomUUID().toString();
    @Mock(answer = Answers.RETURNS_MOCKS)
    private HttpsURLConnection mockUrlConnection;
    @Mock
    private Marshaller mockMarshaller;

    @Before
    public void setUp() throws Exception {
        URLStreamHandler stubUrlHandler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return mockUrlConnection;
            }
        };

        URL mockUrl = new URL(null, "http://", stubUrlHandler);
        httpConnection = new HttpConnection(mockUrl, publicKey, secretKey);
        httpConnection.setMarshaller(mockMarshaller);
    }

    @Test
    public void testTimeout() throws Exception {
        int timeout = 12;
        httpConnection.setTimeout(timeout);
        httpConnection.send(new EventBuilder().build());

        verify(mockUrlConnection).setConnectTimeout(timeout);
    }

    @Test
    public void testByPassSecurity() throws Exception {
        httpConnection.send(new EventBuilder().build());
        verify(mockUrlConnection, never()).setHostnameVerifier(any(HostnameVerifier.class));

        reset(mockUrlConnection);
        httpConnection.setBypassSecurity(true);
        httpConnection.send(new EventBuilder().build());
        verify(mockUrlConnection).setHostnameVerifier(any(HostnameVerifier.class));

        reset(mockUrlConnection);
        httpConnection.setBypassSecurity(false);
        httpConnection.send(new EventBuilder().build());
        verify(mockUrlConnection, never()).setHostnameVerifier(any(HostnameVerifier.class));
    }

    @Test
    public void testContentMarshalled() throws Exception {
        Event event = new EventBuilder().build();

        httpConnection.send(event);

        verify(mockMarshaller).marshall(eq(event), any(OutputStream.class));
    }

    @Test
    public void testApiUrlCreation() throws Exception {
        Dsn dsn = mock(Dsn.class);
        String projectId = UUID.randomUUID().toString();
        String uri = "http://host/sentry/";
        when(dsn.getUri()).thenReturn(new URI(uri));
        when(dsn.getProjectId()).thenReturn(projectId);

        URL sentryApiUrl = HttpConnection.getSentryApiUrl(dsn);

        assertThat(sentryApiUrl.toString(), is(uri + "api/" + projectId + "/store/"));
    }
}