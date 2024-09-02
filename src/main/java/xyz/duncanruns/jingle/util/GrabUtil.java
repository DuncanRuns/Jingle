package xyz.duncanruns.jingle.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

public final class GrabUtil {
    private static final Gson GSON = new Gson();
    private static final HttpClient httpClient;

    static {
        try {
            httpClient = getHttpClient();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private GrabUtil() {
    }

    private static HttpClient getHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        //https://stackoverflow.com/a/28847175

        try {
            // Test a Let's Encrypt valid page
            IOUtils.toString(new URL("https://valid-isrgrootx1.letsencrypt.org/").openStream(), Charset.defaultCharset());
            // Normal functionality!
            return HttpClientBuilder.create().build();
        } catch (Exception e) {
            System.out.println("Outdated Java, GrabUtil is using an insecure HttpClient!");
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

    public static String grab(String origin) throws IOException {
        HttpGet request = new HttpGet(origin);
        CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String out = EntityUtils.toString(entity);
        response.close();
        return out;
    }

    public static JsonObject grabJson(String origin) throws IOException, JsonSyntaxException {
        return GSON.fromJson(grab(origin), JsonObject.class);
    }

    public static void download(String origin, Path destination, Consumer<Integer> bytesReadConsumer, int consumeInterval) throws IOException {
        HttpGet request = new HttpGet(origin);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request);
             BufferedInputStream sourceStream = new BufferedInputStream(response.getEntity().getContent());
             OutputStream destinationStream = Files.newOutputStream(destination)) {
            int bufferSize = 1024;
            int totalBytesRead = 0;
            {
                byte[] dataBuffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = sourceStream.read(dataBuffer, 0, bufferSize)) != -1) {
                    destinationStream.write(dataBuffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    if (totalBytesRead % consumeInterval == 0) {
                        bytesReadConsumer.accept(totalBytesRead);
                    }
                }
            }
        }
    }

    public static void download(String origin, Path destination) throws IOException {
        download(origin, destination, integer -> {
        }, 1024);
    }

    /**
     * Tells the length of the content, if known.
     *
     * @return the number of bytes of the content, or
     * a negative number if unknown. If the content length is known
     * but exceeds {@link Long#MAX_VALUE Long.MAX_VALUE},
     * a negative number is returned.
     */
    public static long getFileSize(String origin) throws IOException {
        HttpGet request = new HttpGet(origin);
        try (CloseableHttpResponse response = (CloseableHttpResponse) httpClient.execute(request)) {

            return response.getEntity().getContentLength();
        }
    }
}