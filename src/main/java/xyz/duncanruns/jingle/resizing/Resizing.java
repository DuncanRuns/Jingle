package xyz.duncanruns.jingle.resizing;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;
import xyz.duncanruns.jingle.util.WindowStateUtil;
import xyz.duncanruns.jingle.win32.User32;

import java.awt.*;

// Thanks to Priffin and Draconix for their work on eye zoom and resize stuff
public final class Resizing {
    private static boolean currentlyResized = false;
    private static int previousWidth = 0;
    private static int previousHeight = 0;
    private static long previousStyle = 0;
    private static long previousExStyle = 0;

    private Resizing() {
    }

    /**
     * @return true if the width/height was applied to the instance, false if the width/height is undone
     */
    public static boolean toggleResize(int width, int height) {
        synchronized (Jingle.class) {
            return toggleResizeInternal(width, height);
        }
    }

    private static boolean toggleResizeInternal(int width, int height) {
        assert Jingle.getMainInstance().isPresent();
        OpenedInstanceInfo instance = Jingle.getMainInstance().get();
        WinDef.HWND hwnd = instance.hwnd;

        Rectangle previousRectangle = WindowStateUtil.getHwndRectangle(hwnd);

        int centerX = (int) previousRectangle.getCenterX();
        int centerY = (int) previousRectangle.getCenterY();

        boolean resizing = !currentlyResized || previousRectangle.width != width || previousRectangle.height != height;

        if (resizing) {
            // Get and store previous size, style, and exstyle
            if (!currentlyResized) {
                previousWidth = previousRectangle.width;
                previousHeight = previousRectangle.height;
                previousStyle = User32.INSTANCE.GetWindowLongA(hwnd, User32.GWL_STYLE);
                previousExStyle = User32.INSTANCE.GetWindowLongA(hwnd, User32.GWL_EXSTYLE);
            }

            Rectangle newRectangle = WindowStateUtil.withTopLeftToCenter(new Rectangle(centerX, centerY, width, height));

            // Set window position
            WindowStateUtil.setHwndBorderless(hwnd);
            User32.INSTANCE.SetWindowPos(hwnd, new WinDef.HWND(new Pointer(0)), newRectangle.x, newRectangle.y, newRectangle.width, newRectangle.height, 0x0400);
            return (currentlyResized = true);
        } else {
            undoResize();
            return false;
        }

    }

    public static void undoResize() {
        assert Jingle.getMainInstance().isPresent();
        if (!currentlyResized) return;
        OpenedInstanceInfo instance = Jingle.getMainInstance().get();
        WinDef.HWND hwnd = instance.hwnd;
        Rectangle previousRectangle = WindowStateUtil.getHwndRectangle(hwnd);

        int centerX = (int) previousRectangle.getCenterX();
        int centerY = (int) previousRectangle.getCenterY();

        Rectangle newRectangle = WindowStateUtil.withTopLeftToCenter(new Rectangle(centerX, centerY, previousWidth, previousHeight));

        User32.INSTANCE.SetWindowLongA(hwnd, User32.GWL_STYLE, previousStyle);
        User32.INSTANCE.SetWindowLongA(hwnd, User32.GWL_EXSTYLE, previousExStyle);
        User32.INSTANCE.SetWindowPos(hwnd, new WinDef.HWND(new Pointer(0)), newRectangle.x, newRectangle.y, newRectangle.width, newRectangle.height, 0x0400);
        currentlyResized = false;
    }
}
