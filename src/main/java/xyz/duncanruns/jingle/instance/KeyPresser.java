package xyz.duncanruns.jingle.instance;

import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef.HWND;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ToolscreenUtil;

@SuppressWarnings("unused")
public class KeyPresser {
    private final HWND hwnd;
    private long lastToolscreenCheck = 0;
    private boolean toolscreenInstalled = false;

    private void checkToolscreen() {
        if (toolscreenInstalled || System.currentTimeMillis() - lastToolscreenCheck < 5000) return;
        lastToolscreenCheck = System.currentTimeMillis();
        toolscreenInstalled = ToolscreenUtil.hasToolscreen(this.hwnd);
        if (toolscreenInstalled) {
            Jingle.log(Level.INFO, "Found toolscreen version " + ToolscreenUtil.getToolscreenVersion(this.hwnd));
        }
    }

    public KeyPresser(HWND hwnd) {
        this.hwnd = hwnd;
    }

    public void releaseAllModifiers() {
        checkToolscreen();
        ToolscreenUtil.releaseAllModifiersNoRebind(this.hwnd, toolscreenInstalled);
    }

    public void pressF3Esc() {
        checkToolscreen();
        ToolscreenUtil.sendKeyDownNoRebind(this.hwnd, Win32VK.VK_F3, toolscreenInstalled);
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_ESCAPE, toolscreenInstalled);
        ToolscreenUtil.sendKeyUpNoRebind(this.hwnd, Win32VK.VK_F3, toolscreenInstalled);
    }

    public void pressF1() {
        checkToolscreen();
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_F1, toolscreenInstalled);
    }

    public void pressEsc() {
        checkToolscreen();
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_ESCAPE, toolscreenInstalled);
    }

    public void pressKey(Integer virtualKey) {
        if (virtualKey != null) {
            checkToolscreen();
            ToolscreenUtil.sendKeyNoRebind(this.hwnd, virtualKey, toolscreenInstalled);
        }
    }

    public void pressShiftTabEnter() {
        checkToolscreen();
        ToolscreenUtil.sendKeyDownNoRebind(this.hwnd, Win32VK.VK_LSHIFT, toolscreenInstalled);
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_TAB, toolscreenInstalled);
        ToolscreenUtil.sendKeyUpNoRebind(this.hwnd, Win32VK.VK_LSHIFT, toolscreenInstalled);
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_RETURN, toolscreenInstalled);
    }

    public void pressShiftF3() {
        checkToolscreen();
        ToolscreenUtil.sendKeyDownNoRebind(this.hwnd, Win32VK.VK_LSHIFT, toolscreenInstalled);
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_F3, toolscreenInstalled);
        ToolscreenUtil.sendKeyUpNoRebind(this.hwnd, Win32VK.VK_LSHIFT, toolscreenInstalled);
    }

    private void pressF3() {
        checkToolscreen();
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_F3, toolscreenInstalled);
    }

    public void pressTab(int times) {
        checkToolscreen();
        for (int i = 0; i < times; i++) {
            ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_TAB, toolscreenInstalled);
        }
    }

    public void pressTab() {
        checkToolscreen();
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_TAB, toolscreenInstalled);
    }

    public void pressEnter() {
        checkToolscreen();
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_RETURN, toolscreenInstalled);
    }

    public void pressShiftTab(int times) {
        checkToolscreen();
        ToolscreenUtil.sendKeyDownNoRebind(this.hwnd, Win32VK.VK_LSHIFT, toolscreenInstalled);
        for (int i = 0; i < times; i++) {
            ToolscreenUtil.sendKeyNoRebind(this.hwnd, Win32VK.VK_TAB, toolscreenInstalled);
        }
        ToolscreenUtil.sendKeyUpNoRebind(this.hwnd, Win32VK.VK_LSHIFT, toolscreenInstalled);
    }

    public void type(String text) {
        checkToolscreen();
        for (char c : text.toCharArray()) {
            ToolscreenUtil.sendCharNoRebind(this.hwnd, c, toolscreenInstalled);
        }
    }

    public void sendChar(int character) {
        checkToolscreen();
        ToolscreenUtil.sendCharNoRebind(this.hwnd, character, toolscreenInstalled);
    }

    public void pressKeyDown(Integer pressedModifier) {
        if (pressedModifier != null) {
            checkToolscreen();
            ToolscreenUtil.sendKeyDownNoRebind(this.hwnd, pressedModifier, toolscreenInstalled);
        }
    }

    public void pressKeyUp(int key) {
        checkToolscreen();
        ToolscreenUtil.sendKeyUpNoRebind(this.hwnd, key, toolscreenInstalled);
    }

    public void holdKey(int key, long duration) {
        checkToolscreen();
        ToolscreenUtil.sendKeyNoRebind(this.hwnd, key, duration, toolscreenInstalled);
    }
}
