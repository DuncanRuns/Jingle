package xyz.duncanruns.jingle.script.lua;

import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.bopping.Bopping;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.instance.InstanceState;
import xyz.duncanruns.jingle.obs.OBSProjector;
import xyz.duncanruns.jingle.resizing.Resizing;
import xyz.duncanruns.jingle.script.CustomizableManager;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.ScriptStuff;
import xyz.duncanruns.jingle.util.*;
import xyz.duncanruns.jingle.win32.User32;
import xyz.duncanruns.jingle.win32.Win32Con;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Optional;


@SuppressWarnings("unused")
class JingleLuaLibrary extends LuaLibrary {
    public JingleLuaLibrary(ScriptFile script, Globals globals) {
        super("jingle", script, globals);
    }

    @NotALuaFunction
    private static Runnable wrapFunction(LuaFunction function) {
        return () -> {
            synchronized (Jingle.class) {
                function.call();
            }
        };
    }

    @LuaDocumentation(description = "Registers a hotkey action. If a user sets up a hotkey with the given hotkey action name and then presses their set hotkey, the given function will be ran.")
    public void addHotkey(String hotkeyName, LuaFunction function) {
        assert this.script != null;
        assert this.globals != null;
        if (hotkeyName.contains(":")) {
            Jingle.log(Level.ERROR, "Can't add hotkey script: script name \"" + hotkeyName + "\" contains a colon!");
        }
        ScriptStuff.addHotkeyAction(this.script, hotkeyName, wrapFunction(function));
    }

    @LuaDocumentation(description = "Registers the customization function for this script. If a user presses the \"Customize\" button for this script, the given function will be ran.")
    public void setCustomization(LuaFunction function) {
        assert this.script != null;
        ScriptStuff.setCustomization(this.script, wrapFunction(function));
    }

    @LuaDocumentation(description = "Registers an extra function for this script. If a user presses the button of the given name found next to this script, the given function will be ran.")
    public void addExtraFunction(String functionName, LuaFunction function) {
        assert this.script != null;
        ScriptStuff.addExtraFunction(this.script, functionName, wrapFunction(function));
    }

    @LuaDocumentation(description = "Runs a resize toggle of the given width and height. Returns true if the size is applied, and returns false if the size is undone.")
    public boolean toggleResize(int width, int height) {
        return Resizing.toggleResize(width, height);
    }

    @LuaDocumentation(description = "Returns true if the instance is active, otherwise false.")
    public boolean isInstanceActive() {
        return Jingle.isInstanceActive();
    }

    @LuaDocumentation(description = "Sets the OBS eye measuring projector to be directly behind the instance, bringing it above everything except for the game itself.")
    public void bringOBSProjectorToTop() {
        OBSProjector.bringOBSProjectorToTop();
    }

    @LuaDocumentation(description = "Dumps the OBS eye measuring projector to the bottom of the window Z order.")
    public void dumpOBSProjector() {
        OBSProjector.dumpOBSProjector();
    }

    @LuaDocumentation(description = "Gets the current Windows cursor speed.")
    public int getCursorSpeed() {
        return MouseUtil.getCurrentCursorSpeed();
    }

    @LuaDocumentation(description = "Sets the current Windows cursor speed.")
    public void setCursorSpeed(int speed) {
        MouseUtil.setCursorSpeed(speed);
    }

    @LuaDocumentation(description = "Registers a function to an event.\nEvents: START_TICK, END_TICK, MAIN_INSTANCE_CHANGED, STATE_CHANGE, EXIT_WORLD, ENTER_WORLD")
    public void listen(String eventName, LuaFunction function) {
        ScriptStuff.RunnableEventType.valueOf(eventName).register(wrapFunction(function));
    }

    @LuaDocumentation(description = "Sets and stores a customizable string.\nValues stored are only accessible to runs of this script and are persistent through Julti restarts.")
    public void setCustomizable(String key, String value) {
        assert this.script != null;
        CustomizableManager.set(this.script.getName(), key, value);
    }

    @LuaDocumentation(description = "Gets a stored customizable string. A default value can optionally be provided in the case that no value is found in the customizables storage.", paramTypes = {"string", "string|nil"})
    @Nullable
    public String getCustomizable(String key, String def) {
        assert this.script != null;
        String out = CustomizableManager.get(this.script.getName(), key);
        return out == null ? def : out;
    }


    @LuaDocumentation(description = "Presents the user with a message and Yes/No/Cancel buttons. Returns true for yes, false for no, and nil for cancel or if the user closes the window.", returnTypes = "boolean|nil")
    @Nullable
    public Boolean askYesNo(String message) {
        assert this.script != null;
        int ans = JOptionPane.showConfirmDialog(JingleGUI.get(), message, "Jingle Script: " + this.script.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        switch (ans) {
            case 0:
                return true;
            case 1:
                return false;
            default:
                return null;
        }
    }

    @LuaDocumentation(description = "Shows a message in a message box to the user.")
    public void showMessageBox(String message) {
        assert this.script != null;
        JOptionPane.showMessageDialog(JingleGUI.get(), message, "Jingle Script: " + this.script.getName(), JOptionPane.PLAIN_MESSAGE, null);
    }

    public String getScriptName() {
        assert this.script != null;
        return this.script.getName();
    }

    // TODO
//    @LuaDocumentation(description = "Sends a chat message in the active instance. A slash needs to be given if executing a command (eg. julti.sendChatMessage(\"/kill\")).")
//    public void sendChatMessage(String message) {
//    }

    @LuaDocumentation(description = "Clears all but the last 5 worlds the instance, or for all instances ever seen if clearFromAllSeenInstances is set to true.")
    public void clearWorlds(boolean clearFromAllSeenInstances) {
        Bopping.bop(clearFromAllSeenInstances);
    }

    @LuaDocumentation(description = "Closes the instance.")
    public void closeInstance() {
        Jingle.getMainInstance().ifPresent(instance -> User32.INSTANCE.SendNotifyMessageA(instance.hwnd, new WinDef.UINT(User32.WM_SYSCOMMAND), new WinDef.WPARAM(Win32Con.SC_CLOSE), new WinDef.LPARAM(0)));
    }


    @LuaDocumentation(description = "Replicates a hotkey action exactly. For example, jingle.replicateHotkey('script','test.lua:Test Hotkey')")
    public boolean replicateHotkey(String type, String action) {
        Optional<Runnable> hotkeyAction = HotkeyManager.findHotkeyAction(type, action);
        hotkeyAction.ifPresent(Runnable::run);
        return hotkeyAction.isPresent();
    }

    @LuaDocumentation(description = "Opens the specified file as if it were double-clicked in file explorer.")
    public void openFile(String filePath) {
        OpenUtil.openFile(filePath);
    }

    @LuaDocumentation(description = "Opens the currently active instance's world to lan.", paramTypes = "boolean|nil")
    public void openInstanceToLan(Boolean enableCheats) {
    }

    @LuaDocumentation(description = "Sleeps for the specified amount of milliseconds.")
    public void sleep(long millis) {
        SleepUtil.sleep(millis);
    }

    @LuaDocumentation(description = "Gets the current state of the instance. Returns \"WAITING\", \"INWORLD\", \"TITLE\", \"GENERATING\", \"WALL\", or \"PREVIEWING\".")
    @Nullable
    public String getInstanceState() {
        return Jingle.getMainInstance().map(i -> i.stateTracker.getInstanceState().name()).orElse(null);
    }

    @LuaDocumentation(description = "Gets a more detailed state of the \"INWORLD\" state. Returns \"UNPAUSED\", \"PAUSED\", or \"GAMESCREENOPEN\".")
    public String getInstanceInWorldState(int instanceNum) {
        return Jingle.getMainInstance().map(i -> i.stateTracker.getInWorldState().name()).orElse(null);
    }

    @LuaDocumentation(description = "Gets the last time the specified state started. Input values are equal to return values given by jingle.getInstanceInWorldState().")
    public Long getLastStateStartOf(String stateName) {
        return Jingle.getMainInstance().map(i -> i.stateTracker.getLastStartOf(InstanceState.valueOf(stateName))).orElse(null);
    }

    @LuaDocumentation(description = "Gets the last time the specified state ended. Input values are equal to return values given by jingle.getInstanceInWorldState().")
    public Long getLastStateOccurrenceOf(String stateName) {
        return Jingle.getMainInstance().map(i -> i.stateTracker.getLastOccurrenceOf(InstanceState.valueOf(stateName))).orElse(null);
    }

    @LuaDocumentation(description = "Gets the current time in milliseconds.")
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    @LuaDocumentation(description = "Gets the position of the mouse.", returnTypes = {"number", "number"})
    public Varargs getMousePosition() {
        Point mousePos = MouseUtil.getMousePos();
        return varargsOf(new LuaValue[]{valueOf(mousePos.x), valueOf(mousePos.y)});
    }

    @LuaDocumentation(description = "Checks if a script of the specified name exists.")
    public boolean scriptExists(String scriptName) {
        return ScriptStuff.getLoadedScripts().stream().anyMatch(sf -> sf.name.equals(scriptName));
    }

    // TODO
//    @LuaDocumentation(description = "Gets a table of all modids for the instance.")
//    public LuaTable getFabricMods() {
//        LuaTable table = tableOf();
//        int i = 0;
//        for (FabricJarUtil.FabricJarInfo jar : getFabricJarInfos(instanceNum)) {
//            table.set(valueOf(++i), valueOf(jar.id));
//        }
//        return table;
//    }

    // TODO
//    @LuaDocumentation(description = "Gets the version of a fabric mod installed on the instance.")
//    @Nullable
//    public String getFabricModVersion(String modid) {
//        return getFabricJarInfos(instanceNum).stream().filter(i -> i.id.equalsIgnoreCase(modid)).findFirst().map(i -> i.version).orElse(null);
//    }

    // TODO
//    @LuaDocumentation(description = "Checks if the instance has a mod of the specified modid.")
//    public boolean hasFabricMod(int instanceNum, String modid) {
//        return getFabricJarInfos(instanceNum).stream().anyMatch(i -> i.id.equalsIgnoreCase(modid));
//    }

    @LuaDocumentation(description = "Compares two version strings. Examples:\njinglecompareVersionStrings(\"1.0.0\", \"1.0.1\") -> -1\njinglecompareVersionStrings(\"1.1.0\", \"1.0.1\") -> 1\njinglecompareVersionStrings(\"1.1.0\", \"1.1.0\") -> 0\njinglecompareVersionStrings(\"mario\", \"1.1.0\", 100) -> 100")
    public int compareVersionStrings(String a, String b, Integer onFailure) {
        return VersionUtil.tryCompare(a, b, onFailure);
    }

    @LuaDocumentation(description = "Retrieves a value from the instance's options.txt.")
    public String getInstanceOption(String optionName) {
        return Jingle.getMainInstance().flatMap(i -> i.optionsTxt.getOption(optionName)).orElse(null);
    }

    @LuaDocumentation(description = "Retrieves a value from the instance's standard options.")
    public String getInstanceStandardOption(int instanceNum, String optionName) {
        return Jingle.getMainInstance().flatMap(i -> i.standardSettings.getOption(optionName)).orElse(null);
    }

    // TODO
//    @LuaDocumentation(description = "Retrieves a minecraft key option from the instance's standard options or options.txt and converts it into a Windows key integer.")
//    public Integer getInstanceKeyOption(int instanceNum, String keyOptionName) {
//        MinecraftInstance instance = getInstanceFromInt(instanceNum);
//        return GameOptionsUtil.getKey(instance.getPath(), keyOptionName, MCVersionUtil.isOlderThan(instance.getVersionString(), "1.13"));
//    }

    @LuaDocumentation(description = "Sends a key down and up message to the instance with no delay between.")
    public void sendKeyToInstance(int key) {
        Jingle.getMainInstance().ifPresent(instance -> KeyboardUtil.sendKeyToHwnd(instance.hwnd, key));
    }

    @LuaDocumentation(description = "Sends a key down message to the instance.")
    public void sendKeyDownToInstance(int key) {
        Jingle.getMainInstance().ifPresent(instance -> KeyboardUtil.sendKeyDownToHwnd(instance.hwnd, key));
    }

    @LuaDocumentation(description = "Sends a key up message to the instance.")
    public void sendKeyUpToInstance(int key) {
        Jingle.getMainInstance().ifPresent(instance -> KeyboardUtil.sendKeyUpToHwnd(instance.hwnd, key));
    }

    @LuaDocumentation(description = "Sends a key down and up message to the instance with a specified delay between.")
    public void sendKeyHoldToInstance(int instanceNum, int key, long millis) {
        Jingle.getMainInstance().ifPresent(instance -> KeyboardUtil.sendKeyToHwnd(instance.hwnd, key, millis));
    }


    @LuaDocumentation(description = "Logs a message to the Jingle log.")
    public void log(String message) {
        assert this.script != null;
        Jingle.log(Level.INFO, String.format("(%s) %s", this.script.getName(), message));
    }
}
