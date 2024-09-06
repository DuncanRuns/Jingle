package xyz.duncanruns.jingle.gui;

import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.script.ScriptFile;
import xyz.duncanruns.jingle.script.ScriptStuff;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ScriptListPanel extends JPanel {
    public ScriptListPanel() {
        this.setLayout(new GridBagLayout());
        this.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.reload();
    }

    private static JButton getCustomizeButton(ScriptFile loadedScript) {
        Optional<Runnable> customizationFunction = ScriptStuff.getCustomizationFunction(loadedScript.name);
        JButton customizeButton = new JButton("Customize");
        customizeButton.setEnabled(customizationFunction.isPresent());
        customizeButton.addActionListener(e -> customizationFunction.get().run());
        return customizeButton;
    }

    private static JButton getMoreButton(ScriptFile loadedScript) {
        JButton moreButton = new JButton("More...");
        Optional<Map<String, Runnable>> extraFunctions;
        synchronized (Jingle.class) {
            extraFunctions = ScriptStuff.getExtraFunctions(loadedScript.getName());
        }
        moreButton.setEnabled(extraFunctions.isPresent());
        moreButton.addActionListener(a -> {
            JPopupMenu menu = new JPopupMenu();
            extraFunctions.ifPresent(map -> {
                for (Map.Entry<String, Runnable> entry : map.entrySet()) {
                    JMenuItem item = menu.add(new JMenuItem());
                    item.setAction(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            entry.getValue().run();
                        }
                    });
                    item.setText(entry.getKey());
                }
            });
            menu.show(moreButton, 0, 0);
        });

        return moreButton;
    }

    public void reload() {
        this.removeAll();
        GridBagConstraints constraints = new GridBagConstraints(-1, 0, 1, 1, 1, 0, 17, 0, new Insets(0, 5, 5, 5), 0, 0);

        List<ScriptFile> loadedScripts = ScriptStuff.getLoadedScripts();
        loadedScripts = Stream.concat(loadedScripts.stream().filter(s -> !s.fromFolder), loadedScripts.stream().filter(s -> s.fromFolder)).collect(Collectors.toList());
        Set<String> disabledDefaultScripts = Jingle.options.disabledDefaultScripts;
        for (ScriptFile loadedScript : loadedScripts) {
            JLabel nameLabel = new JLabel(loadedScript.name);
            this.add(nameLabel, constraints.clone());
            if (disabledDefaultScripts.contains(loadedScript.name)) {
                nameLabel.setForeground(new Color(128, 128, 128));
            }
            this.add(getCustomizeButton(loadedScript), constraints.clone());
            this.add(getMoreButton(loadedScript), constraints.clone());

            if (!loadedScript.fromFolder) {
                this.add(this.getToggleDefaultScriptButton(loadedScript), constraints.clone());
            }
            constraints.gridy++;
        }
        this.revalidate();
    }

    private JButton getToggleDefaultScriptButton(ScriptFile loadedScript) {
        boolean isDisabled;
        synchronized (Jingle.class) {
            isDisabled = Jingle.options.disabledDefaultScripts.contains(loadedScript.name);
        }
        JButton toggleButton = new JButton(isDisabled ? "Enable" : "Disable");
        toggleButton.addActionListener(a -> {
            synchronized (Jingle.class) {
                if (isDisabled) {
                    Jingle.options.disabledDefaultScripts.remove(loadedScript.name);
                } else {
                    Jingle.options.disabledDefaultScripts.add(loadedScript.name);
                }
                ScriptStuff.reloadScripts();
                HotkeyManager.reload();
            }
            this.reload();
        });
        return toggleButton;
    }
}
