/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.core.sceneexplorer;

import java.util.List;

/**
 *
 * @author Nehon
 */
public interface MaterialChangeProvider {

    public String getKey();

    public void addMaterialChangeListener(MaterialChangeListener listener);

    public void removeMaterialChangeListener(MaterialChangeListener listener);

    public void addAllMaterialChangeListener(List<MaterialChangeListener> listeners);
    
    public void clearMaterialChangeListeners();
}
