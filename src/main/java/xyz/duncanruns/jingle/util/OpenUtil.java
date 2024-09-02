package xyz.duncanruns.jingle.util;

import com.sun.jna.platform.win32.Shell32;

import java.nio.file.Paths;

public final class OpenUtil {
    private OpenUtil() {
    }

    public static void openFile(String path) {
        Shell32.INSTANCE.ShellExecute(null, "open", path, null, Paths.get(path).getParent().toString(), 1);
    }
}
