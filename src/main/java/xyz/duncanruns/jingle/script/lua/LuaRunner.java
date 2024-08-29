package xyz.duncanruns.jingle.script.lua;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import xyz.duncanruns.jingle.script.ScriptFile;

public final class LuaRunner {
    public static void runLuaScript(ScriptFile script) {
        Globals globals = getSafeGlobals();
        LuaLibraries.addLibraries(script, globals);
        LuaValue chunk = globals.load(script.contents);
        chunk.call();
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
