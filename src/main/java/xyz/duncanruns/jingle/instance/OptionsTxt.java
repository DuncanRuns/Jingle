package xyz.duncanruns.jingle.instance;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.FileUtil;
import xyz.duncanruns.jingle.util.MCKeyUtil;
import xyz.duncanruns.jingle.util.MCVersionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;

public class OptionsTxt {
    private final Path path;
    private final boolean pre113;

    private long mTime = 0L;
    private final HashMap<String, String> options = new HashMap<>();

    public OptionsTxt(Path optionsTxtPath, String versionString) {
        this.path = optionsTxtPath;
        this.pre113 = MCVersionUtil.isOlderThan(versionString, "1.13");
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

        // this.options.clear(); // We don't clear in case we are reading a file which is currently being written, instead we can just override options.
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

    public Optional<Integer> getKeyOption(String optionName) {
        if (this.pre113) {
            return this.getOption(optionName).map(MCKeyUtil::getVkFromLWJGL);
        } else {
            return this.getOption(optionName).map(MCKeyUtil::getVkFromMCTranslation);
        }
    }
}
