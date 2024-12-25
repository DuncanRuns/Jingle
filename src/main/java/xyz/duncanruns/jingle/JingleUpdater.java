package xyz.duncanruns.jingle;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.GrabUtil;
import xyz.duncanruns.jingle.util.PowerShellUtil;
import xyz.duncanruns.jingle.util.VersionUtil;

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

        if (currentVersion.equals("DEV") || JingleAppLaunch.launchedWithDevPlugin) {
            return;
        }

        if (meta == null) {
            try {
                meta = GrabUtil.grabJson("https://raw.githubusercontent.com/DuncanRuns/Jingle/main/meta.json");
            } catch (Exception e) {
                Jingle.logError("Failed to grab Jingle update meta:", e);
                return;
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

        if (!(meta.has(latestVersionKey) && meta.has(latestDownloadKey))) {
            Jingle.log(Level.ERROR, "Update meta has invalid json!");
            return;
        }

        String foundLatestVersion = meta.get(latestVersionKey).getAsString();
        String downloadLink = meta.get(latestDownloadKey).getAsString();

        Jingle.log(Level.DEBUG, String.format("Checking if Jingle should update (current=%s, found=%s, lastChecked=%s)", currentVersion, foundLatestVersion, lastCheckedVersion));
        if (!shouldUpdate(currentVersion, foundLatestVersion, lastCheckedVersion)) return;

        synchronized (Jingle.class) {
            Jingle.options = JingleOptions.load();
            Jingle.options.lastCheckedVersion = foundLatestVersion;
        }

        // Update available!!!
        int ans = JOptionPane.showConfirmDialog(JingleGUI.get(), String.format("A new version of Jingle is available! Current version: %s, newest version: %s.", currentVersion, foundLatestVersion), "Jingle: Update available", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (ans != 0) return;

        try {
            update(downloadLink);
        } catch (Exception e) {
            Jingle.logError("Failed to update (wtf)!", e);
            JOptionPane.showMessageDialog(null, "Failed to update Jingle!!!! (BAD)\n" + ExceptionUtil.toDetailedString(e));
        }
    }

    private static boolean shouldUpdate(String currentVersion, String foundLatestVersion, String lastCheckedVersion) {
        // Cancel if the latest found is the exact same as the current one
        if (Objects.equals(currentVersion, foundLatestVersion)) {
            Jingle.log(Level.INFO, "No new updates found: on latest version.");
            return false;
        }

        // Cancel if the current > latest found
        int comp = VersionUtil.tryCompare(
                VersionUtil.extractVersion(currentVersion),
                VersionUtil.extractVersion(foundLatestVersion),
                -1
        );
        if (comp > 0) {
            Jingle.log(Level.INFO, "No new updates found: on a later version than the latest found.");
            return false;
        }

        // Cancel if latest is a pre-release of the current version (never downgrade to pre release)
        if (comp == 0 && foundLatestVersion.contains("+pre") && !currentVersion.contains("+pre")) {
            Jingle.log(Level.INFO, "No new updates found: latest found version is a pre release of the currently used version.");
            return false;
        }

        // Cancel if already checked latest version
        if (Objects.equals(lastCheckedVersion, foundLatestVersion)) {
            Jingle.log(Level.INFO, "No new updates found: already skipped this version.");
            return false;
        }
        return true;
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

    public static void main(String[] args) {
        System.out.println("E = expected, logs for false reason should be above the test print.\n");
        System.out.println("1 (E: true): " + assertEqualsAndReturn(true, shouldUpdate("1.0.0", "1.1.0", "1.0.0")));
        System.out.println("2 (E: true): " + assertEqualsAndReturn(true, shouldUpdate("1.0.0", "1.1.0+pre1234", "1.0.0")));
        System.out.println("3 (E: false): " + assertEqualsAndReturn(false, shouldUpdate("1.0.0", "0.9", "1.0.0")));
        System.out.println("4 (E: false): " + assertEqualsAndReturn(false, shouldUpdate("1.0.0", "1.0.0+pre1", "1.0.0")));
        System.out.println("5 (E: false): " + assertEqualsAndReturn(false, shouldUpdate("1.0.0", "1.1.0", "1.1.0")));
        System.out.println("6 (E: true): " + assertEqualsAndReturn(true, shouldUpdate("1.1.0+pre1", "1.1.0", "1.0.0")));
        System.out.println("7 (E: true): " + assertEqualsAndReturn(true, shouldUpdate("1.1.0+pre1", "1.1.0+pre2", "1.0.0")));
        System.out.println("Tests Completed!");
        System.exit(0);
    }

    private static Object assertEqualsAndReturn(Object expected, Object value) {
        if (!Objects.equals(expected, value)) throw new AssertionError("Expected: " + expected + ", Value: " + value);
        return value;
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
