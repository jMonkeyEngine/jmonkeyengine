/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.io;

import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.shader.VarType;

/**
 *
 * @author codex
 * @param <T>
 */
public class MatParamTargetControl <T> extends AbstractControl implements GraphTarget<T> {
    
    private final String name;
    private final VarType type;
    private Material material;
    private T value;
    
    public MatParamTargetControl(String name, VarType type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        if (value != null) {
            material.setParam(name, type, value);
        }
    }
    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {}
    @Override
    public void setSpatial(Spatial spat) {
        if (spatial == spat) {
            return;
        }
        super.setSpatial(spat);
        if (spatial != null) {
            for (Spatial s : new SceneGraphIterator(spatial)) {
                if (s instanceof Geometry) {
                    material = ((Geometry)s).getMaterial();
                    break;
                }
            }
        } else {
            material = null;
        }
    }
    @Override
    public void setGraphValue(ViewPort viewPort, T value) {
        this.value = value;
    }
    
    public T getValue() {
        return value;
    }
    
}
