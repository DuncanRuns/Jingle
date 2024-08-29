package xyz.duncanruns.jingle.hotkey;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.plugin.PluginRegistries;
import xyz.duncanruns.jingle.script.ScriptRegistries;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
            if (!(hotkeyJson.has("type") && hotkeyJson.has("action") && hotkeyJson.has("keys") && hotkeyJson.get("keys").isJsonArray()))
                continue;
            String name = hotkeyJson.get("action").getAsString();
            String type = hotkeyJson.get("type").getAsString();
            List<Integer> keys = hotkeyJson.getAsJsonArray("keys")
                    .asList()
                    .stream()
                    .filter(JsonElement::isJsonPrimitive)
                    .map(JsonElement::getAsJsonPrimitive)
                    .filter(JsonPrimitive::isNumber)
                    .map(JsonPrimitive::getAsInt).collect(Collectors.toList());
            if (keys.isEmpty()) continue;

            boolean ignoreModifiers = hotkeyJson.has("ignoreModifiers") && hotkeyJson.get("ignoreModifiers").getAsBoolean();

            Optional<Runnable> hotkeyAction = Optional.empty();
            switch (type) {
                case "script":
                    hotkeyAction = ScriptRegistries.getHotkeyAction(name);
                    break;
                case "plugin":
                    hotkeyAction = PluginRegistries.getHotkeyAction(name);
                    break;
                case "builtin":
                    hotkeyAction = Jingle.getBuiltinHotkeyAction(name);
                    break;
            }
            hotkeyAction.ifPresent(runnable -> addHotkey(Hotkey.of(keys, ignoreModifiers), runnable));
        }
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
                    hotkeyAction.getRight().run();
                }
            }
        }
    }
}