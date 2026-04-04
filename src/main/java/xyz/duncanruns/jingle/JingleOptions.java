package xyz.duncanruns.jingle;

import com.google.gson.*;
import xyz.duncanruns.jingle.hotkey.SavedHotkey;
import xyz.duncanruns.jingle.util.FileUtil;

import javax.annotation.Nullable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class JingleOptions {
    private static final int DEFAULT_LOADED_OPTIONS_VERSION = 1;
    private static final int CURRENT_OPTIONS_VERSION = 8;
    public static final JingleOptions DEFAULTS = createNew();

    private static final Gson LOAD_GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Gson SAVE_GSON = new GsonBuilder().setPrettyPrinting().serializeNulls()
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    return field.getAnnotation(LoadOnly.class) != null;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            }).create();

    public Integer optionsVersion = DEFAULT_LOADED_OPTIONS_VERSION;

    // Minecraft Instance
    public Map<String, Long> seenPaths = new HashMap<>();
    @Nullable
    public int[] borderlessPosition = null;
    public boolean autoBorderless = false;

    // Hotkey
    public List<JsonObject> hotkeys = new ArrayList<>();

    // Script
    public Set<String> disabledScripts = new HashSet<>(Collections.singletonList("Coop Mode"));

    // Application
    public boolean checkForUpdates = true;
    public boolean usePreReleases = Jingle.VERSION.contains("+pre");
    public String lastCheckedVersion = "";
    public boolean minimizeToTray = false;
    public int[] lastPosition = new int[]{50, 50};
    public int[] lastSize = new int[]{600, 400};

    public boolean seenGuttingWarning = false;

    @Deprecated
    @LoadOnly
    @Nullable
    public Set<String> disabledDefaultScripts = null;

    private JingleOptions() {
    }

    private static JingleOptions createNew() {
        JingleOptions options = new JingleOptions();
        options.optionsVersion = CURRENT_OPTIONS_VERSION;
        return options;
    }

    public static JingleOptions load() {
        List<Path> optionsPaths = new ArrayList<>();
        for (int i = CURRENT_OPTIONS_VERSION; i > 5; i--) {
            optionsPaths.add(getOptionsPathForVersion(i, false));
            optionsPaths.add(getOptionsPathForVersion(i, true));
        }
        optionsPaths = optionsPaths.stream().filter(Files::exists).collect(Collectors.toList());
        for (Path optionsPath : optionsPaths) {
            Optional<JingleOptions> o = loadFrom(optionsPath);
            if (o.isPresent()) {
                return o.get();
            }
        }
        return createNew();
    }

    private static Path getOptionsPathForVersion(int optionsVersion, boolean backup) {
        if (optionsVersion < 7) {
            return Jingle.FOLDER.resolve("options.json" + (backup ? ".backup" : ""));
        }
        return Jingle.FOLDER.resolve("options." + optionsVersion + ".json" + (backup ? ".backup" : ""));
    }

    private static Optional<JingleOptions> loadFrom(Path path) {
        if (Files.exists(path)) {
            try {
                JingleOptions options = FileUtil.readJson(path, JingleOptions.class, LOAD_GSON);
                options.fix();
                return Optional.of(options);
            } catch (Exception e) {
                Jingle.logError("Failed to load " + path.getFileName().toString(), e);
            }
        }
        return Optional.empty();
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
        if (this.disabledDefaultScripts != null) {
            this.disabledScripts.addAll(this.disabledDefaultScripts);
        }

        // Change Extra Keys Reset Before 20s to Quick Reset
        if (this.optionsVersion < 7) {
            this.setSavedHotkeys(this.copySavedHotkeys().stream().map(sh -> sh.action.equals("Extra Keys:Reset Before 20s") ? new SavedHotkey(sh.type, "Extra Keys:Quick Reset", sh.keys, sh.ignoreModifiers) : sh).collect(Collectors.toList()));
        }

        this.optionsVersion = DEFAULTS.optionsVersion;
    }

    public void save() {
        try {
            FileUtil.writeString(getOptionsPathForVersion(CURRENT_OPTIONS_VERSION, false), SAVE_GSON.toJson(this));
            FileUtil.writeString(getOptionsPathForVersion(CURRENT_OPTIONS_VERSION, true), SAVE_GSON.toJson(this));
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

    /**
     * Marks a field to be loaded from JSON but not saved back to JSON.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface LoadOnly {
    }
}
