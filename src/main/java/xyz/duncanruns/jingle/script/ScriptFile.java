package xyz.duncanruns.jingle.script;

import org.apache.commons.io.IOUtils;
import xyz.duncanruns.jingle.util.FileUtil;
import xyz.duncanruns.jingle.util.ResourceUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ScriptFile {
    public final String contents;
    public final String name;
    public final boolean fromFolder;

    private ScriptFile(String name, String contents, boolean fromFolder) {
        this.name = name;
        this.contents = contents;
        this.fromFolder = fromFolder;
    }

    public static ScriptFile loadFile(Path path) throws IOException {
        String contents = FileUtil.readString(path);
        return new ScriptFile(path.getFileName().toString(), contents, true);
    }

    public static ScriptFile loadResource(String resourceLocation) throws IOException {
        try (InputStream stream = ResourceUtil.getResourceAsStream(resourceLocation)) {
            // Split the resourceLocation by '/' and get the last one for name
            List<String> parts = Arrays.stream(resourceLocation.split("/")).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            String contents = parts.get(parts.size() - 1);
            return new ScriptFile(
                    // Remove .lua for resource loaded script
                    contents.substring(0, contents.length() - 4),
                    // Read the contents of the resource stream to get lua script contents
                    IOUtils.toString(stream, Charset.defaultCharset()),
                    false
            );
        }
    }

    public String getName() {
        return this.name;
    }
}
