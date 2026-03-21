package xyz.duncanruns.jingle.util;


import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.jingle.win32.User32;

public class ToolscreenUtil {
    private static final WinDef.UINT TOOL_SCREEN_IS_INSTALLED = User32.INSTANCE.RegisterWindowMessageA("Toolscreen_IsInstalled");
    private static final WinDef.UINT TOOL_SCREEN_GET_VERSION = User32.INSTANCE.RegisterWindowMessageA("Toolscreen_GetVersion");

    private static final WinDef.UINT WM_TOOLSCREEN_CHAR_NO_REBIND = new WinDef.UINT(0x8000 + 0x2A1);
    private static final WinDef.UINT WM_TOOLSCREEN_KEYDOWN_NO_REBIND = new WinDef.UINT(0x8000 + 0x2A2);
    private static final WinDef.UINT WM_TOOLSCREEN_KEYUP_NO_REBIND = new WinDef.UINT(0x8000 + 0x2A3);


    public static boolean hasToolscreen(WinDef.HWND hwnd) {
        WinDef.LRESULT lresult = User32.INSTANCE.SendMessageA(hwnd, TOOL_SCREEN_IS_INSTALLED, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
        System.out.println("toolscreen installed: " + lresult.intValue());
        return lresult.intValue() != 0;
    }

    public static String getToolscreenVersion(WinDef.HWND hwnd) {
        int i = User32.INSTANCE.SendMessageA(hwnd, TOOL_SCREEN_GET_VERSION, new WinDef.WPARAM(0), new WinDef.LPARAM(0)).intValue();
        int major = (i >> 16) & 0xFF;
        int minor = (i >> 8) & 0xFF;
        int patch = i & 0xFF;
        return major + "." + minor + "." + patch;
    }

    public static void sendKeyDownNoRebind(WinDef.HWND hwnd, int virtualKey, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyDownToHwnd(hwnd, virtualKey);
            return;
        }
        User32.INSTANCE.PostMessageA(hwnd, WM_TOOLSCREEN_KEYDOWN_NO_REBIND, new WinDef.WPARAM(virtualKey), KeyboardUtil.createLParamKeyDown(virtualKey));
    }

    public static void sendKeyDownNoRebind(WinDef.HWND hwnd, Win32VK virtualKey, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyDownToHwnd(hwnd, virtualKey);
            return;
        }
        sendKeyDownNoRebind(hwnd, virtualKey.code, true);
    }

    public static void sendKeyUpNoRebind(WinDef.HWND hwnd, int virtualKey, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyUpToHwnd(hwnd, virtualKey);
            return;
        }
        User32.INSTANCE.PostMessageA(hwnd, WM_TOOLSCREEN_KEYUP_NO_REBIND, new WinDef.WPARAM(virtualKey), KeyboardUtil.createLParamKeyUp(virtualKey));
    }

    public static void sendKeyUpNoRebind(WinDef.HWND hwnd, Win32VK virtualKey, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyUpToHwnd(hwnd, virtualKey);
            return;
        }
        sendKeyUpNoRebind(hwnd, virtualKey.code, true);
    }

    public static void sendKeyNoRebind(WinDef.HWND hwnd, int virtualKey, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyToHwnd(hwnd, virtualKey);
            return;
        }
        sendKeyDownNoRebind(hwnd, virtualKey, true);
        sendKeyUpNoRebind(hwnd, virtualKey, true);
    }

    public static void sendKeyNoRebind(WinDef.HWND hwnd, Win32VK virtualKey, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyToHwnd(hwnd, virtualKey);
            return;
        }
        sendKeyNoRebind(hwnd, virtualKey.code, true);
    }

    public static void sendCharNoRebind(WinDef.HWND hwnd, int character, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendCharToHwnd(hwnd, character);
            return;
        }
        User32.INSTANCE.PostMessageA(hwnd, WM_TOOLSCREEN_CHAR_NO_REBIND, new WinDef.WPARAM(character), new WinDef.LPARAM(0));
    }

    public static void sendKeyNoRebind(WinDef.HWND hwnd, int virtualKey, long pressTime, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyToHwnd(hwnd, virtualKey, pressTime);
            return;
        }
        sendKeyDownNoRebind(hwnd, virtualKey, true);
        if (pressTime > 0) {
            SleepUtil.sleep(pressTime);
        }
        sendKeyUpNoRebind(hwnd, virtualKey, true);
    }

    public static void sendKeyNoRebind(WinDef.HWND hwnd, Win32VK virtualKey, long pressTime, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.sendKeyToHwnd(hwnd, virtualKey, pressTime);
            return;
        }
        sendKeyNoRebind(hwnd, virtualKey.code, pressTime, true);
    }

    public static void releaseAllModifiersNoRebind(WinDef.HWND hwnd, boolean toolscreenInstalled) {
        if (!toolscreenInstalled) {
            KeyboardUtil.releaseAllModifiersForHwnd(hwnd);
            return;
        }
        for (int key : KeyboardUtil.ALL_MODIFIERS) {
            sendKeyUpNoRebind(hwnd, key, true);
        }
    }
}
