/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shadow;

import com.jme3.app.VRApplication;
import com.jme3.asset.AssetManager;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;

/**
 * An instanced version of the {@link DirectionalLightShadowFilterVR directional light shadow filter}.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class InstancedDirectionalShadowFilter extends DirectionalLightShadowFilterVR {    
    
    private final Vector4f temp4f = new Vector4f(), temp4f2 = new Vector4f();
    
    /**
     * Create a new instanced version of the {@link DirectionalLightShadowFilterVR directional light shadow filter}.
     * @param assetManager the asset manager to use.
     * @param shadowMapSize the size of the rendered shadowmaps (512, 1024, 2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps).
     */
    public InstancedDirectionalShadowFilter(AssetManager assetManager, int shadowMapSize, int nbSplits) {
        super(assetManager, shadowMapSize, nbSplits, "Common/MatDefs/VR/PostShadowFilter.j3md");
    }        

    @Override    
    protected void preFrame(float tpf) {
        shadowRenderer.preFrame(tpf);
        if( VRApplication.isInstanceVRRendering() ) {
            material.setMatrix4("ViewProjectionMatrixInverseRight", VRApplication.getVRViewManager().getCamRight().getViewProjectionMatrix().invert());
            Matrix4f m = VRApplication.getVRViewManager().getCamRight().getViewProjectionMatrix();
            material.setVector4("ViewProjectionMatrixRow2Right", temp4f2.set(m.m20, m.m21, m.m22, m.m23));
        }
        material.setMatrix4("ViewProjectionMatrixInverse", viewPort.getCamera().getViewProjectionMatrix().invert());
        Matrix4f m = viewPort.getCamera().getViewProjectionMatrix();
        material.setVector4("ViewProjectionMatrixRow2", temp4f.set(m.m20, m.m21, m.m22, m.m23));
    }
}
