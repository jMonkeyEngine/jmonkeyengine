/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node.properties;

import com.jme3.math.Vector2f;
import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 *
 * @author Nehon
 */
public class VectorInplaceEditor implements InplaceEditor {

    private VectorTextField field;
    private PropertyEditor editor = null;
    private PropertyModel model;
    private PropertyEnv env;

    public VectorInplaceEditor(int capacity) {
        field = new VectorTextField(capacity);
    }

    public void connect(PropertyEditor pe, PropertyEnv env) {
        editor = pe;
        this.env = env;
        reset();
    }

    public JComponent getComponent() {
        return field;
    }

    public void clear() {
        editor = null;
        model = null;
    }

    public Object getValue() {
        return field.getText();
    }

    public void setValue(Object o) {
        field.setText((String) o);
    }

    public boolean supportsTextEntry() {
        return true;
    }

    public void reset() {
        if (editor.getValue() != null) {
            field.setText((String) editor.getValue());
        }
    }

    public void addActionListener(ActionListener al) {
        field.addActionListener(al);
    }

    public void removeActionListener(ActionListener al) {
        field.removeActionListener(al);
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
        model = pm;
    }

    public boolean isKnownComponent(Component cmpnt) {
        return cmpnt == field || field.isAncestorOf(cmpnt);
    }
}
