package xyz.duncanruns.jingle.script;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.event.RunnableEventType;
import xyz.duncanruns.jingle.script.lua.LuaRunner;
import xyz.duncanruns.jingle.util.ResourceUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ScriptStuff {
    private static final Map<String, Runnable> HOTKEYS_ACTIONS = new HashMap<>(); // Example: "test.lua:run" -> (runHotkey, cancellingFunction)
    private static final Map<String, Runnable> CUSTOMIZATION_FUNCTIONS = new HashMap<>(); // Example: "test.lua" -> (function)
    private static final Map<String, Map<String, Runnable>> EXTRA_FUNCTIONS = new HashMap<>();
    private static final List<ScriptFile> LOADED_SCRIPTS = new ArrayList<>();

    public static final RunnableEventType START_TICK = new RunnableEventType();
    public static final RunnableEventType END_TICK = new RunnableEventType();
    public static final RunnableEventType MAIN_INSTANCE_CHANGED = new RunnableEventType();
    public static final RunnableEventType STATE_CHANGE = new RunnableEventType();
    public static final RunnableEventType EXIT_WORLD = new RunnableEventType();
    public static final RunnableEventType ENTER_WORLD = new RunnableEventType();

    private static final Map<String, RunnableEventType> SCRIPT_EVENTS = new HashMap<>();

    static {
        SCRIPT_EVENTS.put("START_TICK", START_TICK);
        SCRIPT_EVENTS.put("END_TICK", END_TICK);
        SCRIPT_EVENTS.put("MAIN_INSTANCE_CHANGED", MAIN_INSTANCE_CHANGED);
        SCRIPT_EVENTS.put("STATE_CHANGE", STATE_CHANGE);
        SCRIPT_EVENTS.put("EXIT_WORLD", EXIT_WORLD);
        SCRIPT_EVENTS.put("ENTER_WORLD", ENTER_WORLD);
    }

    private ScriptStuff() {
    }

    public static Set<String> getHotkeyActionNames() {
        return Collections.unmodifiableSet(HOTKEYS_ACTIONS.keySet());
    }

    public static Optional<Runnable> getHotkeyAction(String hotkeyName) {
        return Optional.ofNullable(HOTKEYS_ACTIONS.getOrDefault(hotkeyName, null));
    }

    public static Optional<Runnable> getCustomizationFunction(String scriptName) {
        return Optional.ofNullable(CUSTOMIZATION_FUNCTIONS.getOrDefault(scriptName, null));
    }

    public static Optional<Map<String, Runnable>> getExtraFunctions(String scriptName) {
        return Optional.ofNullable(EXTRA_FUNCTIONS.getOrDefault(scriptName, null)).map(LinkedHashMap::new);
    }

    public static List<ScriptFile> getLoadedScripts() {
        return Collections.unmodifiableList(LOADED_SCRIPTS);
    }

    private static void clear() {
        HOTKEYS_ACTIONS.clear();
        CUSTOMIZATION_FUNCTIONS.clear();
        EXTRA_FUNCTIONS.clear();
        LOADED_SCRIPTS.clear();
        SCRIPT_EVENTS.values().forEach(RunnableEventType::clear);
    }

    public static void addHotkeyAction(ScriptFile script, String hotkeyName, Runnable hotkeyFunction) {
        HOTKEYS_ACTIONS.put(script.getName() + ":" + hotkeyName, hotkeyFunction);
    }

    public static void setCustomization(ScriptFile scriptFile, Runnable customizationFunction) {
        CUSTOMIZATION_FUNCTIONS.put(scriptFile.getName(), customizationFunction);
    }

    /**
     * HotkeyManager should be reloaded directly afterward.
     */
    public static void reloadScripts() {
        clear();
        CustomizableManager.load();
        Path scriptsFolder = Jingle.FOLDER.resolve("scripts");
        if (!Files.exists(scriptsFolder)) {
            try {
                Files.createDirectory(scriptsFolder);
            } catch (IOException e) {
                Jingle.logError("Failed to create scripts folder:", e);
                return;
            }
        }
        try {
            Files.list(scriptsFolder).filter(path -> path.getFileName().toString().endsWith(".lua")).forEach(path -> {
                try {
                    ScriptFile script = ScriptFile.loadFile(path);
                    LuaRunner.runLuaScript(script);
                    LOADED_SCRIPTS.add(script);
                } catch (Exception e) {
                    Jingle.logError("Failed to load script \"" + path.getFileName() + "\":", e);
                }
            });
        } catch (Exception e) {
            Jingle.logError("Failed to load scripts:", e);
        }
        try {
            for (String s : ResourceUtil.getResourcesFromFolder("defaultscripts")) {
                try {
                    ScriptFile script = ScriptFile.loadResource("/defaultscripts/" + s);
                    if (!Jingle.options.disabledDefaultScripts.contains(s.substring(0, s.length() - 4))) {
                        LuaRunner.runLuaScript(script);
                    }
                    LOADED_SCRIPTS.add(script);
                } catch (Exception e) {
                    Jingle.logError("Failed to load script \"" + s + "\":", e);
                }
            }
        } catch (Exception e) {
            Jingle.logError("Failed to load default scripts:", e);
        }
    }

    public static void addExtraFunction(ScriptFile script, String functionName, Runnable runnable) {
        EXTRA_FUNCTIONS.computeIfAbsent(script.getName(), s -> new LinkedHashMap<>()).put(functionName, runnable);
    }

    public static boolean registerEventListener(String eventName, Runnable runnable) {
        return Optional.ofNullable(SCRIPT_EVENTS.get(eventName)).map(e -> {
            e.register(runnable);
            return true;
        }).orElse(false);
    }
}
