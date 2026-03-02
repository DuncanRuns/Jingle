package xyz.duncanruns.jingle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.WinDef;
import me.duncanruns.kerykeion.Kerykeion;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.instance.*;
import xyz.duncanruns.jingle.plugin.PluginEvents;
import xyz.duncanruns.jingle.script.HermesScriptRelay;
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
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static xyz.duncanruns.jingle.util.SleepUtil.sleep;

public final class Jingle {
    public static final Path FOLDER = Paths.get(System.getProperty("user.home")).resolve(".config").resolve("Jingle").toAbsolutePath();
    public static final String VERSION = Optional.ofNullable(Jingle.class.getPackage().getImplementationVersion()).orElse("DEV");
    public static final Logger LOGGER = LogManager.getLogger("Jingle");

    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Jingle"));
    private static final ScheduledExecutorService LOG_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Jingle-Log"));

    private static boolean started = false;
    private static volatile boolean running = false;

    private static long lastInstanceCheck = 0;
    private static boolean legalModCheckNeeded = false;

    private static boolean shouldScheduleBorderless = false;
    private static long borderlessScheduledTime = -1;

    public static JingleOptions options = null;

    @Nullable
    private static OpenedInstance mainInstance = null;

    @Nullable
    public static WinDef.HWND activeHwnd = null;
    public static int activePid = -1;

    private static boolean guiWasFocused = false;

    private Jingle() {
    }


    public static void log(Level level, String message) {
        try {

            SwingUtilities.invokeLater(() -> {
                String messageWithTime = String.format("[%s/%s] %s", new SimpleDateFormat("HH:mm:ss").format(new Date()), level, message);
                if (!JingleGUI.instanceExists()) return;
                JingleGUI.get().logDocumentWithDebug.addLineWithRolling(messageWithTime);
                if (level.equals(Level.DEBUG)) return;
                JingleGUI.get().logDocument.addLineWithRolling(messageWithTime);
            });
        } catch (RejectedExecutionException ignored) {
        }
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
        loadCommunity();
        loadLegalMods();
        new Thread(JingleUpdater::checkForUpdates, "update-checker").start();

        String usedJava = System.getProperty("java.home");
        log(Level.INFO, "You are running Jingle v" + VERSION + " with java: " + usedJava);

        JingleUpdater.checkDeleteOldJar();

        MonitorUtil.retrieveMinY();

        checkJavaVersion();

        log(Level.INFO, "Jingle process ID: " + PidUtil.getPidForSelf());

        Kerykeion.addListener(InstanceChecker.HermesChecker.get(), 100, EXECUTOR);
        Kerykeion.addListener(HermesScriptRelay.get(), 1, EXECUTOR);
        Kerykeion.setErrorLogger((s, throwable) -> logError("(Kerykeion) " + s, throwable));
        Kerykeion.start(true);
        EXECUTOR.scheduleWithFixedDelay(Jingle::tryTick, 0, 1, TimeUnit.MILLISECONDS);
    }

    private static void checkJavaVersion() {
        Optional<Integer> majorJavaVersion = JavaVersionUtil.getMajorJavaVersion();
        if (majorJavaVersion.isPresent()) {
            log(Level.INFO, "Java Major Version: " + majorJavaVersion.get());
        } else {
            log(Level.WARN, "Java Major Version unknown. Likely running on Java 8 or lower. Jingle may not work correctly.");
        }
        if (majorJavaVersion.isPresent() && majorJavaVersion.get() > 8) return;
        SwingUtilities.invokeLater(() -> {
            JingleGUI gui = JingleGUI.get();
            int ans = JOptionPane.showOptionDialog(gui, "You are running Jingle with Java 8 or lower. Things may not work as expected. Updating to Java 17 or higher is recommended.", "Jingle: Java 8 Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]{"Download Adoptium Java 21", "Ignore"}, "Download Adoptium Java 21");
            if (ans == 0) {
                OpenUtil.openLink("https://adoptium.net/temurin/releases?version=21", gui);
            }
        });
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
                JingleGUI.get().showSupporters(GrabUtil.grab("https://raw.githubusercontent.com/DuncanRuns/Jingle/meta/supporters.txt").split(", "));
            } catch (Exception e) {
                logError("Failed to obtain list of supporters!", e);
            }
        }, "supporter-loader").start();
    }

    private static void loadCommunity() {
        new Thread(() -> {
            try {
                JsonObject json = GrabUtil.grabJson("https://raw.githubusercontent.com/DuncanRuns/Jingle/meta/community.json");
                List<Pair<String, String>> buttons = json.getAsJsonArray("buttons").asList().stream()
                        .map(JsonElement::getAsJsonObject)
                        .map(j -> Pair.of(j.get("display").getAsString(), j.get("link").getAsString()))
                        .collect(Collectors.toList());
                JingleGUI.get().showCommunityButtons(buttons);
            } catch (Exception e) {
                logError("Failed to obtain list of community buttons!", e);
                JingleGUI.get().showCommunityButtons(null);
            }
        }, "community-loader").start();
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
        try {
            activePid = PidUtil.getPidFromHwnd(activeHwnd);
        } catch (Exception e) {
            activePid = -1;
        }
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
        // Try assign hwnd to main instance
        getMainInstance().ifPresent(i -> i.checkWindow(activeHwnd));
        if (shouldScheduleBorderless && getMainInstanceHwnd().isPresent()) {
            shouldScheduleBorderless = false;
            borderlessScheduledTime = System.currentTimeMillis() + 3000;
        }
        if (borderlessScheduledTime != -1 && System.currentTimeMillis() >= borderlessScheduledTime) {
            goBorderless();
            borderlessScheduledTime = -1;
        }
        if (JingleGUI.instanceExists()) {
            tickFocusCheck();
        }
        PluginEvents.END_TICK.runAll();
        ScriptStuff.END_TICK.runAll();
    }

    private static void tickFocusCheck() {
        boolean focused = JingleGUI.get().isFocused();
        if (focused == guiWasFocused) return;
        guiWasFocused = focused;
        if (!focused) {
            PluginEvents.GUI_LOST_FOCUS.runAll();
            log(Level.DEBUG, "Unfocused, saving options...");
            options.save();
        }
    }

    private static void checkLegalMods() {
        assert getMainInstance().isPresent();
        for (InstanceMods.ModInfo jar : getMainInstance().get().mods.getInfos()) {
            if (!jar.fromModsFolder) continue;
            if (!LegalModsUtil.isLegalMod(jar.id)) {
                Jingle.log(Level.WARN, "Warning: Mod " + jar.name + " is not a legal mod!");
            }
        }
        Jingle.log(Level.INFO, "Finished checking legal mods for this instance.");
        legalModCheckNeeded = false;
    }

    private static void updateWindowTitle(OpenedInstance instance) {
        instance.getHwnd().ifPresent(hwnd -> {
            if (!WindowTitleUtil.getHwndTitle(hwnd).equals("Minecraft* - Instance 1")) {
                User32.INSTANCE.SetWindowTextA(hwnd, "Minecraft* - Instance 1");
            }
        });
    }

    public static synchronized boolean isInstanceActive() {
        return getMainInstance().map(i -> i.checkWindow(activeHwnd)).orElse(false);
    }

    private static void updateMainInstance() {
        if (isInstanceActive()) return;
        final boolean mainInstancePreviouslyExists = getMainInstance().isPresent();

        if (mainInstancePreviouslyExists && (getMainInstance().get().pid == activePid || getMainInstance().get().checkWindow(activeHwnd)))
            return;

        Set<OpenedInstance> allOpenedInstances = InstanceChecker.getAllOpenedInstances();
        if (allOpenedInstances.isEmpty()) {
            setMainInstance(null, null);
            return;
        }

        Optional<OpenedInstance> newActiveInstance = allOpenedInstances.stream().filter(openedInstanceInfo -> Objects.equals(activePid, openedInstanceInfo.pid)).findAny();
        if (newActiveInstance.isPresent()) {
            setMainInstance(newActiveInstance.get(), activeHwnd);
            return;
        }
        if (!(mainInstancePreviouslyExists && getMainInstance().get().getHwnd().map(User32.INSTANCE::IsWindow).orElse(PidUtil.isProcessRunning(getMainInstance().get().pid)))) {
            setMainInstance(allOpenedInstances.stream().findAny().orElse(null), null);
        }
    }

    public static synchronized Optional<OpenedInstance> getMainInstance() {
        return Optional.ofNullable(mainInstance);
    }

    public static Optional<WinDef.HWND> getMainInstanceHwnd() {
        return getMainInstance().flatMap(OpenedInstance::getHwnd);
    }

    public static Optional<KeyPresser> getMainInstanceKeyPresser() {
        return getMainInstance().flatMap(OpenedInstance::getKeyPresser);
    }

    public static void setMainInstance(@Nullable OpenedInstance instance, WinDef.HWND hwnd) {
        if (mainInstance == instance) return;
        undoWindowTitle(mainInstance);
        mainInstance = instance;
        legalModCheckNeeded = instance != null;
        JingleGUI.get().setInstance(getLatestInstancePath().orElse(null), instance != null);
        borderlessScheduledTime = -1;
        shouldScheduleBorderless = false;
        if (instance != null) {
            seeInstancePath(instance.instancePath);
            mainInstance.setHwnd(hwnd);
            shouldScheduleBorderless = options.autoBorderless && hwnd != null;
        }
        log(Level.INFO, instance == null ? "No instances are open." : ("Instance Found! " + instance.instancePath + ", " + instance.versionString));
        PluginEvents.MAIN_INSTANCE_CHANGED.runAll();
        ScriptStuff.MAIN_INSTANCE_CHANGED.runAll();
    }

    private static void undoWindowTitle(OpenedInstance instance) {
        getMainInstance().flatMap(OpenedInstance::getHwnd).ifPresent(hwnd -> {
            if (User32.INSTANCE.IsWindow(hwnd)) {
                User32.INSTANCE.SetWindowTextA(hwnd, "Minecraft*");
            }
        });
    }

    private static void seeInstancePath(Path instancePath) {
        options.seenPaths = new HashMap<>(options.seenPaths);
        options.seenPaths.entrySet().removeIf(stringLongEntry -> !Files.isDirectory(Paths.get(stringLongEntry.getKey())));
        options.seenPaths.put(instancePath.toAbsolutePath().toString(), System.currentTimeMillis());
    }

    public static void stop(boolean allowSystemExit) {
        Kerykeion.stop();
        try {
            running = false;
            EXECUTOR.shutdown();
            LOG_EXECUTOR.shutdown();
            waitForMainLoopStopOrTerminate(5000);
            PluginEvents.STOP.runAll();
            synchronized (Jingle.class) {
                assert options != null;
                getMainInstance().ifPresent(Jingle::undoWindowTitle);
                options.save();
            }
            JingleAppLaunch.releaseLock();
            log(Level.INFO, "Shutdown successful");
        } catch (Throwable t) {
            logError("Failed to shutdown:", t);
            if (allowSystemExit) System.exit(1);
        }

        if (!allowSystemExit) return;
        Thread finalShutdownThread = new Thread(() -> {
            log(Level.DEBUG, "Started final shutdown thread. Waiting 20 seconds for JVM to exit...");
            sleep(20000);
            log(Level.ERROR, "JVM did not exit after 20 seconds! Force exiting!");
            System.exit(1);
        }, "final-shutdown");
        finalShutdownThread.setDaemon(true);
        finalShutdownThread.start();
    }

    private static void waitForMainLoopStopOrTerminate(@SuppressWarnings("SameParameterValue") final long timeout) {
        log(Level.DEBUG, "Waiting up to 5 seconds for main loop to stop...");
        try {
            if (!EXECUTOR.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                log(Level.ERROR, "Main loop did not stop in time! Force exiting!");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            log(Level.ERROR, "Main loop wait was interrupted! Force exiting!");
            System.exit(1);
        }
    }

    public static synchronized void goBorderless() {
        getMainInstance().flatMap(OpenedInstance::getHwnd).ifPresent(hwnd -> {
            WindowStateUtil.ensureNotMinimized(hwnd);
            WindowStateUtil.setHwndBorderless(hwnd);
            int[] bp = Jingle.options.borderlessPosition;
            if (bp == null) {
                Rectangle pBounds = MonitorUtil.getPrimaryMonitor().getPBounds();
                WindowStateUtil.setHwndRectangle(hwnd, new Rectangle(pBounds.x, pBounds.y, pBounds.width, pBounds.height - 1));
                WindowStateUtil.setHwndRectangle(hwnd, pBounds);
            } else {
                WindowStateUtil.setHwndRectangle(hwnd, new Rectangle(bp[0], bp[1], bp[2], bp[3]));
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

    /**
     * Runs common key presses to open a world to lan. Does not check if the instance is already opened to lan, or if
     * it is in a world. Scripts/plugins should determine these things before running this.
     */
    public static void openToLan(boolean alreadyPaused, boolean enableCheats) {
        Optional<OpenedInstance> instanceOpt = getMainInstance();
        if (!instanceOpt.isPresent()) return;
        OpenedInstance instance = instanceOpt.get();

        Optional<KeyPresser> keyPresserOpt = instance.getKeyPresser();
        if (!keyPresserOpt.isPresent()) return;
        KeyPresser keyPresser = keyPresserOpt.get();

        keyPresser.releaseAllModifiers();
        if (!alreadyPaused) {
            keyPresser.pressEsc();
        }
        keyPresser.pressTab(7);
        keyPresser.pressEnter();
        keyPresser.pressShiftTab(1);
        String versionString = instance.versionString;
        if (MCVersionUtil.isNewerThan(versionString, "1.16.5")) {
            keyPresser.pressTab(2);
        }
        if (enableCheats) {
            keyPresser.pressEnter();
        }
        keyPresser.pressTab(MCVersionUtil.isNewerThan(versionString, "1.19.2") ? 2 : 1);
        keyPresser.pressEnter();

        if (isInstanceActive()) for (Integer pressedModifier : KeyboardUtil.getPressedModifiers()) {
            WinDef.HWND hwnd = getMainInstanceHwnd().orElseThrow(() -> new IllegalStateException("Key Presser exists without hwnd!"));
            KeyboardUtil.sendKeyDownToHwnd(hwnd, pressedModifier);
        }
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

    public static Optional<Path> getLatestInstancePath() {
        return Optional.ofNullable(getMainInstance().map(i -> i.instancePath).orElse(
                Jingle.options.seenPaths.entrySet().stream()
                        .max(Comparator.comparingLong(Map.Entry::getValue))
                        .map(Map.Entry::getKey).map(Paths::get)
                        .orElse(null)
        ));
    }
}
