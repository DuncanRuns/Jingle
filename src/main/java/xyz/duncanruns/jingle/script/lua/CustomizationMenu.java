package xyz.duncanruns.jingle.script.lua;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.luaj.vm2.LuaFunction;
import xyz.duncanruns.jingle.gui.JingleGUI;
import xyz.duncanruns.jingle.script.CustomizableManager;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

class CustomizationMenu extends JDialog {
    private JPanel contentPane;
    private JButton okButton;
    private JButton cancelButton;
    private JPanel mainPanel;
    boolean cancelled = false;
    final Map<String, String> values = new HashMap<>();
    final Set<Component> okButtonDisablers = new HashSet<>();

    public CustomizationMenu(JingleLuaLibrary library, List<Element> elements) {
        super(JingleGUI.get());
        assert library.script != null;
        this.setTitle("Jingle Script: " + library.script.name);
        this.setContentPane(this.contentPane);
        this.setModal(true);
        this.getRootPane().setDefaultButton(this.okButton);

        this.okButton.addActionListener(a -> CustomizationMenu.this.onOK());

        this.cancelButton.addActionListener(a -> CustomizationMenu.this.onCancel());

        // call onCancel() when cross is clicked
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                CustomizationMenu.this.onCancel();
            }
        });

        // call onCancel() on ESCAPE
        this.contentPane.registerKeyboardAction(a -> CustomizationMenu.this.onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;

        for (Element element : elements) {
            if (element instanceof TextElement) {
                for (String s : ((TextElement) element).message.split("\n")) {
                    this.mainPanel.add(new JLabel(s), constraints.clone());
                }
            } else if (element instanceof CheckBoxElement) {
                CheckBoxElement checkBoxElement = (CheckBoxElement) element;
                JCheckBox jCheckBox = new JCheckBox(checkBoxElement.checkBoxLabel);
                jCheckBox.setSelected(Optional.ofNullable(CustomizableManager.get(library.getScriptName(), checkBoxElement.key)).orElse(checkBoxElement.defaultVal ? "true" : "false").equals("true"));
                jCheckBox.addActionListener(e -> this.values.put(checkBoxElement.key, jCheckBox.isSelected() ? "true" : "false"));
                this.mainPanel.add(jCheckBox, constraints.clone());
            } else if (element instanceof TextFieldElement) {
                JTextField field = new JTextField();
                field.setText(Optional.ofNullable(CustomizableManager.get(library.getScriptName(), ((TextFieldElement) element).key)).orElse(((TextFieldElement) element).defaultVal));
                field.addKeyListener(new KeyAdapter() {
                    {
                        this.update();
                    }

                    private void update() {
                        String value = field.getText();
                        Predicate<String> validator = Optional.ofNullable(((TextFieldElement) element).validator).map(function -> (Predicate<String>) ((s) -> function.call(s).toboolean())).orElse(s -> true);
                        if (validator.test(value)) {
                            field.setForeground(null);
                            CustomizationMenu.this.values.put(((TextFieldElement) element).key, value);
                            CustomizationMenu.this.okButtonDisablers.remove(field);
                            if (CustomizationMenu.this.okButtonDisablers.isEmpty())
                                CustomizationMenu.this.okButton.setEnabled(true);
                        } else {
                            field.setForeground(Color.RED);
                            CustomizationMenu.this.okButtonDisablers.add(field);
                            CustomizationMenu.this.okButton.setEnabled(false);
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
                this.mainPanel.add(field, constraints.clone());
            }
        }
    }

    private void onOK() {
        // add your code here
        this.dispose();
    }

    private void onCancel() {
        this.cancelled = true;
        this.dispose();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
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
        okButton = new JButton();
        okButton.setText("OK");
        panel2.add(okButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel2.add(cancelButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        contentPane.add(mainPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    interface Element {
    }

    static class CheckBoxElement implements Element {
        private final String key;
        private final boolean defaultVal;
        private final String checkBoxLabel;

        CheckBoxElement(String key, boolean defaultVal, String checkBoxLabel) {
            this.key = key;
            this.defaultVal = defaultVal;
            this.checkBoxLabel = checkBoxLabel;
        }
    }

    static class TextElement implements Element {
        private final String message;

        TextElement(String message) {
            this.message = message;
        }
    }

    static class TextFieldElement implements Element {
        private final String key;
        private final String defaultVal;
        @Nullable
        private final LuaFunction validator;

        TextFieldElement(String key, String defaultVal, @Nullable LuaFunction validator) {
            this.key = key;
            this.defaultVal = defaultVal;
            this.validator = validator;
        }
    }
}
