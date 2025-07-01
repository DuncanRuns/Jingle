package xyz.duncanruns.jingle.packaging;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.util.FileUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class Packaging {
    private Packaging() {
    }

    /**
     * @author DuncanRuns
     * @author draconix6
     */
    public static Path prepareSubmission(Path instancePath) throws IOException, SecurityException, JsonSyntaxException {
        Path savesPath = instancePath.resolve("saves");
        if (!Files.isDirectory(savesPath)) {
            Jingle.log(Level.ERROR, "Saves path for instance not found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        Path logsPath = instancePath.resolve("logs");
        if (!Files.isDirectory(logsPath)) {
            Jingle.log(Level.ERROR, "Logs path for instance not found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        Collection<Path> worldsToCopy = getWorldsUsingLastCompletedRun(savesPath);

        if (worldsToCopy.isEmpty()) {
            Jingle.log(Level.ERROR, "No worlds found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        // save submission to folder
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String submissionFolderName = ("Submission (" + dtf.format(now) + ")")
                .replace(":", "-")
                .replace("/", "-");

        Path submissionPath = Jingle.FOLDER
                .resolve("submissionpackages")
                .resolve(submissionFolderName);
        submissionPath.toFile().mkdirs();
        Jingle.log(Level.INFO, "Created folder for submission.");

        Path savesDest = submissionPath.resolve("Worlds");
        savesDest.toFile().mkdirs();
        try {
            for (Path currentPath : worldsToCopy) {
                File currentSave = currentPath.toFile();
                Jingle.log(Level.INFO, "Copying " + currentSave.getName() + " to submission folder...");
                FileUtils.copyDirectoryToDirectory(currentSave, savesDest.toFile());
            }
        } catch (FileSystemException e) {
            String message = "Cannot package files - a world appears to be open! Please press Options > Stop Resets & Quit in your instance.";
            JOptionPane.showMessageDialog(JingleGUI.get(), message, "Jingle: Package Files Error", JOptionPane.ERROR_MESSAGE);
            Jingle.log(Level.ERROR, message);
            return null;
        }

        // last 3 logs
        List<Path> logsToCopy = getFilesByMostRecent(logsPath);
        File logsDest = submissionPath.resolve("Logs").toFile();
        logsDest.mkdirs();
        for (Path currentPath : logsToCopy.subList(0, Math.min(logsToCopy.size(), 6))) {
            File currentLog = currentPath.toFile();
            Jingle.log(Level.INFO, "Copying " + currentLog.getName() + " to submission folder...");
            FileUtils.copyFileToDirectory(currentLog, logsDest);
        }

        Jingle.log(Level.INFO, "Zipping submission folder...");
        try (ZipFile zipFile = new ZipFile(submissionPath.resolve(submissionFolderName + ".zip").toFile())) {
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionMethod(CompressionMethod.DEFLATE);
            parameters.setCompressionLevel(CompressionLevel.NORMAL);

            zipFile.addFolder(logsDest, parameters);
            zipFile.addFolder(savesDest.toFile(), parameters);
        }

        // Remove folders
        Jingle.log(Level.INFO, "Deleting temporary folders...");
        FileUtils.deleteDirectory(logsDest);
        FileUtils.deleteDirectory(savesDest.toFile());

        Jingle.log(Level.INFO, "Saved submission files for instance to /Jingle/submissionpackages.\r\nPlease submit a download link to your files through this form: https://forms.gle/v7oPXfjfi7553jkp7");

        return submissionPath;
    }

    private static List<Path> getFilesByMostRecent(Path path) {
        return Arrays.stream(Objects.requireNonNull(path.toFile().list()))
                .map(path::resolve)
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
    }

    public static List<Pair<Path, Long>> getWorldsByCreationTime(Path savesPath) {
        return Arrays.stream(Objects.requireNonNull(savesPath.toFile().list()))
                .parallel()
                .map(savesPath::resolve)
                .filter(path -> Files.isRegularFile(path.resolve("level.dat")))
                .map(path -> {
                    try {
                        return Pair.of(path, getCreationTime(path));
                    } catch (IOException e) {
                        return null;
                    }
                }).filter(Objects::nonNull)
                .sorted(Comparator.comparing(Pair::getRight, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    public static Pair<Path, Long> findTargetRun(List<Pair<Path, Long>> worlds) {
        long currentTime = System.currentTimeMillis();
        return worlds.stream()
                .parallel()
                .filter(pair -> currentTime - pair.getRight() < (1000L * 60 * 60 * 48)) // Filter for worlds created in the last 2 days
                .filter(pair -> (Files.isRegularFile(pair.getLeft().resolve("speedrunigt/record.json"))))
                .filter(pair -> {
                    try {
                        JsonObject jsonObject = FileUtil.readJson(pair.getLeft().resolve("speedrunigt/record.json"));
                        if (jsonObject.has("is_completed") && jsonObject.get("is_completed").getAsBoolean())
                            return true;
                        // If any% is completed while the wrong category is selected, a special split log is created:
                        return Files.isRegularFile(pair.getLeft().resolve("speedrunigt/logs/igt_timer_any%_split.log"));
                    } catch (IOException e) {
                        return false;
                    }
                })
                .max(Comparator.comparingLong(Pair::getRight)) // Could use findFirst but this is more defensive
                // ^ Method 1: Find the latest completed run via record.json
                .orElseGet(() -> worlds.isEmpty() ? null : worlds.get(0)); // Method 2: Find the latest world via creation time
    }

    private static Collection<Path> getWorldsUsingLastCompletedRun(final Path savesPath) {
        // Get worlds and find target run
        final List<Pair<Path, Long>> allWorlds = getWorldsByCreationTime(savesPath);
        final Pair<Path, Long> targetRunWithTime = findTargetRun(allWorlds);
        if (targetRunWithTime == null) {
            Jingle.log(Level.ERROR, "Failed to find target run!");
            return Collections.emptyList();
        }
        final Path targetRun = targetRunWithTime.getLeft();
        Jingle.log(Level.INFO, "Target run: " + targetRun.getFileName().toString());
        long targetRunModificationTime;
        try {
            // Folder modification time is not reliable, so we use the level.dat's modification time, which is present in every Minecraft version
            targetRunModificationTime = Files.getLastModifiedTime(targetRun.resolve("level.dat")).toMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Set<Path> outWorlds = new HashSet<Path>() {
            @Override
            public boolean add(Path p) {
                Jingle.log(Level.DEBUG, "Adding " + p);
                return super.add(p);
            }
        };

        // All worlds after the run, including the run itself and the previous 5, up to the creation times equaling the target run's modification time.
        Jingle.log(Level.DEBUG, "Adding all worlds after the target run by creation time, also including the target run and the previous 5");
        int targetRunIndex = allWorlds.indexOf(targetRunWithTime);
        allWorlds.stream()
                .limit(targetRunIndex + 8)
                .filter(pair -> pair.getRight() <= targetRunModificationTime)
                .forEach(pair -> outWorlds.add(pair.getLeft()));

        return outWorlds;
    }

    private static long getCreationTime(Path path) throws IOException {
        return Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes().creationTime().toMillis();
    }
}
