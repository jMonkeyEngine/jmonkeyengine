/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.SafeArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A `FilterPostProcessor` is a {@link SceneProcessor} that can apply several
 * {@link Filter}s to a rendered scene. It manages a list of filters that will be
 * applied in the order in which they have been added. This processor handles
 * rendering the main scene to an offscreen framebuffer, then applying each enabled
 * filter sequentially, optionally with anti-aliasing (multisampling) and depth texture
 * support.
 *
 * @author Nehon
 */
public class FilterPostProcessor implements SceneProcessor, Savable {

    /**
     * The simple name of this class, used for profiling.
     */
    public static final String FPP = FilterPostProcessor.class.getSimpleName();

    private RenderManager renderManager;
    private Renderer renderer;
    private ViewPort viewPort;
    private FrameBuffer renderFrameBufferMS;
    private int numSamples = 1;
    private FrameBuffer renderFrameBuffer;
    private Texture2D filterTexture;
    private Texture2D depthTexture;
    private SafeArrayList<Filter> filters = new SafeArrayList<>(Filter.class);
    private AssetManager assetManager;
    private Picture fsQuad;
    private boolean computeDepth = false;
    private FrameBuffer outputBuffer;
    private int width;
    private int height;
    private float bottom;
    private float left;
    private float right;
    private float top;
    private int originalWidth;
    private int originalHeight;
    private int lastFilterIndex = -1;
    private boolean cameraInit = false;
    private boolean multiView = false;
    private AppProfiler prof;

    private Format fbFormat = Format.RGB111110F;
    private Format depthFormat = Format.Depth;

    /**
     * Creates a new `FilterPostProcessor`.
     *
     * @param assetManager The asset manager to be used by filters for loading assets.
     */
    public FilterPostProcessor(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Serialization-only constructor. Do not use this constructor directly;
     * use {@link #FilterPostProcessor(AssetManager)}.
     */
    protected FilterPostProcessor() {
    }

    /**
     * Adds a filter to the list of filters to be applied. Filters are applied
     * in the order they are added. If the processor is already initialized,
     * the filter is immediately initialized as well.
     *
     * @param filter The filter to add (not null).
     * @throws IllegalArgumentException If the provided filter is null.
     */
    public void addFilter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null.");
        }
        filters.add(filter);

        if (isInitialized()) {
            initFilter(filter, viewPort);
        }

        setFilterState(filter, filter.isEnabled());
    }

    /**
     * Removes a specific filter from the list. The filter's `cleanup` method
     * is called upon removal.
     *
     * @param filter The filter to remove (not null).
     * @throws IllegalArgumentException If the provided filter is null.
     */
    public void removeFilter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null.");
        }
        filters.remove(filter);
        filter.cleanup(renderer);
        updateLastFilterIndex();
    }

    /**
     * Returns an iterator over the filters currently managed by this processor.
     *
     * @return An `Iterator` of {@link Filter} objects.
     */
    public Iterator<Filter> getFilterIterator() {
        return filters.iterator();
    }

    /**
     * Initializes the `FilterPostProcessor`. This method is called by the
     * `RenderManager` when the processor is added to a viewport.
     *
     * @param rm The `RenderManager` instance.
     * @param vp The `ViewPort` this processor is attached to.
     */
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        renderer = rm.getRenderer();
        viewPort = vp;
        fsQuad = new Picture("filter full screen quad");
        fsQuad.setWidth(1);
        fsQuad.setHeight(1);

        // Determine optimal framebuffer format based on renderer capabilities
        if (!renderer.getCaps().contains(Caps.PackedFloatTexture)) {
            if (renderer.getCaps().contains(Caps.FloatColorBufferRGB)) {
                fbFormat = Format.RGB16F;
            } else if (renderer.getCaps().contains(Caps.FloatColorBufferRGBA)) {
                fbFormat = Format.RGBA16F;
            } else {
                fbFormat = Format.RGB8;
            }
        }

        Camera cam = vp.getCamera();

        // Save original viewport dimensions
        left = cam.getViewPortLeft();
        right = cam.getViewPortRight();
        top = cam.getViewPortTop();
        bottom = cam.getViewPortBottom();
        originalWidth = cam.getWidth();
        originalHeight = cam.getHeight();

        // First call to reshape to set up internal framebuffers and textures
        reshape(vp, cam.getWidth(), cam.getHeight());
    }

    /**
     * Returns the default color buffer format used for the internal rendering
     * passes of the filters. This format is determined during initialization
     * based on the renderer's capabilities.
     *
     * @return The default `Format` for the filter pass textures.
     */
    public Format getDefaultPassTextureFormat() {
        return fbFormat;
    }

    /**
     * Initializes a single filter. This method is called when a filter is added
     * or when the post-processor is initialized/reshaped. It sets the processor
     * for the filter, handles depth texture requirements, and calls the filter's
     * `init` method.
     *
     * @param filter The {@link Filter} to initialize.
     * @param vp The `ViewPort` associated with this processor.
     */
    private void initFilter(Filter filter, ViewPort vp) {
        filter.setProcessor(this);
        if (filter.isRequiresDepthTexture()) {
            if (!computeDepth && renderFrameBuffer != null) {
                // If depth texture is required and not yet created, create it
                depthTexture = new Texture2D(width, height, depthFormat);
                renderFrameBuffer.setDepthTarget(FrameBufferTarget.newTarget(depthTexture));
            }
            computeDepth = true; // Mark that depth texture is needed
            filter.init(assetManager, renderManager, vp, width, height);
            filter.setDepthTexture(depthTexture);
        } else {
            filter.init(assetManager, renderManager, vp, width, height);
        }
    }

    /**
     * Renders a filter's material onto a full-screen quad. This method
     * handles setting up the rendering context (framebuffer, camera, material)
     * for a filter pass. It correctly resizes the camera and adjusts material
     * states based on whether the target buffer is the final output buffer or an
     * intermediate filter buffer.
     *
     * @param r The `Renderer` instance.
     * @param buff The `FrameBuffer` to render to.
     * @param mat The `Material` to use for rendering the filter.
     */
    private void renderProcessing(Renderer r, FrameBuffer buff, Material mat) {
        // Adjust camera and viewport based on target framebuffer
        if (buff == outputBuffer) {
            viewPort.getCamera().resize(originalWidth, originalHeight, false);
            viewPort.getCamera().setViewPort(left, right, bottom, top);
            // viewPort.getCamera().update(); // Redundant as resize and setViewPort call onXXXChange
            renderManager.setCamera(viewPort.getCamera(), false);
            // Disable depth test/write for final pass to prevent artifacts
            if (mat.getAdditionalRenderState().isDepthWrite()) {
                mat.getAdditionalRenderState().setDepthTest(false);
                mat.getAdditionalRenderState().setDepthWrite(false);
            }
        } else {
            // Rendering to an intermediate framebuffer for a filter pass
            viewPort.getCamera().resize(buff.getWidth(), buff.getHeight(), false);
            viewPort.getCamera().setViewPort(0, 1, 0, 1);
            // viewPort.getCamera().update(); // Redundant as resize and setViewPort call onXXXChange
            renderManager.setCamera(viewPort.getCamera(), false);
            // Enable depth test/write for intermediate passes if material needs it
            mat.getAdditionalRenderState().setDepthTest(true);
            mat.getAdditionalRenderState().setDepthWrite(true);
        }

        fsQuad.setMaterial(mat);
        fsQuad.updateGeometricState();

        r.setFrameBuffer(buff);
        r.clearBuffers(true, true, true); // Clear color, depth, and stencil buffers
        renderManager.renderGeometry(fsQuad);
    }

    /**
     * Checks if the `FilterPostProcessor` has been initialized.
     *
     * @return True if initialized, false otherwise.
     */
    @Override
    public boolean isInitialized() {
        return viewPort != null;
    }

    @Override
    public void postQueue(RenderQueue rq) {
        for (Filter filter : filters.getArray()) {
            if (filter.isEnabled()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostQueue, FPP, filter.getName());
                }
                filter.postQueue(rq);
            }
        }
    }

    /**
     * Renders the chain of filters. This method is the core of the post-processing.
     * It iterates through each enabled filter, handling pre-filter passes,
     * setting up textures (scene, depth), performing the main filter rendering,
     * and managing intermediate framebuffers.
     *
     * @param r The `Renderer` instance.
     * @param sceneFb The framebuffer containing the rendered scene (either MS or single-sample).
     */
    private void renderFilterChain(Renderer r, FrameBuffer sceneFb) {
        Texture2D tex = filterTexture;
        FrameBuffer buff = sceneFb;
        boolean msDepth = depthTexture != null && depthTexture.getImage().getMultiSamples() > 1;

        for (int i = 0; i < filters.size(); i++) {
            Filter filter = filters.get(i);
            if (prof != null) {
                prof.spStep(SpStep.ProcPostFrame, FPP, filter.getName());
            }

            if (filter.isEnabled()) {
                // Handle additional passes a filter might have (e.g., blur passes)
                if (filter.getPostRenderPasses() != null) {
                    for (Filter.Pass pass : filter.getPostRenderPasses()) {
                        if (prof != null) {
                            prof.spStep(SpStep.ProcPostFrame, FPP, filter.getName(), pass.toString());
                        }
                        pass.beforeRender();

                        // Set scene texture if required by the pass
                        if (pass.requiresSceneAsTexture()) {
                            pass.getPassMaterial().setTexture("Texture", tex);
                            if (tex.getImage().getMultiSamples() > 1) {
                                pass.getPassMaterial().setInt("NumSamples", tex.getImage().getMultiSamples());
                            } else {
                                pass.getPassMaterial().clearParam("NumSamples");

                            }
                        }

                        // Set depth texture if required by the pass
                        if (pass.requiresDepthAsTexture()) {
                            pass.getPassMaterial().setTexture("DepthTexture", depthTexture);
                            if (msDepth) {
                                pass.getPassMaterial().setInt("NumSamplesDepth", depthTexture.getImage().getMultiSamples());
                            } else {
                                pass.getPassMaterial().clearParam("NumSamplesDepth");
                            }
                        }
                        renderProcessing(r, pass.getRenderFrameBuffer(), pass.getPassMaterial());
                    }
                }
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, FPP, filter.getName(), "postFrame");
                }
                filter.postFrame(renderManager, viewPort, buff, sceneFb);

                Material mat = filter.getMaterial();
                if (msDepth && filter.isRequiresDepthTexture()) {
                    mat.setInt("NumSamplesDepth", depthTexture.getImage().getMultiSamples());
                }
                
                if (filter.isRequiresSceneTexture()) {
                    mat.setTexture("Texture", tex);
                    if (tex.getImage().getMultiSamples() > 1) {
                        mat.setInt("NumSamples", tex.getImage().getMultiSamples());
                    } else {
                        mat.clearParam("NumSamples");
                    }
                }

                // Apply bilinear filtering if requested by the filter
                boolean wantsBilinear = filter.isRequiresBilinear();
                if (wantsBilinear) {
                    tex.setMagFilter(Texture.MagFilter.Bilinear);
                    tex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
                }

                // Determine target framebuffer and source texture for the next pass
                buff = outputBuffer;
                if (i != lastFilterIndex) {
                    buff = filter.getRenderFrameBuffer();
                    tex = filter.getRenderedTexture();
                }
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, FPP, filter.getName(), "render");
                }
                // Render the main filter pass
                renderProcessing(r, buff, mat);
                if (prof != null) {
                    prof.spStep(SpStep.ProcPostFrame, FPP, filter.getName(), "postFilter");
                }
                // Call filter's postFilter for final adjustments
                filter.postFilter(r, buff);

                // Revert texture filtering if it was changed
                if (wantsBilinear) {
                    tex.setMagFilter(Texture.MagFilter.Nearest);
                    tex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
                }
            }
        }
    }

    @Override
    public void postFrame(FrameBuffer out) {

        FrameBuffer sceneBuffer = renderFrameBuffer;
        if (renderFrameBufferMS != null && !renderer.getCaps().contains(Caps.OpenGL32)) {
            renderer.copyFrameBuffer(renderFrameBufferMS, renderFrameBuffer, true, true);
        } else if (renderFrameBufferMS != null) {
            sceneBuffer = renderFrameBufferMS;
        }

        // Execute the filter chain
        renderFilterChain(renderer, sceneBuffer);

        // Restore the original output framebuffer for the viewport
        renderer.setFrameBuffer(outputBuffer);

        // viewport can be null if no filters are enabled
        if (viewPort != null) {
            renderManager.setCamera(viewPort.getCamera(), false);
        }
    }

    @Override
    public void preFrame(float tpf) {
        if (filters.isEmpty() || lastFilterIndex == -1) {
            // If no filters are enabled, restore the camera's original viewport
            // and output framebuffer to bypass the post-processor.
            if (cameraInit) {
                viewPort.getCamera().resize(originalWidth, originalHeight, true);
                viewPort.getCamera().setViewPort(left, right, bottom, top);
                viewPort.setOutputFrameBuffer(outputBuffer);
                cameraInit = false;
            }
        } else {
            setupViewPortFrameBuffer();
            // If in a multi-view situation, resize the camera to the viewport size
            // so that the back buffer is rendered correctly for filtering.
            if (multiView) {
                viewPort.getCamera().resize(width, height, false);
                viewPort.getCamera().setViewPort(0, 1, 0, 1);
                viewPort.getCamera().update();
                renderManager.setCamera(viewPort.getCamera(), false);
            }
        }

        // Call preFrame on all enabled filters
        for (Filter filter : filters.getArray()) {
            if (filter.isEnabled()) {
                if (prof != null) {
                    prof.spStep(SpStep.ProcPreFrame, FPP, filter.getName());
                }
                filter.preFrame(tpf);
            }
        }
    }

    /**
     * Sets the enabled state of a specific filter. If the filter is part of
     * this processor's list, its `enabled` flag is updated, and the
     * `lastFilterIndex` is recomputed.
     *
     * @param filter The {@link Filter} to modify (not null).
     * @param enabled True to enable the filter, false to disable it.
     */
    protected void setFilterState(Filter filter, boolean enabled) {
        if (filters.contains(filter)) {
            filter.enabled = enabled;
            updateLastFilterIndex();
        }
    }

    /**
     * Computes the index of the last enabled filter in the list. This is used
     * to determine which filter should render to the final output framebuffer
     * and which should render to intermediate framebuffers. If no filters are
     * enabled, the viewport's output framebuffer is restored to its original.
     */
    private void updateLastFilterIndex() {
        lastFilterIndex = -1;
        for (int i = filters.size() - 1; i >= 0 && lastFilterIndex == -1; i--) {
            if (filters.get(i).isEnabled()) {
                lastFilterIndex = i;
                // If the FPP is initialized but the viewport framebuffer is the
                // original output framebuffer (meaning no filter was enabled
                // previously), then redirect it to the FPP's internal framebuffer.
                if (isInitialized() && viewPort.getOutputFrameBuffer() == outputBuffer) {
                    setupViewPortFrameBuffer();
                }
                return;
            }
        }
        // If no filters are enabled, restore the original framebuffer to the viewport.
        if (isInitialized() && lastFilterIndex == -1) {
            viewPort.setOutputFrameBuffer(outputBuffer);
        }
    }

    @Override
    public void cleanup() {
        if (viewPort != null) {
            // Reset the viewport camera and output framebuffer to their initial values
            viewPort.getCamera().resize(originalWidth, originalHeight, true);
            viewPort.getCamera().setViewPort(left, right, bottom, top);
            viewPort.setOutputFrameBuffer(outputBuffer);
            viewPort = null;

            // Dispose of internal framebuffers and textures
            if (renderFrameBuffer != null) {
                renderFrameBuffer.dispose();
            }
            if (depthTexture != null) {
                depthTexture.getImage().dispose();
            }
            filterTexture.getImage().dispose();
            if (renderFrameBufferMS != null) {
                renderFrameBufferMS.dispose();
            }
            for (Filter filter : filters.getArray()) {
                filter.cleanup(renderer);
            }
        }
    }

    /**
     * Sets the profiler instance for this processor.
     *
     * @param profiler The `AppProfiler` instance to use for performance monitoring.
     */
    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

    /**
     * Reshapes the `FilterPostProcessor` when the viewport or canvas size changes.
     * This method recalculates internal framebuffer dimensions, creates new
     * framebuffers and textures if necessary (e.g., for anti-aliasing), and
     * reinitializes all filters with the new dimensions. It also detects
     * multi-view scenarios.
     *
     * @param vp The `ViewPort` being reshaped.
     * @param w The new width of the viewport's canvas.
     * @param h The new height of the viewport's canvas.
     */
    @Override
    public void reshape(ViewPort vp, int w, int h) {
        Camera cam = vp.getCamera();
        // This sets the camera viewport to its full extent (0-1) for rendering to the FPP's internal buffer.
        cam.setViewPort(left, right, bottom, top);
        // Resizing the camera to fit the new viewport and saving original dimensions
        cam.resize(w, h, true);
        left = cam.getViewPortLeft();
        right = cam.getViewPortRight();
        top = cam.getViewPortTop();
        bottom = cam.getViewPortBottom();
        originalWidth = w;
        originalHeight = h;

        // Computing real dimension of the viewport based on its relative size within the canvas
        width = (int) (w * (Math.abs(right - left)));
        height = (int) (h * (Math.abs(bottom - top)));
        width = Math.max(1, width);
        height = Math.max(1, height);

        // Test if original dimensions differ from actual viewport dimensions.
        // If they are different, we are in a multiview situation, and the
        // camera must be handled differently (e.g., resized to the sub-viewport).
        if (originalWidth != width || originalHeight != height) {
            multiView = true;
        }

        cameraInit = true;
        computeDepth = false;

        if (renderFrameBuffer == null && renderFrameBufferMS == null) {
            outputBuffer = viewPort.getOutputFrameBuffer();
        }

        Collection<Caps> caps = renderer.getCaps();

        // antialiasing on filters only supported in opengl 3 due to depth read problem
        if (numSamples > 1 && caps.contains(Caps.FrameBufferMultisample)) {
            renderFrameBufferMS = new FrameBuffer(width, height, numSamples);

            // If OpenGL 3.2+ is supported, multisampled textures can be attached directly
            if (caps.contains(Caps.OpenGL32)) {
                Texture2D msColor = new Texture2D(width, height, numSamples, fbFormat);
                Texture2D msDepth = new Texture2D(width, height, numSamples, depthFormat);
                renderFrameBufferMS.setDepthTarget(FrameBufferTarget.newTarget(msDepth));
                renderFrameBufferMS.addColorTarget(FrameBufferTarget.newTarget(msColor));
                filterTexture = msColor;
                depthTexture = msDepth;
            } else {
                // Otherwise, multisampled framebuffer must use internal texture, which cannot be directly read
                renderFrameBufferMS.setDepthTarget(FrameBufferTarget.newTarget(depthFormat));
                renderFrameBufferMS.addColorTarget(FrameBufferTarget.newTarget(fbFormat));
            }
        }

        // Setup single-sampled framebuffer if no multisampling, or if OpenGL 3.2+ is not supported
        // (because for non-GL32, a single-sampled buffer is still needed to copy MS content into).
        if (numSamples <= 1 || !caps.contains(Caps.OpenGL32) || !caps.contains(Caps.FrameBufferMultisample)) {
            renderFrameBuffer = new FrameBuffer(width, height, 1);
            renderFrameBuffer.setDepthTarget(FrameBufferTarget.newTarget(depthFormat));
            filterTexture = new Texture2D(width, height, fbFormat);
            renderFrameBuffer.addColorTarget(FrameBufferTarget.newTarget(filterTexture));
        }

        // Set names for debugging
        if (renderFrameBufferMS != null) {
            renderFrameBufferMS.setName("FilterPostProcessor MS");
        }

        if (renderFrameBuffer != null) {
            renderFrameBuffer.setName("FilterPostProcessor");
        }

        // Initialize all existing filters with the new dimensions
        for (Filter filter : filters.getArray()) {
            initFilter(filter, vp);
        }
        setupViewPortFrameBuffer();
    }

    /**
     * Returns the number of samples used for anti-aliasing.
     *
     * @return The number of samples.
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     * Removes all filters currently added to this processor.
     */
    public void removeAllFilters() {
        filters.clear();
        updateLastFilterIndex();
    }

    /**
     * Sets the number of samples for anti-aliasing. A value of 1 means no
     * anti-aliasing. This method should generally be called before the
     * processor is initialized to have an effect.
     *
     * @param numSamples The number of samples. Must be greater than 0.
     * @throws IllegalArgumentException If `numSamples` is less than or equal to 0.
     */
    public void setNumSamples(int numSamples) {
        if (numSamples <= 0) {
            throw new IllegalArgumentException("numSamples must be > 0");
        }

        this.numSamples = numSamples;
    }

    /**
     * Sets the asset manager for this processor
     *
     * @param assetManager to load assets
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Sets the preferred `Image.Format` to be used for the internal frame buffer's
     * color buffer.
     *
     * @param fbFormat The desired `Format` for the color buffer.
     */
    public void setFrameBufferFormat(Format fbFormat) {
        this.fbFormat = fbFormat;
    }

    /**
     * Sets the preferred `Image.Format` to be used for the internal frame buffer's
     * depth buffer.
     *
     * @param depthFormat The desired `Format` for the depth buffer.
     */
    public void setFrameBufferDepthFormat(Format depthFormat) {
        this.depthFormat = depthFormat;
    }

    /**
     * Returns the `Image.Format` currently used for the internal frame buffer's
     * depth buffer.
     *
     * @return The current depth `Format`.
     */
    public Format getFrameBufferDepthFormat() {
        return depthFormat;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(numSamples, "numSamples", 0);
        oc.writeSavableArrayList(new ArrayList(filters), "filters", null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        numSamples = ic.readInt("numSamples", 0);
        filters = new SafeArrayList<>(Filter.class, ic.readSavableArrayList("filters", null));
        for (Filter filter : filters.getArray()) {
            filter.setProcessor(this);
            setFilterState(filter, filter.isEnabled());
        }
        assetManager = im.getAssetManager();
    }

    /**
     * For internal use only.
     * Returns the depth texture generated from the scene's depth buffer.
     * This texture is available if any filter requires a depth texture.
     *
     * @return The `Texture2D` containing the scene's depth information, or null if not computed.
     */
    public Texture2D getDepthTexture() {
        return depthTexture;
    }

    /**
     * For internal use only.
     * Returns the color texture that contains the rendered scene or the output
     * of the previous filter in the chain. This texture serves as input for subsequent filters.
     *
     * @return The `Texture2D` containing the scene's color information or the intermediate filter output.
     */
    public Texture2D getFilterTexture() {
        return filterTexture;
    }

    /**
     * Returns the first filter in the managed list that is assignable from the
     * given filter type. Useful for retrieving specific filters to modify their properties.
     *
     * @param <T> The type of the filter to retrieve.
     * @param filterType The `Class` object representing the filter type.
     * @return A filter instance assignable from `filterType`, or null if no such filter is found.
     */
    @SuppressWarnings("unchecked")
    public <T extends Filter> T getFilter(Class<T> filterType) {
        for (Filter f : filters.getArray()) {
            if (filterType.isAssignableFrom(f.getClass())) {
                return (T) f;
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable version of the list of filters currently
     * managed by this processor.
     *
     * @return An unmodifiable `List` of {@link Filter} objects.
     */
    public List<Filter> getFilterList(){
        return Collections.unmodifiableList(filters);
    }

    private void setupViewPortFrameBuffer() {
        if (renderFrameBufferMS != null) {
            viewPort.setOutputFrameBuffer(renderFrameBufferMS);
        } else {
            viewPort.setOutputFrameBuffer(renderFrameBuffer);
        }
    }
}
