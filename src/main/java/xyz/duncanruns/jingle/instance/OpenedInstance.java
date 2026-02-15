package xyz.duncanruns.jingle.instance;

import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.PidUtil;

import java.util.Objects;
import java.util.Optional;

public class OpenedInstance extends OpenedInstanceInfo {
    public final OptionsTxt optionsTxt;
    public final StandardSettings standardSettings;

    private WinDef.HWND hwnd = null;
    private KeyPresser keyPresser = null;

    public OpenedInstance(OpenedInstanceInfo openedInstanceInfo) {
        super(openedInstanceInfo, openedInstanceInfo.hasHermesCore, openedInstanceInfo.hermesInfo, openedInstanceInfo.mods, openedInstanceInfo.pid);
        this.optionsTxt = new OptionsTxt(this.instancePath.resolve("options.txt"), this.versionString);
        this.standardSettings = new StandardSettings(this.instancePath);
    }

    public Optional<WinDef.HWND> getHwnd() {
        return Optional.ofNullable(this.hwnd);
    }

    /**
     * Checks if the window is of this instance, if it is, it will set the hwnd if it is not already set.
     * Returns true if the window is of this instance.
     */
    public boolean checkWindow(WinDef.HWND hwnd) {
        if (hwnd == null) return false;
        if (Objects.equals(hwnd, this.hwnd)) return true;
        int pidFromHwnd;
        try {
            pidFromHwnd = PidUtil.getPidFromHwnd(hwnd);
        } catch (Exception e) {
            Jingle.logError("Failed to get pid from hwnd:", e);
            return false;
        }
        if (pidFromHwnd != this.pid) return false;
        if (this.hwnd == null) {
            setHwnd(hwnd);
        }
        return true;
    }

    public void setHwnd(WinDef.HWND hwnd) {
        this.hwnd = hwnd;
        this.keyPresser = new KeyPresser(hwnd);
    }

    public Optional<KeyPresser> getKeyPresser() {
        return Optional.ofNullable(this.keyPresser);
    }
}
