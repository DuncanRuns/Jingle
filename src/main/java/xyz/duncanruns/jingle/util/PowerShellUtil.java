package xyz.duncanruns.jingle.util;

import com.github.tuupertunut.powershelllibjava.PowerShell;
import com.github.tuupertunut.powershelllibjava.PowerShellExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public final class PowerShellUtil {
    private static PowerShell POWER_SHELL = null;

    private PowerShellUtil() {
    }

    private static PowerShell getPowerShell() throws IOException {
        if (POWER_SHELL != null) {
            return POWER_SHELL;
        }
        Optional<Path> powerShellExecutable = Optional.empty();
        try {
            powerShellExecutable = Files.walk(Paths.get("C:\\Windows\\System32\\WindowsPowerShell")).filter(path -> path.getFileName().toString().equals("powershell.exe")).findAny();
        } catch (IOException ignored) {
        }
        POWER_SHELL = powerShellExecutable.isPresent() ? PowerShell.open(powerShellExecutable.get().toString()) : PowerShell.open();
        return POWER_SHELL;
    }

    public static String execute(String command) throws PowerShellExecutionException, IOException {
        return getPowerShell().executeCommands(command);
    }
}
