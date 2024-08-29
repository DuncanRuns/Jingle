package xyz.duncanruns.jingle.util;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Psapi;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import xyz.duncanruns.jingle.win32.User32;

public final class PidUtil {
    private static final byte[] executablePathBuffer = new byte[1024];

    private PidUtil() {
    }

    public static int getPidFromHwnd(WinDef.HWND hwnd) {
        final IntByReference pidPointer = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidPointer);
        return pidPointer.getValue();
    }

    public static int getPidForSelf() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    public static String getProcessExecutable(int processId) {
        //Help from https://stackoverflow.com/questions/15693210/getmodulefilename-for-window-in-focus-jna-windows-os
        WinNT.HANDLE process = Kernel32.INSTANCE.OpenProcess(0x0400 | 0x0010, false, processId);

        StringBuilder out = new StringBuilder();
        synchronized (executablePathBuffer) {
            Psapi.INSTANCE.GetModuleFileNameExA(process, null, executablePathBuffer, 1024);
            for (byte a : executablePathBuffer) {
                if (a == 0) {
                    break;
                }
                out.append((char) a);
            }
        }
        Kernel32.INSTANCE.CloseHandle(process);
        return out.toString();
    }
}
