package xyz.duncanruns.jingle.instance;

import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.WinDef;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.PidUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

public class OpenedInstance extends InstanceInfo {
    public final OptionsTxt optionsTxt;
    public final StandardSettings standardSettings;

    private WinDef.HWND hwnd = null;
    private KeyPresser keyPresser = null;
    public final int pid;
    public InstanceMods mods;

    private boolean hermesCore;
    private JsonObject hermesInfo;

    private OpenedInstance(int pid, InstanceMods mods, InstanceInfo instanceInfo) {
        this(pid, false, null, mods, instanceInfo.versionString, instanceInfo.instancePath);
    }

    private OpenedInstance(int pid, boolean hermesCore, JsonObject hermesInfo, InstanceMods mods, String versionString, Path instancePath) {
        super(versionString, instancePath);
        this.optionsTxt = new OptionsTxt(this.instancePath.resolve("options.txt"), this.versionString);
        this.standardSettings = new StandardSettings(this.instancePath);
        this.pid = pid;
        this.hermesCore = hermesCore;
        this.hermesInfo = hermesInfo;
        this.mods = mods;
    }

    public static OpenedInstance getInstanceFromHermes(JsonObject jsonObject, int pid) {
        Path instancePath = Paths.get(jsonObject.get("game_dir").getAsString());
        String versionString = jsonObject.get("game_version").getAsString();
        InstanceMods mods = new InstanceMods(instancePath.resolve("mods"), jsonObject);
        return new OpenedInstance(pid, true, jsonObject, mods, versionString, instancePath);
    }

    public static OpenedInstance getInstanceFromOther(InstanceInfo instanceInfo, int pid) {
        InstanceMods mods = new InstanceMods(instanceInfo.instancePath.resolve("mods"), null);
        return new OpenedInstance(pid, mods, instanceInfo);
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

    public boolean hasHermesCore() {
        return hermesCore;
    }

    public Optional<JsonObject> getHermesInfo() {
        return Optional.ofNullable(hermesInfo);
    }

    public void updateWithHermes(JsonObject hermesInfo){
        this.hermesCore = true;
        this.hermesInfo = hermesInfo;
        this.mods = new InstanceMods(this.instancePath.resolve("mods"), hermesInfo);
        this.instancePath = Paths.get(hermesInfo.get("game_dir").getAsString());
        this.versionString = hermesInfo.get("game_version").getAsString();
    }
}
