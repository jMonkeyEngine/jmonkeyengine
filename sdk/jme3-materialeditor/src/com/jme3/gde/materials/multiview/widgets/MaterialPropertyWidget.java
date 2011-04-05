/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.materials.multiview.widgets;

import com.jme3.gde.materials.MaterialProperty;
import javax.swing.JPanel;

/**
 *
 * @author normenhansen
 */
public abstract class MaterialPropertyWidget extends JPanel{
    protected MaterialProperty property;
    private MaterialWidgetListener listener;

    public void registerChangeListener(MaterialWidgetListener listener){
        this.listener=listener;
    }

    protected void fireChanged(){
        if(listener==null) return;
        listener.propertyChanged(property);
    }

    /**
     * @return the properties
     */
    public MaterialProperty getProperty() {
        return property;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperty(MaterialProperty property) {
        this.property = property;
        readProperty();
    }

    protected abstract void readProperty();

}
