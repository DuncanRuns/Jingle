package xyz.duncanruns.jingle.instance;

import com.sun.jna.platform.win32.WinDef.HWND;

public class OpenedInstanceInfo extends InstanceInfo {
    public final HWND hwnd;

    public OpenedInstanceInfo(InstanceInfo instanceInfo, HWND hwnd) {
        super(instanceInfo.versionString, instanceInfo.instancePath);
        this.hwnd = hwnd;
    }

    @Override
    public String toString() {
        return "OpenedInstanceInfo{" +
                "hwnd=" + this.hwnd +
                ", versionString='" + this.versionString + '\'' +
                ", instancePath=" + this.instancePath +
                '}';
    }
}
