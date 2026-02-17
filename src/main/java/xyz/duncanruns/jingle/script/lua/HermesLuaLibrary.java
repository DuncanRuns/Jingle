package xyz.duncanruns.jingle.script.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import xyz.duncanruns.jingle.script.HermesScriptRelay;
import xyz.duncanruns.jingle.script.ScriptFile;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class HermesLuaLibrary extends LuaLibrary {
    public HermesLuaLibrary(@Nullable ScriptFile script, @Nullable Globals globals) {
        super("hermes", script, globals);
    }

    @LuaDocumentation(description = "Gets the latest Hermes state data. Should be used with jingle.listen(\"HERMES_STATE_CHANGE\", ...).")
    public LuaValue getState() {
        return HermesScriptRelay.getLatestState();
    }

    @LuaDocumentation(description = "Gets the latest Hermes world log data. Should be used with jingle.listen(\"HERMES_WORLD_LOG\", ...).")
    public LuaValue getWorldLogEntry() {
        return HermesScriptRelay.getLatestWorldLogEntry();
    }
}
