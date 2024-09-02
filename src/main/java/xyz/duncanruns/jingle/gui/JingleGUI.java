package xyz.duncanruns.jingle.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.bopping.Bopping;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.hotkey.SavedHotkey;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;
import xyz.duncanruns.jingle.obs.OBSProjector;
import xyz.duncanruns.jingle.packaging.Packaging;
import xyz.duncanruns.jingle.script.ScriptStuff;
import xyz.duncanruns.jingle.util.ExceptionUtil;
import xyz.duncanruns.jingle.util.KeyboardUtil;
import xyz.duncanruns.jingle.util.OpenUtil;
import xyz.duncanruns.jingle.util.ResourceUtil;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private JButton copyScriptPathButton;
    private JCheckBox projectorCheckBox;
    private JCheckBox autoProjectorPosBox;
    private JPanel projectorPositionPanel;
    private JTextField projPosXField;
    private JTextField projPosYField;
    private JTextField projPosWField;
    private JTextField projPosHField;
    private JButton projPosApplyButton;
    private JButton openPluginsFolderButton;
    private JButton donateButton;
    private JLabel supporter1Label;
    private JLabel supporter2Label;
    private JLabel supporter3Label;
    private JButton packageSubmissionFilesButton;

    public RollingDocument logDocumentWithDebug = new RollingDocument();
    public RollingDocument logDocument = new RollingDocument();

    public JingleGUI() {
        this.$$$setupUI$$$();
        this.finalizeComponents();
        this.setTitle("Jingle v" + Jingle.VERSION);
        this.setContentPane(this.mainPanel);
        this.setPreferredSize(new Dimension(600, 400));
        this.setLocation(Jingle.options.lastPosition[0], Jingle.options.lastPosition[1]);
        this.setIconImage(getLogo());
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
        this.pack();
    }

    public static JingleGUI get() {
        return instance;
    }

    @SuppressWarnings("unused")
    public static void addPluginTab(String name, JPanel panel) {
        JingleGUI.get().addPluginTabInternal(name, panel);
    }

    public static Image getLogo() {
        try {
            return ResourceUtil.getImageResource("/logo.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setInstance(OpenedInstanceInfo instance) {
        boolean instanceExists = instance != null;
        this.clearWorldsButton.setEnabled(instanceExists);
        this.goBorderlessButton.setEnabled(instanceExists);
        this.openMinecraftFolderButton.setEnabled(instanceExists);
        this.packageSubmissionFilesButton.setEnabled(instanceExists);
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

        this.setCheckBoxBoolean(this.revertWindowAfterResetCheckBox, Jingle.options.revertWindowAfterReset, b -> Jingle.options.revertWindowAfterReset = b);

        this.openScriptsFolderButton.addActionListener(a -> OpenUtil.openFile(Jingle.FOLDER.resolve("scripts").toString()));
        this.reloadScriptsButton.addActionListener(a -> {
            synchronized (Jingle.class) {
                ScriptStuff.reloadScripts();
                HotkeyManager.reload();
                this.scriptListPanel.reload();
            }
        });

        this.finalizeOBSComponents();

        this.openPluginsFolderButton.addActionListener(a -> OpenUtil.openFile(Jingle.FOLDER.resolve("plugins").toString()));

        this.donateButton.addActionListener(a -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://ko-fi.com/duncanruns"));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to open link. Donations can be done at https://ko-fi.com/duncanruns.", "Jingle: Failed to open link", JOptionPane.ERROR_MESSAGE, new ImageIcon(getLogo()));
            }
        });

        this.packageSubmissionFilesButton.addActionListener(a -> {
            Jingle.getMainInstance().ifPresent(i -> {
                try {
                    Path path = Packaging.prepareSubmission(i);
                    if (path != null) {
                        OpenUtil.openFile(path.toString());
                    }
                } catch (IOException e) {
                    Jingle.logError("Preparing File Submission Failed:", e);
                    JOptionPane.showMessageDialog(this, "Preparing File Submission Failed:\n" + ExceptionUtil.toDetailedString(e), "Jingle: Packaging failed", 0, new ImageIcon(this.getIconImage()));
                }
            });
        });

        this.hotkeyListPanel.reload();
    }

    private void finalizeOBSComponents() {
        this.copyScriptPathButton.addActionListener(a -> {
            try {
                KeyboardUtil.copyToClipboard(Jingle.FOLDER.resolve("jingle-obs-link.lua").toString());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to copy to clipboard: " + ExceptionUtil.toDetailedString(e));
            }
        });
        JTextField[] ppFields = new JTextField[]{this.projPosXField, this.projPosYField, this.projPosWField, this.projPosHField};

        if (Jingle.options.projectorPosition != null) {
            for (int i = 0; i < 4; i++) {
                ppFields[i].setText("" + Jingle.options.projectorPosition[i]);
            }
        }

        this.refreshPPFields(ppFields);

        this.setCheckBoxBoolean(this.projectorCheckBox, Jingle.options.projectorEnabled, b -> {
            Jingle.options.projectorEnabled = b;
            this.refreshPPFields(ppFields);
        });
        this.setCheckBoxBoolean(this.autoProjectorPosBox, Jingle.options.projectorPosition == null, b -> {
            if (b) {
                Jingle.options.projectorPosition = null;
                for (JTextField ppField : ppFields) {
                    ppField.setText("");
                    ppField.setEditable(false);
                    ppField.setEnabled(false);
                    this.projPosApplyButton.setEnabled(false);
                }
            } else {
                Rectangle pp = OBSProjector.getProjectorPosition();
                Jingle.options.projectorPosition = new int[]{pp.x, pp.y, pp.width, pp.height};
                for (int i = 0; i < 4; i++) {
                    ppFields[i].setText("" + Jingle.options.projectorPosition[i]);
                    ppFields[i].setEditable(true);
                    ppFields[i].setEnabled(true);
                }
                this.projPosApplyButton.setEnabled(true);
            }
            OBSProjector.applyProjectorPosition();
        });
        this.projPosApplyButton.addActionListener(a -> {
            int[] newPos = new int[4];
            for (int i = 0; i < 4; i++) {
                JTextField ppField = ppFields[i];
                String text = ppField.getText();
                char[] charArray = text.toCharArray();
                boolean isNegative = text.startsWith("-");
                String numbers = IntStream.range(0, charArray.length).mapToObj(ci -> charArray[ci])
                        .filter(Character::isDigit)
                        .map(String::valueOf)
                        .collect(Collectors.joining());
                int val = numbers.isEmpty() ? 0 : (isNegative ? -1 : 1) * Integer.parseInt(numbers);
                ppField.setText("" + val);
                newPos[i] = val;
            }
            OBSProjector.setProjectorPosition(newPos[0], newPos[1], newPos[2], newPos[3]);
        });
    }

    private void refreshPPFields(JTextField[] ppFields) {
        boolean projectorEnabled = Jingle.options.projectorEnabled;
        this.autoProjectorPosBox.setEnabled(projectorEnabled);
        boolean positionIsCustomizable = projectorEnabled && !(Jingle.options.projectorPosition == null);
        for (JTextField ppField : ppFields) {
            ppField.setEnabled(positionIsCustomizable);
            ppField.setEditable(positionIsCustomizable);
        }
        this.projPosApplyButton.setEnabled(positionIsCustomizable);
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(7, 1, new Insets(5, 5, 5, 5), -1, -1));
        panel1.setEnabled(true);
        panel1.putClientProperty("html.disable", Boolean.FALSE);
        mainTabbedPane.addTab("Jingle", panel1);
        instancePanel = new JPanel();
        instancePanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(instancePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        instanceLabel = new JLabel();
        instanceLabel.setText("Instance: No instances opened!");
        instancePanel.add(instanceLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearWorldsButton = new JButton();
        clearWorldsButton.setText("Clear Worlds");
        instancePanel.add(clearWorldsButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        goBorderlessButton = new JButton();
        goBorderlessButton.setText("Go Borderless");
        instancePanel.add(goBorderlessButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        packageSubmissionFilesButton = new JButton();
        packageSubmissionFilesButton.setText("Package Submission Files");
        instancePanel.add(packageSubmissionFilesButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openMinecraftFolderButton = new JButton();
        openMinecraftFolderButton.setText("Open Minecraft Folder");
        instancePanel.add(openMinecraftFolderButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(2, 2, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Log", panel2);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel2.add(scrollPane1, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        scrollPane1.setViewportView(logTextArea);
        showDebugLogsCheckBox = new JCheckBox();
        showDebugLogsCheckBox.setText("Show Debug Logs");
        panel2.add(showDebugLogsCheckBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Hotkeys", panel3);
        final JScrollPane scrollPane2 = new JScrollPane();
        panel3.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane2.setViewportView(hotkeyListPanel);
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
        final JScrollPane scrollPane3 = new JScrollPane();
        panel5.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane3.setViewportView(scriptListPanel);
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
        final JScrollPane scrollPane4 = new JScrollPane();
        mainTabbedPane.addTab("OBS", scrollPane4);
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(12, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane4.setViewportView(panel7);
        final Spacer spacer5 = new Spacer();
        panel7.add(spacer5, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("OBS Link Script Installation:");
        panel7.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("1. Open OBS, at the top, click Tools, and then Scripts.");
        panel7.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("2. Check if jingle-obs-link.lua is listed under \"Loaded Scripts\", in this case you are already done.");
        panel7.add(label4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel7.add(panel8, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("3. Press this button:");
        panel8.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyScriptPathButton = new JButton();
        copyScriptPathButton.setText("Copy Script Path");
        panel8.add(copyScriptPathButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("5. Press the bottom bar and press Ctrl+V to paste the script path, then press Open to add the script.");
        panel7.add(label6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        panel7.add(separator5, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        projectorCheckBox = new JCheckBox();
        projectorCheckBox.setText("Enable Eye Measuring Projector");
        panel7.add(projectorCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoProjectorPosBox = new JCheckBox();
        autoProjectorPosBox.setText("Automatically Position Eye Measuring Projector");
        panel7.add(autoProjectorPosBox, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectorPositionPanel = new JPanel();
        projectorPositionPanel.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
        projectorPositionPanel.setEnabled(true);
        panel7.add(projectorPositionPanel, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        projPosXField = new JTextField();
        projectorPositionPanel.add(projPosXField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Position:");
        projectorPositionPanel.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Size:");
        projectorPositionPanel.add(label8, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projPosYField = new JTextField();
        projectorPositionPanel.add(projPosYField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosWField = new JTextField();
        projectorPositionPanel.add(projPosWField, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosHField = new JTextField();
        projectorPositionPanel.add(projPosHField, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosApplyButton = new JButton();
        projPosApplyButton.setText("Apply");
        projectorPositionPanel.add(projPosApplyButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("4. Click on the + icon at the bottom left of the OBS Scripts window.");
        panel7.add(label9, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("6. Press the \"Regenerate\" button in the jingle-obs-link.lua script.");
        panel7.add(label10, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainTabbedPane.addTab("Plugins", panel9);
        pluginsTabbedPane = new JTabbedPane();
        panel9.add(pluginsTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        noPluginsLoadedTab = new JPanel();
        noPluginsLoadedTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pluginsTabbedPane.addTab("No Plugins Loaded", noPluginsLoadedTab);
        final JLabel label11 = new JLabel();
        label11.setText("No Plugins Loaded");
        noPluginsLoadedTab.add(label11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 5, 5), -1, -1));
        panel9.add(panel10, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openPluginsFolderButton = new JButton();
        openPluginsFolderButton.setText("Open Plugins Folder");
        panel10.add(openPluginsFolderButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(6, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Donate", panel11);
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel12, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setForeground(new Color(-14894848));
        label12.setOpaque(false);
        label12.setText("Support Jingle:");
        label12.putClientProperty("html.disable", Boolean.FALSE);
        panel12.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        donateButton = new JButton();
        donateButton.setText("Donate");
        panel12.add(donateButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("Thank you supporters!");
        panel11.add(label13, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel11.add(panel13, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        supporter1Label = new JLabel();
        supporter1Label.setText(" ");
        panel13.add(supporter1Label, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        supporter2Label = new JLabel();
        supporter2Label.setText(" ");
        panel13.add(supporter2Label, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        supporter3Label = new JLabel();
        supporter3Label.setText(" ");
        panel13.add(supporter3Label, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel11.add(spacer6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel11.add(spacer7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel11.add(spacer8, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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

    public void showSupporters(String[] supporters) {
        AtomicInteger lastShownSupporter = new AtomicInteger(-1);
        Supplier<String> nextSupporterSupplier = () -> {
            int i = lastShownSupporter.incrementAndGet();
            if (i >= supporters.length) {
                i = 0;
                lastShownSupporter.set(0);
            }
            return supporters[i];
        };
        Runnable showNext = () -> {
            this.supporter1Label.setText(nextSupporterSupplier.get());
            this.supporter2Label.setText(nextSupporterSupplier.get());
            this.supporter3Label.setText(nextSupporterSupplier.get());
        };

        showNext.run();
        new Timer(5000, a -> showNext.run()).start();
    }
}
