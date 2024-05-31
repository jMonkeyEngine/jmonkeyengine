/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
 * Controls a material parameter based on values provided by a framegraph.
 * 
 * @author codex
 * @param <T>
 */
public class MatParamTargetControl <T> extends AbstractControl implements GraphTarget<T> {
    
    private final String name;
    private final VarType type;
    private ViewPort[] viewPorts;
    private Material material;
    private T value;
    
    public MatParamTargetControl(String name, VarType type) {
        this.name = name;
        this.type = type;
    }
    
    @Override
    protected void controlUpdate(float tpf) {
        //System.out.println("check: assign to material?");
        if (value != null) {
            //System.out.println("assign "+value.getClass().getSimpleName()+" to material");
            //material.setParam(name, type, value);
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
        if (containsViewPort(viewPort)) {
            this.value = value;
            if (this.value != null) {
                material.setParam(name, type, this.value);
            } else {
                material.clearParam(name);
            }
        }
    }
    
    private boolean containsViewPort(ViewPort vp) {
        if (viewPorts == null) return true;
        for (ViewPort p : viewPorts) {
            if (p == vp) return true;
        }
        return false;
    }
    
    public void setViewPorts(ViewPort... viewPorts) {
        this.viewPorts = viewPorts;
    }
    
    public T getValue() {
        return value;
    }
    public ViewPort[] getViewPorts() {
        return viewPorts;
    }
    
}
