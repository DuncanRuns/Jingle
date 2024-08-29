package xyz.duncanruns.jingle.plugin;

import java.util.*;

public class PluginRegistries {
    private static final Map<String, Runnable> HOTKEYS_ACTIONS = new HashMap<>();

    public static void addHotkeyAction(String name, Runnable runnable) {
        HOTKEYS_ACTIONS.put(name, runnable);
    }

    public static Set<String> getHotkeyActionNames() {
        return Collections.unmodifiableSet(HOTKEYS_ACTIONS.keySet());
    }

    public static Optional<Runnable> getHotkeyAction(String hotkeyName) {
        return Optional.ofNullable(HOTKEYS_ACTIONS.getOrDefault(hotkeyName, null));
    }
}
