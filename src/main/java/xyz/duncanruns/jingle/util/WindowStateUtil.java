package xyz.duncanruns.jingle.util;

import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import xyz.duncanruns.jingle.win32.User32;

import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public final class WindowStateUtil {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Pattern OBS_EXECUTABLE_PATTERN = Pattern.compile("^.+(\\/|\\\\)obs\\d\\d.exe$");
    private static final int BORDERLESS_STYLE = ~(User32.WS_BORDER
            | User32.WS_DLGFRAME
            | User32.WS_THICKFRAME
            | User32.WS_MINIMIZEBOX
            | User32.WS_MAXIMIZEBOX
            | User32.WS_SYSMENU);

    private static final int RESIZEABLE_BORDERLESS_STYLE = ~(User32.WS_BORDER | User32.WS_MINIMIZEBOX);

    private WindowStateUtil() {
    }

    /**
     * @return true if the hwnd points to a window from an OBS executable and the window does not have WS_BORDER, otherwise false
     */
    public static boolean isOBSProjector(HWND hwnd) {
        return (!WindowStateUtil.hwndHasBorderBasic(hwnd)) && OBS_EXECUTABLE_PATTERN.matcher(PidUtil.getProcessExecutable(PidUtil.getPidFromHwnd(hwnd))).matches();
    }

    public static boolean isHwndBorderless(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        int styleWithBorderless = style & BORDERLESS_STYLE;
        return styleWithBorderless == style;
    }

    public static boolean isHwndResizeableBorderless(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        int styleWithBorderless = style & RESIZEABLE_BORDERLESS_STYLE;
        return styleWithBorderless == style;
    }

    /**
     * Only checks the WS_BORDER value
     */
    public static boolean hwndHasBorderBasic(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        return (style & User32.WS_BORDER) != 0;
    }

    public static void setHwndBorderless(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        style &= BORDERLESS_STYLE;
        setHwndStyle(hwnd, style);
    }

    public static void setHwndResizeableBorderless(HWND hwnd) {
        int style = getHwndStyle(hwnd);
        style &= RESIZEABLE_BORDERLESS_STYLE;
        setHwndStyle(hwnd, style);
    }

    public static int getHwndStyle(HWND hwnd) {
        return (int) User32.INSTANCE.GetWindowLongA(hwnd, User32.GWL_STYLE);
    }

    public static void setHwndStyle(HWND hwnd, int style) {
        User32.INSTANCE.SetWindowLongA(hwnd, User32.GWL_STYLE, style);
    }

    public static void undoHwndBorderless(HWND hwnd) {
        setHwndStyle(hwnd, 382664704);
    }

    public static void maximizeHwnd(HWND hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWMAXIMIZED);
    }

    public static boolean isHwndMaximized(HWND hwnd) {
        return User32.INSTANCE.IsZoomed(hwnd);
    }

    public static void restoreHwnd(HWND hwnd) {
        User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWNOACTIVATE);
    }

    public static Rectangle getHwndRectangle(HWND hwnd) {
        RECT rect = new RECT();
        User32.INSTANCE.GetWindowRect(hwnd, rect);
        return new Rectangle(rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
    }

    public static void queueSetHwndRectangle(HWND hwnd, Rectangle rectangle) {
        EXECUTOR.execute(() -> setHwndRectangle(hwnd, rectangle));
    }

    public static void setHwndRectangle(HWND hwnd, Rectangle rectangle) {
        User32.INSTANCE.MoveWindow(hwnd, rectangle.x, rectangle.y, rectangle.width, rectangle.height, true);
    }

    public static Rectangle withTopLeftToCenter(Rectangle bounds) {
        return new Rectangle(bounds.x - bounds.width / 2, bounds.y - bounds.height / 2, bounds.width, bounds.height);
    }

    public static void ensureNotMinimized(HWND hwnd) {
        // https://stackoverflow.com/questions/29837268/how-can-i-restore-a-winapi-window-if-its-minimized
        if (User32.INSTANCE.IsIconic(hwnd)) {
            WinUser.WINDOWPLACEMENT windowplacement = new WinUser.WINDOWPLACEMENT();
            User32.INSTANCE.GetWindowPlacement(hwnd, windowplacement);
            switch (windowplacement.showCmd) {
                case User32.SW_SHOWMAXIMIZED:
                    User32.INSTANCE.ShowWindow(hwnd, User32.SW_SHOWMAXIMIZED);
                    break;
                case User32.SW_SHOWMINIMIZED:
                    User32.INSTANCE.ShowWindow(hwnd, User32.SW_RESTORE);
                    break;
                default:
                    User32.INSTANCE.ShowWindow(hwnd, User32.SW_NORMAL);
                    break;
            }
        }
    }
}
