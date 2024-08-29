package xyz.duncanruns.jingle.script;

import java.util.*;

public class ScriptRegistries {
    private static final Map<String, Runnable> HOTKEYS_ACTIONS = new HashMap<>(); // Example: "test.lua:run" -> (runHotkey, cancellingFunction)
    private static final Map<String, Runnable> CUSTOMIZATIONS = new HashMap<>(); // Example: "test.lua" -> (function)

    public static Set<String> getHotkeyActionNames() {
        return Collections.unmodifiableSet(HOTKEYS_ACTIONS.keySet());
    }

    public static Optional<Runnable> getHotkeyAction(String hotkeyName) {
        return Optional.ofNullable(HOTKEYS_ACTIONS.getOrDefault(hotkeyName, null));
    }

    public static Map<String, Runnable> getCustomizations() {
        return Collections.unmodifiableMap(CUSTOMIZATIONS);
    }

    public static void clear() {
        HOTKEYS_ACTIONS.clear();
        CUSTOMIZATIONS.clear();
    }

    public static void addHotkeyAction(ScriptFile script, String hotkeyName, Runnable hotkeyFunction) {
        HOTKEYS_ACTIONS.put(script.getName() + ":" + hotkeyName, hotkeyFunction);
    }

    public static void addCustomization(ScriptFile scriptFile, Runnable customizationFunction) {
        CUSTOMIZATIONS.put(scriptFile.getName(), customizationFunction);
    }
}
