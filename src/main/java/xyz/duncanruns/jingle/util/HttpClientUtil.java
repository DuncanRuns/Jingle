package xyz.duncanruns.jingle.util;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public final class HttpClientUtil {
    private static final HttpClient httpClient;

    static {
        try {
            httpClient = initHttpClient();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpClientUtil() {
    }

    private static HttpClient initHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        //https://stackoverflow.com/a/28847175

        try {
            // Test a Let's Encrypt valid page
            IOUtils.toString(new URL("https://valid-isrgrootx1.letsencrypt.org/").openStream(), Charset.defaultCharset());
            // Normal functionality!
            return HttpClientBuilder.create().build();
        } catch (Exception e) {
            System.out.println("Outdated Java, HttpClientUtil has an insecure HttpClient!");
        }

        HttpClientBuilder b = HttpClientBuilder.create();


        SSLContext sslContext = SSLContextBuilder.create().loadTrustMaterial((chain, authType) -> true).build();
        b.setSSLContext(sslContext);

        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        b.setConnectionManager(connMgr);
        return b.build();
    }

    public static HttpClient getHttpClient() {
        return httpClient;
    }
}
