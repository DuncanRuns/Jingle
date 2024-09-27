package xyz.duncanruns.jingle.obs;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.MonitorUtil;
import xyz.duncanruns.jingle.util.PidUtil;
import xyz.duncanruns.jingle.util.WindowStateUtil;
import xyz.duncanruns.jingle.util.WindowTitleUtil;
import xyz.duncanruns.jingle.win32.User32;
import xyz.duncanruns.jingle.win32.Win32Con;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.Objects;
import java.util.regex.Pattern;

public final class OBSProjector {
    private static final Pattern OBS_EXECUTABLE_PATTERN = Pattern.compile("^.+(\\/|\\\\)obs\\d\\d.exe$");

    @Nullable
    private static WinDef.HWND projectorHwnd = null;

    private static long lastCheck = 0;
    private static long requestProjector = -1L;
    private static int requestSlowerifier = 0;

    private static boolean coverJultiMag = true;

    private OBSProjector() {
    }

    public static boolean shouldCoverJultiMag() {
        return coverJultiMag;
    }

    public static long getRequestProjectorTime() {
        return requestProjector;
    }

    public static synchronized Rectangle getProjectorPosition() {
        synchronized (Jingle.class) {
            assert Jingle.options != null;
            int[] pp = Jingle.options.projectorPosition;
            if (pp != null) {
                return new Rectangle(pp[0], pp[1], pp[2], pp[3]);
            } else {
                MonitorUtil.Monitor monitor = MonitorUtil.getPrimaryMonitor();
                int w = (monitor.pWidth - 384) / 2;
                int h = ((monitor.pHeight * w) / monitor.pWidth);
                return new Rectangle(0, (monitor.pHeight - h) / 2, w, h);
            }
        }
    }

    public static synchronized void setProjectorPosition(int x, int y, int w, int h) {
        synchronized (Jingle.class) {
            assert Jingle.options != null;
            Jingle.options.projectorPosition = new int[]{x, y, w, h};
        }
        if (projectorHwnd != null) {
            applyProjectorPosition();
        }
    }

    public static synchronized void tick() {
        long currentTime = System.currentTimeMillis();
        boolean projectorEnabled = Jingle.options.projectorEnabled;
        if (!projectorEnabled) {
            requestProjector = -1L;
            requestSlowerifier = 0;
            projectorHwnd = null;
            return;
        }
        if (Math.abs(currentTime - lastCheck) > 500) {
            if (projectorHwnd != null && !User32.INSTANCE.IsWindow(projectorHwnd)) {
                projectorHwnd = null;
            }

            if (projectorHwnd == null) {
                lastCheck = currentTime;
                User32.INSTANCE.EnumWindows((hWnd, data) -> {
                    if (isProjectorMagnifier(hWnd)) {
                        projectorHwnd = hWnd;
                        onProjectorFound();
                        return false;
                    }
                    return true;
                }, null);
                // Close extra projectors
                if (projectorHwnd != null) {
                    closeExtraProjectors();
                }
            }
            if (projectorHwnd == null) {
                if (isPowerOfTwo(requestSlowerifier++)) {
                    requestProjector = currentTime;
                }
                if (requestSlowerifier == 33) requestSlowerifier -= 16;
            }
        }
    }

    private static void onProjectorFound() {
        applyProjectorPosition();
        setProjectorZOrder(1);
        requestProjector = -1L;
        requestSlowerifier = 0;
        if (Jingle.options.minimizeProjector) minimizeProjector();
    }

    private static boolean isPowerOfTwo(int x) {
        return (x & (x - 1)) == 0;
    }

    public static synchronized void applyProjectorPosition() {
        WindowStateUtil.setHwndBorderless(projectorHwnd);
        WindowStateUtil.setHwndRectangle(projectorHwnd, getProjectorPosition());
    }

    public static synchronized void bringOBSProjectorToTop() {
        if (projectorHwnd != null) {
            unminimizeProjector();
            coverJultiMag = false;
            setProjectorZOrder(0);
            setProjectorZOrder(-1);
        }
    }

    public static void unminimizeProjector() {
        WindowStateUtil.ensureNotMinimized(projectorHwnd);
    }

    private static void setProjectorZOrder(int hwndInsertAfter) {
        User32.INSTANCE.SetWindowPos(projectorHwnd,
                new WinDef.HWND(new Pointer(hwndInsertAfter)),
                0, 0, 0, 0, // Does not matter due to flags
                User32.SWP_NOSIZE | User32.SWP_NOMOVE | User32.SWP_NOACTIVATE
        );
    }

    public static synchronized void dumpOBSProjector() {
        if (projectorHwnd != null) {
            if (Jingle.options.minimizeProjector) {
                minimizeProjector();
            }
            setProjectorZOrder(1);
        }
        coverJultiMag = true;
    }

    public static void minimizeProjector() {
        if (projectorHwnd != null) User32.INSTANCE.ShowWindow(projectorHwnd, User32.SW_MINIMIZE);
    }

    private static boolean isProjectorMagnifier(WinDef.HWND hwnd) {
        return isProjectorTitle(WindowTitleUtil.getHwndTitle(hwnd)) && OBS_EXECUTABLE_PATTERN.matcher(PidUtil.getProcessExecutable(PidUtil.getPidFromHwnd(hwnd))).matches();
    }

    private static boolean isProjectorTitle(String title) {
        String regex = '^' + Jingle.options.projectorWindowPattern.trim().toLowerCase().replaceAll("([^a-zA-Z0-9 ])", "\\\\$1").replace("\\*", ".*") + '$';
        return Pattern.compile(regex).matcher(title.toLowerCase()).matches();
    }

    public static synchronized void closeAnyMeasuringProjectors() {
        projectorHwnd = null;
        closeExtraProjectors();
    }

    private static synchronized void closeExtraProjectors() {
        User32.INSTANCE.EnumWindows((hWnd, data) -> {
            if ((projectorHwnd != null && Objects.equals(hWnd, projectorHwnd)) || !isProjectorMagnifier(hWnd))
                return true;
            User32.INSTANCE.SendNotifyMessageA(hWnd, new WinDef.UINT(User32.WM_SYSCOMMAND), new WinDef.WPARAM(Win32Con.SC_CLOSE), new WinDef.LPARAM(0));
            return true;
        }, null);
    }
}
