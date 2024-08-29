package xyz.duncanruns.jingle.win32;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

public interface Msimg32 extends StdCallLibrary {
    Msimg32 INSTANCE = Native.load("msimg32", Msimg32.class);

    boolean TransparentBlt(WinDef.HDC hdcDest, int xoriginDest, int yoriginDest, int wDest, int hDest, WinDef.HDC hdcSrc, int xoriginSrc, int yoriginSrc, int wSrc, int hSrc, WinDef.UINT crTransparent);
}
