package xyz.duncanruns.jingle.bopping;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.instance.OpenedInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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
                AtomicInteger threadNum = new AtomicInteger(0);
                Stream<Path> paths;
                if (allSeen) {
                    paths = Jingle.options.seenPaths.keySet().stream()
                            .map(Paths::get);
                } else {
                    Optional<OpenedInstance> mainInstance = Jingle.getMainInstance();
                    assert mainInstance.isPresent();
                    paths = Stream.of(mainInstance.get().instancePath);
                }
                List<Thread> clearWorldThreads = paths
                        .map(Path::toAbsolutePath)
                        .distinct()
                        .filter(Files::isDirectory)
                        .map(path -> new Thread(() -> {
                            try {
                                Jingle.log(Level.INFO, "Clearing worlds for \"" + path + "\"...");
                                clearWorlds(path);
                            } catch (IOException e) {
                                Jingle.logError("Failed to clear worlds:", e);
                            }
                        }, "world-bopper-" + threadNum.addAndGet(1)))
                        .collect(Collectors.toList());
                clearWorldThreads.forEach(Thread::start);
                clearWorldThreads.forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
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
        // Get all worlds that are allowed to be deleted
        List<Path> worldsToRemove = Arrays.stream(Objects.requireNonNull(savesPath.toFile().list())) // Get all world names
                .map(savesPath::resolve) // Map to world paths
                .filter(Bopping::shouldDelete) // Filter for only ones that should be deleted
                .sorted(Comparator.comparing(value -> value.toFile().lastModified(), Comparator.reverseOrder())) // Sort by most recent first
                .collect(Collectors.toList());
        // Remove the first 6 (or less) worlds
        worldsToRemove.subList(0, Math.min(6, worldsToRemove.size())).clear();
        // Actually delete stuff
        int i = 0;
        int total = worldsToRemove.size();
        for (Path path : worldsToRemove) {
            if (++i % 50 == 0) {
                Jingle.log(Level.INFO, "Clearing \"" + instancePath + "\": " + i + "/" + total);
            }
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private static boolean shouldDelete(Path path) {
        if ((!path.toFile().isDirectory()) || (path.resolve("Reset Safe.txt").toFile().isFile()) || !Files.isRegularFile(path.resolve("level.dat"))) {
            return false;
        }
        String name = path.getFileName().toString();
        if (name.startsWith("_")) {
            return false;
        }
        return name.startsWith("New World") || name.startsWith("Benchmark Reset #") || name.contains("Speedrun #") || name.contains("Practice Seed") || name.contains("Seed Paster");
    }
}
