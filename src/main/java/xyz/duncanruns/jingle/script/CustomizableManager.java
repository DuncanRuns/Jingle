package xyz.duncanruns.jingle.script;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.FileUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CustomizableManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static JsonObject json = new JsonObject();
    private static final Path STORAGE_PATH = Jingle.FOLDER.resolve("scripts").resolve("customizable.storage");

    private CustomizableManager() {
    }

    public synchronized static void load() {
        if (Files.exists(STORAGE_PATH)) {
            String s;
            try {
                s = FileUtil.readString(STORAGE_PATH);
                json = GSON.fromJson(s, JsonObject.class);
            } catch (IOException e) {
                Jingle.logError("Failed to load customizable storage:", e);
            }
        }
    }

    public synchronized static void save() {
        try {
            FileUtil.writeString(STORAGE_PATH, GSON.toJson(json));
        } catch (IOException e) {
            Jingle.logError("Failed to save customizable storage:", e);
        }
    }

    public synchronized static void set(String scriptName, String key, Object val) {
        if (!json.has(scriptName)) {
            json.add(scriptName, new JsonObject());
        }
        JsonObject scriptSpace = json.getAsJsonObject(scriptName);
        scriptSpace.addProperty(key, val == null ? null : val.toString());
        save();
    }

    @Nullable
    public synchronized static String get(String scriptName, String key) {
        if (!json.has(scriptName)) {
            return null;
        }
        JsonObject scriptSpace = json.getAsJsonObject(scriptName);
        if (!scriptSpace.has(key)) {
            return null;
        }
        JsonElement element = scriptSpace.get(key);
        return element.isJsonNull() ? null : element.getAsString();
    }
}
