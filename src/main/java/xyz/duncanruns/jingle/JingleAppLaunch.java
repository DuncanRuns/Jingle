package xyz.duncanruns.jingle;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.i18n.I18nManager;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.plugin.PluginManager;
import xyz.duncanruns.jingle.script.lua.LuaLibraries;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.LockUtil;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;

public final class JingleAppLaunch{
    private static final Path LOCK_FILE = Jingle.FOLDER.resolve("LOCK");
    private static final Log log = LogFactory.getLog(JingleAppLaunch.class);
    private static LockUtil.LockStuff lockStuff = null;
    public static String[] args;
    public static boolean launchedWithDevPlugin = false;

    private JingleAppLaunch() {
    }

    @SuppressWarnings("unused")
    public static void launchWithDevPlugin(String[] args, PluginManager.JinglePluginData pluginData, Runnable pluginInitializer) {
        launchedWithDevPlugin = true;
        PluginManager.registerPlugin(pluginData, pluginInitializer);
        main(args);
    }

    public static void main(String[] args) {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            JOptionPane.showMessageDialog(null, "Jingle is only compatible with Windows. Jingle will now exit to prevent any issues from trying to launch on an invalid operating system.");
            return;
        }

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

        I18nManager.init(Jingle.options.language);

        JingleGUI ignored = JingleGUI.get();

        PluginManager.loadPlugins();
        PluginManager.initializePlugins();

        LuaLibraries.generateLuaDocs();

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
        Runtime.getRuntime().addShutdownHook(new Thread(JingleAppLaunch::releaseLock));
    }

    public static void releaseLock() {
        LockUtil.releaseLock(lockStuff);
    }


    private static void showMultiJingleWarning() {
        if (0 != JOptionPane.showConfirmDialog(null, "Jingle is already running! Are you sure you want to open Jingle again?", "Jingle: Already Opened", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)) {
            System.exit(0);
        }
    }
}
