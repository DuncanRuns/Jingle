package xyz.duncanruns.jingle.util;

import com.sun.jna.platform.win32.WinDef.HWND;
import xyz.duncanruns.jingle.win32.User32;

import java.util.regex.Pattern;

public final class WindowTitleUtil {
    private static final Pattern MC_PATTERN = Pattern.compile("^Minecraft\\*? ?.+$");

    private WindowTitleUtil() {
    }

    public static String getHwndTitle(HWND hwnd) {
        byte[] x = new byte[128];
        User32.INSTANCE.GetWindowTextA(hwnd, x, 128);
        StringBuilder out = new StringBuilder();
        for (byte a : x) {
            if (a == 0) {
                break;
            }
            out.append((char) a);
        }
        return out.toString();
    }

    public static boolean matchesMinecraft(String title) {
        return MC_PATTERN.matcher(title).matches();
    }
}
