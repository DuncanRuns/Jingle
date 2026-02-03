package xyz.duncanruns.jingle.instance;

import java.util.function.BiConsumer;

public class OpenedInstance extends OpenedInstanceInfo {
    public final KeyPresser keyPresser;
    public final OptionsTxt optionsTxt;
    public final StandardSettings standardSettings;
    public final LegacyStateTracker legacyStateTracker;

    public OpenedInstance(OpenedInstanceInfo openedInstanceInfo, BiConsumer<LegacyInstanceState, LegacyInstanceState> onStateChange) {
        super(openedInstanceInfo, openedInstanceInfo.hwnd, openedInstanceInfo.hasHermesCore, openedInstanceInfo.hermesInfo, openedInstanceInfo.mods, openedInstanceInfo.pid);
        this.keyPresser = new KeyPresser(this.hwnd);
        this.optionsTxt = new OptionsTxt(this.instancePath.resolve("options.txt"), this.versionString);
        this.standardSettings = new StandardSettings(this.instancePath);
        this.legacyStateTracker = getLegacyStateTracker(onStateChange);
    }

    private LegacyStateTracker getLegacyStateTracker(BiConsumer<LegacyInstanceState, LegacyInstanceState> onStateChange) {
        if (!mods.has("hermes") || mods.has("state-output")) {
            return new LegacyStateTrackerImpl(this.instancePath.resolve("wpstateout.txt"), onStateChange);
        } else {
            return new HermesFakeStateTracker(onStateChange, this.pid, mods.has("worldpreview"));
        }
    }
}
