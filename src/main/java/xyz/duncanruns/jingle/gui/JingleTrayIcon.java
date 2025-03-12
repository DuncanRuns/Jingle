package xyz.duncanruns.jingle.gui;

import xyz.duncanruns.jingle.Jingle;

import java.awt.*;

import static java.awt.Frame.*;

public class JingleTrayIcon extends TrayIcon {

    public JingleTrayIcon(JingleGUI jingleGUI, Image image) {
        super(image);

        // https://stackoverflow.com/questions/7461477/how-to-hide-a-jframe-in-system-tray-of-taskbar?noredirect=1&lq=1
        this.setImageAutoSize(true);

        this.addActionListener(e -> {
            jingleGUI.setVisible(true);
            jingleGUI.setExtendedState(NORMAL);
        });
        try {
            SystemTray.getSystemTray().add(this);
        } catch (AWTException ignored) {
        }

        jingleGUI.addWindowStateListener(e -> {
            boolean minimizeToTray;
            synchronized (Jingle.class) {
                minimizeToTray = Jingle.options.minimizeToTray;
            }
            if (e.getNewState() == ICONIFIED && minimizeToTray) {
                jingleGUI.setVisible(false);
            }
            if (e.getNewState() == MAXIMIZED_BOTH || e.getNewState() == NORMAL) {
                jingleGUI.setVisible(true);
            }
        });
    }

}
