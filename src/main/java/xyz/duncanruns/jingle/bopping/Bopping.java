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
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Bopping {
    private static final Pattern WORLD_NAME_EXTRACTOR = Pattern.compile("^(.+?)(?: \\(\\d+\\))?$");
    private static final Set<String> NEW_WORLD_NAMES = new HashSet<>(Arrays.asList(
            "Nieuwe wereld",
            "Nýggjur heimur",
            "新規ワールド",
            "Naujas pasaulis",
            "Saoghal ùr",
            "Nuwe Wêreld",
            "Ụwa ọhụrụ",
            "Νέος Κόσμος",
            "Neui Welt",
            "ໂລກໃໝ່",
            "Uusi maailma",
            "Ny wärd",
            "Dunia Baharu",
            "Яңа дөнья",
            "Ny värld",
            "Nový svět",
            "Шинэ дэлхий",
            "Nowe wirreld",
            "ಹೊಸ ಪ್ರಪಂಚ",
            "Neie Wöd",
            "Novi svijet",
            "Monde Novèl",
            "Noveu mundeu",
            "Новий світ",
            "ao hou",
            "Яңы донъя",
            "Vinya ambar",
            "Mundo nuevo",
            "Саҥа дойду",
            "新生界",
            "สร้างเวิลด์ใหม่",
            "Lume nouă",
            "Uus maailm",
            "Neue Weld",
            "Nowy śwjot",
            "Nova mondo",
            "Byd Newydd",
            "Nowy świŏt",
            "新的世界",
            "Новий сьвіт",
            "Nei Welt",
            "Նոր Աշխարհ",
            "دنيا بهارو",
            "Nuovo mondo",
            "Új világ",
            "Mundus Nouus",
            "Mond noeuv",
            "Nuevo mundo",
            "Nuie Welt",
            "Ny heim",
            "Novo Mundo",
            "Novy svět",
            "دنیای جدید",
            "Bagong Daigdig",
            "Ođđa máilbmi",
            "Новый світ",
            "Dunida cusub",
            "Gnûf mont",
            "cnino munje",
            "Нов свет",
            "ახალი სამყარო",
            "Hou Honua",
            "Nouveau monde",
            "נייַע וועלט",
            "New Sea",
            "Nýr Heimur",
            "Novo mundo",
            "새로운 세계",
            "Niujis Fairƕus",
            "Mundus novus",
            "Sasendajni narua",
            "Jauna Pasaule",
            "New World",
            "Oshki-aki",
            "Nowy świat",
            "Nieje waereld",
            "Νέος κόσμος",
            "Новый мир",
            "Dinja Ġdida",
            "Нов свят",
            "Nuuje Welt",
            "Novy sviet",
            "עולם חדש",
            "Ođđa Máilbmi",
            "Mondo Novo",
            "Nije wrâld",
            "Dunia Baru",
            "qo' chu'",
            "ma sin",
            "Нови свет",
            "Bys Nowydh",
            "Neo velt",
            "Jauna pasaule",
            "Mundu nuevu",
            "Neue Welt",
            "Seihll Noa",
            "Ny verd",
            "دنيای جديد",
            "Новый міръ",
            "Neu Welt",
            "Thế giới mới",
            "புதிய உலகம்",
            "Doenia baroe",
            "Жаңа дүние",
            "Vinya Ambar",
            "Bagong Mundo",
            "Mundu berria",
            "Yeni Dünya",
            "Жаңа әлем",
            "Neie Weyd",
            "โลกใหม่",
            "Monde nòu",
            "Nov svet",
            "Nie Wiält",
            "New Wurld",
            "नई दुनिया",
            "Nou món",
            "pꞁɹoM ʍǝN",
            "Mundo nuebo",
            "Новы свет",
            "Aye tuntun",
            "Жаңы дүйнө",
            "Yankwīk tlāltipaktli",
            "Bed Nevez",
            "Nový svet",
            "Mundo Nuevo",
            "عالم جديد",
            "Nee Wiält",
            "Ny verden",
            "Hou honua",
            "Munnu novu",
            "New sea",
            "Domhan Nua",
            "Botë e re",
            "Amaḍal amaynut",
            "Ach' balumil",
            "Яңы ғәләм",
            "Novi svet"
    ));

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

        return isNewWorld(name)
                || name.contains("Speedrun #")
                || name.contains("Practice Seed")
                || name.contains("Seed Paster");
    }

    private static boolean isNewWorld(String name) {
        if (name.isEmpty()) return false;
        Matcher matcher = WORLD_NAME_EXTRACTOR.matcher(name);
        if (!matcher.matches()) return false;
        String nameNoNumber = matcher.group(1);
        return NEW_WORLD_NAMES.contains(nameNoNumber);
    }
}
