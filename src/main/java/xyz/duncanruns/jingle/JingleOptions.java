package xyz.duncanruns.jingle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import xyz.duncanruns.jingle.util.FileUtil;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JingleOptions {
    public static final Path OPTIONS_PATH = Jingle.FOLDER.resolve("options.json").toAbsolutePath();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Map<String, Long> seenPaths = new HashMap<>();

    public static JingleOptions load() {
        if (Files.exists(OPTIONS_PATH)) {
            try {
                return FileUtil.readJson(OPTIONS_PATH, JingleOptions.class);
            } catch (Exception e) {
                Jingle.logError("Failed to load options.json", e);
            }
        }
        return new JingleOptions();
    }

    public static void ensureFolderExists() {
        if (!Files.exists(Jingle.FOLDER)) {
            Jingle.FOLDER.toFile().mkdirs();
        }
        assert Files.isDirectory(Jingle.FOLDER);
    }

    public void save() {
        try {
            FileUtil.writeString(OPTIONS_PATH, GSON.toJson(this));
        } catch (Exception e) {
            Jingle.logError("Failed to save options.json:", e);
        }
    }
}
