package xyz.duncanruns.jingle.gui;

import org.apache.commons.lang3.StringUtils;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.hotkey.Hotkey;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.hotkey.SavedHotkey;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class HotkeyListPanel extends JPanel {
    private final JFrame owner;

    public HotkeyListPanel(JFrame owner) {
        this.owner = owner;
        this.setLayout(new GridBagLayout());
        this.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.reload();
    }

    private static JComponent getButtonSpacer() {
        return new JComponent() {
            private final Dimension size = new Dimension(5, 0);

            @Override
            public Dimension getPreferredSize() {
                return this.size;
            }

            @Override
            public Dimension getMaximumSize() {
                return this.size;
            }

            @Override
            public Dimension getMinimumSize() {
                return this.size;
            }
        };
    }

    public void reload() {
        this.removeAll();
        GridBagConstraints constraints = new GridBagConstraints(-1, 1, 1, 1, 1, 0, 17, 0, new Insets(0, 10, 5, 10), 0, 0);

        List<Hotkey.HotkeyTypeAndAction> possibleActions = Hotkey.getHotkeyActions();

        List<SavedHotkey> savedHotkeys;
        synchronized (Jingle.class) {
            savedHotkeys = Jingle.options == null ? Collections.emptyList() : Jingle.options.copySavedHotkeys();
        }
        boolean hasInvalidHotkeys = false;

        for (final SavedHotkey hotkey : savedHotkeys) {
            constraints.gridy++;

            JLabel actionLabel = new JLabel(String.format("%s (%s)", Jingle.formatAction(hotkey.action), StringUtils.capitalize(hotkey.type)));
            this.add(actionLabel, constraints.clone());
            JLabel setHotkeyLabel = new JLabel((hotkey.ignoreModifiers ? "* " : "") + Hotkey.formatKeys(hotkey.keys));
            this.add(setHotkeyLabel, constraints.clone());
            if (possibleActions.stream().noneMatch(h -> Objects.equals(h.action, hotkey.action) && Objects.equals(h.type, hotkey.type))) {
                actionLabel.setForeground(new Color(255, 0, 0));
//                setHotkeyLabel.setForeground(new Color(255, 0, 0));
                hasInvalidHotkeys = true;
            }

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

            JButton editButton = this.getEditButton(hotkey);
            JButton removeButton = this.getRemoveButton(hotkey);
            buttonsPanel.add(editButton);
            buttonsPanel.add(getButtonSpacer());
            buttonsPanel.add(removeButton);
            this.add(buttonsPanel, constraints.clone());
        }
        if (constraints.gridy == 1) {
            this.add(new JLabel("No hotkeys added!"));
        } else {
            if (hasInvalidHotkeys) {
                constraints.gridy = 0;
                constraints.gridwidth = 4;
                JLabel warningLabel = new JLabel("Warning: some of your hotkeys have an invalid action");
                warningLabel.setForeground(new Color(255, 0, 0));
                this.add(warningLabel, constraints.clone());
                constraints.gridwidth = 1;
            }
            constraints.gridy = 1;
            this.add(new JLabel("Action"), constraints.clone());
            this.add(new JLabel("Hotkey"), constraints.clone());
        }
        this.revalidate();
    }

    private JButton getRemoveButton(SavedHotkey hotkey) {
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(a -> {
            synchronized (Jingle.class) {
                Jingle.options.setSavedHotkeys(Jingle.options.copySavedHotkeys().stream().filter(h -> !h.equals(hotkey)).collect(Collectors.toList()));
                this.reload();
                HotkeyManager.reload();
            }
            JingleGUI.get().refreshHack();
        });
        return removeButton;
    }

    private JButton getEditButton(SavedHotkey hotkey) {
        JButton editButton = new JButton("Edit");
        editButton.addActionListener(a -> {
            synchronized (Jingle.class) {
                EditHotkeyDialog dialog = new EditHotkeyDialog(this.owner, hotkey.action, hotkey.type, hotkey.keys, hotkey.ignoreModifiers);
                dialog.setVisible(true);
                if (dialog.cancelled) return;

                Jingle.options.setSavedHotkeys(Jingle.options.copySavedHotkeys().stream().map(h -> h.equals(hotkey) ? new SavedHotkey(dialog.type, dialog.action, dialog.keys, dialog.ignoreModifiers) : h).collect(Collectors.toList()));

                HotkeyManager.reload();
            }
            this.reload();
            JingleGUI.get().refreshHack();
        });
        return editButton;
    }

}
