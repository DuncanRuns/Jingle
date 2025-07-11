package xyz.duncanruns.jingle.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class MCLogsUtil {

    private static final Gson GSON = new Gson();

    private MCLogsUtil() {
    }

    /*
         https://api.mclo.gs/

         Paste a log file (POST https://api.mclo.gs/1/log)
         Field      Content     Description
         content    string	    The raw log file content as string. Maximum length is 10MiB and 25k lines,
                                will be shortened if necessary.
         */
    public static JsonObject uploadLog(Path latestTxt) throws IOException {
        HttpPost request = new HttpPost("https://api.mclo.gs/1/log");

        List<String> lines = Files.readAllLines(latestTxt);
        List<NameValuePair> pairs = Collections.singletonList(
                new BasicNameValuePair("content", String.join("\n", lines))
        );
        request.setEntity(new UrlEncodedFormEntity(pairs, "UTF-8"));

        HttpResponse response = HttpClientUtil.getHttpClient().execute(request);
        HttpEntity entity = response.getEntity();

        return GSON.fromJson(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8), JsonObject.class);
    }

}