/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.materials;

import com.jme3.material.MatParam;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture2D;

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
        Object obj = param.getValue();
        this.value = obj.toString();
        //TODO: change to correct string
        if(obj instanceof ColorRGBA){
            value = value.replaceAll("Color\\[([^\\]]*)\\]", "$1");
            value = value.replaceAll(",", "");
        }
        else if(obj instanceof Texture2D)
        {
            value = value.replaceAll("Texture2D\\[name=([^,]*)\\,([^\\]]*)]", "$1");
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
