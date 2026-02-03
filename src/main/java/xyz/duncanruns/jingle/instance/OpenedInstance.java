package xyz.duncanruns.jingle.instance;

public class OpenedInstance extends OpenedInstanceInfo {
    public final KeyPresser keyPresser;
    public final OptionsTxt optionsTxt;
    public final StandardSettings standardSettings;

    public OpenedInstance(OpenedInstanceInfo openedInstanceInfo) {
        super(openedInstanceInfo, openedInstanceInfo.hwnd, openedInstanceInfo.hasHermesCore, openedInstanceInfo.hermesInfo, openedInstanceInfo.mods, openedInstanceInfo.pid);
        this.keyPresser = new KeyPresser(this.hwnd);
        this.optionsTxt = new OptionsTxt(this.instancePath.resolve("options.txt"), this.versionString);
        this.standardSettings = new StandardSettings(this.instancePath);
    }
}
