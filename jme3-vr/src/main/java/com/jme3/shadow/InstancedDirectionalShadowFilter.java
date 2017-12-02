/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.shadow;

import com.jme3.app.Application;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector4f;
import com.jme3.renderer.Camera;

/**
 * An instanced version of the {@link DirectionalLightShadowFilterVR directional light shadow filter} dedi.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public class InstancedDirectionalShadowFilter extends DirectionalLightShadowFilterVR {    
    
    private final Vector4f temp4f = new Vector4f(), temp4f2 = new Vector4f();
    
    private boolean instanceRendering = false;

    private Camera rightCamera = null;
    
	/**
     * Create a new instanced version of the {@link DirectionalLightShadowFilterVR directional light shadow filter}.
     * @param application the application that this filter is attached to.
     * @param camera 
     * @param shadowMapSize the size of the rendered shadowmaps (512, 1024, 2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps the more quality, the less fps).
     * @param instancedRendering <code>true</code> if this filter has to use instance rendering and <code>false</code> otherwise.
     * @param rightCamera the camera used as right eye in stereo rendering mode.
     */
    public InstancedDirectionalShadowFilter(Application application, Camera camera, int shadowMapSize, int nbSplits, boolean instancedRendering, Camera rightCamera) {
        super(application.getAssetManager(), shadowMapSize, nbSplits, "Common/MatDefs/VR/PostShadowFilter.j3md");
        this.instanceRendering = instancedRendering;
        this.rightCamera = rightCamera;
    }        
    
    @Override    
    protected void preFrame(float tpf) {
        shadowRenderer.preFrame(tpf);
        if( instanceRendering ) {
            material.setMatrix4("ViewProjectionMatrixInverseRight", rightCamera.getViewProjectionMatrix().invert());
            Matrix4f m = rightCamera.getViewProjectionMatrix();
            material.setVector4("ViewProjectionMatrixRow2Right", temp4f2.set(m.m20, m.m21, m.m22, m.m23));
        }
        material.setMatrix4("ViewProjectionMatrixInverse", viewPort.getCamera().getViewProjectionMatrix().invert());
        Matrix4f m = viewPort.getCamera().getViewProjectionMatrix();
        material.setVector4("ViewProjectionMatrixRow2", temp4f.set(m.m20, m.m21, m.m22, m.m23));
    }
    
    /**
     * Get if this filter is using instance rendering.
     * @return <code>true</code> if this filter is using instance rendering and <code>false</code> otherwise.
     * @see #setInstanceRendering(boolean)
     */
    public boolean isInstanceRendering() {
		return instanceRendering;
	}

    /**
     * Set if this filter has to use instance rendering.
     * @param instanceRendering <code>true</code> if this filter has to use instance rendering and <code>false</code> otherwise.
     * @see #isInstanceRendering()
     */
	public void setInstanceRendering(boolean instanceRendering) {
		this.instanceRendering = instanceRendering;
	}
}
