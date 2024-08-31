package xyz.duncanruns.jingle.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.obs.OBSProjector;
import xyz.duncanruns.jingle.resizing.Resizing;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.ScriptStuff;


@SuppressWarnings("unused")
class JingleLuaLibrary extends LuaLibrary {
    public JingleLuaLibrary(ScriptFile script, Globals globals) {
        super("jingle", script, globals);
    }

    @NotALuaFunction
    private static Runnable wrapFunction(LuaFunction function) {
        return () -> {
            synchronized (Jingle.class) {
                function.call();
            }
        };
    }

    @LuaDocumentation(description = "Registers a hotkey action. If a user sets up a hotkey with the given hotkey action name and then presses their set hotkey, the given function will be ran.")
    public void addHotkey(String hotkeyName, LuaFunction function) {
        assert this.script != null;
        assert this.globals != null;
        if (hotkeyName.contains(":")) {
            Jingle.log(Level.ERROR, "Can't add hotkey script: script name \"" + hotkeyName + "\" contains a colon!");
        }
        ScriptStuff.addHotkeyAction(this.script, hotkeyName, wrapFunction(function));
    }

    @LuaDocumentation(description = "Registers the customization function for this script. If a user presses the \"Customize\" button for this script, the given function will be ran.")
    public void setCustomization(LuaFunction function) {
        assert this.script != null;
        ScriptStuff.setCustomization(this.script, wrapFunction(function));
    }

    @LuaDocumentation(description = "Registers an extra function for this script. If a user presses the button of the given name found next to this script, the given function will be ran.")
    public void addExtraFunction(String functionName, LuaFunction function) {
        assert this.script != null;
        ScriptStuff.addExtraFunction(this.script, functionName, wrapFunction(function));
    }

    @LuaDocumentation(description = "Runs a resize toggle of the given width and height. Returns true if the size is applied, and returns false if the size is undone.")
    public boolean toggleResize(int width, int height) {
        return Resizing.toggleResize(width, height);
    }

    @LuaDocumentation(description = "Returns true if the instance is active, otherwise false.")
    public boolean isInstanceActive() {
        return Jingle.isInstanceActive();
    }

    @LuaDocumentation(description = "Sets the OBS eye measuring projector to be directly behind the instance, bringing it above everything except for the game itself.")
    public void ensureOBSProjectorZ() {
        OBSProjector.ensureOBSProjectorZ();
    }

    @LuaDocumentation(description = "Dumps the OBS eye measuring projector to the bottom of the window Z order.")
    public void dumpOBSProjector() {
        OBSProjector.dumpOBSProjector();
    }

    @LuaDocumentation(description = "Logs a message to the Jingle log.")
    public void log(String message) {
        assert this.script != null;
        Jingle.log(Level.INFO, String.format("(%s) %s", this.script.getName(), message));
    }
}
