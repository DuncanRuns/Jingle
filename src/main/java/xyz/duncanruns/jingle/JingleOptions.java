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
    private static final int DEFAULT_LOADED_OPTIONS_VERSION = 1;
    private static final int CURRENT_OPTIONS_VERSION = 2;
    public static final Path OPTIONS_PATH = Jingle.FOLDER.resolve("options.json").toAbsolutePath();
    public static final JingleOptions DEFAULTS = createNew();


    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    public Integer optionsVersion = DEFAULT_LOADED_OPTIONS_VERSION;

    // Minecraft Instance
    public Map<String, Long> seenPaths = new HashMap<>();
    @Nullable
    public int[] borderlessPosition = null;
    public boolean autoBorderless = false;

    // Hotkey
    public List<JsonObject> hotkeys = new ArrayList<>();

    // Script
    public Set<String> disabledDefaultScripts = new HashSet<>(Collections.singletonList("Coop Mode"));

    // Application
    public boolean checkForUpdates = true;
    public boolean usePreReleases = Jingle.VERSION.contains("+pre");
    public String lastCheckedVersion = "";
    public boolean minimizeToTray = false;
    public int[] lastPosition = new int[]{50, 50};
    public int[] lastSize = new int[]{600, 400};

    // OBS Projector
    public boolean projectorEnabled;
    // null for auto, [x,y,w,h] for custom
    @Nullable
    public int[] projectorPosition = null;
    public String projectorWindowPattern = "*- Jingle Mag";
    public boolean minimizeProjector;

    private JingleOptions() {
    }

    private static JingleOptions createNew() {
        JingleOptions options = new JingleOptions();
        options.optionsVersion = CURRENT_OPTIONS_VERSION;
        return options;
    }

    public static JingleOptions load() {
        if (Files.exists(OPTIONS_PATH)) {
            try {
                JingleOptions options = FileUtil.readJson(OPTIONS_PATH, JingleOptions.class);
                options.fix();
                return options;
            } catch (Exception e) {
                Jingle.logError("Failed to load options.json", e);
            }
        }
        return createNew();
    }

    public static void ensureFolderExists() {
        if (!Files.exists(Jingle.FOLDER)) {
            Jingle.FOLDER.toFile().mkdirs();
        }
        assert Files.isDirectory(Jingle.FOLDER);
    }

    private void fix() {
        if (this.optionsVersion == null) this.optionsVersion = 1;
        if (this.optionsVersion < 2) {
            // "Misc" hotkeys -> "Extra Keys" hotkeys
            this.setSavedHotkeys(this.copySavedHotkeys().stream().map(sh -> sh.action.startsWith("Misc:") ? new SavedHotkey(sh.type, "Extra Keys" + sh.action.substring(sh.action.indexOf(":")), sh.keys, sh.ignoreModifiers) : sh).collect(Collectors.toList()));
        }

        this.optionsVersion = DEFAULTS.optionsVersion;
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
