package xyz.duncanruns.jingle;

import com.formdev.flatlaf.FlatDarkLaf;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.plugin.PluginManager;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.LockUtil;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Arrays;

public class JingleAppLaunch {
    private static final Path LOCK_FILE = Jingle.FOLDER.resolve("LOCK");
    private static LockUtil.LockStuff lockStuff = null;

    public static String[] args;
    public static boolean launchedWithDevPlugin = false;

    public static void launchWithDevPlugin(String[] args, PluginManager.JinglePluginData pluginData, Runnable pluginInitializer) {
        launchedWithDevPlugin = true;
        PluginManager.registerPlugin(pluginData, pluginInitializer);
        main(args);
    }

    public static void main(String[] args) {
        JingleAppLaunch.args = args;
        System.out.println("Launched with args: " + Arrays.toString(args));

        try {
            launch();
        } catch (Throwable t) {
            Jingle.logError("Failed to start Jingle!", t);
            JOptionPane.showMessageDialog(null, "Failed to start Jingle! " + ExceptionUtil.toDetailedString(t), "Jingle: Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private static void launch() {
        FlatDarkLaf.setup();
        JingleOptions.ensureFolderExists();

        doLockStuff();

        Jingle.options = JingleOptions.load();

        JingleGUI ignored = JingleGUI.get();

        PluginManager.loadPlugins();
        PluginManager.initializePlugins();

        Jingle.start();
    }

    private static void doLockStuff() {
        if (LockUtil.isLocked(LOCK_FILE)) {
            showMultiJingleWarning();
            LockUtil.keepTryingLock(LOCK_FILE, ls -> {
                lockStuff = ls;
                Jingle.log(Level.DEBUG, "Obtained Lock");
            });
        } else {
            lockStuff = LockUtil.lock(LOCK_FILE);
            Jingle.log(Level.DEBUG, "Obtained Lock");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> LockUtil.releaseLock(lockStuff)));
    }


    private static void showMultiJingleWarning() {
        if (0 != JOptionPane.showConfirmDialog(null, "Jingle is already running! Are you sure you want to open Jingle again?", "Jingle: Already Opened", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
            System.exit(0);
        }
    }
}
