package xyz.duncanruns.jingle.gui;

import com.formdev.flatlaf.ui.FlatBorder;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleOptions;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GuttingWarning {
    public static void check() {
        JingleOptions options = Jingle.options;
        if (!options.seenGuttingWarning) {
            SwingUtilities.invokeLater(GuttingWarning::showWarning);
        }
    }

    private static void showWarning() {
        JingleGUI gui = JingleGUI.get();
        JDialog dialog = new JDialog(gui, "Jingle v2 Info", true);

        JPanel outerPanel = new JPanel();
        dialog.add(outerPanel);
        outerPanel.setBorder(new FlatBorder());

        JPanel innerPanel = new JPanel();
        outerPanel.add(innerPanel);
        innerPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));

        String text = "IMPORTANT\n" +
                "\n" +
                "Resizing and other related features have been REMOVED from\n" +
                "Jingle in favor of Toolscreen. Jingle is now focused on\n" +
                "plugins and scripts typically useful for RSG speedruns.\n" +
                "To get Toolscreen, you can use the button in Jingle's\n" +
                "Community tab to go to the Toolscreen GitHub repository.\n ";
        for (String s : text.split("\\n")) {
            JLabel label = new JLabel(s.isEmpty() ? " " : s);
            label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
            innerPanel.add(label);
        }

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            dialog.dispose();
            synchronized (Jingle.class) {
                Jingle.options.seenGuttingWarning = true;
            }
        });
        okButton.setEnabled(false);
        okButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        innerPanel.add(okButton);


        Timer timer = new Timer(5000, e -> {
            okButton.setEnabled(true);
        });
        timer.setRepeats(false);
        timer.start();

        dialog.setUndecorated(true);
        dialog.pack();

        dialog.setLocationRelativeTo(null);
        dialog.setLocation(gui.getX() + (gui.getWidth() - dialog.getWidth()) / 2, gui.getY() + (gui.getHeight() - dialog.getHeight()) / 2);

        dialog.setVisible(true);
    }
}
