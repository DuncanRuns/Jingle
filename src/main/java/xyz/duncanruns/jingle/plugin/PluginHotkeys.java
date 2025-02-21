package xyz.duncanruns.jingle.plugin;

import java.util.*;

public final class PluginHotkeys {
    private static final Map<String, Runnable> HOTKEYS_ACTIONS = new HashMap<>();

    private PluginHotkeys() {
    }

    public static void addHotkeyAction(String name, Runnable runnable) {
        HOTKEYS_ACTIONS.put(name, runnable);
    }

    public static void removeHotkeyAction(String name) {
        HOTKEYS_ACTIONS.remove(name);
    }

    public static Set<String> getHotkeyActionNames() {
        return Collections.unmodifiableSet(HOTKEYS_ACTIONS.keySet());
    }

    public static Optional<Runnable> getHotkeyAction(String hotkeyName) {
        return Optional.ofNullable(HOTKEYS_ACTIONS.getOrDefault(hotkeyName, null));
    }
}
