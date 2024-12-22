package xyz.duncanruns.jingle.win32;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WTypes.LPSTR;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA interface with Window's user32.dll.
 * This interface is an extension of the User32 interface packaged with JNA.
 *
 * @author Pete S, DuncanRuns, & Lxnus
 */
public interface User32 extends com.sun.jna.platform.win32.User32 {
    User32 INSTANCE = Native.load("user32", User32.class);
    int MOUSEEVENTF_ABSOLUTE = 0x8000;
    int MOUSEEVENTF_LEFTDOWN = 0x0002;
    int MOUSEEVENTF_LEFTUP = 0x0004;
    int MOUSEEVENTF_MIDDLEDOWN = 0x0020;
    int MOUSEEVENTF_MIDDLEUP = 0x0040;
    int MOUSEEVENTF_MOVE = 0x0001;
    int MOUSEEVENTF_RIGHTDOWN = 0x0008;
    int MOUSEEVENTF_RIGHTUP = 0x0010;
    int MOUSEEVENTF_WHEEL = 0x0800;
    int MOUSEEVENTF_XDOWN = 0x0080;
    int MOUSEEVENTF_XUP = 0x0100;
    int MOUSEEVENTF_HWHEEL = 0x01000;

    int GetWindowTextA(HWND hWnd, byte[] lpString, int nMaxCount);

    long GetWindowLongA(HWND hWnd, int nIndex);

    LONG SetWindowLongA(HWND hWnd, int nIndex, long dwNewLong);

    boolean PostMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    BOOL SendNotifyMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    BOOL SendMessageA(HWND hWnd, UINT Msg, WPARAM wParam, LPARAM lParam);

    boolean SetWindowTextA(HWND hWnd, String lpString);

    boolean IsIconic(HWND hWnd);

    UINT MapVirtualKeyA(UINT uCode, UINT uMapType);

    HDC GetWindowDC(HWND hWnd);

    int FillRect(HDC hdc, RECT rect, HBRUSH hbrush);

    boolean IsZoomed(HWND hWnd);

    BOOL GetCursorInfo(CURSORINFO pci);

    boolean GetPhysicalCursorPos(POINT p);

    void SystemParametersInfoA(int uiAction, int uiParam, IntByReference pvParam, int fWinIni);

    boolean SystemParametersInfoA(int uiAction, int uiParam, int pvParam, int fWinIni);

    int GetKeyNameTextA(LONG lParam, LPSTR lpString, int cchSize);

    @Structure.FieldOrder({"cbSize", "flags", "hCursor", "ptScreenPos"})
    class CURSORINFO extends Structure {
        public DWORD cbSize;
        public DWORD flags;
        public HCURSOR hCursor;
        public POINT ptScreenPos;
    }
}