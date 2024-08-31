package xyz.duncanruns.jingle.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.resizing.Resizing;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.ScriptStuff;


@SuppressWarnings("unused")
class JingleLuaLibrary extends LuaLibrary {
    public JingleLuaLibrary(ScriptFile script, Globals globals) {
        super("jingle", script, globals);
    }

    private static Runnable wrapFunction(LuaFunction function) {
        return () -> {
            synchronized (Jingle.class) {
                function.call();
            }
        };
    }

    public void addHotkey(String hotkeyName, LuaFunction function) {
        assert this.script != null;
        assert this.globals != null;
        if (hotkeyName.contains(":")) {
            Jingle.log(Level.ERROR, "Can't add hotkey script: script name \"" + hotkeyName + "\" contains a colon!");
        }
        ScriptStuff.addHotkeyAction(this.script, hotkeyName, wrapFunction(function));
    }

    public void setCustomization(LuaFunction function) {
        assert this.script != null;
        ScriptStuff.setCustomization(this.script, wrapFunction(function));
    }

    public void addExtraFunction(String functionName, LuaFunction function) {
        assert this.script != null;
        ScriptStuff.addExtraFunction(this.script, functionName, wrapFunction(function));
    }

    public void toggleResize(int width, int height) {
        Resizing.toggleResize(width, height);
    }

    public boolean isInstanceActive() {
        return Jingle.isInstanceActive();
    }

    public void log(String message) {
        assert this.script != null;
        Jingle.log(Level.INFO, String.format("(%s) %s", this.script.getName(), message));
    }

}
