package xyz.duncanruns.jingle.instance;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InstanceMods {
    private static final Gson GSON = new Gson();

    private final Path path;
    private final List<ModInfo> infos;

    public InstanceMods(Path modsFolder, @Nullable JsonObject hermesInfo) {
        this.path = modsFolder;
        List<ModInfo> infos;
        if (hermesInfo == null) {
            infos = tryGetFabricJarInfos();
        } else {
            infos = fromHermes(hermesInfo);
        }
        this.infos = infos;
    }

    private List<ModInfo> fromHermes(JsonObject hermesInfo) {
        List<JsonObject> modObjs = StreamSupport.stream(hermesInfo.get("mods").getAsJsonArray().spliterator(), false)
                .map(JsonElement::getAsJsonObject)
                .collect(Collectors.toList());
        boolean fabric = modObjs.stream()
                .anyMatch(j -> j.has("id") && j.get("id").getAsString().equals("fabricloader"));
        return modObjs.stream()
                .map(j -> tryHermesModEntryToInfo(j, fabric))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static ModInfo tryHermesModEntryToInfo(JsonObject j, boolean fabric) {
        try {
            return hermesModEntryToInfo(j, fabric);
        } catch (Exception e) {
            Jingle.log(Level.ERROR, "Failed to parse mod origin: " + ExceptionUtil.toDetailedString(e));
            return null;
        }
    }

    private static ModInfo hermesModEntryToInfo(JsonObject j, boolean fabric) {
        ModInfo modInfo = new ModInfo(GSON.fromJson(j, BasicModInfo.class));
        if (!fabric) return modInfo;
        modInfo.fromModsFolder = false;
        if (!j.has("origin")) {
            Jingle.log(Level.DEBUG, "Mod info is missing origin!");
            return modInfo;
        }
        Origin origin = GSON.fromJson(j.get("origin"), Origin.class);
        if (origin.type.equals("PATH")) {
            Origin.PathValue pathValue = GSON.fromJson(origin.value, Origin.PathValue.class);
            modInfo.fromModsFolder = pathValue.relative && pathValue.path.startsWith("mods/");
        }

        return modInfo;
    }

    private static Optional<BasicModInfo> getJarInfo(Path jarPath) throws IOException {
        return getJarFMJContents(jarPath).map(s -> GSON.fromJson(s, BasicModInfo.class));
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

    public List<ModInfo> getInfos() {
        return Collections.unmodifiableList(this.infos);
    }

    public boolean has(String modId) {
        return this.infos.stream().anyMatch(i -> i.id.equals(modId));
    }

    private List<ModInfo> tryGetFabricJarInfos() {
        List<ModInfo> infos;
        try {
            infos = this.getAllJarInfos();
        } catch (IOException e) {
            Jingle.logError("Failed to get fabric mod jars:", e);
            infos = Collections.emptyList();
        }
        return infos;
    }

    private List<ModInfo> getAllJarInfos() throws IOException {
        // List files in mod folder -> filter for .jar -> map to jar infos -> return
        if (!Files.isDirectory(this.path)) return Collections.emptyList();
        try (Stream<Path> list = Files.list(this.path)) {
            return list.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .map(path -> {
                        try {
                            return getJarInfo(path);
                        } catch (IOException e) {
                            Jingle.log(Level.WARN, "Invalid jar " + path.getFileName() + " found in " + this.path + ". Exception below:\n" + ExceptionUtil.toDetailedString(e));
                            return Optional.<ModInfo>empty();
                        }
                    })
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(bmi -> new ModInfo(bmi, true))
                    .collect(Collectors.toList());
        }
    }

    private static class Origin {
        public String type;
        public JsonElement value;

        private static class PathValue {
            public boolean relative;
            public String path;
        }
    }

    public static class BasicModInfo {
        public String name = null;
        public String id = null;
        public String version = null;
    }

    public static class ModInfo extends BasicModInfo {
        public Boolean fromModsFolder; // null = unknown

        public ModInfo(BasicModInfo bmi) {
            this.name = bmi.name;
            this.id = bmi.id;
            this.version = bmi.version;
        }

        public ModInfo(BasicModInfo bmi, Boolean fromModsFolder) {
            this.name = bmi.name;
            this.id = bmi.id;
            this.version = bmi.version;
            this.fromModsFolder = fromModsFolder;
        }

        @Override
        public String toString() {
            return String.format("%s v%s (ID: %s)", this.name, this.version, this.id);
        }
    }
}
