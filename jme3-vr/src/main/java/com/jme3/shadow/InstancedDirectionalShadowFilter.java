/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shadow;

import com.jme3.app.VRApplication;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;

/**
 * An instanced version of the {@link DirectionalLightShadowFilterVR directional light shadow filter}.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class InstancedDirectionalShadowFilter extends DirectionalLightShadowFilterVR {    
    
    private final Vector4f temp4f = new Vector4f(), temp4f2 = new Vector4f();
    
    private VRApplication application;
    
    /**
     * Create a new instanced version of the {@link DirectionalLightShadowFilterVR directional light shadow filter}.
     * @param application the VR application that this filter is attached to.
     * @param camera 
     * @param shadowMapSize the size of the rendered shadowmaps (512, 1024, 2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps).
     * @param instancedRendering <code>true</code> if this filter has to use instance rendering and <code>false</code> otherwise.
     */
    public InstancedDirectionalShadowFilter(VRApplication application, Camera camera, int shadowMapSize, int nbSplits, boolean instancedRendering) {
        super(application.getAssetManager(), shadowMapSize, nbSplits, "Common/MatDefs/VR/PostShadowFilter.j3md");
    }        
    
    @Override    
    protected void preFrame(float tpf) {
        shadowRenderer.preFrame(tpf);
        if( application.isInstanceVRRendering() ) {
            material.setMatrix4("ViewProjectionMatrixInverseRight", application.getVRViewManager().getRightCamera().getViewProjectionMatrix().invert());
            Matrix4f m = application.getVRViewManager().getRightCamera().getViewProjectionMatrix();
            material.setVector4("ViewProjectionMatrixRow2Right", temp4f2.set(m.m20, m.m21, m.m22, m.m23));
        }
        material.setMatrix4("ViewProjectionMatrixInverse", viewPort.getCamera().getViewProjectionMatrix().invert());
        Matrix4f m = viewPort.getCamera().getViewProjectionMatrix();
        material.setVector4("ViewProjectionMatrixRow2", temp4f.set(m.m20, m.m21, m.m22, m.m23));
    }
}
