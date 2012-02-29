/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.post;

import com.jme3.asset.AssetManager;
import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Filters are 2D effects applied to the rendered scene.<br>
 * The filter is fed with the rendered scene image rendered in an offscreen frame buffer.<br>
 * This texture is applied on a fullscreen quad, with a special material.<br>
 * This material uses a shader that aplly the desired effect to the scene texture.<br>
 * <br>
 * This class is abstract, any Filter must extend it.<br>
 * Any filter holds a frameBuffer and a texture<br>
 * The getMaterial must return a Material that use a GLSL shader immplementing the desired effect<br>
 *
 * @author Rémy Bouquet aka Nehon
 */
public abstract class Filter implements Savable {


    private String name;
    protected Pass defaultPass;
    protected List<Pass> postRenderPasses;
    protected Material material;
    protected boolean enabled = true;
    protected FilterPostProcessor processor;

    public Filter(String name) {
        this.name = name;
    }

    /**
     * Inner class Pass
     * Pass are like filters in filters.
     * Some filters will need multiple passes before the final render
     */
    public class Pass {

        protected FrameBuffer renderFrameBuffer;
        protected Texture2D renderedTexture;
        protected Texture2D depthTexture;
        protected Material passMaterial;

        /**
         * init the pass called internally
         * @param renderer
         * @param width
         * @param height
         * @param textureFormat
         * @param depthBufferFormat
         * @param numSamples
         */
        public void init(Renderer renderer, int width, int height, Format textureFormat, Format depthBufferFormat, int numSamples, boolean renderDepth) {
            Collection<Caps> caps = renderer.getCaps();
            if (numSamples > 1 && caps.contains(Caps.FrameBufferMultisample) && caps.contains(Caps.OpenGL31)) {
                renderFrameBuffer = new FrameBuffer(width, height, numSamples);
                renderedTexture = new Texture2D(width, height, numSamples, textureFormat);
                renderFrameBuffer.setDepthBuffer(depthBufferFormat);
                if (renderDepth) {
                    depthTexture = new Texture2D(width, height, numSamples, depthBufferFormat);
                    renderFrameBuffer.setDepthTexture(depthTexture);
                }
            } else {
                renderFrameBuffer = new FrameBuffer(width, height, 1);
                renderedTexture = new Texture2D(width, height, textureFormat);
                renderFrameBuffer.setDepthBuffer(depthBufferFormat);
                if (renderDepth) {
                    depthTexture = new Texture2D(width, height, depthBufferFormat);
                    renderFrameBuffer.setDepthTexture(depthTexture);
                }
            }

            renderFrameBuffer.setColorTexture(renderedTexture);


        }

        /**
         *  init the pass called internally
         * @param renderer
         * @param width
         * @param height
         * @param textureFormat
         * @param depthBufferFormat
         */
        public void init(Renderer renderer, int width, int height, Format textureFormat, Format depthBufferFormat) {
            init(renderer, width, height, textureFormat, depthBufferFormat, 1);
        }

        public void init(Renderer renderer, int width, int height, Format textureFormat, Format depthBufferFormat, int numSamples) {
            init(renderer, width, height, textureFormat, depthBufferFormat, numSamples, false);
        }

        /**
         *  init the pass called internally
         * @param renderer
         * @param width
         * @param height
         * @param textureFormat
         * @param depthBufferFormat
         * @param numSample
         * @param material
         */
        public void init(Renderer renderer, int width, int height, Format textureFormat, Format depthBufferFormat, int numSample, Material material) {
            init(renderer, width, height, textureFormat, depthBufferFormat, numSample);
            passMaterial = material;
        }

        public boolean requiresSceneAsTexture() {
            return false;
        }

        public boolean requiresDepthAsTexture() {
            return false;
        }

        public void beforeRender() {
        }

        public FrameBuffer getRenderFrameBuffer() {
            return renderFrameBuffer;
        }

        public void setRenderFrameBuffer(FrameBuffer renderFrameBuffer) {
            this.renderFrameBuffer = renderFrameBuffer;
        }

        public Texture2D getDepthTexture() {
            return depthTexture;
        }

        public Texture2D getRenderedTexture() {
            return renderedTexture;
        }

        public void setRenderedTexture(Texture2D renderedTexture) {
            this.renderedTexture = renderedTexture;
        }

        public Material getPassMaterial() {
            return passMaterial;
        }

        public void setPassMaterial(Material passMaterial) {
            this.passMaterial = passMaterial;
        }

        public void cleanup(Renderer r) {
        }
    }

    /**
     * returns the default pass texture format
     * @return
     */
    protected Format getDefaultPassTextureFormat() {
        return Format.RGBA8;
    }

    /**
     * returns the default pass depth format
     * @return
     */
    protected Format getDefaultPassDepthFormat() {
        return Format.Depth;
    }

    /**
     * contruct a Filter
     */
    protected Filter() {
        this("filter");
    }

    /**
     *
     * initialize this filter
     * use InitFilter for overriding filter initialization
     * @param manager the assetManager
     * @param renderManager the renderManager
     * @param vp the viewport
     * @param w the width
     * @param h the height
     */
    protected final void init(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        //  cleanup(renderManager.getRenderer());
        defaultPass = new Pass();
        defaultPass.init(renderManager.getRenderer(), w, h, getDefaultPassTextureFormat(), getDefaultPassDepthFormat());
        initFilter(manager, renderManager, vp, w, h);
    }

    /**
     * cleanup this filter
     * @param r
     */
    protected final void cleanup(Renderer r) {   
        processor = null;
        if (defaultPass != null) {
            defaultPass.cleanup(r);
        }
        if (postRenderPasses != null) {
            for (Iterator<Pass> it = postRenderPasses.iterator(); it.hasNext();) {
                Pass pass = it.next();
                pass.cleanup(r);
            }
        }
        cleanUpFilter(r);
    }

    /**
     * Initialization of sub classes filters
     * This method is called once when the filter is added to the FilterPostProcessor
     * It should contain Material initializations and extra passes initialization
     * @param manager the assetManager
     * @param renderManager the renderManager
     * @param vp the viewPort where this filter is rendered
     * @param w the width of the filter
     * @param h the height of the filter
     */
    protected abstract void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h);

    /**
     * override this method if you have some cleanup to do
     * @param r the renderer
     */
    protected void cleanUpFilter(Renderer r) {
    }

    /**
     * Must return the material used for this filter.
     * this method is called every frame.
     *
     * @return the material used for this filter.
     */
    protected abstract Material getMaterial();
    
    /**
     * Override if you want to do something special with the depth texture;
     * @param depthTexture 
     */
    protected void setDepthTexture(Texture depthTexture){
        getMaterial().setTexture("DepthTexture", depthTexture);
    }

    /**
     * Override this method if you want to make a pre pass, before the actual rendering of the frame
     * @param renderManager
     * @param viewPort
     */
    protected void postQueue(RenderManager renderManager, ViewPort viewPort) {
    }

    /**
     * Override this method if you want to modify parameters according to tpf before the rendering of the frame.
     * This is usefull for animated filters
     * Also it can be the place to render pre passes
     * @param tpf the time used to render the previous frame
     */
    protected void preFrame(float tpf) {
    }

    /**
     * Override this method if you want to make a pass just after the frame has been rendered and just before the filter rendering
     * @param renderManager
     * @param viewPort
     * @param prevFilterBuffer
     * @param sceneBuffer
     */
    protected void postFrame(RenderManager renderManager, ViewPort viewPort, FrameBuffer prevFilterBuffer, FrameBuffer sceneBuffer) {
    }

    /**
     * Override this method if you want to save extra properties when the filter is saved else only basic properties of the filter will be saved
     * This method should always begin by super.write(ex);
     * @param ex
     * @throws IOException
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(name, "name", "");
        oc.write(enabled, "enabled", true);
    }

    /**
     * Override this method if you want to load extra properties when the filter
     * is loaded else only basic properties of the filter will be loaded
     * This method should always begin by super.read(im);
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        name = ic.readString("name", "");
        enabled = ic.readBoolean("enabled", true);
    }

    /**
     * returns the name of the filter
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the filter
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * returns the default pass frame buffer
     * @return
     */
    protected FrameBuffer getRenderFrameBuffer() {
        return defaultPass.renderFrameBuffer;
    }

    /**
     * sets the default pas frame buffer
     * @param renderFrameBuffer
     */
    protected void setRenderFrameBuffer(FrameBuffer renderFrameBuffer) {
        this.defaultPass.renderFrameBuffer = renderFrameBuffer;
    }

    /**
     * returns the rendered texture of this filter
     * @return
     */
    protected Texture2D getRenderedTexture() {
        return defaultPass.renderedTexture;
    }

    /**
     * sets the rendered texture of this filter
     * @param renderedTexture
     */
    protected void setRenderedTexture(Texture2D renderedTexture) {
        this.defaultPass.renderedTexture = renderedTexture;
    }

    /**
     * Override this method and return true if your Filter needs the depth texture
     *
     * @return true if your Filter need the depth texture
     */
    protected boolean isRequiresDepthTexture() {
        return false;
    }

    /**
     * Override this method and return false if your Filter does not need the scene texture
     *
     * @return false if your Filter does not need the scene texture
     */
    protected boolean isRequiresSceneTexture() {
        return true;
    }

    /**
     * returns the list of the postRender passes
     * @return
     */
    protected List<Pass> getPostRenderPasses() {
        return postRenderPasses;
    }

    /**
     * Enable or disable this filter
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        if (processor != null) {
            processor.setFilterState(this, enabled);
        } else {
            this.enabled = enabled;
        }
    }

    /**
     * returns ttrue if the filter is enabled
     * @return enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * sets a reference to the FilterPostProcessor ti which this filter is attached
     * @param proc
     */
    protected void setProcessor(FilterPostProcessor proc) {
        processor = proc;
    }
}
