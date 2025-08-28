package xyz.duncanruns.jingle;

import com.sun.jna.platform.win32.WinDef;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.instance.*;
import xyz.duncanruns.jingle.obs.OBSLink;
import xyz.duncanruns.jingle.obs.OBSProjector;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.script.ScriptStuff;
import xyz.duncanruns.jingle.util.*;
import xyz.duncanruns.jingle.win32.User32;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static xyz.duncanruns.jingle.util.SleepUtil.sleep;

public final class Jingle {
    public static final Path FOLDER = Paths.get(System.getProperty("user.home")).resolve(".config").resolve("Jingle").toAbsolutePath();
    public static final String VERSION = Optional.ofNullable(Jingle.class.getPackage().getImplementationVersion()).orElse("DEV");
    public static final Logger LOGGER = LogManager.getLogger("Jingle");

    private static boolean started = false;
    private static boolean running = false;

    private static long lastInstanceCheck = 0;
    private static boolean legalModCheckNeeded = false;

    public static JingleOptions options = null;

    @Nullable
    private static OpenedInstance mainInstance = null;

    @Nullable
    public static WinDef.HWND activeHwnd = null;

    private static boolean openedToLan = false;

    private Jingle() {
    }


    public static void log(Level level, String message) {
        new Thread(() -> {
            String messageWithTime = String.format("[%s/%s] %s", new SimpleDateFormat("HH:mm:ss").format(new Date()), level, message);
            if (!JingleGUI.instanceExists()) return;
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
        SwingUtilities.invokeLater(() -> {
            JingleGUI.get().setInstance(getLatestInstancePath().orElse(null), false);
            JingleGUI.get().scriptListPanel.reload();
            JingleGUI.get().refreshQuickActions();
            JingleGUI.get().refreshHack();
        });
        HotkeyManager.reload();
        HotkeyManager.start();

        loadSupporters();
        loadLegalMods();
        new Thread(JingleUpdater::checkForUpdates, "update-checker").start();

        generateResources();

        String usedJava = System.getProperty("java.home");
        log(Level.INFO, "You are running Jingle v" + VERSION + " with java: " + usedJava);

        JingleUpdater.checkDeleteOldJar();

        mainLoop();
    }

    private static void loadLegalMods() {
        new Thread(() -> {
            try {
                LegalModsUtil.updateLegalMods();
                log(Level.INFO, "Successfully obtained legal mods");
            } catch (IOException e) {
                logError("Failed to update legal mods!", e);
            }
        }, "legal-mods-checker").start();
    }

    private static void loadSupporters() {
        new Thread(() -> {
            try {
                JingleGUI.get().showSupporters(GrabUtil.grab("https://raw.githubusercontent.com/DuncanRuns/Jingle/main/supporters.txt").split(", "));
            } catch (Exception e) {
                logError("Failed to obtain list of supporters!", e);
            }
        }, "supporter-loader").start();
    }

    private static void generateResources() {
        try {
            ResourceUtil.copyResourceToFile("/jingle-obs-link.lua", FOLDER.resolve("jingle-obs-link.lua"));
            Jingle.log(Level.INFO, "Regenerated obs link script");
        } catch (IOException e) {
            Jingle.logError("Failed to write Script!", e);
            Jingle.log(Level.ERROR, "You can download the script manually from https://github.com/DuncanRuns/Jingle/blob/main/src/main/resources/jingle-obs-link.lua");
        }
        Path overlayPngPath = FOLDER.resolve("measuring_overlay.png");
        if (Files.exists(overlayPngPath)) return;
        try {
            ResourceUtil.copyResourceToFile("/measuring_overlay.png", overlayPngPath);
            Jingle.log(Level.INFO, "Created measuring_overlay.png");
        } catch (IOException e) {
            Jingle.logError("Failed to measuring_overlay.png!", e);
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
        PluginEvents.START_TICK.runAll();
        ScriptStuff.START_TICK.runAll();
        activeHwnd = User32.INSTANCE.GetForegroundWindow();
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - lastInstanceCheck) > 500) {
            lastInstanceCheck = currentTime;
            updateMainInstance();
            getMainInstance().ifPresent(i -> {
                updateWindowTitle(i);
                i.standardSettings.tryUpdate();
                i.optionsTxt.tryUpdate();
                if (legalModCheckNeeded && LegalModsUtil.hasUpdated()) {
                    checkLegalMods();
                }
            });
        }
        getMainInstance().ifPresent(i -> i.stateTracker.tryUpdate());
        OBSProjector.tick();
        OBSLink.tick();
        PluginEvents.END_TICK.runAll();
        ScriptStuff.END_TICK.runAll();
    }

    private static void checkLegalMods() {
        assert getMainInstance().isPresent();
        for (FabricModFolder.FabricJarInfo jar : getMainInstance().get().fabricModFolder.getInfos()) {
            if (!LegalModsUtil.isLegalMod(jar.id)) {
                Jingle.log(Level.WARN, "Warning: Mod " + jar.name + " is not a legal mod!");
            }
        }
        Jingle.log(Level.INFO, "Finished checking legal mods for this instance.");
        legalModCheckNeeded = false;
    }

    private static void updateWindowTitle(OpenedInstance instance) {
        if (!WindowTitleUtil.getHwndTitle(instance.hwnd).equals("Minecraft* - Instance 1")) {
            User32.INSTANCE.SetWindowTextA(instance.hwnd, "Minecraft* - Instance 1");
        }
    }

    public static synchronized boolean isInstanceActive() {
        return getMainInstance().map(i -> Objects.equals(i.hwnd, activeHwnd)).orElse(false);
    }

    private static void updateMainInstance() {
        if (isInstanceActive()) return;
        final boolean mainInstancePreviouslyExists = getMainInstance().isPresent();

        if (mainInstancePreviouslyExists && getMainInstance().get().hwnd == activeHwnd) return;

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
        if (!(mainInstancePreviouslyExists && User32.INSTANCE.IsWindow(getMainInstance().get().hwnd))) {
            setMainInstance(allOpenedInstances.stream().findAny().orElse(null));
        }
    }

    public static synchronized Optional<OpenedInstance> getMainInstance() {
        return Optional.ofNullable(mainInstance);
    }

    public static void setMainInstance(@Nullable OpenedInstanceInfo instance) {
        undoWindowTitle(mainInstance);
        if (mainInstance == instance) return;
        mainInstance = instance == null ? null : new OpenedInstance(instance, Jingle::onInstanceStateChange);
        legalModCheckNeeded = instance != null;
        resetStates();
        JingleGUI.get().setInstance(getLatestInstancePath().orElse(null), instance != null);
        if (instance != null) seeInstancePath(instance.instancePath);
        if (options.autoBorderless) goBorderless();
        log(Level.INFO, instance == null ? "No instances are open." : ("Instance Found! " + instance.instancePath + ", " + instance.versionString));
        PluginEvents.MAIN_INSTANCE_CHANGED.runAll();
        ScriptStuff.MAIN_INSTANCE_CHANGED.runAll();
    }

    private static void undoWindowTitle(OpenedInstance instance) {
        if (instance == null) return;
        if (User32.INSTANCE.IsWindow(instance.hwnd)) {
            User32.INSTANCE.SetWindowTextA(instance.hwnd, "Minecraft*");
        }
    }

    private static void resetStates() {
        openedToLan = false;
    }

    private static void onInstanceStateChange(InstanceState previousState, InstanceState newState) {
        boolean previouslyInWorld = previousState.equals(InstanceState.INWORLD);
        boolean currentlyInWorld = newState.equals(InstanceState.INWORLD);
        if (previouslyInWorld && !currentlyInWorld) {
            onExitWorld();
        } else if (!previouslyInWorld && currentlyInWorld) {
            onEnterWorld();
        }
        PluginEvents.STATE_CHANGE.runAll();
        ScriptStuff.STATE_CHANGE.runAll();
    }

    private static void onExitWorld() {
        PluginEvents.EXIT_WORLD.runAll();
        ScriptStuff.EXIT_WORLD.runAll();

        openedToLan = false;
    }

    private static void onEnterWorld() {
        PluginEvents.ENTER_WORLD.runAll();
        ScriptStuff.ENTER_WORLD.runAll();
    }

    private static void seeInstancePath(Path instancePath) {
        options.seenPaths = new HashMap<>(options.seenPaths);
        options.seenPaths.entrySet().removeIf(stringLongEntry -> !Files.isDirectory(Paths.get(stringLongEntry.getKey())));
        options.seenPaths.put(instancePath.toAbsolutePath().toString(), System.currentTimeMillis());
    }

    public synchronized static void stop(boolean doSystemExit) {
        try {
            running = false;
            assert options != null;
            PluginEvents.STOP.runAll();
            try {
                OBSProjector.closeAnyMeasuringProjectors();
            } catch (Throwable ignored) { // We really don't care if this fails lol
            }
            getMainInstance().ifPresent(Jingle::undoWindowTitle);
            options.save();
            log(Level.INFO, "Shutdown successful");
        } catch (Throwable t) {
            logError("Failed to shutdown:", t);
            if (doSystemExit) System.exit(1);
        }
        if (doSystemExit) System.exit(0);
    }

    public static synchronized void goBorderless() {
        getMainInstance().ifPresent(mainInstance -> {
            WindowStateUtil.ensureNotMinimized(mainInstance.hwnd);
            WindowStateUtil.setHwndBorderless(mainInstance.hwnd);
            int[] bp = Jingle.options.borderlessPosition;
            if (bp == null) {
                Rectangle pBounds = MonitorUtil.getPrimaryMonitor().getPBounds();
                WindowStateUtil.setHwndRectangle(mainInstance.hwnd, new Rectangle(pBounds.x, pBounds.y, pBounds.width, pBounds.height - 1));
                WindowStateUtil.setHwndRectangle(mainInstance.hwnd, pBounds);
            } else {
                WindowStateUtil.setHwndRectangle(mainInstance.hwnd, new Rectangle(bp[0], bp[1], bp[2], bp[3]));
            }
        });
    }

    public static void openInstanceFolder() {
        getLatestInstancePath().ifPresent(p -> OpenUtil.openFile(p.toString()));
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

    public static void openToLan(boolean alreadyPaused, boolean enableCheats) {
        if (!getMainInstance().isPresent()) return;

        WinDef.HWND hwnd = getMainInstance().get().hwnd;

        if (openedToLan) {
            return;
        } else {
            if (!getMainInstance().get().stateTracker.isCurrentState(InstanceState.INWORLD)) {
                return;
            } else if (WindowTitleUtil.getHwndTitle(hwnd).endsWith("(LAN)")) {
                openedToLan = true;
                return;
            }
        }

        KeyPresser keyPresser = getMainInstance().get().keyPresser;
        keyPresser.releaseAllModifiers();
        if (!alreadyPaused) {
            keyPresser.pressEsc();
        }
        keyPresser.pressTab(7);
        keyPresser.pressEnter();
        keyPresser.pressShiftTab(1);
        String versionString = getMainInstance().get().versionString;
        if (MCVersionUtil.isNewerThan(versionString, "1.16.5")) {
            keyPresser.pressTab(2);
        }
        if (enableCheats) {
            keyPresser.pressEnter();
        }
        keyPresser.pressTab(MCVersionUtil.isNewerThan(versionString, "1.19.2") ? 2 : 1);
        keyPresser.pressEnter();
        openedToLan = true;
    }

    /**
     * Returns the path of the "code source".
     * <p>
     * This will be a path to the jar file when running as a jar, and a root directory of the compiled classes when ran in a development environment.
     */
    public static Path getSourcePath() {
        try {
            return Paths.get(Jingle.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void showMeasuringProjector() {
        OBSProjector.bringOBSProjectorToTop();
        PluginEvents.SHOW_PROJECTOR.runAll();
    }

    public static void dumpMeasuringProjector() {
        OBSProjector.dumpOBSProjector();
        PluginEvents.DUMP_PROJECTOR.runAll();
    }

    public static Optional<Path> getLatestInstancePath() {
        return Optional.ofNullable(getMainInstance().map(i -> i.instancePath).orElse(
                Jingle.options.seenPaths.entrySet().stream()
                        .max(Comparator.comparingLong(Map.Entry::getValue))
                        .map(Map.Entry::getKey).map(Paths::get)
                        .orElse(null)
        ));
    }
}
