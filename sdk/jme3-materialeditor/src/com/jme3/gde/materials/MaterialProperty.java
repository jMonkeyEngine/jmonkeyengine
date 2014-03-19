/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials;

import com.jme3.material.MatParam;

/**
 *
 * @author normenhansen
 */
public class MaterialProperty {

    private String type;
    private String name;
    private String value;

    public MaterialProperty() {
    }

    public MaterialProperty(String type, String name, String value) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public MaterialProperty(MatParam param) {
        this.type = param.getVarType().name();
        this.name = param.getName();
        if (param.getValue() != null) {
            try {
                this.value = param.getValueAsString();
            } catch (UnsupportedOperationException e) {
            }
        }
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
}
