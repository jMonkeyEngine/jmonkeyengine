/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.sceneexplorer.nodes.actions.impl;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.actions.AbstractNewSpatialAction;
import com.jme3.gde.core.sceneexplorer.nodes.actions.NewGeometryAction;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/**
 *
 * @author david.bernard.31
 */
@org.openide.util.lookup.ServiceProvider(service = NewGeometryAction.class)
public class NewGeometryQuadAction extends AbstractNewSpatialAction implements NewGeometryAction {

    public NewGeometryQuadAction() {
        name = "Quad";
    }

    @Override
    protected Spatial doCreateSpatial(Node parent) {
        NewGeometrySettings cfg = new NewGeometrySettings();
        Quad b = new Quad(cfg.getQuadWidth(), cfg.getQuadHeight(), cfg.getQuadFlipCoords());
        b.setMode(cfg.getQuadMode());
        Geometry geom = new Geometry(cfg.getQuadName(), b);
        switch(cfg.getQuadPlan()) {
            case XZ: {
                Quaternion q = new Quaternion();
                q.fromAngles((float)Math.PI/-2f, 0.0f, 0.0f);
                geom.setLocalRotation(q);
                break;
            }
            case YZ: {
                Quaternion q = new Quaternion();
                q.fromAngles(0.0f, (float)Math.PI/-2f, 0.0f);
                geom.setLocalRotation(q);
                break;
            }
        }
        Material mat = new Material(pm, "Common/MatDefs/Misc/Unshaded.j3md");
        ColorRGBA  c = cfg.getMatRandom() ?ColorRGBA.randomColor() : cfg.getMatColor();
        mat.setColor("Color", c);
        geom.setMaterial(mat);    
        parent.attachChild(geom);
        return geom;
    }
}
