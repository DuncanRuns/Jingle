package xyz.duncanruns.jingle.bopping;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.util.FileUtil;
import xyz.duncanruns.jingle.util.MCWorldUtils;

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
import java.util.Arrays;

public final class Bopping {

    private static final List<String> NEW_WORLD_NAMES = Arrays.asList(
        // English
        "New World", 
        // Western European (French, Spanish, German, Italian, Portuguese)
        "Nouveau monde", "Nuevo mundo", "Neue Welt", "Nuovo mondo", "Mundo novo", 
        // Eastern European (Ukrainian, Polish, Czech)
        "Новий світ", "Nowy Świat", "Nový svět",
        // Asian Languages (Japanese, Chinese Simp., Chinese Trad., Korean)
        "新たなワールド", "新的世界", "新世界", "새로운 세계",
        // Northern European (Dutch, Swedish, Danish, Finnish)
        "Nieuwe wereld", "Ny värld", "Ny verden", "Uusi maailma"
    );

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
                    paths = Jingle.getLatestInstancePath().map(Stream::of).orElse(Stream.empty());
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
                    JingleGUI.get().clearWorldsButton.setEnabled(Jingle.getLatestInstancePath().isPresent());
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

        // (Don't require level.dat for now, we will check it later so that we can also clear Benchmark worlds)
        List<Pair<Path, Long>> allWorlds = MCWorldUtils.getWorldsByCreationTime(savesPath, false);

        List<Path> worldsToRemove = allWorlds.stream()
                .parallel()
                .filter(Bopping::shouldDelete) // Filter for only ones that should be deleted
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

    private static boolean shouldDelete(Pair<Path, Long> pair) {
        // If the world was created within the last 2 days, and has is_completed = true, keep it
        if (System.currentTimeMillis() - pair.getRight() < 1000L * 60 * 60 * 48) {
            try {
                if (Files.isRegularFile(pair.getLeft().resolve("speedrunigt/record.json"))) {
                    JsonObject jsonObject = FileUtil.readJson(pair.getLeft().resolve("speedrunigt/record.json"));
                    if (jsonObject.has("is_completed") && jsonObject.get("is_completed").getAsBoolean()) {
                        return false;
                    }
                }
            } catch (IOException e) {
                Jingle.logError("Failed to read record.json for " + pair.getLeft() + ".", e);
            }
        }

        Path path = pair.getLeft();
        if (!path.toFile().isDirectory() || path.resolve("Reset Safe.txt").toFile().isFile()) return false;

        String name = path.getFileName().toString();
        if (name.startsWith("Benchmark Reset #")) return true; // Sometimes benchmark might not have level.dat

        if (!Files.isRegularFile(path.resolve("level.dat"))) return false;
        if (name.startsWith("_")) return false;

        return NEW_WORLD_NAMES.stream().anyMatch(name::startsWith)
                || name.contains("Speedrun #")
                || name.contains("Practice Seed")
                || name.contains("Seed Paster");
    }
}
