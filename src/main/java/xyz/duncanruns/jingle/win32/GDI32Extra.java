package xyz.duncanruns.jingle.win32;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.win32.W32APIOptions;

/**
 * JNA interface with Window's gdi32.dll
 * <p>
 * <a href="https://github.com/Lxnus/ScreenCapture4J/blob/master/screencapture4j/GDI32Extra.java">(Source)</a>
 *
 * @Author Lxnus & DuncanRuns
 */
public interface GDI32Extra extends GDI32 {

    GDI32Extra INSTANCE = Native.load("gdi32", GDI32Extra.class, W32APIOptions.DEFAULT_OPTIONS);

    boolean BitBlt(HDC hObject, int nXDest, int nYDest, int nWidth, int nHeight, HDC hObjectSource, int nXSrc, int nYSrc, DWORD dwRop);

    boolean StretchBlt(HDC hdcDest, int xDest, int yDest, int wDest, int hDest, HDC hdcSrc, int xSrc, int ySrc, int wSrc, int hSrc, DWORD rop);

    int SetStretchBltMode(HDC hdc, int mode);

    Pointer GetStockObject(int i);

    int SetPixel(HDC hdc, int x, int y, int color);

    boolean GetColorAdjustment(HDC hdc, COLORADJUSTMENT lpca);

    boolean SetColorAdjustment(HDC hdc, COLORADJUSTMENT lpca);

    @Structure.FieldOrder({"caSize", "caFlags", "caIlluminantIndex", "caRedGamma", "caGreenGamma", "caBlueGamma", "caReferenceBlack", "caReferenceWhite", "caContrast", "caBrightness", "caColorfulness", "caRedGreenTint"})
    class COLORADJUSTMENT extends Structure {
        public WinDef.WORD caSize;
        public WinDef.WORD caFlags;
        public WinDef.WORD caIlluminantIndex;
        public WinDef.WORD caRedGamma;
        public WinDef.WORD caGreenGamma;
        public WinDef.WORD caBlueGamma;
        public WinDef.WORD caReferenceBlack;
        public WinDef.WORD caReferenceWhite;
        public WinDef.SHORT caContrast;
        public WinDef.SHORT caBrightness;
        public WinDef.SHORT caColorfulness;
        public WinDef.SHORT caRedGreenTint;
    }
}