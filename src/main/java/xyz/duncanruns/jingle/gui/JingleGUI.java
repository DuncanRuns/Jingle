package xyz.duncanruns.jingle.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.bopping.Bopping;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.hotkey.SavedHotkey;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;
import xyz.duncanruns.jingle.util.OpenUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class JingleGUI extends JFrame {
    private static final JingleGUI instance = new JingleGUI();
    public JPanel mainPanel;
    public JLabel instanceLabel;
    public JButton openMinecraftFolderButton;
    public JButton clearWorldsButton;
    public JButton goBorderlessButton;
    public JTextArea logTextArea;
    public JTabbedPane mainTabbedPane;
    public JButton clearWorldsFromAllButton;
    public JButton addHotkeyButton;
    private JPanel instancePanel;
    private JTabbedPane pluginsTabbedPane;
    private JPanel noPluginsLoadedTab;
    private JCheckBox showDebugLogsCheckBox;
    private HotkeyListPanel hotkeyListPanel;
    private JCheckBox revertWindowAfterResetCheckBox;
    private JPanel extraButtonsPanel;
    private JButton openJingleFolderButton;
    public ScriptListPanel scriptListPanel;
    private JButton reloadScriptsButton;
    private JButton openScriptsFolderButton;

    public RollingDocument logDocumentWithDebug = new RollingDocument();
    public RollingDocument logDocument = new RollingDocument();

    public JingleGUI() {
        this.$$$setupUI$$$();
        this.finalizeComponents();
        this.setTitle("Jingle v" + Jingle.VERSION);
        this.setContentPane(this.mainPanel);
        this.setPreferredSize(new Dimension(600, 400));
        this.pack();
        this.setLocation(Jingle.options.lastPosition[0], Jingle.options.lastPosition[1]);
        this.setInstance(null);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Point location = JingleGUI.this.getLocation();
                Jingle.options.lastPosition = new int[]{location.x, location.y};
                Jingle.stop();
            }
        });

        this.setVisible(true);
    }

    public static JingleGUI get() {
        return instance;
    }

    @SuppressWarnings("unused")
    public static void addPluginTab(String name, JPanel panel) {
        JingleGUI.get().addPluginTabInternal(name, panel);
    }

    public void setInstance(OpenedInstanceInfo instance) {
        boolean instanceExists = instance != null;
        this.clearWorldsButton.setEnabled(instanceExists);
        this.goBorderlessButton.setEnabled(instanceExists);
        this.openMinecraftFolderButton.setEnabled(instanceExists);
        if (instanceExists) {
            this.instanceLabel.setText("Instance: " + instance.instancePath);
        } else {
            this.instanceLabel.setText("Instance: No instances opened!");
        }
    }

    private void addPluginTabInternal(String name, JPanel panel) {
        this.pluginsTabbedPane.remove(this.noPluginsLoadedTab);
        this.pluginsTabbedPane.add(name, new JScrollPane(panel));
    }

    private void finalizeComponents() {
        this.clearWorldsButton.addActionListener(a -> Bopping.bop(false));
        this.clearWorldsFromAllButton.addActionListener(a -> Bopping.bop(true));
        this.goBorderlessButton.addActionListener(a -> Jingle.goBorderless());
        this.openMinecraftFolderButton.addActionListener(a -> Jingle.openInstanceFolder());

        ((DefaultCaret) this.logTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        this.logTextArea.setDocument(this.logDocument);

        this.showDebugLogsCheckBox.addActionListener(e -> {
            boolean enabled = this.showDebugLogsCheckBox.isSelected();
            this.logTextArea.setDocument(enabled ? this.logDocumentWithDebug : this.logDocument);
            this.jumpToEndOfLog();
        });

        this.mainTabbedPane.addChangeListener(e -> this.jumpToEndOfLog());

        this.addHotkeyButton.addActionListener(e -> {
            EditHotkeyDialog dialog = new EditHotkeyDialog(this, "none", "builtin", Collections.emptyList(), true);
            dialog.setVisible(true);
            if (dialog.cancelled) return;

            synchronized (Jingle.class) {
                SavedHotkey newHotkey = new SavedHotkey(dialog.type, dialog.action, dialog.keys, dialog.ignoreModifiers);
                List<SavedHotkey> hotkeys = Jingle.options.copySavedHotkeys();
                hotkeys.add(newHotkey);
                Jingle.options.setSavedHotkeys(hotkeys);
            }
            this.hotkeyListPanel.reload();
            HotkeyManager.reload();
        });

        this.openJingleFolderButton.addActionListener(a -> OpenUtil.openFile(Jingle.FOLDER.toString()));

        setCheckBoxBoolean(revertWindowAfterResetCheckBox, Jingle.options.revertWindowAfterReset, b -> Jingle.options.revertWindowAfterReset = b);

        this.hotkeyListPanel.reload();
    }

    private void jumpToEndOfLog() {
        this.logTextArea.setCaretPosition(this.logTextArea.getDocument().getLength());
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.setEnabled(true);
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setTabLayoutPolicy(1);
        mainPanel.add(mainTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, new Dimension(0, 0), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainTabbedPane.addTab("Jingle", scrollPane1);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(7, 1, new Insets(5, 5, 5, 5), -1, -1));
        panel1.setEnabled(true);
        panel1.putClientProperty("html.disable", Boolean.FALSE);
        scrollPane1.setViewportView(panel1);
        instancePanel = new JPanel();
        instancePanel.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(instancePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        instanceLabel = new JLabel();
        instanceLabel.setText("Instance: No instances opened!");
        instancePanel.add(instanceLabel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearWorldsButton = new JButton();
        clearWorldsButton.setText("Clear Worlds");
        instancePanel.add(clearWorldsButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        goBorderlessButton = new JButton();
        goBorderlessButton.setText("Go Borderless");
        instancePanel.add(goBorderlessButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openMinecraftFolderButton = new JButton();
        openMinecraftFolderButton.setText("Open Minecraft Folder");
        instancePanel.add(openMinecraftFolderButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Basic Options");
        panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        revertWindowAfterResetCheckBox = new JCheckBox();
        revertWindowAfterResetCheckBox.setEnabled(true);
        revertWindowAfterResetCheckBox.setText("Revert Window after Reset");
        panel1.add(revertWindowAfterResetCheckBox, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extraButtonsPanel = new JPanel();
        extraButtonsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(extraButtonsPanel, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        clearWorldsFromAllButton = new JButton();
        clearWorldsFromAllButton.setText("Clear Worlds from All Instances");
        extraButtonsPanel.add(clearWorldsFromAllButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openJingleFolderButton = new JButton();
        openJingleFolderButton.setText("Open Jingle Folder");
        extraButtonsPanel.add(openJingleFolderButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel1.add(separator2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Log", panel2);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        panel2.add(scrollPane2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        scrollPane2.setViewportView(logTextArea);
        showDebugLogsCheckBox = new JCheckBox();
        showDebugLogsCheckBox.setText("Show Debug Logs");
        panel2.add(showDebugLogsCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Hotkeys", panel3);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel3.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane3.setViewportView(hotkeyListPanel);
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel3.add(separator3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addHotkeyButton = new JButton();
        addHotkeyButton.setText("Add");
        panel4.add(addHotkeyButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Scripts", panel5);
        final Spacer spacer4 = new Spacer();
        panel5.add(spacer4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane4 = new JScrollPane();
        panel5.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane4.setViewportView(scriptListPanel);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openScriptsFolderButton = new JButton();
        openScriptsFolderButton.setText("Open Scripts Folder");
        panel6.add(openScriptsFolderButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reloadScriptsButton = new JButton();
        reloadScriptsButton.setText("Reload Scripts");
        panel6.add(reloadScriptsButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        panel5.add(separator4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pluginsTabbedPane = new JTabbedPane();
        mainTabbedPane.addTab("Plugins", pluginsTabbedPane);
        noPluginsLoadedTab = new JPanel();
        noPluginsLoadedTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pluginsTabbedPane.addTab("No Plugins Loaded", noPluginsLoadedTab);
        final JLabel label2 = new JLabel();
        label2.setText("No Plugins Loaded");
        noPluginsLoadedTab.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
        this.hotkeyListPanel = new HotkeyListPanel(this);
        this.scriptListPanel = new ScriptListPanel();
    }

    private void setCheckBoxBoolean(JCheckBox box, boolean initialValue, Consumer<Boolean> onToggle) {
        box.setSelected(initialValue);
        box.addActionListener(a -> {
            synchronized (Jingle.class) {
                onToggle.accept(box.isSelected());
            }
        });
    }
}
