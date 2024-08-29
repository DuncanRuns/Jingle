package xyz.duncanruns.jingle.gui;

import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.hotkey.Hotkey;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HotkeyListPanel extends JPanel {
    private final JFrame owner;

    public HotkeyListPanel(JFrame owner) {
        this.owner = owner;
        this.setLayout(new GridBagLayout());
        this.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.reload();
    }

    public void reload() {
        this.removeAll();
        GridBagConstraints constraints = new GridBagConstraints(-1, 0, 1, 1, 1, 0, 17, 0, new Insets(0, 10, 5, 10), 0, 0);
        List<JsonObject> hotkeys = Optional.ofNullable(Jingle.options).map(o -> o.hotkeys).orElse(Collections.emptyList());

        for (final JsonObject hotkeyJson : hotkeys) {
            if (!(hotkeyJson.has("type") && hotkeyJson.has("action") && hotkeyJson.has("keys") && hotkeyJson.get("keys").isJsonArray()))
                continue;
            String action = hotkeyJson.get("action").getAsString();
            String type = hotkeyJson.get("type").getAsString();
            List<Integer> keys = Hotkey.keysFromJson(hotkeyJson.getAsJsonArray("keys"));

            boolean ignoreModifiers = hotkeyJson.has("ignoreModifiers") && hotkeyJson.get("ignoreModifiers").getAsBoolean();

            constraints.gridy++;
            this.add(new JLabel(String.format("%s (%s)", action, StringUtils.capitalize(type))), constraints.clone());
            this.add(new JLabel((ignoreModifiers ? "* " : "") + Hotkey.formatKeys(keys)), constraints.clone());

            JPanel buttonsPanel = new JPanel();
            buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));

            JButton editButton = new JButton("Edit");
            editButton.addActionListener(a -> {
                EditHotkeyDialog dialog = new EditHotkeyDialog(this.owner, action, type, keys, ignoreModifiers);
                dialog.setVisible(true);
                if (dialog.cancelled) return;
                int i = Jingle.options.hotkeys.indexOf(hotkeyJson);
                hotkeyJson.addProperty("type", dialog.type);
                hotkeyJson.addProperty("action", dialog.action);
                hotkeyJson.addProperty("ignoreModifiers", dialog.ignoreModifiers);
                hotkeyJson.add("keys", Hotkey.jsonFromKeys(dialog.keys));
                Jingle.options.hotkeys.set(i, hotkeyJson);
                this.reload();
                HotkeyManager.reload();
            });
            JButton removeButton = new JButton("Remove");
            removeButton.addActionListener(a -> {
                Jingle.options.hotkeys.remove(hotkeyJson);
                this.reload();
                HotkeyManager.reload();
            });
            buttonsPanel.add(editButton);
            buttonsPanel.add(new JComponent() {
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
            });
            buttonsPanel.add(removeButton);
            this.add(buttonsPanel, constraints.clone());
        }
        if (constraints.gridy == 0) {
            this.add(new JLabel("No hotkeys added!"));
        } else {
            constraints.gridy = 0;
            this.add(new JLabel("Action"), constraints.clone());
            this.add(new JLabel("Hotkey"), constraints.clone());
        }
        this.revalidate();
    }
}
