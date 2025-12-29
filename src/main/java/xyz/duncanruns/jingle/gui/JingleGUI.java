package xyz.duncanruns.jingle.gui;

import com.google.gson.JsonObject;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleOptions;
import xyz.duncanruns.jingle.JingleUpdater;
import xyz.duncanruns.jingle.bopping.Bopping;
import xyz.duncanruns.jingle.hotkey.HotkeyManager;
import xyz.duncanruns.jingle.hotkey.SavedHotkey;
import xyz.duncanruns.jingle.instance.OpenedInstanceInfo;
import xyz.duncanruns.jingle.obs.OBSProjector;
import xyz.duncanruns.jingle.packaging.Packaging;
import xyz.duncanruns.jingle.script.ScriptStuff;
import xyz.duncanruns.jingle.util.*;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JingleGUI extends JFrame {
    private static final int MAX_INSTANCE_PATH_DISPLAY_LENGTH = 50;
    private static JingleGUI instance = null;
    private static BufferedImage logo = null;
    private final List<Pair<Integer, Supplier<JButton>>> quickActionButtonSuppliers = new ArrayList<>();
    public JPanel mainPanel;
    public JLabel instanceLabel;
    public JButton openMinecraftFolderButton;
    public JButton clearWorldsButton;
    public JButton goBorderlessButton;
    public JTextArea logTextArea;
    public JTabbedPane mainTabbedPane;
    public JButton clearWorldsFromAllButton;
    public JButton addHotkeyButton;
    public ScriptListPanel scriptListPanel;
    public boolean jingleUpdating = false;
    public RollingDocument logDocumentWithDebug = new RollingDocument();
    public RollingDocument logDocument = new RollingDocument();
    private JPanel instancePanel;
    private JTabbedPane pluginsTabbedPane;
    private JPanel noPluginsLoadedTab;
    private JButton uploadLogButton;
    private JCheckBox showDebugLogsCheckBox;
    private HotkeyListPanel hotkeyListPanel;
    private JPanel extraButtonsPanel;
    private JButton openJingleFolderButton;
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
    private JCheckBox preReleaseCheckBox;
    private JCheckBox checkForUpdatesCheckBox;
    private JCheckBox minimizeToTrayCheckBox;
    private JPanel hotkeysJPanel;
    private JPanel logJPanel;
    private JPanel scriptsJPanel;
    private JTextField projWindowPatternField;
    private JPanel pluginsTab;
    private JCheckBox minimizeProjectorBox;
    private JButton resetProjNameButton;
    private JButton customizeBorderlessButton;
    private JCheckBox autoBorderlessCheckBox;
    private JPanel quickActionsPanel;
    private JPanel communityButtonsPanel;
    private JLabel communityButtonsLabel;

    private JingleGUI() {
        this.$$$setupUI$$$();
        this.finalizeComponents();
        this.setTitle("Jingle v" + Jingle.VERSION);
        this.setContentPane(this.mainPanel);
        this.initPosition(new Point(Jingle.options.lastPosition[0], Jingle.options.lastPosition[1]), new Dimension(Jingle.options.lastSize[0], Jingle.options.lastSize[1]));
        this.setIconImage(getLogo());
        this.noInstanceYet();
        JingleTrayIcon jingleTrayIcon = new JingleTrayIcon(this, getLogo());
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SystemTray.getSystemTray().remove(jingleTrayIcon);
                Point location = JingleGUI.this.getLocation();
                Dimension size = JingleGUI.this.getSize();
                Jingle.options.lastPosition = new int[]{location.x, location.y};
                Jingle.options.lastSize = new int[]{size.width, size.height};
                if (!JingleGUI.this.jingleUpdating) Jingle.stop(true);
            }
        });

        this.pack();
        this.setVisible(true);
    }

    public static boolean instanceExists() {
        return instance != null;
    }

    public static synchronized JingleGUI get() {
        if (instance == null) {
            try {
                SwingUtilities.invokeAndWait(() -> instance = new JingleGUI());
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    @SuppressWarnings("unused")
    public static void addPluginTab(String name, JPanel panel) {
        addPluginTab(name, panel, null);
    }

    public static void addPluginTab(String name, JPanel panel, Runnable onSwitchTo) {
        JingleGUI.get().addPluginTabInternal(name, panel, onSwitchTo);
    }

    public static Image getLogo() {
        if (logo != null) return logo;
        try {
            logo = ResourceUtil.getImageResource("/logo.png");
            return logo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void setCheckBoxBoolean(JCheckBox box, boolean initialValue, Consumer<Boolean> onToggle) {
        box.setSelected(initialValue);
        box.addActionListener(a -> {
            synchronized (Jingle.class) {
                onToggle.accept(box.isSelected());
            }
        });
    }

    public static void setTextFieldFunction(JTextField field, String initialValue, Consumer<String> onChange) {
        field.setText(initialValue);
        AtomicReference<String> last = new AtomicReference<>(initialValue);
        field.addKeyListener(new KeyAdapter() {
            private void update() {
                String currentText = field.getText();
                if (!Objects.equals(currentText, last.get())) {
                    last.set(currentText);
                    synchronized (Jingle.class) {
                        onChange.accept(currentText);
                    }
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                this.update();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                this.update();
            }
        });
    }

    @SuppressWarnings("unused")
    public static JButton makeButton(String text, @Nullable Runnable onClick, @Nullable Runnable onRightClick, @Nullable String toolTipText, @Nullable Boolean enabled) {
        JButton button = new JButton(text);
        if (onClick != null) button.addActionListener(a -> onClick.run());
        if (onRightClick != null) button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) onRightClick.run();
            }
        });
        if (toolTipText != null) button.setToolTipText(toolTipText);
        if (enabled != null) button.setEnabled(enabled);
        return button;
    }

    @SuppressWarnings("unused")
    public void registerQuickActionButton(int priority, Supplier<JButton> buttonSupplier) {
        this.quickActionButtonSuppliers.add(Pair.of(-priority, buttonSupplier));
    }

    public void setInstance(OpenedInstanceInfo instance) {
        this.setInstance(instance == null ? null : instance.instancePath, instance != null);
    }

    public void setInstance(Path instancePath, boolean open) {
        boolean exists = instancePath != null;
        this.clearWorldsButton.setEnabled(exists);
        this.goBorderlessButton.setEnabled(open);
        this.openMinecraftFolderButton.setEnabled(exists);
        this.packageSubmissionFilesButton.setEnabled(exists);
        if (exists) {
            String instancePathString = instancePath.toString();
            if (instancePathString.length() > MAX_INSTANCE_PATH_DISPLAY_LENGTH) {
                instancePathString = "..." + instancePathString.substring(instancePathString.length() - MAX_INSTANCE_PATH_DISPLAY_LENGTH + 3);
            }
            this.instanceLabel.setText((open ? "Instance: " : "Instance (Closed): ") + instancePathString);
        } else {
            this.instanceLabel.setText("No instances ever opened!");
        }
    }

    private void noInstanceYet() {
        this.clearWorldsButton.setEnabled(false);
        this.goBorderlessButton.setEnabled(false);
        this.openMinecraftFolderButton.setEnabled(false);
        this.packageSubmissionFilesButton.setEnabled(false);
        this.instanceLabel.setText("Loading...");
    }

    private void addPluginTabInternal(String name, JPanel panel, Runnable onSwitchTo) {
        this.pluginsTabbedPane.remove(this.noPluginsLoadedTab);
        JScrollPane tab = new JScrollPane(panel);
        this.pluginsTabbedPane.add(name, tab);
        if (onSwitchTo == null) return;
        this.pluginsTabbedPane.addChangeListener(e -> {
            if (this.pluginsTabbedPane.getSelectedComponent() == tab) onSwitchTo.run();
        });
        this.mainTabbedPane.addChangeListener(e -> {
            if (this.mainTabbedPane.getSelectedComponent() == this.pluginsTab && this.pluginsTabbedPane.getSelectedComponent() == tab)
                onSwitchTo.run();
        });
    }

    private void finalizeComponents() {
        this.clearWorldsButton.addActionListener(a -> Bopping.bop(false));
        this.clearWorldsFromAllButton.addActionListener(a -> Bopping.bop(true));
        this.goBorderlessButton.addActionListener(a -> Jingle.goBorderless());
        this.goBorderlessButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) customizeBorderless();
            }
        });
        this.goBorderlessButton.setToolTipText("Right Click to Configure");
        this.openMinecraftFolderButton.addActionListener(a -> Jingle.openInstanceFolder());

        ((DefaultCaret) this.logTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        this.logTextArea.setDocument(this.logDocument);

        this.showDebugLogsCheckBox.addActionListener(e -> {
            boolean enabled = this.showDebugLogsCheckBox.isSelected();
            this.logTextArea.setDocument(enabled ? this.logDocumentWithDebug : this.logDocument);
            this.jumpToEndOfLog();
        });

        this.uploadLogButton.addActionListener(a -> {
            this.uploadLogButton.setEnabled(false);

            new Thread(() -> {
                try {
                    JsonObject response = MCLogsUtil.uploadLog(Jingle.FOLDER.resolve("logs").resolve("latest.log"));
                    if (response.get("success").getAsBoolean()) {
                        String url = response.get("url").getAsString();
                        Object[] options = new Object[]{"Copy URL", "Close"};
                        JEditorPane pane = new UploadedLogPane(url);

                        int button = JOptionPane.showOptionDialog(
                                this,
                                pane,
                                "Jingle: Uploaded Log",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                options,
                                null
                        );

                        if (button == 0) {
                            // copy to clipboard
                            StringSelection sel = new StringSelection(url);
                            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                            clipboard.setContents(sel, null);
                        }
                    } else {
                        String error = response.get("error").getAsString();
                        JOptionPane.showMessageDialog(this, String.format("Error while uploading log:\n%s", error), "Jingle: Upload Log Failed", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    Jingle.logError("Failed to upload log:", ex);
                    JOptionPane.showMessageDialog(this, "Error while uploading log.", "Jingle: Upload Log Failed", JOptionPane.ERROR_MESSAGE);
                }

                this.uploadLogButton.setEnabled(true);
            }, "log-uploader").start();
        });

        this.mainTabbedPane.addChangeListener(e -> {
            Component selectedTab = this.mainTabbedPane.getSelectedComponent();
            if (selectedTab == this.logJPanel) {
                this.jumpToEndOfLog();
            } else if (selectedTab == this.hotkeysJPanel) {
                synchronized (Jingle.class) {
                    this.hotkeyListPanel.reload();
                }
            } else if (selectedTab == this.scriptsJPanel) {
                synchronized (Jingle.class) {
                    this.scriptListPanel.reload();
                }
            }
        });

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
        setCheckBoxBoolean(this.checkForUpdatesCheckBox, Jingle.options.checkForUpdates, b -> {
            Jingle.options.checkForUpdates = b;
            this.preReleaseCheckBox.setEnabled(b);
            if (b) {
                new Thread(JingleUpdater::checkForUpdates, "update-checker").start();
            }
        });
        this.preReleaseCheckBox.setEnabled(Jingle.options.checkForUpdates);
        setCheckBoxBoolean(this.preReleaseCheckBox, Jingle.options.usePreReleases, b -> {
            Jingle.options.usePreReleases = b;
            new Thread(JingleUpdater::checkForUpdates, "update-checker").start();
        });

        setCheckBoxBoolean(this.minimizeToTrayCheckBox, Jingle.options.minimizeToTray, b -> Jingle.options.minimizeToTray = b);

        this.openScriptsFolderButton.addActionListener(a -> OpenUtil.openFile(Jingle.FOLDER.resolve("scripts").toString()));
        this.reloadScriptsButton.addActionListener(a -> {
            synchronized (Jingle.class) {
                ScriptStuff.reloadScripts();
                HotkeyManager.reload();
                this.scriptListPanel.reload();
            }
        });

        this.finalizeOBSComponents();
        this.finalizeCommunityComponents();

        this.openPluginsFolderButton.addActionListener(a -> OpenUtil.openFile(Jingle.FOLDER.resolve("plugins").toString()));

        this.packageSubmissionFilesButton.addActionListener(a -> packageSubmissionFiles());

        this.hotkeyListPanel.reload();

        this.customizeBorderlessButton.addActionListener(e -> customizeBorderless());
        setCheckBoxBoolean(this.autoBorderlessCheckBox, Jingle.options.autoBorderless, b -> {
            Jingle.options.autoBorderless = b;
            if (b) Jingle.goBorderless();
        });
    }

    private void packageSubmissionFiles() {

        Jingle.getLatestInstancePath().ifPresent(p -> {
            this.packageSubmissionFilesButton.setEnabled(false);
            this.packageSubmissionFilesButton.setText("Packaging...");
            // Switch to log tab and lock tabbed pane
            this.mainTabbedPane.setSelectedComponent(this.logJPanel);
            this.mainTabbedPane.setEnabled(false);
            Thread thread = new Thread(() -> {
                try {
                    Path path = Packaging.prepareSubmission(p);
                    if (path != null) {
                        OpenUtil.openFile(path.toString());
                    }
                } catch (IOException e) {
                    Jingle.logError("Preparing File Submission Failed:", e);
                    JOptionPane.showMessageDialog(this, "Preparing File Submission Failed:\n" + ExceptionUtil.toDetailedString(e), "Jingle: Packaging failed", JOptionPane.ERROR_MESSAGE);
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        this.packageSubmissionFilesButton.setText("Package Submission Files");
                        this.packageSubmissionFilesButton.setEnabled(true);
                        this.mainTabbedPane.setEnabled(true);
                    });
                }
            });
            thread.start();
        });
    }

    private void customizeBorderless() {
        int ans = JOptionPane.showOptionDialog(JingleGUI.this, "Customize Borderless Behaviour. Choose \"Automatic\" to snap to main monitor, or \"Custom\" to set an exact position.", "Jingle: Customize Borderless", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{"Automatic", "Custom"}, "Automatic");
        if (ans == 0) { // Automatic
            Jingle.options.borderlessPosition = null;
        } else if (ans == 1) { // Custom
            int[] bp = Jingle.options.borderlessPosition;
            Function<String, Object> askFunc = s -> JOptionPane.showInputDialog(JingleGUI.this, s + "Input the x, y, width, and height separated with commas (e.g. \"0,0,1920,1080\").", "Jingle: Customize Borderless", JOptionPane.QUESTION_MESSAGE, null, null, bp == null ? "" : String.format("%d,%d,%d,%d", bp[0], bp[1], bp[2], bp[3]));
            Pattern pattern = Pattern.compile("^ *(-?\\d+) *, *(-?\\d+) *, *(-?\\d+) *, *(-?\\d+) *$");
            Object sizeAnsObj = askFunc.apply("");
            while (sizeAnsObj != null && !pattern.matcher(sizeAnsObj.toString()).matches()) {
                sizeAnsObj = askFunc.apply("Invalid input!\n");
            }
            if (sizeAnsObj == null) return;
            Matcher matcher;
            if (!(matcher = pattern.matcher(sizeAnsObj.toString())).matches())
                throw new RuntimeException("This should never be reached; a pattern that should have matched did not match!");
            Jingle.options.borderlessPosition = IntStream.range(1, 5).mapToObj(matcher::group).map(Integer::parseInt).mapToInt(i -> i).toArray();
        }
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

        setCheckBoxBoolean(this.projectorCheckBox, Jingle.options.projectorEnabled, b -> {
            Jingle.options.projectorEnabled = b;
            this.refreshPPFields(ppFields);
            this.projWindowPatternField.setEnabled(b);
            this.minimizeProjectorBox.setEnabled(b);
            this.resetProjNameButton.setEnabled(b);
            if (!b) {
                OBSProjector.closeAnyMeasuringProjectors();
            }
        });
        setCheckBoxBoolean(this.autoProjectorPosBox, Jingle.options.projectorPosition == null, b -> {
            if (b) {
                Jingle.options.projectorPosition = null;
                for (JTextField ppField : ppFields) {
                    ppField.setText("");
                    ppField.setEditable(false);
                    ppField.setEnabled(false);
                }
                this.projPosApplyButton.setEnabled(false);
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
        setCheckBoxBoolean(this.minimizeProjectorBox, Jingle.options.hideProjector, b -> {
            Jingle.options.hideProjector = b;
            if (b) OBSProjector.hideProjector();
            else OBSProjector.showProjector();
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
        setTextFieldFunction(this.projWindowPatternField, Jingle.options.projectorWindowPattern, s -> Jingle.options.projectorWindowPattern = s);
        this.resetProjNameButton.addActionListener(e -> this.projWindowPatternField.setText(Jingle.options.projectorWindowPattern = JingleOptions.DEFAULTS.projectorWindowPattern));
        this.projWindowPatternField.setEnabled(Jingle.options.projectorEnabled);
        this.minimizeProjectorBox.setEnabled(Jingle.options.projectorEnabled);
        this.resetProjNameButton.setEnabled(Jingle.options.projectorEnabled);
    }

    private void finalizeCommunityComponents() {
        donateButton.addActionListener(a -> OpenUtil.openLink("https://ko-fi.com/DuncanRuns", this));

        // Might be a good idea to put the invite link in some meta file on GitHub
//        discordButton.addActionListener(a -> openLink("https://discord.gg/cXf86mXAWR"));
//        githubButton.addActionListener(a -> openLink("https://github.com/DuncanRuns/Jingle"));
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
        panel1.setLayout(new GridLayoutManager(6, 1, new Insets(5, 5, 5, 5), -1, -1));
        panel1.setEnabled(true);
        panel1.putClientProperty("html.disable", Boolean.FALSE);
        mainTabbedPane.addTab("Jingle", panel1);
        instancePanel = new JPanel();
        instancePanel.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(instancePanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        instanceLabel = new JLabel();
        instanceLabel.setText("Instance: No instances opened!");
        instancePanel.add(instanceLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        instancePanel.add(openMinecraftFolderButton, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        extraButtonsPanel = new JPanel();
        extraButtonsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(extraButtonsPanel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        clearWorldsFromAllButton = new JButton();
        clearWorldsFromAllButton.setText("Clear Worlds from All Instances");
        extraButtonsPanel.add(clearWorldsFromAllButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openJingleFolderButton = new JButton();
        openJingleFolderButton.setText("Open Jingle Folder");
        extraButtonsPanel.add(openJingleFolderButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel1.add(separator1, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        panel1.add(separator2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Quick Actions");
        panel1.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        panel1.add(scrollPane1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        quickActionsPanel = new JPanel();
        quickActionsPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
        scrollPane1.setViewportView(quickActionsPanel);
        final JScrollPane scrollPane2 = new JScrollPane();
        mainTabbedPane.addTab("Options", scrollPane2);
        scrollPane2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(7, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane2.setViewportView(panel2);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        checkForUpdatesCheckBox = new JCheckBox();
        checkForUpdatesCheckBox.setText("Check for Updates");
        panel2.add(checkForUpdatesCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        preReleaseCheckBox = new JCheckBox();
        preReleaseCheckBox.setText("Enable Pre-Release Updates");
        panel2.add(preReleaseCheckBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minimizeToTrayCheckBox = new JCheckBox();
        minimizeToTrayCheckBox.setText("Minimize to Tray");
        panel2.add(minimizeToTrayCheckBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel2.add(separator3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        customizeBorderlessButton = new JButton();
        customizeBorderlessButton.setText("Customize Borderless");
        panel2.add(customizeBorderlessButton, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoBorderlessCheckBox = new JCheckBox();
        autoBorderlessCheckBox.setText("Auto Borderless");
        panel2.add(autoBorderlessCheckBox, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        logJPanel = new JPanel();
        logJPanel.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Log", logJPanel);
        final JScrollPane scrollPane3 = new JScrollPane();
        scrollPane3.setHorizontalScrollBarPolicy(31);
        logJPanel.add(scrollPane3, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(true);
        scrollPane3.setViewportView(logTextArea);
        showDebugLogsCheckBox = new JCheckBox();
        showDebugLogsCheckBox.setText("Show Debug Logs");
        logJPanel.add(showDebugLogsCheckBox, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        uploadLogButton = new JButton();
        uploadLogButton.setText("Upload Log");
        logJPanel.add(uploadLogButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        logJPanel.add(spacer2, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        hotkeysJPanel = new JPanel();
        hotkeysJPanel.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        mainTabbedPane.addTab("Hotkeys", hotkeysJPanel);
        final JScrollPane scrollPane4 = new JScrollPane();
        hotkeysJPanel.add(scrollPane4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane4.setViewportView(hotkeyListPanel);
        final Spacer spacer3 = new Spacer();
        hotkeysJPanel.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSeparator separator4 = new JSeparator();
        hotkeysJPanel.add(separator4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        hotkeysJPanel.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addHotkeyButton = new JButton();
        addHotkeyButton.setText("Add");
        panel3.add(addHotkeyButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane5 = new JScrollPane();
        mainTabbedPane.addTab("Scripts", scrollPane5);
        scrollPane5.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scriptsJPanel = new JPanel();
        scriptsJPanel.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane5.setViewportView(scriptsJPanel);
        final Spacer spacer4 = new Spacer();
        scriptsJPanel.add(spacer4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane6 = new JScrollPane();
        scriptsJPanel.add(scrollPane6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        scrollPane6.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        scrollPane6.setViewportView(scriptListPanel);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        scriptsJPanel.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openScriptsFolderButton = new JButton();
        openScriptsFolderButton.setText("Open Scripts Folder");
        panel4.add(openScriptsFolderButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        reloadScriptsButton = new JButton();
        reloadScriptsButton.setText("Reload Scripts");
        panel4.add(reloadScriptsButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator5 = new JSeparator();
        scriptsJPanel.add(separator5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pluginsTab = new JPanel();
        pluginsTab.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainTabbedPane.addTab("Plugins", pluginsTab);
        pluginsTabbedPane = new JTabbedPane();
        pluginsTab.add(pluginsTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        noPluginsLoadedTab = new JPanel();
        noPluginsLoadedTab.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pluginsTabbedPane.addTab("No Plugins Loaded", noPluginsLoadedTab);
        final JLabel label2 = new JLabel();
        label2.setText("No Plugins Loaded");
        noPluginsLoadedTab.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 5, 5), -1, -1));
        pluginsTab.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        openPluginsFolderButton = new JButton();
        openPluginsFolderButton.setText("Open Plugins Folder");
        panel5.add(openPluginsFolderButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane7 = new JScrollPane();
        mainTabbedPane.addTab("OBS", scrollPane7);
        scrollPane7.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(14, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane7.setViewportView(panel6);
        final Spacer spacer5 = new Spacer();
        panel6.add(spacer5, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("OBS Link Script Installation:");
        panel6.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("1. Open OBS, at the top, click Tools, and then Scripts.");
        panel6.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("2. Check if jingle-obs-link.lua is listed under \"Loaded Scripts\", in this case you are already done.");
        panel6.add(label5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("3. Press this button:");
        panel7.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyScriptPathButton = new JButton();
        copyScriptPathButton.setText("Copy Script Path");
        panel7.add(copyScriptPathButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("5. Press the bottom bar and press Ctrl+V to paste the script path, then press Open to add the script.");
        panel6.add(label7, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator6 = new JSeparator();
        panel6.add(separator6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        projectorCheckBox = new JCheckBox();
        projectorCheckBox.setText("Enable OBS Eye Measuring Projector");
        panel6.add(projectorCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoProjectorPosBox = new JCheckBox();
        autoProjectorPosBox.setText("Automatically Position OBS Eye Measuring Projector");
        panel6.add(autoProjectorPosBox, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projectorPositionPanel = new JPanel();
        projectorPositionPanel.setLayout(new GridLayoutManager(1, 7, new Insets(0, 0, 0, 0), -1, -1));
        projectorPositionPanel.setEnabled(true);
        panel6.add(projectorPositionPanel, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        projPosXField = new JTextField();
        projectorPositionPanel.add(projPosXField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Position:");
        projectorPositionPanel.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label9 = new JLabel();
        label9.setText("Size:");
        projectorPositionPanel.add(label9, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projPosYField = new JTextField();
        projectorPositionPanel.add(projPosYField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosWField = new JTextField();
        projectorPositionPanel.add(projPosWField, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosHField = new JTextField();
        projectorPositionPanel.add(projPosHField, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosApplyButton = new JButton();
        projPosApplyButton.setText("Apply");
        projectorPositionPanel.add(projPosApplyButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("4. Click on the + icon at the bottom left of the OBS Scripts window.");
        panel6.add(label10, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("6. Press the \"Regenerate\" button in the jingle-obs-link.lua script.");
        panel6.add(label11, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel8, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        projWindowPatternField = new JTextField();
        projWindowPatternField.setText("");
        panel8.add(projWindowPatternField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("OBS Projector Name Pattern:");
        panel8.add(label12, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resetProjNameButton = new JButton();
        resetProjNameButton.setText("Reset");
        panel8.add(resetProjNameButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minimizeProjectorBox = new JCheckBox();
        minimizeProjectorBox.setText("Hide Projector When Inactive");
        panel6.add(minimizeProjectorBox, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane8 = new JScrollPane();
        mainTabbedPane.addTab("Community", scrollPane8);
        scrollPane8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(6, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane8.setViewportView(panel9);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setForeground(new Color(-14894848));
        label13.setOpaque(false);
        label13.setText("Support Jingle:");
        label13.putClientProperty("html.disable", Boolean.FALSE);
        panel10.add(label13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        donateButton = new JButton();
        donateButton.setText("Donate");
        panel10.add(donateButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setText("Thank you supporters!");
        panel9.add(label14, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel11, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        supporter1Label = new JLabel();
        supporter1Label.setText(" ");
        panel11.add(supporter1Label, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        supporter2Label = new JLabel();
        supporter2Label.setText(" ");
        panel11.add(supporter2Label, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        supporter3Label = new JLabel();
        supporter3Label.setText(" ");
        panel11.add(supporter3Label, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel9.add(spacer6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSeparator separator7 = new JSeparator();
        panel9.add(separator7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        communityButtonsPanel = new JPanel();
        communityButtonsPanel.setLayout(new GridBagLayout());
        panel9.add(communityButtonsPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        communityButtonsLabel = new JLabel();
        communityButtonsLabel.setText("Loading Community Buttons...");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        communityButtonsPanel.add(communityButtonsLabel, gbc);
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

    public void showCommunityButtons(List<Pair<String, String>> buttons) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showCommunityButtons(buttons));
            return;
        }
        this.communityButtonsPanel.removeAll();
        if (buttons == null) {
            this.communityButtonsLabel.setText("Failed to load community buttons!");
            this.communityButtonsPanel.add(communityButtonsLabel);
            return;
        }
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = -1;
        constraints.insets = new Insets(0, 0, 5, 0);
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        buttons.forEach(pair -> {
            JButton button = new JButton(pair.getLeft());
            button.addActionListener(a -> OpenUtil.openLink(pair.getRight(), button));
            this.communityButtonsPanel.add(button, constraints.clone());
        });
    }

    public void showSupporters(String[] supporters) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> showSupporters(supporters));
            return;
        }
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

    public void refreshQuickActions() {
        quickActionsPanel.removeAll();
        quickActionsPanel.setLayout(new WrapLayout());
        List<JButton> buttons = quickActionButtonSuppliers.stream()
                .sorted(Comparator.comparingInt(Pair::getLeft))
                .map(Pair::getRight)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (buttons.isEmpty()) quickActionsPanel.add(new JLabel("No Quick Actions Available"));
        else buttons.forEach(quickActionsPanel::add);
    }

    public void refreshHack() {
        int selectedIndex = mainTabbedPane.getSelectedIndex();
        mainTabbedPane.setSelectedIndex((selectedIndex + 1) % mainTabbedPane.getTabCount());
        mainTabbedPane.setSelectedIndex(selectedIndex);
    }

    @SuppressWarnings("unused")
    public void openTab(Container tab) {
        if (!openTabInternal(tab)) {
            throw new IllegalArgumentException("Tab does not exist!");
        }
    }

    private boolean openTabInternal(Container tab) {
        while (tab != null && tab != this) {
            Container parent = tab.getParent();
            if (parent instanceof JTabbedPane) {
                ((JTabbedPane) parent).setSelectedComponent(tab);
            }
            tab = parent;
        }
        return tab == this;
    }

    public void initPosition(Point topLeft, Dimension size) {
        this.setPreferredSize(size);
        boolean topLeftInBounds = false;
        boolean topRightInBounds = false;
        Point topRight = new Point(topLeft.x + size.width, topLeft.y);
        for (MonitorUtil.Monitor monitor : MonitorUtil.getAllMonitors()) {
            if (monitor.getVBounds().contains(topLeft)) {
                topLeftInBounds = true;
            }
            if (monitor.getVBounds().contains(topRight)) {
                topRightInBounds = true;
            }
            if (topLeftInBounds && topRightInBounds) break;
        }
        if (topLeftInBounds && topRightInBounds) {
            this.setLocation(topLeft);
        } else {
            setLocation(0, 0);
        }
    }

}
