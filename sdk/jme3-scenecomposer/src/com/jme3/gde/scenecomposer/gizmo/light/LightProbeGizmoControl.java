/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.bounding.BoundingSphere;
import com.jme3.environment.util.LightsDebugState;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;

/**
 * Updates the marker's position whenever the light probe has moved. 
 * Also update the gizmo radius according to the probe radius.
 */
public class LightProbeGizmoControl extends AbstractControl{

    private final Vector3f lastPos = new Vector3f();
    private final LightProbe lightProbe;

    LightProbeGizmoControl(LightProbe light) {
        lightProbe = light;

    }

    @Override
    protected void controlUpdate(float f) {       

        if (!lightProbe.getPosition().equals(lastPos)) {
            if (getSpatial() != null) {
                lastPos.set(lightProbe.getPosition());
                getSpatial().setLocalTranslation(lastPos);
            }
        }
          
        Geometry probeGeom = (Geometry) ((Node) getSpatial()).getChild(0);
        Material m = probeGeom.getMaterial();        
        if (lightProbe.isReady()) {            
            m.setTexture("CubeMap", lightProbe.getPrefilteredEnvMap());            
        } 
        Geometry probeRadius = (Geometry) ((Node) getSpatial()).getChild(1);
        probeRadius.setLocalScale(((BoundingSphere) lightProbe.getBounds()).getRadius());
        

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        
    }

}
