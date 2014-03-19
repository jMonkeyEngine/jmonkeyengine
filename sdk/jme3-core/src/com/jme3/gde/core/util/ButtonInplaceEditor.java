package com.jme3.gde.core.util;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 *
 * @author Nehon
 */
public class ButtonInplaceEditor extends JButton implements InplaceEditor {

    private PropertyEditor editor = null;
    private PropertyModel model;
    private PropertyEnv env;

    public ButtonInplaceEditor() {
        this("Click");
        setActionCommand(COMMAND_SUCCESS);
        setForeground(Color.BLACK);
    }

    public ButtonInplaceEditor(String text) {
        super(text);
        setActionCommand(COMMAND_SUCCESS);
        setForeground(Color.BLACK);
    }

    public ButtonInplaceEditor(Icon icon) {
        super(icon);
        setActionCommand(COMMAND_SUCCESS);
        setForeground(Color.BLACK);

    }

    public ButtonInplaceEditor(String text, Icon icon) {
        super(text, icon);
        setActionCommand(COMMAND_SUCCESS);
        setForeground(Color.BLACK);
    }

    public void connect(PropertyEditor pe, PropertyEnv pe1) {
        editor = pe;
        env = pe1;   
        reset();
    }

    public JComponent getComponent() {
        return this;
    }

    public void clear() {
        editor = null;
        model = null;
    }

    public Object getValue() {
        repaint();
        updateUI();
        return "";
    }

    public void setValue(Object o) {
        repaint();
        updateUI();
    }

    public boolean supportsTextEntry() {
        return false;
    }

    public void reset() {
    }

    public KeyStroke[] getKeyStrokes() {
        return new KeyStroke[0];
    }

    public PropertyEditor getPropertyEditor() {
        return editor;
    }

    public PropertyModel getPropertyModel() {
        return model;
    }

    public void setPropertyModel(PropertyModel pm) {

        this.model = pm;
    }

    public boolean isKnownComponent(Component cmpnt) {
        return false;
    }
}