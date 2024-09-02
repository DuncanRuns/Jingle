package xyz.duncanruns.jingle.instance;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class OptionsTxt {
    private final Path path;
    private long mTime = 0L;
    private final HashMap<String, String> options = new HashMap<>();

    public OptionsTxt(Path optionsTxtPath) {
        this.path = optionsTxtPath;
        this.tryUpdate();
    }

    public void tryUpdate() {
        try {
            this.update();
        } catch (IOException e) {
            Jingle.logError("Failed to update options.txt:", e);
        }
    }

    public void update() throws IOException {
        if (!Files.exists(this.path)) return;

        long newMTime = Files.getLastModifiedTime(this.path).toMillis();
        if (newMTime == this.mTime) return;
        this.mTime = newMTime;

        this.options.clear();
        for (String line : FileUtil.readString(this.path).split("\\n")) {
            line = line.trim();
            int i = line.indexOf(":");
            if (i <= 0) continue;
            this.options.put(line.substring(0, i), line.substring(i + 1));
        }
    }

    public Optional<String> getOption(String optionName) {
        return Optional.ofNullable(this.options.getOrDefault(optionName, null));
    }
}
