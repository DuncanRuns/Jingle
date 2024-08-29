package xyz.duncanruns.jingle.script;

import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.lua.InterruptibleDebugLib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ScriptRegistries {
    private static final Map<String, Pair<Runnable, Supplier<ScriptInterrupter>>> HOTKEYS = new HashMap<>(); // Example: "test.lua:run" -> (runHotkey, cancellingFunction)
    private static final Map<String, Runnable> CUSTOMIZATIONS = new HashMap<>(); // Example: "test.lua" -> (function)

    public static Map<String, Runnable> getHotkeys() {
        Map<String, Runnable> map = new HashMap<>();
        for (Map.Entry<String, Pair<Runnable, Supplier<ScriptInterrupter>>> entry : HOTKEYS.entrySet()) {
            Runnable hotkeyFunction = entry.getValue().getLeft();
            Supplier<ScriptInterrupter> interrupterSupplier = entry.getValue().getRight();
            map.put(entry.getKey(), () -> {
                ScriptInterrupter scriptInterrupter = interrupterSupplier.get();
                try {
                    synchronized (Jingle.class) {
                        hotkeyFunction.run();
                    }
                } catch (InterruptibleDebugLib.LuaInterruptedException ignored) {
                }
                scriptInterrupter.invalidate();
            });
        }
        return map;
    }

    /**
     * @param fullName The script file name and registered hotkey name (example: "test.lua:run")
     */
    public static Optional<Runnable> getScriptHotkeyRunner(String fullName) {
        Pair<Runnable, Supplier<ScriptInterrupter>> stuff = HOTKEYS.get(fullName);
        if (stuff == null) return Optional.empty();
        Runnable hotkeyFunction = stuff.getLeft();
        Supplier<ScriptInterrupter> interrupterSupplier = stuff.getRight();

        return Optional.of(() -> {
            ScriptInterrupter scriptInterrupter = interrupterSupplier.get();
            try {
                synchronized (Jingle.class) {
                    hotkeyFunction.run();
                }
            } catch (InterruptibleDebugLib.LuaInterruptedException ignored) {
            }
            scriptInterrupter.invalidate();
        });

    }

    public static Map<String, Runnable> getCustomizations() {
        return Collections.unmodifiableMap(CUSTOMIZATIONS);
    }

    public static void clear() {
        HOTKEYS.clear();
        CUSTOMIZATIONS.clear();
    }

    public static void addHotkey(ScriptFile script, String hotkeyName, Runnable hotkeyFunction, Supplier<ScriptInterrupter> interrupterSupplier) {
        HOTKEYS.put(script.getName() + ":" + hotkeyName, Pair.of(hotkeyFunction, interrupterSupplier));
    }
}
