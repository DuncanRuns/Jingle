package xyz.duncanruns.jingle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import xyz.duncanruns.jingle.hotkey.SavedHotkey;
import xyz.duncanruns.jingle.util.FileUtil;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JingleOptions {
    public static final Path OPTIONS_PATH = Jingle.FOLDER.resolve("options.json").toAbsolutePath();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public int[] lastPosition = new int[]{50, 50};
    public Map<String, Long> seenPaths = new HashMap<>();
    public List<JsonObject> hotkeys = new ArrayList<>();
    public Set<String> disabledDefaultScripts = new HashSet<>(Collections.singletonList("Coop Mode"));
    public boolean checkForUpdates = true;
    public boolean usePreReleases = false;
    public String lastCheckedVersion = "";
    public boolean minimizeToTray = false;

    public boolean projectorEnabled;
    // null for auto, [x,y,w,h] for custom
    @Nullable public int[] projectorPosition = null;
    public String projectorWindowPattern = "*- Jingle Mag";

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

    /**
     * Converts the stored hotkey JsonObjects into SavedHotkey objects and returns as a copy. Modifying the returned list does not affect saved hotkeys.
     */
    public List<SavedHotkey> copySavedHotkeys() {
        return new ArrayList<>(this.hotkeys.stream().map(SavedHotkey::fromJson).filter(Optional::isPresent).map(Optional::get).distinct().collect(Collectors.toList()));
    }

    /**
     * Converts the given list of SavedHotkey objects into JsonObjects and stores them in this options object.
     */
    public void setSavedHotkeys(List<SavedHotkey> hotkeys) {
        this.hotkeys = hotkeys.stream().distinct().map(SavedHotkey::toJson).collect(Collectors.toList());
    }
}
