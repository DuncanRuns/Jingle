package xyz.duncanruns.jingle.util;

import com.sun.jna.platform.win32.Shell32;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

public final class OpenUtil {
    private OpenUtil() {
    }

    public static void openFile(String path) {
        Shell32.INSTANCE.ShellExecute(null, "open", path, null, Paths.get(path).getParent().toString(), 1);
    }

    public static void openLink(String url, Component parent) {
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, "Failed to open link. Open a browser and go to " + url, "Jingle: Failed to open link", JOptionPane.ERROR_MESSAGE);
        }
    }
}
