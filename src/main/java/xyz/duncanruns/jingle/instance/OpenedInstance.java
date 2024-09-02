package xyz.duncanruns.jingle.instance;

import java.util.function.BiConsumer;

public class OpenedInstance extends OpenedInstanceInfo {
    public final StateTracker stateTracker;
    public final KeyPresser keyPresser;
    public final OptionsTxt optionsTxt;
    public final StandardSettings standardSettings;

    public OpenedInstance(OpenedInstanceInfo openedInstanceInfo, BiConsumer<InstanceState, InstanceState> onStateChange) {
        super(openedInstanceInfo, openedInstanceInfo.hwnd);
        this.stateTracker = new StateTracker(this.instancePath.resolve("wpstateout.txt"), onStateChange);
        this.keyPresser = new KeyPresser(this.hwnd);
        this.optionsTxt = new OptionsTxt(this.instancePath.resolve("options.txt"));
        this.standardSettings = new StandardSettings(this.instancePath);
    }
}
