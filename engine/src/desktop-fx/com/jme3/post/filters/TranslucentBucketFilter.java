/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;

/**
 *
 * @author Nehon
 */
public final class TranslucentBucketFilter extends Filter {

    private RenderManager renderManager;

    @Override
    public void initFilter(AssetManager manager, RenderManager rm, ViewPort vp, int w, int h) {
        this.renderManager = rm;
        material = new Material(manager, "Common/MatDefs/Post/Overlay.j3md");
        material.setColor("Color", ColorRGBA.White);
        Texture2D tex = processor.getFilterTexture();
        material.setTexture("Texture", tex);
        if (tex.getImage().getMultiSamples() > 1) {
            material.setInt("NumSamples", tex.getImage().getMultiSamples());
        } else {
            material.clearParam("NumSamples");
        }
        renderManager.setHandleTranslucentBucket(false);
    }
    
     /**
     * Override this method and return false if your Filter does not need the scene texture
     * @return
     */
    public boolean isRequiresSceneTexture() {
        return false;
    }

    @Override
    public void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        renderManager.setCamera(viewPort.getCamera(), false);
        renderManager.getRenderer().copyFrameBuffer(prevFilterBuffer, sceneBuffer, false);
        renderManager.getRenderer().setFrameBuffer(sceneBuffer);
        viewPort.getQueue().renderQueue(RenderQueue.Bucket.Translucent, renderManager, viewPort.getCamera());
    }

    @Override
    public void cleanUpFilter(Renderer r) {
        if (renderManager != null) {
            renderManager.setHandleTranslucentBucket(true);
        }
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (renderManager != null) {
            renderManager.setHandleTranslucentBucket(!enabled);
        }
    }
}
