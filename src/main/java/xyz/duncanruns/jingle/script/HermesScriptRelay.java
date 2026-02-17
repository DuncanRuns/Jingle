package xyz.duncanruns.jingle.script;

import com.google.gson.JsonObject;
import me.duncanruns.kerykeion.listeners.HermesStateListener;
import me.duncanruns.kerykeion.listeners.HermesWorldLogListener;
import org.luaj.vm2.LuaValue;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.lua.LuaConverter;

/**
 * Relays Hermes state changes to scripts.
 */
public class HermesScriptRelay implements HermesStateListener, HermesWorldLogListener {
    public static final HermesScriptRelay INSTANCE = new HermesScriptRelay();
    private static LuaValue latestState = LuaValue.NIL;
    private static LuaValue latestWorldLogEntry = LuaValue.NIL;

    public static HermesScriptRelay get() {
        return INSTANCE;
    }

    @Override
    public void onInstanceStateChange(JsonObject instanceInfo, JsonObject stateJson) {
        Integer current = Jingle.getMainInstance().map(i -> i.pid).orElse(null);
        if (current == null) return;
        if (instanceInfo.get("pid").getAsInt() != current) return;
        latestState = LuaConverter.fromJson(stateJson);
        ScriptStuff.HERMES_STATE_CHANGE.runAll();
    }

    @Override
    public void onWorldLogEntry(JsonObject instanceInfo, JsonObject logJson, boolean isNew) {
        Integer current = Jingle.getMainInstance().map(i -> i.pid).orElse(null);
        if (current == null) return;
        if (!isNew) return;
        if (instanceInfo.get("pid").getAsInt() != current) return;
        latestWorldLogEntry = LuaConverter.fromJson(logJson);
        ScriptStuff.HERMES_WORLD_LOG.runAll();
    }

    public static LuaValue getLatestState() {
        return latestState;
    }

    public static LuaValue getLatestWorldLogEntry() {
        return latestWorldLogEntry;
    }
}
