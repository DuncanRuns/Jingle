package xyz.duncanruns.jingle.gui;

import com.google.gson.JsonObject;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.JingleAppLaunch;
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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    private JComboBox comboBox1;
    private Map<String, Locale> localeMap = new HashMap<>();
    private Map<String, String> displayNameMap = new HashMap<>();

    private JingleGUI() {
        this.$$$setupUI$$$();
        this.loadAvailableLocales();
        this.finalizeComponents();
        this.setTitle("Jingle v" + Jingle.VERSION);
        this.setContentPane(this.mainPanel);
        this.setPreferredSize(new Dimension(Jingle.options.lastSize[0], Jingle.options.lastSize[1]));
        this.setLocation(Jingle.options.lastPosition[0], Jingle.options.lastPosition[1]);
        this.setIconImage(getLogo());
        this.noInstanceYet();
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Point location = JingleGUI.this.getLocation();
                Dimension size = JingleGUI.this.getSize();
                Jingle.options.lastPosition = new int[]{location.x, location.y};
                Jingle.options.lastSize = new int[]{size.width, size.height};
                if (!JingleGUI.this.jingleUpdating) Jingle.stop(true);
            }
        });

        new JingleTrayIcon(this, getLogo());
        this.pack();
        this.setVisible(true);
    }

    public static synchronized JingleGUI get() {
        if (instance == null) {
            try {
                SwingUtilities.invokeAndWait(() -> instance = new JingleGUI());
            } catch (InterruptedException | InvocationTargetException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    private void loadAvailableLocales() {
        localeMap.clear();
        displayNameMap.clear();
        List<String> i18n = ResourceUtil.getResourcesFromFolder("i18n");
        for (String i18nFile : i18n) {
            if (!i18nFile.startsWith("Jingle_") || !i18nFile.endsWith(".properties")) continue;
            String langCode = i18nFile.substring(7, i18nFile.length() - 11);

            String[] parts = langCode.split("_");
            Locale locale = parts.length > 1 ?
                    new Locale(parts[0], parts[1]) :
                    new Locale(parts[0]);

            localeMap.put(langCode, locale);

            String displayName = locale.getDisplayName(locale);
            displayNameMap.put(langCode, displayName);
        }
        if (localeMap.isEmpty()) {
            Locale defaultLocale = Locale.getDefault();
            String defaultLangCode = defaultLocale.getLanguage() + "_" + defaultLocale.getCountry();
            localeMap.put(defaultLangCode, defaultLocale);
            displayNameMap.put(defaultLangCode, defaultLocale.getDisplayName(defaultLocale));
        }
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
            this.instanceLabel.setText((open ? I18nUtil.getString("jingle.instance.open") + " " : I18nUtil.getString("jingle.instance.closed") + " ") + instancePathString);
        } else {
            this.instanceLabel.setText(I18nUtil.getString("jingle.instance.no_instance"));
        }
    }

    private void noInstanceYet() {
        this.clearWorldsButton.setEnabled(false);
        this.goBorderlessButton.setEnabled(false);
        this.openMinecraftFolderButton.setEnabled(false);
        this.packageSubmissionFilesButton.setEnabled(false);
        this.instanceLabel.setText(I18nUtil.getString("jingle.instance.loading"));
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
        this.setI18n();
        this.clearWorldsButton.addActionListener(a -> Bopping.bop(false));
        this.clearWorldsFromAllButton.addActionListener(a -> Bopping.bop(true));
        this.goBorderlessButton.addActionListener(a -> Jingle.goBorderless());
        this.goBorderlessButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 3) customizeBorderless();
            }
        });
        this.goBorderlessButton.setToolTipText(I18nUtil.getString("jingle.borderless.right_click_configure"));
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
                        Object[] options = new Object[]{I18nUtil.getString("jingle.log.upload_log_copy_url"), I18nUtil.getString("jingle.log.upload_close")};
                        JEditorPane pane = new UploadedLogPane(url);

                        int button = JOptionPane.showOptionDialog(
                                this,
                                pane,
                                I18nUtil.getString("jingle.log.upload_log_title"),
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
                        JOptionPane.showMessageDialog(this, String.format(I18nUtil.getString("jingle.log.error_while_upload") + ":\n%s", error), I18nUtil.getString("jingle.log.error_while_upload_title"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    Jingle.logError("Failed to upload log:", ex);
                    JOptionPane.showMessageDialog(this, I18nUtil.getString("jingle.log.error_while_upload") + ".", I18nUtil.getString("jingle.log.error_while_upload_title"), JOptionPane.ERROR_MESSAGE);
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

        this.openPluginsFolderButton.addActionListener(a -> OpenUtil.openFile(Jingle.FOLDER.resolve("plugins").toString()));

        this.donateButton.addActionListener(a -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://ko-fi.com/duncanruns"));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, I18nUtil.getString("jingle.donate.failed_open_link"), I18nUtil.getString("jingle.donate.failed_open_link_title"), JOptionPane.ERROR_MESSAGE);
            }
        });

        this.packageSubmissionFilesButton.addActionListener(a -> packageSubmissionFiles());

        this.hotkeyListPanel.reload();

        this.customizeBorderlessButton.addActionListener(e -> customizeBorderless());
        setCheckBoxBoolean(this.autoBorderlessCheckBox, Jingle.options.autoBorderless, b -> {
            Jingle.options.autoBorderless = b;
            if (b) Jingle.goBorderless();
        });

        initLanguageComboBox();
    }

    private void packageSubmissionFiles() {

        Jingle.getLatestInstancePath().ifPresent(p -> {
            this.packageSubmissionFilesButton.setEnabled(false);
            this.packageSubmissionFilesButton.setText(I18nUtil.getString("jingle.package.packaging"));
            Thread thread = new Thread(() -> {
                try {
                    Path path = Packaging.prepareSubmission(p);
                    if (path != null) {
                        OpenUtil.openFile(path.toString());
                    }
                } catch (IOException e) {
                    Jingle.logError("Preparing File Submission Failed:", e);
                    JOptionPane.showMessageDialog(this, I18nUtil.getString("jingle_package.preparing_file_fail") + "\n" + ExceptionUtil.toDetailedString(e), I18nUtil.getString("jingle.package.preparing_file_fail_title"), JOptionPane.ERROR_MESSAGE);
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        this.packageSubmissionFilesButton.setText(I18nUtil.getString("gui.jingle.package_submission_files"));
                        this.packageSubmissionFilesButton.setEnabled(true);
                    });
                }
            });
            thread.start();
        });
    }

    private void customizeBorderless() {
        int ans = JOptionPane.showOptionDialog(JingleGUI.this, I18nUtil.getString("jingle.options.customize_borderless"), I18nUtil.getString("jingle.options.customize_borderless_title"), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, new String[]{I18nUtil.getString("jingle.options.auto_borderless"), I18nUtil.getString("jingle.options.custom")}, I18nUtil.getString("jingle.options.auto_borderless"));
        if (ans == 0) { // Automatic
            Jingle.options.borderlessPosition = null;
        } else if (ans == 1) { // Custom
            int[] bp = Jingle.options.borderlessPosition;
            Function<String, Object> askFunc = s -> JOptionPane.showInputDialog(JingleGUI.this, s + I18nUtil.getString("jingle.options.customize_borderless_message"), I18nUtil.getString("jingle.options.customize_borderless_title"), JOptionPane.QUESTION_MESSAGE, null, null, bp == null ? "" : String.format("%d,%d,%d,%d", bp[0], bp[1], bp[2], bp[3]));
            Pattern pattern = Pattern.compile("^ *(-?\\d+) *, *(-?\\d+) *, *(-?\\d+) *, *(-?\\d+) *$");
            Object sizeAnsObj = askFunc.apply("");
            while (sizeAnsObj != null && !pattern.matcher(sizeAnsObj.toString()).matches()) {
                sizeAnsObj = askFunc.apply(I18nUtil.getString("jingle.options.custom_invalid_input") + "\n");
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
                JOptionPane.showMessageDialog(this, I18nUtil.getString("jingle.obs.fail_copy_path") + " " + ExceptionUtil.toDetailedString(e));
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
        setCheckBoxBoolean(this.minimizeProjectorBox, Jingle.options.minimizeProjector, b -> {
            Jingle.options.minimizeProjector = b;
            if (b) OBSProjector.minimizeProjector();
            else OBSProjector.unminimizeProjector();
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
        panel2.setLayout(new GridLayoutManager(8, 5, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane2.setViewportView(panel2);
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(7, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        checkForUpdatesCheckBox = new JCheckBox();
        checkForUpdatesCheckBox.setText("Check for Updates");
        panel2.add(checkForUpdatesCheckBox, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        preReleaseCheckBox = new JCheckBox();
        preReleaseCheckBox.setText("Enable Pre-Release Updates");
        panel2.add(preReleaseCheckBox, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minimizeToTrayCheckBox = new JCheckBox();
        minimizeToTrayCheckBox.setText("Minimize to Tray");
        panel2.add(minimizeToTrayCheckBox, new GridConstraints(2, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator3 = new JSeparator();
        panel2.add(separator3, new GridConstraints(3, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        customizeBorderlessButton = new JButton();
        customizeBorderlessButton.setText("Customize Borderless");
        panel2.add(customizeBorderlessButton, new GridConstraints(4, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoBorderlessCheckBox = new JCheckBox();
        autoBorderlessCheckBox.setText("Auto Borderless");
        panel2.add(autoBorderlessCheckBox, new GridConstraints(5, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Language :");
        panel2.add(label2, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox1 = new JComboBox();
        panel2.add(comboBox1, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        final JLabel label3 = new JLabel();
        label3.setText("No Plugins Loaded");
        noPluginsLoadedTab.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        final JLabel label4 = new JLabel();
        label4.setText("OBS Link Script Installation:");
        panel6.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("1. Open OBS, at the top, click Tools, and then Scripts.");
        panel6.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("2. Check if jingle-obs-link.lua is listed under \"Loaded Scripts\", in this case you are already done.");
        panel6.add(label6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("3. Press this button:");
        panel7.add(label7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        copyScriptPathButton = new JButton();
        copyScriptPathButton.setText("Copy Script Path");
        panel7.add(copyScriptPathButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("5. Press the bottom bar and press Ctrl+V to paste the script path, then press Open to add the script.");
        panel6.add(label8, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        final JLabel label9 = new JLabel();
        label9.setText("Position:");
        projectorPositionPanel.add(label9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label10 = new JLabel();
        label10.setText("Size:");
        projectorPositionPanel.add(label10, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        projPosYField = new JTextField();
        projectorPositionPanel.add(projPosYField, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosWField = new JTextField();
        projectorPositionPanel.add(projPosWField, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosHField = new JTextField();
        projectorPositionPanel.add(projPosHField, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(30, -1), null, 0, false));
        projPosApplyButton = new JButton();
        projPosApplyButton.setText("Apply");
        projectorPositionPanel.add(projPosApplyButton, new GridConstraints(0, 6, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label11 = new JLabel();
        label11.setText("4. Click on the + icon at the bottom left of the OBS Scripts window.");
        panel6.add(label11, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label12 = new JLabel();
        label12.setText("6. Press the \"Regenerate\" button in the jingle-obs-link.lua script.");
        panel6.add(label12, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel8, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        projWindowPatternField = new JTextField();
        projWindowPatternField.setText("");
        panel8.add(projWindowPatternField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label13 = new JLabel();
        label13.setText("OBS Projector Name Pattern:");
        panel8.add(label13, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        resetProjNameButton = new JButton();
        resetProjNameButton.setText("Reset");
        panel8.add(resetProjNameButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        minimizeProjectorBox = new JCheckBox();
        minimizeProjectorBox.setText("Minimize Projector When Inactive");
        panel6.add(minimizeProjectorBox, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane8 = new JScrollPane();
        mainTabbedPane.addTab("Donate", scrollPane8);
        scrollPane8.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(7, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane8.setViewportView(panel9);
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(panel10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label14 = new JLabel();
        label14.setForeground(new Color(-14894848));
        label14.setOpaque(false);
        label14.setText("Support Jingle:");
        panel10.add(label14, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        donateButton = new JButton();
        donateButton.setText("Donate");
        panel10.add(donateButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label15 = new JLabel();
        label15.setText("Thank you supporters!");
        panel9.add(label15, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        panel9.add(spacer6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel9.add(spacer7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel9.add(spacer8, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label16 = new JLabel();
        label16.setText(I18nUtil.getString("gui.support.translator"));
        panel9.add(label16, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    public void refreshQuickActions() {
        quickActionsPanel.removeAll();
        quickActionsPanel.setLayout(new WrapLayout());
        List<JButton> buttons = quickActionButtonSuppliers.stream()
                .sorted(Comparator.comparingInt(Pair::getLeft))
                .map(Pair::getRight)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (buttons.isEmpty()) quickActionsPanel.add(new JLabel(I18nUtil.getString("jingle.action.no_quick_action")));
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

    private void initLanguageComboBox() {
        for (Map.Entry<String, String> entry : displayNameMap.entrySet()) {
            comboBox1.addItem(entry.getValue());
        }

        String currentLang = Jingle.options.language;

        if (currentLang != null && displayNameMap.containsKey(currentLang)) {
            comboBox1.setSelectedItem(displayNameMap.get(currentLang));
            comboBox1.revalidate();
            comboBox1.repaint();
        } else {
            Locale defaultLocale = Locale.getDefault();
            String defaultLangCode = defaultLocale.getLanguage() + "_" + defaultLocale.getCountry();
            String defaultDisplay = defaultLocale.getDisplayName(defaultLocale) + " (" + defaultLangCode + ")";
            comboBox1.setSelectedItem(defaultDisplay);
        }

        comboBox1.addActionListener(e -> {
            String selectedItem = (String) comboBox1.getSelectedItem();
            String selectedLang = null;

            for (Map.Entry<String, String> entry : displayNameMap.entrySet()) {
                String display = entry.getValue();
                if (display.equals(selectedItem)) {
                    selectedLang = entry.getKey();
                    break;
                }
            }

            if (selectedLang != null && !selectedLang.equals(Jingle.options.language)) {

                // 提示重启
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        I18nUtil.getString("jingle.options.language_change_restart"),
                        I18nUtil.getString("jingle.options.language_change_restart_title"),
                        JOptionPane.OK_CANCEL_OPTION
                );

                if (choice == JOptionPane.OK_OPTION) {
                    Jingle.options.language = selectedLang;
                    restartApplication();
                } else {
                    // 如果选择no，则Jingle.options.language不变
                    comboBox1.setSelectedItem(displayNameMap.get(currentLang));
                }
            }
        });
    }

    private void restartApplication() {
        try {
            Jingle.options.save();

            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String jarPath = Jingle.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-jar");
            command.add(jarPath.substring(1));

            if (JingleAppLaunch.args != null) {
                command.addAll(Arrays.asList(JingleAppLaunch.args));
            }
            new ProcessBuilder(command).start();

            System.exit(0);
        } catch (Exception ex) {
            Jingle.logError("Failed to restart application", ex);
            JOptionPane.showMessageDialog(
                    this,
                    "Failed to restart: " + ExceptionUtil.toDetailedString(ex),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void setI18n() {
        // Tab titles
        setTabTitles();

        // Jingle
        instanceLabel.setText(I18nUtil.getString("gui.jingle.no_instances_opened"));
        clearWorldsButton.setText(I18nUtil.getString("gui.jingle.clear_worlds"));
        goBorderlessButton.setText(I18nUtil.getString("gui.jingle.go_borderless"));
        packageSubmissionFilesButton.setText(I18nUtil.getString("gui.jingle.package_submission_files"));
        openMinecraftFolderButton.setText(I18nUtil.getString("gui.jingle.open_minecraft_folder"));
        clearWorldsFromAllButton.setText(I18nUtil.getString("gui.jingle.clear_worlds_all"));
        openJingleFolderButton.setText(I18nUtil.getString("gui.jingle.open_jingle_folder"));
        JLabel quickActionsLabel = findLabelByText(mainPanel, "Quick Actions");
        if (quickActionsLabel != null) {
            quickActionsLabel.setText(I18nUtil.getString("gui.jingle.quick_actions"));
        }

        // Options
        checkForUpdatesCheckBox.setText(I18nUtil.getString("gui.options.check_for_updates"));
        preReleaseCheckBox.setText(I18nUtil.getString("gui.options.pre_release_updates"));
        minimizeToTrayCheckBox.setText(I18nUtil.getString("gui.options.minimize_to_tray"));
        customizeBorderlessButton.setText(I18nUtil.getString("gui.options.customize_borderless"));
        autoBorderlessCheckBox.setText(I18nUtil.getString("gui.options.auto_borderless"));
        JLabel languageLabel = findLabelByText(mainPanel, "Language :");
        if (languageLabel != null) {
            languageLabel.setText(I18nUtil.getString("gui.options.language"));
        }

        // Log
        showDebugLogsCheckBox.setText(I18nUtil.getString("gui.log.show_debug_logs"));
        uploadLogButton.setText(I18nUtil.getString("gui.log.upload_logs"));

        // Hotkeys
        addHotkeyButton.setText(I18nUtil.getString("gui.hotkeys.add"));

        // Scripts
        openScriptsFolderButton.setText(I18nUtil.getString("gui.scripts.open_scripts_folder"));
        reloadScriptsButton.setText(I18nUtil.getString("gui.scripts.reload_scripts"));

        // Plugins
        // TODO pluginsTabbedPane : No Plugins Loaded
        openPluginsFolderButton.setText(I18nUtil.getString("gui.plugins.open_plugins_folder"));

        // OBS
        JLabel obs_text_0 = findLabelByText(mainPanel, "OBS Link Script Installation:");
        if (obs_text_0 != null) {
            obs_text_0.setText(I18nUtil.getString("gui.obs.obs_text_0"));
        }
        JLabel obs_text_1 = findLabelByText(mainPanel, "1. Open OBS, at the top, click Tools, and then Scripts.");
        if (obs_text_1 != null) {
            obs_text_1.setText(I18nUtil.getString("gui.obs.obs_text_1"));
        }
        JLabel obs_text_2 = findLabelByText(mainPanel, "2. Check if jingle-obs-link.lua is listed under \"Loaded Scripts\", in this case you are already done.");
        if (obs_text_2 != null) {
            obs_text_2.setText(I18nUtil.getString("gui.obs.obs_text_2"));
        }
        JLabel obs_text_3 = findLabelByText(mainPanel, "3. Press this button:");
        if (obs_text_3 != null) {
            obs_text_3.setText(I18nUtil.getString("gui.obs.obs_text_3"));
        }
        JLabel obs_text_4 = findLabelByText(mainPanel, "4. Click on the + icon at the bottom left of the OBS Scripts window.");
        if (obs_text_4 != null) {
            obs_text_4.setText(I18nUtil.getString("gui.obs.obs_text_4"));
        }
        JLabel obs_text_5 = findLabelByText(mainPanel, "5. Press the bottom bar and press Ctrl+V to paste the script path, then press Open to add the script.");
        if (obs_text_5 != null) {
            obs_text_5.setText(I18nUtil.getString("gui.obs.obs_text_5"));
        }
        JLabel obs_text_6 = findLabelByText(mainPanel, "6. Press the \"Regenerate\" button in the jingle-obs-link.lua script.");
        if (obs_text_6 != null) {
            obs_text_6.setText(I18nUtil.getString("gui.obs.obs_text_6"));
        }
        JLabel position = findLabelByText(mainPanel, "Position:");
        if (position != null) {
            position.setText(I18nUtil.getString("gui.obs.position"));
        }
        JLabel size = findLabelByText(mainPanel, "Size:");
        if (size != null) {
            size.setText(I18nUtil.getString("gui.obs.size"));
        }
        JLabel obs_projector_name_pattern = findLabelByText(mainPanel, "OBS Projector Name Pattern:");
        if (obs_projector_name_pattern != null) {
            obs_projector_name_pattern.setText(I18nUtil.getString("gui.obs.obs_projector_name_pattern"));
        }
        projectorCheckBox.setText(I18nUtil.getString("gui.obs.obs_eye_projector"));
        autoProjectorPosBox.setText(I18nUtil.getString("gui.obs.auto_position_projector"));
        projPosApplyButton.setText(I18nUtil.getString("gui.obs.apply"));
        resetProjNameButton.setText(I18nUtil.getString("gui.obs.reset"));
        minimizeProjectorBox.setText(I18nUtil.getString("gui.obs.minimize_projector_when_inactive"));
        copyScriptPathButton.setText(I18nUtil.getString("gui.obs.copy_script_path"));

        // Donate
        JLabel supportLabel = findLabelByText(mainPanel, "Support Jingle:");
        if (supportLabel != null) {
            supportLabel.setText(I18nUtil.getString("gui.support.support_jingle"));
        }
        donateButton.setText(I18nUtil.getString("gui.support.donate"));
        JLabel thanks = findLabelByText(mainPanel, "Thank you supporters!");
        if (thanks != null) {
            thanks.setText(I18nUtil.getString("gui.support.thanks"));
        }

    }

    private void setTabTitles() {
        // 获取所有标签页索引
        int tabCount = mainTabbedPane.getTabCount();

        Map<String, String> tabTitleMap = new HashMap<>();
        tabTitleMap.put("Jingle", "gui.tabs.jingle");
        tabTitleMap.put("Options", "gui.tabs.options");
        tabTitleMap.put("Log", "gui.tabs.log");
        tabTitleMap.put("Hotkeys", "gui.tabs.hotkeys");
        tabTitleMap.put("Scripts", "gui.tabs.scripts");
        tabTitleMap.put("Plugins", "gui.tabs.plugins");
        tabTitleMap.put("OBS", "gui.tabs.obs");
        tabTitleMap.put("Donate", "gui.tabs.donate");

        for (int i = 0; i < tabCount; i++) {
            String originalTitle = mainTabbedPane.getTitleAt(i);
            String i18nKey = tabTitleMap.get(originalTitle);

            if (i18nKey != null) {
                mainTabbedPane.setTitleAt(i, I18nUtil.getString(i18nKey));
            }
        }
    }

    private JLabel findLabelByText(Container container, String text) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && text.equals(((JLabel) comp).getText())) {
                return (JLabel) comp;
            }
            if (comp instanceof Container) {
                JLabel found = findLabelByText((Container) comp, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
