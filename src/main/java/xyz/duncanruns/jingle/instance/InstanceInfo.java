package xyz.duncanruns.jingle.instance;

import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.FileUtil;
import xyz.duncanruns.jingle.util.PowerShellUtil;
import xyz.duncanruns.jingle.win32.User32;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstanceInfo {
    // Version Patterns
    private static final Pattern VANILLA_VERSION_PATTERN = Pattern.compile(" --version (fabric-loader-\\d\\.\\d+(\\.\\d+)?-)?(.+?) ");
    private static final Pattern MULTIMC_VERSION_PATTERN = Pattern.compile("minecraft-(.+)-client.jar");
    private static final Pattern MULTIMC_VERSION_PATTERN_2 = Pattern.compile("intermediary/(.+)/intermediary");
    // Vanilla Path Patterns
    private static final Pattern VANILLA_PATH_PATTERN = Pattern.compile("--gameDir (.+?) ");
    private static final Pattern VANILLA_PATH_PATTERN_SPACES = Pattern.compile("--gameDir \"(.+?)\"");
    // MultiMC Path Patterns
    private static final Pattern MULTIMC_PATH_PATTERN = Pattern.compile("-Djava\\.library\\.path=(.+?) ");
    private static final Pattern MULTIMC_PATH_PATTERN_SPACES = Pattern.compile("\"-Djava\\.library\\.path=(.+?)\"");

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
        // Get command line
        String commandLine = getCommandLine(getPidFromHwnd(hwnd));
        // If no command line, return null
        if (commandLine == null) {
            Jingle.log(Level.DEBUG, "InstanceInfoUtil: Command line null!");
            return null;
        }
        // Check launcher type
        try {
            if (commandLine.contains("--gameDir")) {
                if (commandLine.contains("-Djava.library.path=")) {
                    //ColorMC
                    Jingle.log(Level.DEBUG, "InstanceInfoUtil: Detected ColorMC launcher.");
                    return getColorMCInfo(commandLine);
                }
                // Vanilla
                Jingle.log(Level.DEBUG, "InstanceInfoUtil: Detected vanilla launcher.");
                return getVanillaInfo(commandLine);
            } else if (commandLine.contains("-Djava.library.path=")) {
                Jingle.log(Level.DEBUG, "InstanceInfoUtil: Detected MultiMC launcher.");
                // MultiMC or Prism
                return getMultiMCInfo(commandLine);
            }
        } catch (Exception e) {
            Jingle.log(Level.ERROR, "An exception occured while obtaining instance information: " + ExceptionUtil.toDetailedString(e));
        }
        // If the command line does not match MultiMC or Vanilla or ColorMC, or if there was an exception, return null
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Command line does not match MultiMC or Vanilla or ColorMC, or there was an exception, returning null");
        return null;
    }

    private static String getCommandLine(int pid) {
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Getting command line from " + pid);
        try {
            return PowerShellUtil.execute("$proc = Get-CimInstance Win32_Process -Filter \"ProcessId = PIDHERE\";$proc.CommandLine".replace("PIDHERE", String.valueOf(pid)));
        } catch (PowerShellExecutionException | IOException e) {
            Jingle.log(Level.ERROR, "Error getting PowerShell output, please send this log in the Jingle discord: " + e.getMessage());
            return null;
        }
    }

    private static int getPidFromHwnd(WinDef.HWND hwnd) {
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: Getting PID from " + hwnd);
        final IntByReference pidPointer = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, pidPointer);
        Jingle.log(Level.DEBUG, "InstanceInfoUtil: PID is " + pidPointer.getValue());
        return pidPointer.getValue();
    }

    private static InstanceInfo getVanillaInfo(String commandLine) throws InvalidPathException {
        // Declare reusable matcher variable
        Matcher matcher;

        // Check for quotation mark to determine matcher
        if (commandLine.contains("--gameDir \"")) {
            matcher = VANILLA_PATH_PATTERN_SPACES.matcher(commandLine);
        } else {
            matcher = VANILLA_PATH_PATTERN.matcher(commandLine);
        }

        // If no matches are found for the path, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the path out of the group
        String pathString = matcher.group(1);

        // Assign the version matcher
        matcher = VANILLA_VERSION_PATTERN.matcher(commandLine);

        // If no matches are found for the version, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the version out of the group
        String versionString = matcher.group(3);

        return new InstanceInfo(versionString, Paths.get(pathString));
    }

    private static InstanceInfo getColorMCInfo(String commandLine) throws InvalidPathException {
        Matcher matcher;

        // Check for quotation mark to determine matcher
        if (commandLine.contains("--gameDir \"")) {
            matcher = VANILLA_PATH_PATTERN_SPACES.matcher(commandLine);
        } else {
            matcher = VANILLA_PATH_PATTERN.matcher(commandLine);
        }

        // If no matches are found for the path, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the path out of the group
        String pathString = matcher.group(1);

        Path instancePath = Paths.get(pathString);
        String version = getVersionWithColorMC(instancePath);
        if (Files.isDirectory(instancePath)) {
            return new InstanceInfo(version, instancePath);
        }

        return null;
    }

    private static String getVersionWithColorMC(Path instancePath) {
        try {
            String contents = FileUtil.readString(instancePath.resolveSibling("game.json"));
            JsonObject json = new Gson().fromJson(contents, JsonObject.class);
            if (json.has("Version")) {
                return json.get("Version").getAsString();
            }
        } catch (Exception ignored) {
        }
        return "1.16.1";
    }

    private static InstanceInfo getMultiMCInfo(String commandLine) throws InvalidPathException {
        Matcher pathFindMatcher;

        // Check for quotation mark to determine matcher
        if (commandLine.contains("\"-Djava.library.path=")) {
            pathFindMatcher = MULTIMC_PATH_PATTERN_SPACES.matcher(commandLine);
        } else {
            pathFindMatcher = MULTIMC_PATH_PATTERN.matcher(commandLine);
        }

        // If no matches are found for the path, return null
        if (!pathFindMatcher.find()) {
            return null;
        }

        // Get the natives path out of the group
        String nativesPathString = pathFindMatcher.group(1);

        String versionString = getVersionWithPattern(commandLine, MULTIMC_VERSION_PATTERN);
        if (versionString == null) {
            versionString = getVersionWithPattern(commandLine, MULTIMC_VERSION_PATTERN_2);
            if (versionString == null) {
                return null;
            }
        }

        Path nativesPath = Paths.get(nativesPathString);
        Path instancePath = nativesPath.resolveSibling(".minecraft");
        if (Files.isDirectory(instancePath)) {
            return new InstanceInfo(versionString, instancePath);
        }
        instancePath = nativesPath.resolveSibling("minecraft"); // New prism launchers will have `minecraft` as the folder name :skull:
        if (Files.isDirectory(instancePath)) {
            return new InstanceInfo(versionString, instancePath);
        }
        return null;
    }

    private static String getVersionWithPattern(String commandLine, Pattern multimcVersionPattern) {
        // Assign the version matcher
        Matcher matcher = multimcVersionPattern.matcher(commandLine);

        // If no matches are found for the version, return null
        if (!matcher.find()) {
            return null;
        }

        // Get the version out of the group
        return matcher.group(1);
    }

    @Override
    public String toString() {
        return "InstanceInfo{" +
                "versionString='" + this.versionString + '\'' +
                ", instancePath=" + this.instancePath +
                '}';
    }
}
