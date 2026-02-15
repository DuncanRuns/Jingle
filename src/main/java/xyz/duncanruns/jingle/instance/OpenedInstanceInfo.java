package xyz.duncanruns.jingle.instance;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenedInstanceInfo extends InstanceInfo {
    public final int pid;
    public final boolean hasHermesCore;
    public final JsonObject hermesInfo;
    public final InstanceMods mods;

    public OpenedInstanceInfo(InstanceInfo instanceInfo, int pid) {
        super(instanceInfo.versionString, instanceInfo.instancePath);
        this.hasHermesCore = false;
        this.hermesInfo = null;
        this.mods = new InstanceMods(this.instancePath.resolve("mods"), null);
        this.pid = pid;
    }

    public OpenedInstanceInfo(InstanceInfo instanceInfo, boolean hasHermesCore, JsonObject hermesInfo, InstanceMods mods, int pid) {
        super(instanceInfo.versionString, instanceInfo.instancePath);
        this.hasHermesCore = hasHermesCore;
        this.hermesInfo = hermesInfo;
        this.mods = mods;
        this.pid = pid;
    }

    public OpenedInstanceInfo(String gameVersion, Path gameDir, boolean hasHermesCore, JsonObject jsonObject, int pid) {
        super(gameVersion, gameDir);
        this.hasHermesCore = hasHermesCore;
        this.hermesInfo = jsonObject;
        this.mods = new InstanceMods(this.instancePath.resolve("mods"), hermesInfo);
        this.pid = pid;
    }

    @Override
    public String toString() {
        return "OpenedInstanceInfo{" +
                ", versionString='" + this.versionString + '\'' +
                ", instancePath=" + this.instancePath +
                '}';
    }

    public static OpenedInstanceInfo getInstanceInfoFromHermes(JsonObject jsonObject, int pid) {
        return new OpenedInstanceInfo(
                jsonObject.get("game_version").getAsString(),
                Paths.get(jsonObject.get("game_dir").getAsString()),
                true,
                jsonObject,
                pid
        );
    }
}
