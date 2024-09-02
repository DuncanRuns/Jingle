package xyz.duncanruns.jingle.instance;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FabricModFolder {
    private static final Gson GSON = new Gson();

    private final Path path;
    private final List<FabricJarInfo> infos;

    public FabricModFolder(Path modsFolder) {
        this.path = modsFolder;
        this.infos = this.tryGetFabricJarInfos();
    }

    private static FabricJarInfo getJarInfo(List<FabricJarInfo> infos, String id) {
        // Filter for any jars with the correct id
        return infos.stream().filter(info -> id.equals(info.id)).findAny().orElse(null);
    }

    private static Optional<FabricJarInfo> getJarInfo(Path jarPath) throws IOException {
        return getJarFMJContents(jarPath).map(s -> GSON.fromJson(s, FabricJarInfo.class));
    }

    @SuppressWarnings("all") //Suppress the redundant cast warning which resolves an ambiguous case
    private static Optional<String> getJarFMJContents(Path jarPath) throws IOException {
        try (FileSystem fs = FileSystems.newFileSystem(jarPath, (ClassLoader) null)) {
            Path jsonFilePath = fs.getPath("fabric.mod.json");
            byte[] jsonData = Files.readAllBytes(jsonFilePath);
            return Optional.of(new String(jsonData, StandardCharsets.UTF_8));
        } catch (NoSuchFileException e) {
            return Optional.empty();
        }
    }

    public List<FabricJarInfo> getInfos() {
        return Collections.unmodifiableList(this.infos);
    }

    private List<FabricJarInfo> tryGetFabricJarInfos() {
        List<FabricJarInfo> infos;
        try {
            infos = this.getAllJarInfos();
        } catch (IOException e) {
            Jingle.logError("Failed to get fabric mod jars:", e);
            infos = Collections.emptyList();
        }
        return infos;
    }

    private List<FabricJarInfo> getAllJarInfos() throws IOException {
        // List files in mod folder -> filter for .jar -> map to jar infos -> return
        if (!Files.isDirectory(this.path)) return Collections.emptyList();
        try (Stream<Path> list = Files.list(this.path)) {
            return list.filter(path -> path.getFileName().toString().endsWith(".jar")).map(path -> {
                try {
                    return getJarInfo(path);
                } catch (IOException e) {
                    Jingle.log(Level.WARN, "Invalid jar " + path.getFileName() + " found in " + this.path + ". Exception below:\n" + ExceptionUtil.toDetailedString(e));
                    return Optional.<FabricJarInfo>empty();
                }
            }).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        }
    }

    public static class FabricJarInfo {
        public String name = null;
        public String id = null;
        public String version = null;

        @Override
        public String toString() {
            return String.format("%s v%s (ID: %s)", this.name, this.version, this.id);
        }
    }
}
