package xyz.duncanruns.jingle.script;

import xyz.duncanruns.jingle.util.FileUtil;

import java.io.IOException;
import java.nio.file.Path;

public class ScriptFile {
    public final Path path;
    public final String contents;
    public final String name;

    public ScriptFile(Path path, String contents) {
        this.path = path;
        this.contents = contents;
        this.name = path.getFileName().toString();
    }

    public static ScriptFile load(Path path) throws IOException {
        String contents = FileUtil.readString(path);
        return new ScriptFile(path, contents);
    }

    public String getName() {
        return this.name;
    }
}
