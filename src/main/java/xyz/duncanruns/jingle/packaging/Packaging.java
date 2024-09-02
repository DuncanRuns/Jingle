package xyz.duncanruns.jingle.packaging;

import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.instance.OpenedInstance;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.FileUtil;
import xyz.duncanruns.jingle.util.VersionUtil;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Packaging {
    private static final Path SRIGT_LATEST_WORLD_JSON_PATH = Paths.get(System.getProperty("user.home")).resolve("speedrunigt").resolve("latest_world.json");
    private static final Pattern ATUM_WORLD_PATTERN = Pattern.compile("^.*Speedrun #(\\d+)$");

    private Packaging() {
    }

    /**
     * @author DuncanRuns
     * @author draconix6
     */
    public static Path prepareSubmission(OpenedInstance instance) throws IOException, SecurityException, JsonSyntaxException {
        Path savesPath = instance.instancePath.resolve("saves");
        if (!Files.isDirectory(savesPath)) {
            Jingle.log(Level.ERROR, "Saves path for instance not found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        Path logsPath = instance.instancePath.resolve("logs");
        if (!Files.isDirectory(logsPath)) {
            Jingle.log(Level.ERROR, "Logs path for instance not found! Please refer to the speedrun.com rules to submit files yourself.");
            return null;
        }

        boolean hasSeedQueue = instance.fabricModFolder.getInfos().stream().anyMatch(j -> Objects.equals(j.id, "seedqueue"));
        if (hasSeedQueue) {
            if (instance.fabricModFolder.getInfos().stream().noneMatch(j -> Objects.equals(j.id, "speedrunigt") && VersionUtil.tryCompare(j.version.split("\\+")[0], "14.0", -2) >= 0)) {
                Jingle.log(Level.ERROR, "SeedQueue detected without an updated SpeedRunIGT! Please refer to the speedrun.com rules to submit files yourself.");
                return null;
            }
            Jingle.log(Level.DEBUG, "SeedQueue detected, using SeedQueue world yoinking method.");
        }
        // latest world + 5 previous saves or previous 5 worlds + everything after for seedqueue
        List<Path> worldsToCopy = hasSeedQueue ? getLatestWorldsForSQ(savesPath) : getFilesByMostRecent(savesPath);

        if (worldsToCopy.isEmpty()) {
            Jingle.log(Level.ERROR, "No worlds found! Please refer to the speedrun.com rules to submit files yourself.");
            if (hasSeedQueue) {
                Jingle.log(Level.ERROR, "(You are using SeedQueue, so this may be because you selected the wrong instance to package, or your SpeedRunIGT might be out of date!)");
            }
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
            for (Path currentPath : hasSeedQueue ? worldsToCopy : worldsToCopy.subList(0, Math.min(worldsToCopy.size(), 6))) {
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

        Jingle.log(Level.INFO, "Saved submission files for instance to /Jingle/submissionpackages.\r\nPlease submit a download link to your files through this form: https://forms.gle/v7oPXfjfi7553jkp7");

        copyFolderToZip(submissionPath.resolve("Worlds.zip"), submissionPath.resolve("Worlds"));
        copyFolderToZip(submissionPath.resolve("Logs.zip"), submissionPath.resolve("Logs"));

        return submissionPath;
    }

    private static List<Path> getLatestWorldsForSQ(Path savesPath) throws IOException, JsonSyntaxException {
        Path latestWorldPath = Optional.ofNullable(FileUtil.readJson(SRIGT_LATEST_WORLD_JSON_PATH).get("world_path").getAsString()).map(Paths::get).orElse(null);
        if (latestWorldPath == null) {
            return Collections.emptyList();
        }
        List<Path> filesByMostRecent = getFilesByMostRecent(savesPath);
        if (!filesByMostRecent.contains(latestWorldPath)) {
            // Wrong instance!
            return Collections.emptyList();
        }
        Matcher matcher = ATUM_WORLD_PATTERN.matcher(latestWorldPath.getFileName().toString());
        if (!matcher.matches()) {
            // Wrong instance!
            return Collections.emptyList();
        }
        int minimumWorldNum = Integer.parseInt(matcher.group(1)) - 5;
        return filesByMostRecent.stream().filter(path -> {
            Matcher m = ATUM_WORLD_PATTERN.matcher(path.getFileName().toString());
            return m.matches() && Integer.parseInt(m.group(1)) >= minimumWorldNum;
        }).collect(Collectors.toList());
    }

    private static List<Path> getFilesByMostRecent(Path path) {
        return Arrays.stream(Objects.requireNonNull(path.toFile().list())) // Get all world names
                .map(path::resolve) // Map to world paths
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
    }

    private static void copyFolderToZip(Path zipFile, Path sourceFolder) {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            Files.walkFileTree(sourceFolder, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new ZipFileVisitor(zos, sourceFolder));
        } catch (IOException e) {
            Jingle.log(Level.ERROR, "Error while copying zip to folder: \n" + ExceptionUtil.toDetailedString(e));
        }
    }

    private static class ZipFileVisitor extends java.nio.file.SimpleFileVisitor<Path> {

        private final ZipOutputStream zos;
        private final Path sourceFolder;

        public ZipFileVisitor(ZipOutputStream zos, Path sourceFolder) {
            this.zos = zos;
            this.sourceFolder = sourceFolder;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Path relativePath = this.sourceFolder.relativize(file);
            this.zos.putNextEntry(new ZipEntry(relativePath.toString()));
            Files.copy(file, this.zos);
            this.zos.closeEntry();
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }
    }
}
