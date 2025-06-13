package xyz.duncanruns.jingle.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

public final class GrabUtil {
    private static final Gson GSON = new Gson();

    private GrabUtil() {
    }

    public static String grab(String origin) throws IOException {
        HttpGet request = new HttpGet(origin);
        CloseableHttpResponse response = (CloseableHttpResponse) HttpClientUtil.getHttpClient().execute(request);
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
        try (CloseableHttpResponse response = (CloseableHttpResponse) HttpClientUtil.getHttpClient().execute(request);
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
        try (CloseableHttpResponse response = (CloseableHttpResponse) HttpClientUtil.getHttpClient().execute(request)) {

            return response.getEntity().getContentLength();
        }
    }
}