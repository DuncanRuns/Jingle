package xyz.duncanruns.jingle.script.lua;

import com.google.gson.JsonObject;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.util.FileUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SRIGTEventLuaLibrary extends LuaLibrary {
    private static final Path LATEST_WORLD_JSON_PATH = Paths.get(System.getProperty("user.home")).resolve("speedrunigt").resolve("latest_world.json");

    public SRIGTEventLuaLibrary(@Nullable ScriptFile script, @Nullable Globals globals) {
        super("srigtevent", script, globals);
    }

    @NotALuaFunction
    private static Optional<Path> getLatestWorldEventsLogPath() {
        if (!Files.exists(LATEST_WORLD_JSON_PATH)) return Optional.empty();
        JsonObject jsonObject;
        try {
            jsonObject = FileUtil.readJson(LATEST_WORLD_JSON_PATH);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (!jsonObject.has("world_path")) return Optional.empty();
        try {
            return Optional.of(Paths.get(jsonObject.get("world_path").getAsString()).resolve("speedrunigt").resolve("events.log"));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    private static Optional<String> getLatestEventsLogContents() {
        return getLatestWorldEventsLogPath().map(path -> {
            if (!Files.exists(path)) return null;
            try {
                return FileUtil.readString(path);
            } catch (IOException e) {
                return null;
            }
        });
    }

    @NotALuaFunction
    private static Optional<List<String>> getEvents() {
        return getLatestEventsLogContents().map(contents -> {
            ArrayList<String> out = new ArrayList<>();
            for (String s : contents.split("\\n")) {
                out.add(s.trim());
            }
            out.removeIf(String::isEmpty);
            return out;
        });
    }

    @LuaDocumentation(description = "Returns true if the latest events log file exists and doesn't end in a world exit.")
    public boolean isLatestWorldActive() {
        return getEvents().map(events -> {
            if (events.isEmpty()) return true;
            return !events.get(events.size() - 1).contains("leave_world");
        }).orElse(false);
    }

    @LuaDocumentation(description = "Returns true if the latest events log file has an event line that contains the given substring.")
    public boolean hasEvent(String substring) {
        return getEvents().map(strings -> strings.stream().anyMatch(s -> s.contains(substring))).orElse(false);
    }

    @LuaDocumentation(description = "Returns a table of all events in the latest log file, or an empty table if it could not be retrieved.")
    public LuaTable getEventsTable() {
        return getEvents().map(events -> LuaValue.listOf(events.stream().map(LuaValue::valueOf).collect(Collectors.toList()).toArray(new LuaString[0]))).orElse(LuaValue.tableOf());
    }
}
