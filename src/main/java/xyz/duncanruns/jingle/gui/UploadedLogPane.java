package xyz.duncanruns.jingle.gui;

import xyz.duncanruns.jingle.Jingle;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UploadedLogPane extends JEditorPane {

    public UploadedLogPane(String url) {
        super("text/html", String.format("Log has been uploaded to <a href=\"%s\">%s</a>.", url, url));
        addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (IOException | URISyntaxException ex) {
                    Jingle.logError("Failed to open URL:", ex);
                }
            }
        });
        setEditable(false);
        setFocusable(false);
    }

}