package xyz.duncanruns.jingle.hotkey;

import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.util.MouseUtil;

import java.awt.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static xyz.duncanruns.jingle.util.SleepUtil.sleep;

public final class HotkeyManager {
    public static final CopyOnWriteArrayList<Pair<Hotkey, Consumer<Point>>> hotkeys = new CopyOnWriteArrayList<>(); // This lets us run the hotkey checker without ever having to stop it


    private HotkeyManager() {
    }

    public static void start() {
        new Thread(HotkeyManager::run, "hotkey-checker").start();
    }

    public static void reload() {
        hotkeys.clear();
        PluginEvents.RunnableEventType.HOTKEY_MANAGER_RELOAD.runAll();
    }

    public static void addHotkey(Hotkey hotkey, Consumer<Point> onHotkeyPress) {
        if (hotkey.isEmpty()) {
            return;
        }
        hotkeys.add(Pair.of(hotkey, onHotkeyPress));
    }

    private static void run() {
        while (Jingle.isRunning()) {
            sleep(1);
            for (Pair<Hotkey, Consumer<Point>> hotkeyAction : hotkeys) {
                if (hotkeyAction.getLeft().wasPressed()) {
                    hotkeyAction.getRight().accept(MouseUtil.getMousePos());
                }
            }
        }
    }
}