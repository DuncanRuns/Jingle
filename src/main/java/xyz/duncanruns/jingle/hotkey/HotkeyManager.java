package xyz.duncanruns.jingle.hotkey;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.plugin.PluginHotkeys;
import xyz.duncanruns.jingle.script.ScriptStuff;

import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static xyz.duncanruns.jingle.util.SleepUtil.sleep;

public final class HotkeyManager {
    public static final CopyOnWriteArrayList<Pair<Hotkey, Runnable>> HOTKEYS = new CopyOnWriteArrayList<>(); // This lets us run the hotkey checker without ever having to stop it

    private HotkeyManager() {
    }

    public static void start() {
        new Thread(HotkeyManager::run, "hotkey-checker").start();
    }

    public static void reload() {
        HOTKEYS.clear();

        for (JsonObject hotkeyJson : Jingle.options.hotkeys) {
            Optional<SavedHotkey> savedHotkeyOpt = SavedHotkey.fromJson(hotkeyJson);
            if (!savedHotkeyOpt.isPresent()) continue;
            SavedHotkey savedHotkey = savedHotkeyOpt.get();
            if (savedHotkey.keys.isEmpty()) continue;

            Optional<Runnable> hotkeyAction = findHotkeyAction(savedHotkey);
            hotkeyAction.ifPresent(runnable -> addHotkey(Hotkey.of(savedHotkey.keys, savedHotkey.ignoreModifiers), runnable));
        }
    }

    public static Optional<Runnable> findHotkeyAction(SavedHotkey savedHotkey) {
        return findHotkeyAction(savedHotkey.type, savedHotkey.action);
    }

    public static Optional<Runnable> findHotkeyAction(String type, String action) {
        Optional<Runnable> hotkeyAction = Optional.empty();
        switch (type) {
            case "script":
                hotkeyAction = ScriptStuff.getHotkeyAction(action);
                break;
            case "plugin":
                hotkeyAction = PluginHotkeys.getHotkeyAction(action);
                break;
            case "builtin":
                hotkeyAction = Jingle.getBuiltinHotkeyAction(action);
                break;
        }
        return hotkeyAction;
    }

    public static void addHotkey(Hotkey hotkey, Runnable onHotkeyPress) {
        if (hotkey.isEmpty()) {
            return;
        }
        HOTKEYS.add(Pair.of(hotkey, onHotkeyPress));
    }

    private static void run() {
        while (Jingle.isRunning()) {
            sleep(1);
            for (Pair<Hotkey, Runnable> hotkeyAction : HOTKEYS) {
                if (hotkeyAction.getLeft().wasPressed()) {
                    try {
                        hotkeyAction.getRight().run();
                    } catch (Throwable t) {
                        Jingle.logError("Error while running hotkey!", t);
                    }
                }
            }
        }
    }
}