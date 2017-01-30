/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
*/
package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.Image.Format;

/**
 * A Cartoon Screen Space Ambient Occlusion filter with instance rendering capabilities.
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 *
 */
public class CartoonSSAO extends Filter{
    private Pass normalPass;
    private Vector3f frustumCorner;
    private Vector2f frustumNearFar;
    private boolean useOutline = true;
    private float downsample = 1f, applyDistance = 0.0005f;

    private boolean instancedRendering = false;
    
    RenderManager renderManager;
    ViewPort viewPort;

    /**
    * Create a Screen Space Ambient Occlusion Filter.
    * @param instancedRendering <code>true</code> if this filter has to use instance rendering and <code>false</code> (default) otherwise.
    */
    public CartoonSSAO(boolean instancedRendering) {
        super("CartoonSSAO");
        this.instancedRendering = instancedRendering;
    }

    /**
    * Create a Screen Space Ambient Occlusion Filter.
    * @param downsample factor to divide resolution by for filter, >1 increases speed but degrades quality.
    * @param instancedRendering <code>true</code> if this filter has to use instance rendering and <code>false</code> (default) otherwise.
    */
    public CartoonSSAO(float downsample, boolean instancedRendering) {
        this(instancedRendering);
        this.downsample = downsample;
    }
    
    /**
     * Create a Screen Space Ambient Occlusion Filter from the given one (by copy).
     * @param cloneFrom the original filter.
     */
    public CartoonSSAO(CartoonSSAO cloneFrom) {
        this(cloneFrom.downsample, cloneFrom.instancedRendering);
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return true;
    }

    @Override
    protected void postQueue(RenderQueue renderQueue) {
        PreNormalCaching.getPreNormals(renderManager, normalPass, viewPort);
    }
    
    /**
     * Set if outline has to be enabled.
     * @param set <code>true</code> if the outline has to be enabled and <code>false</code> otherwise.
     * @see #isOutlineEnabled()
     */
    public void setOutlineEnabled(boolean set) {
        useOutline = set;
        if( material != null ) {
            if( useOutline ) {
                material.clearParam("disableOutline");
            } else {
                material.setBoolean("disableOutline", true);
            }
        }
    }
    
    /**
     * Is outline rendering is enabled. 
     * @return <code>true</code> if the outline is enabled and <code>false</code> otherwise.
     * @see #setOutlineEnabled(boolean)
     */
    public boolean isOutlineEnabled() {
        return useOutline;
    }
    
    /**
     * Set the down sampling value.
     * @param downsample the down sampling value.
     * @see #getDownsampling()
     */
    public void setDownsampling(float downsample) {
        this.downsample = downsample;
    }
    
    /**
     * Get the down sampling value.
     * @return the down sampling value.
     * @see #setDownsampling(float)
     */
    public float getDownsampling() {
        return this.downsample;
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    /**
     * Set the distance of the material.
     * @param dist the distance of the material.
     */
    public void setDistance(float dist) {
        applyDistance = dist;
        if( material != null ) material.setFloat("Distance", dist);
    }
    
    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        this.renderManager = renderManager;
        this.viewPort = vp;

        int screenWidth = Math.round(w / downsample);
        int screenHeight = Math.round(h / downsample);

        normalPass = new Pass();
        normalPass.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth);

        frustumNearFar = new Vector2f();

        float farY = (vp.getCamera().getFrustumTop() / vp.getCamera().getFrustumNear()) * vp.getCamera().getFrustumFar();
        float farX = farY * ((float) screenWidth / (float) screenHeight);
        frustumCorner = new Vector3f(farX, farY, vp.getCamera().getFrustumFar());
        frustumNearFar.x = vp.getCamera().getFrustumNear();
        frustumNearFar.y = vp.getCamera().getFrustumFar();

        //ssao Pass
        material = new Material(manager, "Common/MatDefs/VR/CartoonSSAO.j3md");
        material.setTexture("Normals", normalPass.getRenderedTexture());        

        material.setVector3("FrustumCorner", frustumCorner);
        material.setVector2("FrustumNearFar", frustumNearFar);
        material.setFloat("Distance", applyDistance);
        if( useOutline == false ) material.setBoolean("disableOutline", true);
        if( instancedRendering ) material.setBoolean("useInstancing", true);
    }

}