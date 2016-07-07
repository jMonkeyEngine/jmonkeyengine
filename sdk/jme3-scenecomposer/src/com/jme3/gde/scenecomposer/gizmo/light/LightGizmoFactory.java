/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.gizmo.light;

import com.jme3.asset.AssetManager;
import com.jme3.environment.util.BoundingSphereDebug;
import com.jme3.gde.scenecomposer.gizmo.light.shape.ProbeRadiusShape;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;

/**
 * Handles the creation of the appropriate light gizmo according to the light type.
 * @author Nehon
 */
public class LightGizmoFactory {
     
    public static Spatial createGizmo(AssetManager assetManager, Light light){
        switch (light.getType()){
            case Probe:
                return createLightProbeGizmo(assetManager, light);
            default:
                return createDefaultGizmo(assetManager, light);                
        }
       
    }
    
    private static Spatial createDefaultGizmo(AssetManager assetManager, Light light){
        Quad q = new Quad(0.5f, 0.5f);
        Geometry g =  new Geometry(light.getName(), q);   
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("com/jme3/gde/scenecomposer/lightbulb32.png");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        g.setMaterial(mat);
        g.addControl(new LightGizmoControl(light));
        g.setQueueBucket(RenderQueue.Bucket.Transparent);
        return g;
    }
    
    private static Spatial createLightProbeGizmo(AssetManager assetManager, Light light){
        Node debugNode = new Node("Environment debug Node");
        Sphere s = new Sphere(16, 16, 0.5f);
        Geometry debugGeom = new Geometry(light.getName(), s);
        Material debugMaterial = new Material(assetManager, "Common/MatDefs/Misc/reflect.j3md");
        debugGeom.setMaterial(debugMaterial);
        Spatial debugBounds = ProbeRadiusShape.createShape(assetManager);
        
        debugNode.attachChild(debugGeom);
        debugNode.attachChild(debugBounds);
        debugNode.addControl(new LightProbeGizmoControl((LightProbe)light));
        
        return debugNode;        
    }
         
}
