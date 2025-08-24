package xyz.duncanruns.jingle.instance;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.*;
import xyz.duncanruns.jingle.util.CommandLineUtil.CommandLineArgs;
import xyz.duncanruns.jingle.win32.User32;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InstanceInfo {
    public final String versionString;
    public final Path instancePath;

    InstanceInfo(String versionString, Path instancePath) {
        this.versionString = versionString;
        this.instancePath = instancePath;
    }

    /**
     * Uses powershell to get the command line of a Minecraft instance and retrieve relevant information about it
     *
     * @param hwnd the window pointer object of the Minecraft instance
     * @return the extracted instance info of the Minecraft instance
     */
    public static InstanceInfo getInstanceInfoFromHwnd(WinDef.HWND hwnd) {
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: getting info from " + hwnd);
        // Get PID from hwnd
        int pid = getPidFromHwnd(hwnd);

        /*
         * Get environment variables, or keep an empty map if it fails
         * Get command line via JNA, or via powershell if it fails, or keep null if it fails
         * If no command line and no environment variables, return null
         * Get instance path via environment variables, or via command line if it fails (first try gameDir then Djava.library.path), return null if it all fails
         * Get version via environment variables, or via command line if it fails, or via mmc-pack.json if it fails, or via game.json if it fails, or default to 1.16.1 if it all fails
         */
        Map<String, String> environmentVariables;
        try {
            environmentVariables = ProcEnvUtil.getEnvironmentVariables(pid);
            Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found environment variables.");
        } catch (Exception e) {
            Jingle.log(Level.ERROR, "Failed to get environment variables: " + ExceptionUtil.toDetailedString(e));
            environmentVariables = Collections.emptyMap();
        }
        String commandLine;
        try {
            commandLine = CommandLineUtil.getCommandLineStringFromPid(pid);
            Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found command line.");
        } catch (Exception e) {
            Jingle.log(Level.ERROR, "Failed to get command line: " + ExceptionUtil.toDetailedString(e));
            commandLine = null;
        }
        if (commandLine == null) {
            try {
                commandLine = getCommandLinePS(pid);
            } catch (Exception e) {
                Jingle.log(Level.ERROR, "Failed to get command line via powershell: " + ExceptionUtil.toDetailedString(e));
            }
        }
        if (commandLine == null && environmentVariables.isEmpty()) {
            return null;
        }
        CommandLineArgs commandLineArgs = CommandLineUtil.getCommandLineArgs(commandLine);

        Path instancePath = Optional.ofNullable(environmentVariables.getOrDefault("INST_MC_DIR", null)).map(Paths::get).orElse(null);
        if (instancePath != null) {
            Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found instance path from environment variables.");
        } else {
            instancePath = Optional.ofNullable(commandLineArgs.options.getOrDefault("gameDir", null)).map(Paths::get).orElse(null);
            if (instancePath != null) {
                Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found instance path from command line.");
            } else {
                // Djava.library.path
                Path libraryPath = Optional.ofNullable(commandLineArgs.options.getOrDefault("Djava.library.path", null)).map(Paths::get).orElse(null);
                if (libraryPath == null) {
                    return null;
                }
                Path dotMinecraftSibling = libraryPath.resolveSibling(".minecraft");
                if (Files.isDirectory(dotMinecraftSibling)) {
                    instancePath = dotMinecraftSibling;
                }
                Path minecraftSibling = libraryPath.resolveSibling("minecraft");
                if (Files.isDirectory(minecraftSibling)) {
                    instancePath = minecraftSibling;
                }
                if (instancePath != null) {
                    Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found instance path from Djava.library.path.");
                } else {
                    return null;
                }
            }
        }
        String versionString = environmentVariables.getOrDefault("INST_MC_VER", null);
        if (versionString != null) {
            Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found version from environment variables.");
        } else {
            versionString = commandLineArgs.options.getOrDefault("version", null);
            if (versionString != null) {
                Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found version from command line.");
            } else {
                try {
                    versionString = getVersionFromMMCPack(instancePath).orElse(null);
                } catch (IOException e) {
                    Jingle.log(Level.ERROR, "Failed to get version from mmc-pack.json: " + ExceptionUtil.toDetailedString(e));
                }
                if (versionString != null) {
                    Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found version from mmc-pack.json.");
                } else {
                    versionString = getVersionWithGameJson(instancePath).orElse(null);
                    if (versionString != null) {
                        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found version from game.json.");
                    } else {
                        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Defaulting to 1.16.1.");
                        versionString = "1.16.1";
                    }
                }
            }
        }
        return new InstanceInfo(versionString, instancePath);

    }

    private static Optional<String> getVersionFromMMCPack(Path instancePath) throws IOException {
        Path mmcPack = instancePath.resolveSibling("mmc-pack.json");
        if (!Files.exists(mmcPack)) return Optional.empty();

        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Found mmc-pack.json (MultiMC/Prism).");
        JsonObject jsonObject = FileUtil.readJson(mmcPack);
        for (String requiredKey : new String[]{"formatVersion", "components"}) {
            if (!jsonObject.has(requiredKey)) {
                Jingle.log(Level.DEBUG, "InstanceInfoUtil: mmc-pack.json does not have " + requiredKey + ".");
                return Optional.empty();
            }
        }
        if (jsonObject.get("formatVersion").getAsInt() != 1) {
            Jingle.log(Level.DEBUG, "InstanceInfoUtil: mmc-pack.json format version is not 1.");
            return Optional.empty();
        }

        return jsonObject.getAsJsonArray("components").asList().stream()
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .filter(j -> j.has("uid"))
                .filter(j -> Objects.equals(j.get("uid").getAsString(), "net.minecraft"))
                .filter(j -> j.has("version"))
                .findFirst()
                .map(j -> j.get("version").getAsString());
    }

    private static Optional<String> getVersionWithGameJson(Path instancePath) {
        try {
            JsonObject json = FileUtil.readJson(instancePath.resolve("game.json"));
            if (json.has("Version")) {
                return Optional.of(json.get("Version").getAsString());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private static int getPidFromHwnd(WinDef.HWND hwnd) {
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Getting PID from " + hwnd);
        final IntByReference pidPointer = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidPointer);
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: PID is " + pidPointer.getValue());
        return pidPointer.getValue();
    }

    private static String getCommandLinePS(int pid) {
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Getting command line from " + pid);
        try {
            return PowerShellUtil.execute("$proc = Get-CimInstance Win32_Process -Filter \"ProcessId = PIDHERE\";$proc.CommandLine".replace("PIDHERE", String.valueOf(pid)));
        } catch (PowerShellExecutionException | IOException e) {
            Jingle.logError("Failed to get command line via powershell.", e);
            return null;
        }
    }

    @Override
    public String toString() {
        return "InstanceInfo{" +
                "versionString='" + this.versionString + '\'' +
                ", instancePath=" + this.instancePath +
                '}';
    }
}
