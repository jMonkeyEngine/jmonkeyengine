/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.niftygui;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.audio.AudioRenderer;
import com.jme3.input.InputManager;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.image.ColorSpace;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.render.batch.BatchRenderConfiguration;
import de.lessvoid.nifty.render.batch.BatchRenderDevice;
import de.lessvoid.nifty.spi.time.impl.AccurateTimeProvider;
import de.lessvoid.nifty.tools.resourceloader.ResourceLocation;
import java.io.InputStream;
import java.net.URL;

public class NiftyJmeDisplay implements SceneProcessor {

    protected boolean inited = false;
    protected Nifty nifty;
    protected AssetManager assetManager;
    protected RenderManager renderManager;
    protected InputManager inputManager;
    protected RenderDeviceJme renderDev;
    protected JmeBatchRenderBackend batchRendererBackend;
    protected InputSystemJme inputSys;
    protected SoundDeviceJme soundDev;
    protected Renderer renderer;
    protected ViewPort vp;

    protected ResourceLocationJme resourceLocation;

    protected int w, h;

    protected class ResourceLocationJme implements ResourceLocation {

        @Override
        public InputStream getResourceAsStream(String path) {
            AssetKey<Object> key = new AssetKey<>(path);
            AssetInfo info = assetManager.locateAsset(key);
            if (info != null) {
                return info.openStream();
            } else {
                throw new AssetNotFoundException(path);
            }
        }

        @Override
        public URL getResource(String path) {
            throw new UnsupportedOperationException();
        }
    }

    //Empty constructor needed for jMP to create replacement input system
    public NiftyJmeDisplay() {
    }

    /**
     * Create a new NiftyJmeDisplay for use with the Batched Nifty Renderer.
     *
     * Nifty will use texture atlases for rendering. Every graphical asset
     * you're rendering through Nifty will be placed into a texture atlas. The
     * goal is to render all Nifty components in a single (or at least very few)
     * draw calls. This should speed up rendering quite a bit.
     *
     * This call will use a default BatchRenderConfiguration for Nifty.
     * See the other method {@link #newNiftyJmeDisplay(com.jme3.asset.AssetManager, com.jme3.input.InputManager, com.jme3.audio.AudioRenderer, com.jme3.renderer.ViewPort, de.lessvoid.nifty.render.batch.BatchRenderConfiguration) }
     * when you want to change the default BatchRenderConfiguration and provide
     * your own.
     *
     * @param assetManager jME AssetManager
     * @param inputManager jME InputManager
     * @param audioRenderer jME AudioRenderer
     * @param viewport Viewport to use
     * @return new NiftyJmeDisplay instance
     */
    public static NiftyJmeDisplay newNiftyJmeDisplay(
            final AssetManager assetManager,
            final InputManager inputManager,
            final AudioRenderer audioRenderer,
            final ViewPort viewport) {
        return newNiftyJmeDisplay(
                assetManager,
                inputManager,
                audioRenderer,
                viewport,
                new BatchRenderConfiguration());
    }

    /**
     * Create a new NiftyJmeDisplay for use with the Batched Nifty Renderer.
     *
     * Nifty will use texture atlas for rendering. Every graphical asset you're
     * rendering through Nifty will be placed into a texture atlas. The goal is
     * to render all Nifty components in a single (or at least very few) draw
     * calls. This should speed up rendering quite a bit.
     *
     * @param assetManager jME AssetManager
     * @param inputManager jME InputManager
     * @param audioRenderer jME AudioRenderer
     * @param viewport Viewport to use
     * @param batchRenderConfiguration the Nifty BatchRenderConfiguration that
     *        you can use to further configure batch rendering. If unsure you
     *        can simply use new BatchRenderConfiguration() in here for the
     *        default configuration which should give you good default values.
     * @return new NiftyJmeDisplay instance
     */
    public static NiftyJmeDisplay newNiftyJmeDisplay(
            final AssetManager assetManager,
            final InputManager inputManager,
            final AudioRenderer audioRenderer,
            final ViewPort viewport,
            final BatchRenderConfiguration batchRenderConfiguration) {
        return new NiftyJmeDisplay(
                assetManager,
                inputManager,
                audioRenderer,
                viewport,
                batchRenderConfiguration);
    }

    /**
     * Create a new NiftyJmeDisplay for use with the Batched Nifty Renderer (improved Nifty rendering performance).
     *
     * Nifty will use a single texture of the given dimensions (see atlasWidth and atlasHeight parameters). Every
     * graphical asset you're rendering through Nifty will be placed into this big texture. The goal is to render
     * all Nifty components in a single (or at least very few) draw calls. This should speed up rendering quite a
     * bit.
     *
     * Currently you have to make sure to not use more image space than this single texture provides. However, Nifty
     * tries to be smart about this and internally will make sure that only the images are uploaded that your GUI
     * really needs. So in general this shouldn't be an issue.
     *
     * A complete re-organisation of the texture atlas happens when a Nifty screen ends and another begins. Dynamically
     * adding images while a screen is running is supported as well.
     *
     * @param assetManager jME AssetManager
     * @param inputManager jME InputManager
     * @param audioRenderer jME AudioRenderer
     * @param viewport Viewport to use
     * @param atlasWidth the width of the texture atlas Nifty uses to speed up rendering (2048 is a good value)
     * @param atlasHeight the height of the texture atlas Nifty uses to speed up rendering (2048 is a good value)
     *
     * @deprecated use the static factory methods {@link #newNiftyJmeDisplay(com.jme3.asset.AssetManager, com.jme3.input.InputManager, com.jme3.audio.AudioRenderer, com.jme3.renderer.ViewPort) }
     * or {@link #newNiftyJmeDisplay(com.jme3.asset.AssetManager, com.jme3.input.InputManager, com.jme3.audio.AudioRenderer, com.jme3.renderer.ViewPort, de.lessvoid.nifty.render.batch.BatchRenderConfiguration) }
     * instead of this constructor.
     */
    public NiftyJmeDisplay(
            final AssetManager assetManager,
            final InputManager inputManager,
            final AudioRenderer audioRenderer,
            final ViewPort viewport,
            final int atlasWidth,
            final int atlasHeight) {
        // The code duplication in here really sucks - it's a copy of the
        // private constructor below that takes a BatchRenderConfiguration as an
        // additional parameter. This method should really be removed soon and
        // users should simply call the new factory methods.
        //
        // For now, I keep this constructor as-is, but have marked it as deprecated
        // to allow migration to the new way to instantiate this class.
        initialize(assetManager, inputManager, audioRenderer, viewport);

        this.renderDev = null;
        this.batchRendererBackend = new JmeBatchRenderBackend(this);

        BatchRenderConfiguration batchRenderConfiguration = new BatchRenderConfiguration();
        batchRenderConfiguration.atlasWidth = atlasWidth;
        batchRenderConfiguration.atlasHeight = atlasHeight;

        nifty = new Nifty(
                new BatchRenderDevice(batchRendererBackend, batchRenderConfiguration),
                soundDev,
                inputSys,
                new AccurateTimeProvider());
        inputSys.setNifty(nifty);

        resourceLocation = new ResourceLocationJme();
        nifty.getResourceLoader().removeAllResourceLocations();
        nifty.getResourceLoader().addResourceLocation(resourceLocation);
    }

    private NiftyJmeDisplay(
            final AssetManager assetManager,
            final InputManager inputManager,
            final AudioRenderer audioRenderer,
            final ViewPort viewport,
            final BatchRenderConfiguration batchRenderConfiguration) {
        initialize(assetManager, inputManager, audioRenderer, viewport);

        this.renderDev = null;
        this.batchRendererBackend = new JmeBatchRenderBackend(this);

        nifty = new Nifty(
                new BatchRenderDevice(batchRendererBackend, batchRenderConfiguration),
                soundDev,
                inputSys,
                new AccurateTimeProvider());
        inputSys.setNifty(nifty);

        resourceLocation = new ResourceLocationJme();
        nifty.getResourceLoader().removeAllResourceLocations();
        nifty.getResourceLoader().addResourceLocation(resourceLocation);
    }

    /**
     * Create a standard NiftyJmeDisplay. This uses the old Nifty renderer.
     * It's probably slower than the batched
     * renderer and is mainly here for backwards compatibility.
     * Nifty colors are assumed to be in Linear colorspace (no gamma correction).
     *
     * @param assetManager jME AssetManager
     * @param inputManager jME InputManager
     * @param audioRenderer jME AudioRenderer
     * @param vp Viewport to use
     */
    public NiftyJmeDisplay(AssetManager assetManager,
            InputManager inputManager,
            AudioRenderer audioRenderer,
            ViewPort vp) {
        this(assetManager, inputManager, audioRenderer, vp, ColorSpace.Linear);
    }

    /**
     * Create a standard NiftyJmeDisplay. This uses the old Nifty renderer.
     * It's probably slower than the batched
     * renderer and is mainly here for backwards compatibility.
     *
     * @param assetManager jME AssetManager
     * @param inputManager jME InputManager
     * @param audioRenderer jME AudioRenderer
     * @param vp Viewport to use
     * @param colorSpace the ColorSpace to use for Nifty colors (sRGB or Linear)
     */
    public NiftyJmeDisplay(AssetManager assetManager,
            InputManager inputManager,
            AudioRenderer audioRenderer,
            ViewPort vp,
            ColorSpace colorSpace) {
        initialize(assetManager, inputManager, audioRenderer, vp);

        this.renderDev = new RenderDeviceJme(this, colorSpace);
        this.batchRendererBackend = null;

        nifty = new Nifty(renderDev, soundDev, inputSys, new AccurateTimeProvider());
        inputSys.setNifty(nifty);

        resourceLocation = new ResourceLocationJme();
        nifty.getResourceLoader().removeAllResourceLocations();
        nifty.getResourceLoader().addResourceLocation(resourceLocation);
    }

    private void initialize(
            final AssetManager assetManager,
            final InputManager inputManager,
            final AudioRenderer audioRenderer,
            final ViewPort viewport) {
        this.assetManager = assetManager;
        this.inputManager = inputManager;
        this.w = viewport.getCamera().getWidth();
        this.h = viewport.getCamera().getHeight();
        this.soundDev = new SoundDeviceJme(assetManager, audioRenderer);
        this.inputSys = new InputSystemJme(inputManager);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.renderManager = rm;
        if (renderDev != null) {
            renderDev.setRenderManager(rm);
        } else {
            batchRendererBackend.setRenderManager(rm);
        }

        if (inputManager != null) {
//            inputSys.setInputManager(inputManager);
            inputManager.addRawInputListener(inputSys);
        }
        inited = true;
        this.vp = vp;
        this.renderer = rm.getRenderer();

        inputSys.reset();

        // window size may have changed since the private initialize() above
        Camera camera = vp.getCamera();
        this.w = camera.getWidth();
        this.h = camera.getHeight();
        inputSys.setHeight(h);
    }

    public Nifty getNifty() {
        return nifty;
    }

    public void simulateKeyEvent(KeyInputEvent event) {
        inputSys.onKeyEvent(event);
    }

    AssetManager getAssetManager() {
        return assetManager;
    }

    RenderManager getRenderManager() {
        return renderManager;
    }

    int getHeight() {
        return h;
    }

    int getWidth() {
        return w;
    }

    Renderer getRenderer() {
        return renderer;
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        this.w = w;
        this.h = h;
        inputSys.setHeight(h);
        nifty.resolutionChanged();
    }

    @Override
    public boolean isInitialized() {
        return inited;
    }

    @Override
    public void preFrame(float tpf) {
    }

    @Override
    public void postQueue(RenderQueue rq) {
        // render nifty before anything else
        renderManager.setCamera(vp.getCamera(), true);
        //nifty.update();
        nifty.render(false);
        renderManager.setCamera(vp.getCamera(), false);
    }

    @Override
    public void postFrame(FrameBuffer out) {
    }

    @Override
    public void cleanup() {
        inited = false;
        inputSys.reset();
        if (inputManager != null) {
            inputManager.removeRawInputListener(inputSys);
        }
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        // not implemented
    }
}
