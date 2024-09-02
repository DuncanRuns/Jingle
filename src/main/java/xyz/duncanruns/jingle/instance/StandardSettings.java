package xyz.duncanruns.jingle.instance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class StandardSettings {
    private final Path instancePath;

    private boolean enabled = false;
    private JsonObject json = new JsonObject();

    private Path redirectPath = null;
    private long globalMTime = -1L; // mTime for the standardsettings.global file
    private long fileMTime = -1L;

    public StandardSettings(Path instancePath) {
        this.instancePath = instancePath;
        this.tryUpdate();
    }

    public void tryUpdate() {
        try {
            this.update();
        } catch (Exception e) {
            Jingle.logError("Failed to update standardsettings:", e);
        }
    }

    public Optional<String> getOption(String optionName) {
        if (!(this.enabled && this.json.has(optionName))) return Optional.empty();

        JsonElement option = this.json.get(optionName);

        if (option.isJsonPrimitive()) return Optional.of(option.getAsString());

        if (!option.isJsonObject()) return Optional.empty();

        JsonObject optionJson = option.getAsJsonObject();
        if (!(optionJson.has("enabled") && optionJson.has("value") && optionJson.get("enabled").getAsBoolean())) {
            return Optional.empty();
        }
        return Optional.of(optionJson.get("value").getAsString());
    }

    public void update() throws IOException, JsonSyntaxException {
        Path usedFilePath = this.getUsedFilePath();
        if (!Files.exists(usedFilePath)) return;
        long newFileMTime = Files.getLastModifiedTime(usedFilePath).toMillis();
        if (newFileMTime == this.fileMTime) return;
        this.fileMTime = newFileMTime;
        // Gather values before assigning in case of error!
        JsonObject json = FileUtil.readJson(usedFilePath);
        boolean enabled = this.json.has("toggleStandardSettings") && this.json.get("toggleStandardSettings").getAsBoolean();
        this.json = json;
        this.enabled = enabled;
    }

    public Path getUsedFilePath() throws IOException {
        Path globalFilePath = this.instancePath.resolve("config").resolve("mcsr").resolve("standardsettings.global");
        Path regularLocationPath = globalFilePath.resolveSibling("standardsettings.json");
        if (!Files.exists(globalFilePath)) {
            return regularLocationPath;
        }
        long newGlobalMTime = Files.getLastModifiedTime(globalFilePath).toMillis();
        if (newGlobalMTime != this.globalMTime) {
            this.globalMTime = newGlobalMTime;
            this.redirectPath = null;
            try {
                Path redirectPath = Paths.get(FileUtil.readString(globalFilePath));
                if (Files.exists(redirectPath)) {
                    this.redirectPath = redirectPath;
                }
            } catch (Exception ignored) {
            }
        }
        return this.redirectPath == null ? regularLocationPath : this.redirectPath;
    }
}
