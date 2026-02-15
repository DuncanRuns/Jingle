package xyz.duncanruns.jingle.script;

import com.google.gson.JsonObject;
import me.duncanruns.kerykeion.listeners.HermesStateListener;
import org.luaj.vm2.LuaValue;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.lua.LuaConverter;

/**
 * Relays Hermes state changes to scripts.
 */
public class HermesStateScriptRelay implements HermesStateListener {
    public static final HermesStateScriptRelay INSTANCE = new HermesStateScriptRelay();
    private static LuaValue latestState = LuaValue.NIL;

    public static HermesStateScriptRelay get() {
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

    public static LuaValue getLatestState() {
        return latestState;
    }
}
