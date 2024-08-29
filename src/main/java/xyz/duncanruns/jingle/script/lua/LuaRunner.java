package xyz.duncanruns.jingle.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.util.ExceptionUtil;

public final class LuaRunner {
    public static void runLuaScript(ScriptFile script) {
        Globals globals = getSafeGlobals();
        LuaLibraries.addLibraries(script, globals);
        LuaValue chunk = globals.load(script.contents);
        try {
            chunk.call();
        } catch (LuaError e) {
            // Get Root Cause
            Throwable cause = ExceptionUtil.getRootCause(e);
            if (cause instanceof CustomizingException) {
                Jingle.log(Level.ERROR, cause.getMessage());
            } else if (!(cause instanceof InterruptibleDebugLib.LuaInterruptedException)) {
                Jingle.log(Level.ERROR, "Error while executing script: " + ExceptionUtil.toDetailedString(cause != null ? cause : e));
            }
        }
    }

    private static Globals getSafeGlobals() {
        Globals globals = new Globals();
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new JseMathLib());
        LoadState.install(globals);
        LuaC.install(globals);
        return globals;
    }
}
