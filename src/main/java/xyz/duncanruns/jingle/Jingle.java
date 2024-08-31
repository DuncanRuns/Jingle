package xyz.duncanruns.jingle;

import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.instance.InstanceChecker;
import xyz.duncanruns.jingle.instance.InstanceState;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;
import xyz.duncanruns.jingle.instance.StateTracker;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.resizing.Resizing;
import xyz.duncanruns.jingle.script.ScriptStuff;
import xyz.duncanruns.jingle.util.*;
import xyz.duncanruns.jingle.win32.User32;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static xyz.duncanruns.jingle.util.SleepUtil.sleep;

public final class Jingle {
    public static final String VERSION = Optional.ofNullable(Jingle.class.getPackage().getImplementationVersion()).orElse("DEV");
    public static final Logger LOGGER = LogManager.getLogger("Jingle");

    private static boolean started = false;
    private static boolean running = false;

    private static long lastInstanceCheck = 0;

    public static JingleOptions options = null;

    @Nullable public static OpenedInstanceInfo mainInstance = null;
    @Nullable public static StateTracker stateTracker = null;
    @Nullable public static WinDef.HWND activeHwnd = null;
    public static final Path FOLDER = Paths.get(System.getProperty("user.home")).resolve(".config").resolve("Jingle").toAbsolutePath();

    private Jingle() {
    }


    public static void log(Level level, String message) {
        new Thread(() -> {
            String messageWithTime = String.format("[%s/%s] %s", new SimpleDateFormat("HH:mm:ss").format(new Date()), level, message);
            JingleGUI.get().logDocumentWithDebug.addLineWithRolling(messageWithTime);
            if (level.equals(Level.DEBUG)) return;
            JingleGUI.get().logDocument.addLineWithRolling(messageWithTime);
        }, "log-append").start();
        LOGGER.log(level, message);
    }

    public static void logError(String failMessage, Throwable t) {
        log(Level.ERROR, failMessage + " " + ExceptionUtil.toDetailedString(t));
    }

    public static void start() {
        assert !started;
        log(Level.INFO, ".config/Jingle/options.json loaded.");

        started = true;
        running = true;

        ScriptStuff.reloadScripts();
        JingleGUI.get().scriptListPanel.reload();
        HotkeyManager.reload();
        HotkeyManager.start();

        generateScript();

        String usedJava = System.getProperty("java.home");
        log(Level.INFO, "You are running Jingle v" + VERSION + " with java: " + usedJava);

        mainLoop();
    }

    private static void generateScript() {
        try {
            ResourceUtil.copyResourceToFile("/jingle-obs-link.lua", FOLDER.resolve("jingle-obs-link.lua"));
            Jingle.log(Level.INFO,"Regenerated obs link script");
        } catch (IOException e) {
            Jingle.logError("Failed to write Script!", e);
            Jingle.log(Level.ERROR, "You can download the script manually from https://github.com/DuncanRuns/Jingle/blob/main/src/main/resources/jingle-obs-link.lua");
        }
    }

    private static void mainLoop() {
        while (running) {
            sleep(1);
            tryTick();
        }
    }

    private static void tryTick() {
        try {
            tick();
        } catch (Throwable t) {
            logError("Error during tick:", t);
            JOptionPane.showMessageDialog(null, "Error during main processing: " + ExceptionUtil.toDetailedString(t), "Jingle: Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static synchronized void tick() {
        PluginEvents.RunnableEventType.START_TICK.runAll();
        activeHwnd = User32.INSTANCE.GetForegroundWindow();
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - lastInstanceCheck) > 500) {
            lastInstanceCheck = currentTime;
            updateMainInstance();
        }
        if (stateTracker != null) stateTracker.tryUpdate();
        PluginEvents.RunnableEventType.END_TICK.runAll();
    }

    public static synchronized boolean isInstanceActive() {
        if (mainInstance == null) return false;
        return Objects.equals(mainInstance.hwnd, activeHwnd);
    }

    private static void updateMainInstance() {
        if (isInstanceActive()) return;
        final boolean mainInstancePreviouslyExists = mainInstance != null;

        if (mainInstancePreviouslyExists && mainInstance.hwnd == activeHwnd) return;

        Set<OpenedInstanceInfo> allOpenedInstances = InstanceChecker.getAllOpenedInstances();
        if (allOpenedInstances.isEmpty()) {
            setMainInstance(null);
            return;
        }

        Optional<OpenedInstanceInfo> newActiveInstance = allOpenedInstances.stream().filter(openedInstanceInfo -> Objects.equals(activeHwnd, openedInstanceInfo.hwnd)).findAny();
        if (newActiveInstance.isPresent()) {
            setMainInstance(newActiveInstance.get());
            return;
        }
        if (!(mainInstancePreviouslyExists && User32.INSTANCE.IsWindow(mainInstance.hwnd))) {
            setMainInstance(allOpenedInstances.stream().findAny().orElse(null));
        }
    }

    public static synchronized Optional<OpenedInstanceInfo> getMainInstance() {
        return Optional.ofNullable(mainInstance);
    }

    public static void setMainInstance(@Nullable OpenedInstanceInfo instance) {
        if (mainInstance == instance) return;
        mainInstance = instance;
        stateTracker = instance == null ? null : new StateTracker(instance.instancePath.resolve("wpstateout.txt"), Jingle::onInstanceStateChange);
        JingleGUI.get().setInstance(instance);
        if (instance != null) seeInstancePath(instance.instancePath);
        log(Level.INFO, instance == null ? "No instances are open." : ("Instance Found! " + instance.instancePath));
        PluginEvents.RunnableEventType.MAIN_INSTANCE_CHANGED.runAll();
    }

    private static void onInstanceStateChange(InstanceState previousState, InstanceState newState) {
        boolean previouslyInWorld = previousState.equals(InstanceState.INWORLD);
        boolean currentlyInWorld = newState.equals(InstanceState.INWORLD);
        if (previouslyInWorld && !currentlyInWorld) {
            onExitWorld();
        } else if (!previouslyInWorld && currentlyInWorld) {
            onEnterWorld();
        }
        PluginEvents.RunnableEventType.STATE_CHANGE.runAll();
        ScriptStuff.RunnableEventType.STATE_CHANGE.runAll();
    }

    private static void onExitWorld() {
        PluginEvents.RunnableEventType.EXIT_WORLD.runAll();
        ScriptStuff.RunnableEventType.EXIT_WORLD.runAll();

        if (Jingle.options.revertWindowAfterReset) {
            Resizing.undoResize();
        }
    }

    private static void onEnterWorld() {
        PluginEvents.RunnableEventType.ENTER_WORLD.runAll();
        ScriptStuff.RunnableEventType.ENTER_WORLD.runAll();
    }

    private static void seeInstancePath(Path instancePath) {
        options.seenPaths = new HashMap<>(options.seenPaths);
        options.seenPaths.entrySet().removeIf(stringLongEntry -> !Files.isDirectory(Paths.get(stringLongEntry.getKey())));
        options.seenPaths.put(instancePath.toAbsolutePath().toString(), System.currentTimeMillis());
    }

    public synchronized static void stop() {
        try {
            running = false;
            assert options != null;
            PluginEvents.RunnableEventType.STOP.runAll();
            options.save();
            log(Level.INFO, "Shutdown successful");
        } catch (Throwable t) {
            logError("Failed to shutdown: ", t);
            System.exit(1);
        }
        System.exit(0);
    }

    public static synchronized void goBorderless() {
        if (mainInstance != null) {
            WindowStateUtil.ensureNotMinimized(mainInstance.hwnd);
            WindowStateUtil.setHwndBorderless(mainInstance.hwnd);
            Rectangle pBounds = MonitorUtil.getPrimaryMonitor().getPBounds();
            WindowStateUtil.setHwndRectangle(mainInstance.hwnd, new Rectangle(pBounds.x, pBounds.y, pBounds.width, pBounds.height - 1));
            WindowStateUtil.setHwndRectangle(mainInstance.hwnd, pBounds);
        }
    }

    public static void openInstanceFolder() {
        OpenedInstanceInfo instance = mainInstance;
        if (instance != null) {
            OpenUtil.openFile(instance.instancePath.toString());
        }
    }

    public static boolean isRunning() {
        return running;
    }

    @SuppressWarnings("all")
    public static Optional<Runnable> getBuiltinHotkeyAction(String name) {
        switch (name) {
            default:
                return Optional.empty();
        }
    }

    @SuppressWarnings("all")
    public static Set<String> getBuiltinHotkeyActionNames() {
        return new HashSet<>(Arrays.asList("None"));
    }

    public static String formatAction(String action) {
        if (!action.contains(":")) return action;
        int i = action.indexOf(':');
        return String.format("%s - %s", action.substring(0, i), action.substring(i + 1));
    }
}
