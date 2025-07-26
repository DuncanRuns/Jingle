package xyz.duncanruns.jingle.util;

import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCWorldUtils {
    private MCWorldUtils() {
    }

    public static List<Pair<Path, Long>> getWorldsByCreationTime(Path savesPath) {
        return getWorldsByCreationTime(savesPath, true);
    }

    public static List<Pair<Path, Long>> getWorldsByCreationTime(Path savesPath, boolean requireLevelDat) {
        Stream<Path> pathStream = Arrays.stream(Objects.requireNonNull(savesPath.toFile().list()))
                .parallel()
                .map(savesPath::resolve);
        if (requireLevelDat) pathStream = pathStream.filter(path -> Files.isRegularFile(path.resolve("level.dat")));
        return pathStream
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

    public static long getCreationTime(Path path) throws IOException {
        return Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes().creationTime().toMillis();
    }
}
