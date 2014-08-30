/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.gui.propertyeditors;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyEditorSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import javax.swing.JLabel;
import org.openide.explorer.propertysheet.ExPropertyEditor;
import org.openide.explorer.propertysheet.PropertyEnv;

/**
 *
 * @author cris
 */
public class SizeEditor  extends PropertyEditorSupport implements ExPropertyEditor, PropertyChangeListener{
    private PropertyEnv env;

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        super.setAsText(text); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getAsText() {
        return super.getAsText(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Component getCustomEditor() {
        jada.ngeditor.guiviews.editors.SizeEditor editor = new jada.ngeditor.guiviews.editors.SizeEditor();
        editor.setValue(this.getValue());
        editor.addPropertyChangeListener(this);
        return editor.getComponent(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean supportsCustomEditor() {
        return true; //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void attachEnv(PropertyEnv env) {
        this.env = env;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
       this.setValue(evt.getNewValue());
    }

   
    
}
