package xyz.duncanruns.jingle.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.ScriptInterrupter;
import xyz.duncanruns.jingle.script.ScriptRegistries;

@SuppressWarnings("unused")
class JingleLuaLibrary extends LuaLibrary {
    public JingleLuaLibrary(ScriptFile script, Globals globals) {
        super("jingle", script, globals);
    }

    public void addHotkey(String hotkeyName, LuaFunction function) {
        assert this.script != null;
        assert this.globals != null;
        if (hotkeyName.contains(":")) {
            Jingle.log(Level.ERROR, "Can't add hotkey script: script name \"" + hotkeyName + "\" contains a colon!");
        }
        ScriptRegistries.addHotkey(this.script, hotkeyName,
                () -> {
                    InterruptibleDebugLib.unInterruptGlobals(this.globals);
                    function.call();
                }, () -> ScriptInterrupter.add(() -> InterruptibleDebugLib.interruptGlobals(this.globals)));
    }

    public void log(String message) {
        assert this.script != null;
        Jingle.log(Level.INFO, String.format("(%s) %s", this.script.getName(), message));
    }
}
