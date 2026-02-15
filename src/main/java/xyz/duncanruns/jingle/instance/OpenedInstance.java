package xyz.duncanruns.jingle.instance;

import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.PidUtil;

import java.util.Objects;
import java.util.Optional;

public class OpenedInstance extends OpenedInstanceInfo {
    public final KeyPresser keyPresser;
    public final OptionsTxt optionsTxt;
    public final StandardSettings standardSettings;

    private WinDef.HWND hwnd = null;

    public OpenedInstance(OpenedInstanceInfo openedInstanceInfo) {
        super(openedInstanceInfo, openedInstanceInfo.hasHermesCore, openedInstanceInfo.hermesInfo, openedInstanceInfo.mods, openedInstanceInfo.pid);
        this.keyPresser = new KeyPresser(this.hwnd);
        this.optionsTxt = new OptionsTxt(this.instancePath.resolve("options.txt"), this.versionString);
        this.standardSettings = new StandardSettings(this.instancePath);
    }

    public Optional<WinDef.HWND> getHwnd() {
        return Optional.ofNullable(this.hwnd);
    }

    public boolean hasWindow(WinDef.HWND hwnd) {
        if (this.hwnd == null) return false;
        if (Objects.equals(hwnd, this.hwnd)) return true;
        int pidFromHwnd;
        try {
            pidFromHwnd = PidUtil.getPidFromHwnd(hwnd);
        } catch (Exception e) {
            Jingle.logError("Failed to get pid from hwnd:", e);
            return false;
        }
        boolean b = pidFromHwnd == this.pid;
        if (b) {
            if (this.hwnd == null) {
                this.hwnd = hwnd;
            }
            return true;
        }
        return false;
    }

    public void setHwnd(WinDef.HWND hwnd) {
        this.hwnd = hwnd;
    }
}
