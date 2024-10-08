package xyz.duncanruns.jingle.bopping;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.gui.JingleGUI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Bopping {
    private Bopping() {
    }

    public static void bop(boolean allSeen) {
        new Thread(() -> {
            synchronized (Bopping.class) {
                JingleGUI.get().clearWorldsButton.setEnabled(false);
                JingleGUI.get().clearWorldsFromAllButton.setEnabled(false);
                Stream<Path> paths;
                if (allSeen) {
                    paths = Jingle.options.seenPaths.keySet().stream()
                            .map(Paths::get);
                } else {
                    paths = Jingle.getMainInstance().map(instance -> Stream.of(instance.instancePath)).orElseGet(Stream::empty);
                }
                paths.map(Path::toAbsolutePath)
                        .distinct()
                        .parallel()
                        .filter(Files::isDirectory)
                        .forEach(path -> {
                            try {
                                clearWorlds(path);
                            } catch (IOException e) {
                                Jingle.logError("Failed to clear worlds:", e);
                            }
                        });
                synchronized (Jingle.class) {
                    JingleGUI.get().clearWorldsButton.setEnabled(Jingle.getMainInstance().isPresent());
                    JingleGUI.get().clearWorldsFromAllButton.setEnabled(true);
                }
            }
        }, "world-bopper").start();
    }


    private static void clearWorlds(Path instancePath) throws IOException {
        Path savesPath = instancePath.resolve("saves");
        // Check if saves folder exists first
        if (!Files.isDirectory(savesPath)) {
            return;
        }
        Jingle.log(Level.INFO, "Finding worlds to delete for \"" + instancePath + "\"...");
        // Get all worlds that are allowed to be deleted

        List<Path> worldsToRemove = Files.list(savesPath) // Get all world paths
                .parallel()
                .map(savesPath::resolve) // Map to world paths
                .filter(Bopping::shouldDelete) // Filter for only ones that should be deleted
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        // Remove the first 36 (or less) worlds
        worldsToRemove.subList(0, Math.min(36, worldsToRemove.size())).clear();
        // Actually delete stuff
        int total = worldsToRemove.size();
        if (worldsToRemove.isEmpty()) {
            Jingle.log(Level.INFO, "No worlds to clear for \"" + instancePath + "\"");
            return;
        }
        AtomicInteger cleared = new AtomicInteger();
        worldsToRemove.parallelStream().forEach(path -> {
            try {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                Jingle.logError("Failed to delete world \"" + path + "\".", e);
            }
            int i = cleared.incrementAndGet();
            if (i % 50 == 0) {
                Jingle.log(Level.INFO, "Clearing \"" + instancePath + "\": " + i + "/" + total);
            }
        });
        Jingle.log(Level.INFO, "Cleared worlds for \"" + instancePath + "\"");
    }

    private static boolean shouldDelete(Path path) {
        if (!path.toFile().isDirectory() || path.resolve("Reset Safe.txt").toFile().isFile()) return false;

        String name = path.getFileName().toString();
        if (name.startsWith("Benchmark Reset #")) return true; // Sometimes benchmark might not have level.dat

        if (!Files.isRegularFile(path.resolve("level.dat"))) return false;
        if (name.startsWith("_")) return false;

        return name.startsWith("New World") || name.contains("Speedrun #") || name.contains("Practice Seed") || name.contains("Seed Paster");
    }
}
