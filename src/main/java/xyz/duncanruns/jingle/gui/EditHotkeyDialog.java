package xyz.duncanruns.jingle.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.commons.lang3.StringUtils;
import xyz.duncanruns.jingle.Jingle;
import xyz.duncanruns.jingle.hotkey.Hotkey;
import xyz.duncanruns.jingle.plugin.PluginRegistries;
import xyz.duncanruns.jingle.script.ScriptRegistries;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class EditHotkeyDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox imBox;
    private JComboBox<HotkeyTypeAndAction> selectedActionBox;
    private JButton keyButton;

    boolean cancelled;

    String action;
    String type;
    List<Integer> keys;
    boolean ignoreModifiers;

    public EditHotkeyDialog(JFrame owner, String action, String type, List<Integer> keys, boolean ignoreModifiers) {
        super(owner);

        this.action = action;
        this.type = type;
        this.keys = new ArrayList<>(keys);
        this.ignoreModifiers = ignoreModifiers;

        this.$$$setupUI$$$();
        this.setContentPane(this.contentPane);
        this.setModal(true);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.getRootPane().setDefaultButton(this.buttonOK);
        this.setTitle("Jingle: Edit Hotkey");

        this.buttonOK.addActionListener(e -> EditHotkeyDialog.this.onOK());

        this.buttonCancel.addActionListener(e -> EditHotkeyDialog.this.onCancel());

        // call onCancel() when cross is clicked
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                EditHotkeyDialog.this.onCancel();
            }
        });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(e -> EditHotkeyDialog.this.onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        this.finalizeComponents();
        this.pack();
        this.setLocation(new Point(owner.getX() + (owner.getWidth() - this.getWidth()) / 2, owner.getY() + (owner.getHeight() - this.getHeight()) / 2));
    }

    private static Vector<HotkeyTypeAndAction> getHotkeyOptions() {
        Vector<HotkeyTypeAndAction> options = new Vector<>();
        Jingle.getBuiltinHotkeyActionNames().stream().sorted().forEach(s -> options.add(new HotkeyTypeAndAction("builtin", s)));
        PluginRegistries.getHotkeyActionNames().stream().sorted().forEach(s -> options.add(new HotkeyTypeAndAction("plugin", s)));
        ScriptRegistries.getHotkeyActionNames().stream().sorted().forEach(s -> options.add(new HotkeyTypeAndAction("script", s)));
        return options;
    }

    private void finalizeComponents() {
        this.selectedActionBox.setModel(new DefaultComboBoxModel<>(getHotkeyOptions()));
        this.selectedActionBox.setSelectedItem(new HotkeyTypeAndAction(this.type, this.action));
        this.keyButton.setText(Hotkey.formatKeys(this.keys));
        if (this.keyButton.getText().isEmpty()) this.keyButton.setText("Set Hotkey Here...");
        this.keyButton.addActionListener(a -> {
            synchronized (this) {
                this.keyButton.setText("...");
                this.keyButton.setEnabled(false);
                Hotkey.onNextHotkey(() -> this.isVisible() && Jingle.isRunning(), hotkey -> {
                    synchronized (this) {
                        this.keys = hotkey.getKeys();
                        this.keyButton.setText(Hotkey.formatKeys(this.keys));
                        if (this.keyButton.getText().isEmpty()) this.keyButton.setText("Set Hotkey Here...");
                        this.keyButton.setEnabled(true);
                        this.pack();
                    }
                });
            }
        });
        this.imBox.setSelected(this.ignoreModifiers);
    }

    private void onOK() {
        HotkeyTypeAndAction selectedItem = (HotkeyTypeAndAction) this.selectedActionBox.getSelectedItem();
        assert selectedItem != null;
        this.action = selectedItem.action;
        this.type = selectedItem.type;
        this.ignoreModifiers = this.imBox.isSelected();
        this.dispose();
    }

    private void onCancel() {
        this.cancelled = true;
        this.dispose();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        imBox = new JCheckBox();
        imBox.setText("Ignore Modifier Keys (Ctrl, Alt, Shift)");
        panel3.add(imBox, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        selectedActionBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        selectedActionBox.setModel(defaultComboBoxModel1);
        panel3.add(selectedActionBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        keyButton = new JButton();
        keyButton.setText("");
        panel3.add(keyButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    private static class HotkeyTypeAndAction {
        private final String type;
        private final String action;

        private HotkeyTypeAndAction(String type, String action) {
            this.type = type;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || this.getClass() != o.getClass()) return false;

            HotkeyTypeAndAction that = (HotkeyTypeAndAction) o;
            return Objects.equals(this.type, that.type) && Objects.equals(this.action, that.action);
        }

        @Override
        public String toString() {
            if (this.action.equalsIgnoreCase("none")) return "";
            return String.format("%s (%s)", Jingle.formatAction(this.action), StringUtils.capitalize(this.type));
        }
    }
}