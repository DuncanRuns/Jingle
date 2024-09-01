package xyz.duncanruns.jingle.instance;

import com.sun.jna.platform.win32.Win32VK;
import com.sun.jna.platform.win32.WinDef.HWND;
import xyz.duncanruns.jingle.util.KeyboardUtil;

public class KeyPresser {
    private final HWND hwnd;

    public KeyPresser(HWND hwnd) {
        this.hwnd = hwnd;
    }

    public void releaseAllModifiers() {
        KeyboardUtil.releaseAllModifiersForHwnd(this.hwnd);
    }

    public void pressF3Esc() {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32VK.VK_F3);
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32VK.VK_ESCAPE);
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32VK.VK_F3);
    }

    public void pressF1() {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32VK.VK_F1);
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32VK.VK_F1);
    }

    public void pressEsc() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32VK.VK_ESCAPE);
    }

    public void pressKey(Integer virtualKey) {
        if (virtualKey != null) {
            KeyboardUtil.sendKeyToHwnd(this.hwnd, virtualKey);
        }
    }

    public void pressShiftTabEnter() {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32VK.VK_LSHIFT);
        this.pressTab();
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32VK.VK_LSHIFT);
        this.pressEnter();
    }

    public void pressShiftF3() {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32VK.VK_LSHIFT);
        this.pressF3();
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32VK.VK_LSHIFT);
    }

    private void pressF3() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32VK.VK_F3);
    }

    public void pressTab(int times) {
        for (int i = 0; i < times; i++) {
            this.pressTab();
        }
    }

    public void pressTab() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32VK.VK_TAB);
    }

    public void pressEnter() {
        KeyboardUtil.sendKeyToHwnd(this.hwnd, Win32VK.VK_RETURN);
    }

    public void pressShiftTab(int times) {
        KeyboardUtil.sendKeyDownToHwnd(this.hwnd, Win32VK.VK_LSHIFT);
        this.pressTab(times);
        KeyboardUtil.sendKeyUpToHwnd(this.hwnd, Win32VK.VK_LSHIFT);
    }
}
