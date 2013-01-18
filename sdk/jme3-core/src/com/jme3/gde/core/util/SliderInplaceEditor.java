package com.jme3.gde.core.util;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.beans.PropertyEditor;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.openide.explorer.propertysheet.InplaceEditor;
import org.openide.explorer.propertysheet.PropertyEnv;
import org.openide.explorer.propertysheet.PropertyModel;

/**
 *
 * @author Nehon
 */
public class SliderInplaceEditor implements InplaceEditor {

    private PropertyEditor editor = null;
    private PropertyModel model;
    private PropertyEnv env;
    private SliderPanel slider = new SliderPanel();

    public SliderInplaceEditor(float min, float max) {
        super();
        slider.setRangeFloat(min, max);
    }

    public SliderInplaceEditor(int min, int max) {
        super();
        slider.setRangeInt(min, max);
    }

    public void connect(PropertyEditor pe, PropertyEnv pe1) {
        editor = pe;
        env = pe1;
        reset();
    }

    public void setRangeInt(int min, int max) {
        slider.setRangeInt(min, max);
    }

    public void setRangeFloat(float min, float max) {
        slider.setRangeFloat(min, max);
    }

    public JComponent getComponent() {
        return slider;
    }

    public void setAsText(String s) {
        slider.setAsText(s);
    }

    public void clear() {
        editor = null;
        model = null;
    }

    public Object getValue() {
        if (slider.floatValue) {
            return slider.getFloatValue();
        } else {
            return slider.getIntValue();
        }
    }

    public void setValue(Object o) {
        if (slider.floatValue) {
            slider.setFloatValue((Float) o);
        } else {
            slider.setIntValue((Integer) o);
        }
    }

    public boolean supportsTextEntry() {
        return true;
    }

    public void reset() {
        if (slider.floatValue) {
            Float f = (Float) editor.getValue();
            if (f != null) {
                slider.setFloatValue(f);
            }

        } else {
            Integer i = (Integer) editor.getValue();
            if (i != null) {
                slider.setIntValue(i);
            }
        }

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
        return cmpnt == slider || slider.isAncestorOf(cmpnt);
    }

    public void addActionListener(ActionListener al) {
        slider.addActionListener(al);
    }

    public void removeActionListener(ActionListener al) {
        slider.removeActionListener(al);
    }
}