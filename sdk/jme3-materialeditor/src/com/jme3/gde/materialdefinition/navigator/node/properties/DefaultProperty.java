/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materialdefinition.navigator.node.properties;

import org.openide.nodes.PropertySupport;

/**
 *
 * @author Nehon
 */
public class DefaultProperty<T> extends PropertySupport.Reflection<T> {

    private boolean readOnly = false;

    public DefaultProperty(Object instance, Class<T> valueType, String displayName, String getter, String setter) throws NoSuchMethodException {
        super(instance, valueType, getter, setter);
        setName(displayName);
        setDisplayName(displayName);
    }

    public DefaultProperty(Object instance, Class<T> valueType, String displayName, String getter, String setter, boolean readOnly) throws NoSuchMethodException {
        this(instance, valueType, displayName, getter, setter);
        this.readOnly = readOnly;
    }

    @Override
    public boolean canWrite() {
        return !readOnly;
    }  
}
