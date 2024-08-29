package xyz.duncanruns.jingle.gui;

import com.google.gson.JsonObject;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.bopping.Bopping;
import xyz.duncanruns.jingle.hotkey.Hotkey;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;

public class JingleGUI extends JFrame {
    private static final JingleGUI instance = new JingleGUI();
    public JPanel mainPanel;
    public JLabel instanceLabel;
    public JButton openMinecraftFolderButton;
    public JButton clearWorldsButton;
    public JButton goBorderlessButton;
    public JTextArea logTextArea;
    private JPanel instancePanel;
    public JButton clearWorldsFromAllButton;
    private JTabbedPane pluginsTabbedPane;
    private JPanel noPluginsLoadedTab;
    private JTabbedPane mainTabbedPane;
    private JCheckBox showDebugLogsCheckBox;
    private HotkeyListPanel hotkeyListPanel;
    private JButton addHotkeyButton;

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
            JsonObject savedHotkey = new JsonObject();
            savedHotkey.addProperty("type", dialog.type);
            savedHotkey.addProperty("action", dialog.action);
            savedHotkey.add("keys", Hotkey.jsonFromKeys(dialog.keys));
            savedHotkey.addProperty("ignoreModifiers", dialog.ignoreModifiers);
            Jingle.options.hotkeys.add(savedHotkey);
            this.hotkeyListPanel.reload();
            HotkeyManager.reload();
        });

        this.reloadHotkeys();
    }

    private void reloadHotkeys() {
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
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        panel1.setEnabled(true);
        panel1.putClientProperty("html.disable", Boolean.FALSE);
        scrollPane1.setViewportView(panel1);
        instancePanel = new JPanel();
        instancePanel.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(instancePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        instancePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
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
        panel1.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        clearWorldsFromAllButton = new JButton();
        clearWorldsFromAllButton.setText("Clear Worlds from All Instances");
        panel1.add(clearWorldsFromAllButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Hotkeys", panel3);
        final JScrollPane scrollPane3 = new JScrollPane();
        panel3.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane3.setViewportView(hotkeyListPanel);
        final Spacer spacer3 = new Spacer();
        panel3.add(spacer3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        addHotkeyButton = new JButton();
        addHotkeyButton.setText("Add");
        panel3.add(addHotkeyButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainTabbedPane.addTab("Plugins", panel4);
        pluginsTabbedPane = new JTabbedPane();
        panel4.add(pluginsTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        noPluginsLoadedTab = new JPanel();
        noPluginsLoadedTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pluginsTabbedPane.addTab("No Plugins Loaded", noPluginsLoadedTab);
        final JLabel label1 = new JLabel();
        label1.setText("No Plugins Loaded");
        noPluginsLoadedTab.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private void createUIComponents() {
        this.hotkeyListPanel = new HotkeyListPanel(this);
    }
}
