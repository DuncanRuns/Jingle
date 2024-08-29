package xyz.duncanruns.jingle.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtil {
    private static final Gson GSON = new Gson();

    private FileUtil() {
    }

    public static void writeString(Path path, String string) throws IOException {
        FileWriter writer = new FileWriter(path.toFile());
        writer.write(string);
        writer.close();
    }

    public static String readString(Path path) throws IOException {
        return new String(Files.readAllBytes(path));
    }

    public static JsonObject readJson(Path path) throws IOException, JsonSyntaxException {
        return readJson(path, JsonObject.class);
    }

    public static <T> T readJson(Path path, Class<T> clazz) throws IOException, JsonSyntaxException {
        return GSON.fromJson(readString(path), clazz);
    }
}
