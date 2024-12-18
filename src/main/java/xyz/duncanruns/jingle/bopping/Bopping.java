package xyz.duncanruns.jingle.bopping;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.gui.JingleGUI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
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
                int totalInstances = allSeen ? Jingle.options.seenPaths.size() : 1;
                if (allSeen) {
                    paths = Jingle.options.seenPaths.keySet().stream()
                            .map(Paths::get);
                    totalInstances = Jingle.options.seenPaths.size();
                } else {
                    paths = Jingle.getMainInstance().map(instance -> Stream.of(instance.instancePath)).orElseGet(Stream::empty);
                }
                int cleared = paths.map(Path::toAbsolutePath)
                        .distinct()
                        .filter(Files::isDirectory)
                        .mapToInt(path -> {
                            try {
                                return clearWorlds(path);
                            } catch (IOException e) {
                                Jingle.logError("Failed to clear worlds:", e);
                            }
                            return 0;
                        }).sum();
                Jingle.log(Level.INFO, "Cleared " + cleared + " world" + (cleared == 1 ? "" : "s") + " from " + totalInstances + " instance" + (totalInstances == 1 ? "" : "s") + ".");
                synchronized (Jingle.class) {
                    JingleGUI.get().clearWorldsButton.setEnabled(Jingle.getMainInstance().isPresent());
                    JingleGUI.get().clearWorldsFromAllButton.setEnabled(true);
                }
            }
        }, "world-bopper").start();
    }


    private static int clearWorlds(Path instancePath) throws IOException {
        Path savesPath = instancePath.resolve("saves");
        // Check if saves folder exists first
        if (!Files.isDirectory(savesPath)) {
            return 0;
        }
        // Get all worlds that are allowed to be deleted

        List<Path> worldsToRemove = Files.list(savesPath) // Get all world paths
                .parallel()
                .map(savesPath::resolve) // Map to world paths
                .filter(Bopping::shouldDelete) // Filter for only ones that should be deleted
                /* Sorting notes:
                   - Sorting directly with lastModified takes quite a long time, probably due to either it being run
                     multiple times per world and/or because doing it during sorting ruins parallelism
                   - Sorting based on the world numbers is practically instant, but presents issues with duplicate
                     world names, and mixing set/random/benchmark/demo seeds
                   - Mapping the path into a pair of the path and its modification timestamp seems to be much quicker
                     than directly sorting on timestamps, but still way slower than world numbers because it has to
                     ask the OS/FS for the timestamps. It's an acceptable amount of time (~2 seconds per 50k worlds),
                     so this is this solution that will be used. */
                .map(path -> Pair.of(path, path.toFile().lastModified()))
                .sorted(Comparator.comparingLong(p -> -p.getRight())) // Sort by most recent first
                .map(Pair::getLeft) // map back to the path of the pair
                .skip(36) // Remove the first 36 (or less) worlds
                .collect(Collectors.toList());

        // Actually delete stuff
        int total = worldsToRemove.size();
        if (worldsToRemove.isEmpty()) {
            return 0;
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
            if (cleared.incrementAndGet() % 500 == 0) {
                Thread.currentThread().setName("world-bopper");
                Jingle.log(Level.INFO, String.format("Cleared %d/%d", cleared.get(), total));
            }
        });
        return cleared.get();
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
