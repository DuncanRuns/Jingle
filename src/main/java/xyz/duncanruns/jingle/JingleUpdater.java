package xyz.duncanruns.jingle;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.GrabUtil;
import xyz.duncanruns.jingle.util.PowerShellUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static xyz.duncanruns.jingle.util.SleepUtil.sleep;

public final class JingleUpdater {
    private static JsonObject meta = null;

    private JingleUpdater() {
    }

    public static void checkDeleteOldJar() {
        List<String> argList = Arrays.asList(JingleAppLaunch.args);
        if (!argList.contains("-deleteOldJar")) {
            return;
        }

        File toDelete = new File(argList.get(argList.indexOf("-deleteOldJar") + 1));

        Jingle.log(Level.INFO, "Deleting old jar " + toDelete.getName());

        for (int i = 0; i < 200 && !toDelete.delete(); i++) {
            sleep(10);
        }

        if (toDelete.exists()) {
            Jingle.log(Level.ERROR, "Failed to delete " + toDelete.getName());
        } else {
            Jingle.log(Level.INFO, "Deleted " + toDelete.getName());
        }
    }

    public synchronized static void checkForUpdates() {
        String currentVersion;

        synchronized (Jingle.class) {
            if (!Jingle.options.checkForUpdates) return;
            currentVersion = Jingle.VERSION;
        }

        if (currentVersion.equals("DEV")) {
            return;
        }

        if (meta == null) {
            try {
                meta = GrabUtil.grabJson("https://raw.githubusercontent.com/DuncanRuns/Jingle/main/meta.json");
            } catch (Exception e) {
                Jingle.logError("Failed to grab Jingle update meta:", e);
            }
        }

        boolean usePreReleases;
        String lastCheckedVersion;
        synchronized (Jingle.class) {
            usePreReleases = Jingle.options.usePreReleases;
            lastCheckedVersion = Jingle.options.lastCheckedVersion;
        }

        String latestVersionKey = "latest";
        String latestDownloadKey = "latest_download";
        if (usePreReleases) {
            latestVersionKey = "latest_dev";
            latestDownloadKey = "latest_dev_download";
        }

        if (!(meta.has(latestVersionKey) && meta.has(latestDownloadKey))) return;

        String foundLatestVersion = meta.get(latestVersionKey).getAsString();
        String downloadLink = meta.get(latestDownloadKey).getAsString();

        if (Objects.equals(currentVersion, foundLatestVersion)) return;
        if (Objects.equals(lastCheckedVersion, foundLatestVersion)) return;

        synchronized (Jingle.class) {
            Jingle.options.lastCheckedVersion = foundLatestVersion;
        }

        // Update available!!!
        int ans = JOptionPane.showConfirmDialog(JingleGUI.get(), String.format("A new version of Jingle is available! Current version: %s, new version: %s.", currentVersion, foundLatestVersion), "Jingle: Update available", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (ans != 0) return;

        try {
            update(downloadLink);
        } catch (Exception e) {
            Jingle.logError("Failed to update (wtf)!", e);
            JOptionPane.showMessageDialog(null, "Failed to update Jingle!!!! (BAD)" + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void update(String download) throws IOException, PowerShellExecutionException {
        Path newJarPath = Jingle.getSourcePath().resolveSibling(URLDecoder.decode(FilenameUtils.getName(download), StandardCharsets.UTF_8.name()));

        JingleGUI jingleGUI = JingleGUI.get();
        jingleGUI.jingleUpdating = true;
        jingleGUI.dispose();
        Jingle.stop(false);

        if (!Files.exists(newJarPath)) {
            downloadWithProgress(download, newJarPath);
        }

        // Release LOCK so updating can go smoothly
        JingleAppLaunch.releaseLock();

        Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("javaw.exe");

        // Use powershell's start-process to start it detached
        String powerCommand = String.format("start-process '%s' '-jar \"%s\" -deleteOldJar \"%s\"'", javaExe, newJarPath, Jingle.getSourcePath());
        Jingle.log(Level.INFO, "Exiting and running powershell command: " + powerCommand);

        PowerShellUtil.execute(powerCommand);
        System.exit(0);
    }


    private static void downloadWithProgress(String download, Path newJarPath) throws IOException {
        Point location = JingleGUI.get().getLocation();
        JProgressBar bar = new DownloadProgressFrame(location).getBar();
        bar.setMaximum((int) GrabUtil.getFileSize(download));
        GrabUtil.download(download, newJarPath, bar::setValue, 128);
    }

    private static class DownloadProgressFrame extends JFrame {
        private final JProgressBar bar;

        public DownloadProgressFrame(Point location) {
            this.setLayout(new GridBagLayout());
            JLabel text = new JLabel("Downloading Jingle...");
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.insets = new Insets(5, 5, 5, 5);
            this.add(text, gbc);
            this.bar = new JProgressBar(0, 100);
            this.add(this.bar, gbc);


            this.setSize(300, 100);
            this.setTitle("Jingle");
            this.setIconImage(JingleGUI.getLogo());
            this.setLocation(location);
            this.setResizable(false);
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            this.setVisible(true);
        }

        public JProgressBar getBar() {
            return this.bar;
        }
    }
}
