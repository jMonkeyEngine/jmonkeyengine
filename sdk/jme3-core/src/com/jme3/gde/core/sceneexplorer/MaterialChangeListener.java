/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.sceneexplorer;

import com.jme3.material.Material;

/**
 *
 * @author Nehon
 */
public interface MaterialChangeListener {

    public String getKey();

    public void setMaterial(Material material);
}
