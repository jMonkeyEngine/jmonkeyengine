/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A filter to handle translucent objects when rendering a scene with filters that uses depth like WaterFilter and SSAOFilter
 * just create a TranslucentBucketFilter and add it to the Filter list of a FilterPostPorcessor
 * @author Nehon
 */
public final class TranslucentBucketFilter extends Filter {

    private final static Logger logger = Logger.getLogger(TranslucentBucketFilter.class.getName());
    private RenderManager renderManager;
    private boolean enabledSoftParticles = false;
    private Texture depthTexture;
    private ViewPort viewPort;

    public TranslucentBucketFilter() {
        super("TranslucentBucketFilter");
    }

    public TranslucentBucketFilter(boolean enabledSoftParticles) {
        this();
        this.enabledSoftParticles = enabledSoftParticles;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager rm, ViewPort vp, int w, int h) {
        this.renderManager = rm;
        this.viewPort = vp;
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
        if (enabledSoftParticles && depthTexture != null) {
            initSoftParticles(vp, true);
        }
    }

    private void initSoftParticles(ViewPort vp, boolean enabledSP) {
        if (depthTexture != null) {
            for (Spatial scene : vp.getScenes()) {
                makeSoftParticleEmitter(scene, enabledSP && enabled);
            }
        }

    }

    @Override
    protected void setDepthTexture(Texture depthTexture) {
        this.depthTexture = depthTexture;
        if (enabledSoftParticles && depthTexture != null) {
            initSoftParticles(viewPort, true);
        }
    }

    /**
     * Override this method and return false if your Filter does not need the scene texture
     * @return
     */
    @Override
    protected boolean isRequiresSceneTexture() {
        return false;
    }

    @Override
    protected boolean isRequiresDepthTexture() {
        return enabledSoftParticles;
    }

    @Override
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
        renderManager.setCamera(viewPort.getCamera(), false);
        if (prevFilterBuffer != sceneBuffer) {
            renderManager.getRenderer().copyFrameBuffer(prevFilterBuffer, sceneBuffer, false);
        }
        renderManager.getRenderer().setFrameBuffer(sceneBuffer);
        viewPort.getQueue().renderQueue(RenderQueue.Bucket.Translucent, renderManager, viewPort.getCamera());
    }

    @Override
    protected void cleanUpFilter(Renderer r) {
        if (renderManager != null) {
            renderManager.setHandleTranslucentBucket(true);
        }

        initSoftParticles(viewPort, false);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (renderManager != null) {
            renderManager.setHandleTranslucentBucket(!enabled);
        }
        initSoftParticles(viewPort, enabledSoftParticles);
    }

    private void makeSoftParticleEmitter(Spatial scene, boolean enabled) {
        if (scene instanceof Node) {
            Node n = (Node) scene;
            for (Spatial child : n.getChildren()) {
                makeSoftParticleEmitter(child, enabled);
            }
        }
        if (scene instanceof ParticleEmitter) {
            ParticleEmitter emitter = (ParticleEmitter) scene;
            if (enabled) {
                enabledSoftParticles = enabled;

                emitter.getMaterial().selectTechnique("SoftParticles", renderManager);
                if( processor.getNumSamples()>1){
                    emitter.getMaterial().setInt("NumSamplesDepth", processor.getNumSamples());
                }
                emitter.getMaterial().setTexture("DepthTexture", processor.getDepthTexture());               
                emitter.setQueueBucket(RenderQueue.Bucket.Translucent);

                logger.log(Level.FINE, "Made particle Emitter {0} soft.", emitter.getName());
            } else {
                emitter.getMaterial().clearParam("DepthTexture");
                emitter.getMaterial().selectTechnique("Default", renderManager);
               // emitter.setQueueBucket(RenderQueue.Bucket.Transparent);
                logger.log(Level.FINE, "Particle Emitter {0} is not soft anymore.", emitter.getName());
            }
        }
    }
}
