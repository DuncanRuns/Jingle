package xyz.duncanruns.jingle.script.lua;

import org.luaj.vm2.Globals;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.script.ScriptFile;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class ClipboardLuaLibrary extends LuaLibrary {
    private final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

    public ClipboardLuaLibrary(@Nullable ScriptFile script, @Nullable Globals globals) {
        super("clipboard", script, globals);
    }

    public String get() {
        try {
            return clipboard.getData(DataFlavor.stringFlavor).toString();
        } catch (IOException | UnsupportedFlavorException e) {
            Jingle.logError("Failed to get clipboard!", e);
            return null;
        }
    }

    public boolean set(String string) {
        try {
            clipboard.setContents(new StringSelection(string), null);
            return true;
        } catch (Exception e) {
            Jingle.logError("Failed to set clipboard!", e);
            return false;
        }
    }
}
