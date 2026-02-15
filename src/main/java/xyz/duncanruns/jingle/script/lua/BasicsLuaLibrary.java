package xyz.duncanruns.jingle.script.lua;

import org.luaj.vm2.Globals;
import xyz.duncanruns.jingle.script.ScriptFile;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
public class BasicsLuaLibrary extends LuaLibrary {
    public BasicsLuaLibrary(@Nullable ScriptFile script, @Nullable Globals globals) {
        super("basics", script, globals);
    }

    public boolean stringEndsWith(String haystack, String needle) {
        return haystack.endsWith(needle);
    }

    public boolean stringStartsWith(String haystack, String needle) {
        return haystack.startsWith(needle);
    }
}
