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
import com.jme3.renderer.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A FilterPostProcessor is a processor that can apply several {@link Filter}s to a rendered scene<br>
 * It manages a list of filters that will be applied in the order in which they've been added to the list
 * @author Rémy Bouquet aka Nehon
 */
public class FilterPostProcessor implements SceneProcessor, Savable {

    private RenderManager renderManager;
    private Renderer renderer;
    private ViewPort viewPort;
    private FrameBuffer renderFrameBufferMS;
    private int numSamples = 1;
    private FrameBuffer renderFrameBuffer;
    private Texture2D filterTexture;
    private Texture2D depthTexture;
    private List<Filter> filters = new ArrayList<Filter>();
    private AssetManager assetManager;
    private Camera filterCam = new Camera(1, 1);
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
    
    /**
     * Create a FilterProcessor 
     * @param assetManager the assetManager
     */
    public FilterPostProcessor(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Don't use this constructor use {@link FilterPostProcessor(AssetManager assetManager)}<br>
     * This constructor is used for serialization only
     */
    public FilterPostProcessor() {
    }

    /**
     * Adds a filter to the filters list<br>
     * @param filter the filter to add
     */
    public void addFilter(Filter filter) {
        filters.add(filter);        

        if (isInitialized()) {
            initFilter(filter, viewPort);
        }

        setFilterState(filter, filter.isEnabled());

    }

    /**
     * removes this filters from the filters list
     * @param filter 
     */
    public void removeFilter(Filter filter) {
        filters.remove(filter);
        filter.cleanup(renderer);
        updateLastFilterIndex();
    }

    public Iterator<Filter> getFilterIterator() {
        return filters.iterator();
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        renderer = rm.getRenderer();
        viewPort = vp;
        fsQuad = new Picture("filter full screen quad");

        Camera cam = vp.getCamera();

        //save view port diensions
        left = cam.getViewPortLeft();
        right = cam.getViewPortRight();
        top = cam.getViewPortTop();
        bottom = cam.getViewPortBottom();
        originalWidth = cam.getWidth();
        originalHeight = cam.getHeight();
        //first call to reshape
        reshape(vp, cam.getWidth(), cam.getHeight());
    }

    /**
     * init the given filter
     * @param filter
     * @param vp 
     */
    private void initFilter(Filter filter, ViewPort vp) {
        filter.setProcessor(this);
        if (filter.isRequiresDepthTexture()) {
            if (!computeDepth && renderFrameBuffer != null) {                
                depthTexture = new Texture2D(width, height, Format.Depth24);
                renderFrameBuffer.setDepthTexture(depthTexture);
            }
            computeDepth = true;
            filter.init(assetManager, renderManager, vp, width, height);
            filter.setDepthTexture(depthTexture);
        } else {
            filter.init(assetManager, renderManager, vp, width, height);
        }
    }

    /**
     * renders a filter on a fullscreen quad
     * @param r
     * @param buff
     * @param mat 
     */
    private void renderProcessing(Renderer r, FrameBuffer buff, Material mat) {
        if (buff == outputBuffer) {
            fsQuad.setWidth(width);
            fsQuad.setHeight(height);
            filterCam.resize(originalWidth, originalHeight, true);
            fsQuad.setPosition(left * originalWidth, bottom * originalHeight);
        } else {
            fsQuad.setWidth(buff.getWidth());
            fsQuad.setHeight(buff.getHeight());
            filterCam.resize(buff.getWidth(), buff.getHeight(), true);
            fsQuad.setPosition(0, 0);
        }

        if (mat.getAdditionalRenderState().isDepthWrite()) {
            mat.getAdditionalRenderState().setDepthTest(false);
            mat.getAdditionalRenderState().setDepthWrite(false);
        }

        fsQuad.setMaterial(mat);
        fsQuad.updateGeometricState();

        renderManager.setCamera(filterCam, true);
        r.setFrameBuffer(buff);
        r.clearBuffers(false, true, true);
        renderManager.renderGeometry(fsQuad);

    }

    public boolean isInitialized() {
        return viewPort != null;
    }

    public void postQueue(RenderQueue rq) {

        for (Iterator<Filter> it = filters.iterator(); it.hasNext();) {
            Filter filter = it.next();
            if (filter.isEnabled()) {
                filter.postQueue(renderManager, viewPort);
            }
        }

    }
    Picture pic = new Picture("debug");

    /**
     * iterate through the filter list and renders filters
     * @param r
     * @param sceneFb 
     */
    private void renderFilterChain(Renderer r, FrameBuffer sceneFb) {
        Texture2D tex = filterTexture;
        FrameBuffer buff = sceneFb;
        boolean msDepth = depthTexture != null && depthTexture.getImage().getMultiSamples() > 1;
        for (int i = 0; i < filters.size(); i++) {
            Filter filter = filters.get(i);
            if (filter.isEnabled()) {
                if (filter.getPostRenderPasses() != null) {
                    for (Iterator<Filter.Pass> it1 = filter.getPostRenderPasses().iterator(); it1.hasNext();) {
                        Filter.Pass pass = it1.next();
                        pass.beforeRender();
                        if (pass.requiresSceneAsTexture()) {
                            pass.getPassMaterial().setTexture("Texture", tex);
                            if (tex.getImage().getMultiSamples() > 1) {
                                pass.getPassMaterial().setInt("NumSamples", tex.getImage().getMultiSamples());
                            } else {
                                pass.getPassMaterial().clearParam("NumSamples");

                            }
                        }
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

                buff = outputBuffer;
                if (i != lastFilterIndex) {
                    buff = filter.getRenderFrameBuffer();
                    tex = filter.getRenderedTexture();

                }
                renderProcessing(r, buff, mat);
            }
        }
    }

    public void postFrame(FrameBuffer out) {

        FrameBuffer sceneBuffer = renderFrameBuffer;
        if (renderFrameBufferMS != null && !renderer.getCaps().contains(Caps.OpenGL31)) {
            renderer.copyFrameBuffer(renderFrameBufferMS, renderFrameBuffer);
        } else if (renderFrameBufferMS != null) {
            sceneBuffer = renderFrameBufferMS;
        }
        renderFilterChain(renderer, sceneBuffer);
        renderer.setFrameBuffer(outputBuffer);

        //viewport can be null if no filters are enabled
        if (viewPort != null) {
            renderManager.setCamera(viewPort.getCamera(), false);
        }

    }

    public void preFrame(float tpf) {
        if (filters.isEmpty() || lastFilterIndex == -1) {
            //If the camera is initialized and there are no filter to render, the camera viewport is restored as it was
            if (cameraInit) {
                viewPort.getCamera().resize(originalWidth, originalHeight, true);
                viewPort.getCamera().setViewPort(left, right, bottom, top);
                viewPort.setOutputFrameBuffer(outputBuffer);
                cameraInit = false;
            }

        } else {
            if (renderFrameBufferMS != null) {
                viewPort.setOutputFrameBuffer(renderFrameBufferMS);
            } else {
                viewPort.setOutputFrameBuffer(renderFrameBuffer);
            }
            //init of the camera if it wasn't already
            if (!cameraInit) {
                viewPort.getCamera().resize(width, height, true);
                viewPort.getCamera().setViewPort(0, 1, 0, 1);
            }
        }

        for (Iterator<Filter> it = filters.iterator(); it.hasNext();) {
            Filter filter = it.next();
            if (filter.isEnabled()) {
                filter.preFrame(tpf);
            }
        }

    }

    /**
     * sets the filter to enabled or disabled
     * @param filter
     * @param enabled 
     */
    protected void setFilterState(Filter filter, boolean enabled) {
        if (filters.contains(filter)) {
            filter.enabled = enabled;
            updateLastFilterIndex();
        }
    }

    /**
     * compute the index of the last filter to render
     */
    private void updateLastFilterIndex() {
        lastFilterIndex = -1;
        for (int i = filters.size() - 1; i >= 0 && lastFilterIndex == -1; i--) {
            if (filters.get(i).isEnabled()) {
                lastFilterIndex = i;
                return;
            }
        }
        if (lastFilterIndex == -1) {
            cleanup();
        }
    }

    public void cleanup() {
        if (viewPort != null) {
            //reseting the viewport camera viewport to its initial value
            viewPort.getCamera().resize(originalWidth, originalHeight, true);
            viewPort.getCamera().setViewPort(left, right, bottom, top);
            viewPort.setOutputFrameBuffer(outputBuffer);            
            viewPort = null;
            for (Filter filter : filters) {
                filter.cleanup(renderer);
            }
        }

    }

    public void reshape(ViewPort vp, int w, int h) {
        //this has no effect at first init but is useful when resizing the canvas with multi views
        Camera cam = vp.getCamera();
        cam.setViewPort(left, right, bottom, top);
        //resizing the camera to fit the new viewport and saving original dimensions
        cam.resize(w, h, false);
        left = cam.getViewPortLeft();
        right = cam.getViewPortRight();
        top = cam.getViewPortTop();
        bottom = cam.getViewPortBottom();
        originalWidth = w;
        originalHeight = h;
        cam.setViewPort(0, 1, 0, 1);

        //computing real dimension of the viewport and resizing he camera 
        width = (int) (w * (Math.abs(right - left)));
        height = (int) (h * (Math.abs(bottom - top)));
        width = Math.max(1, width);
        height = Math.max(1, height);
        cam.resize(width, height, false);
        cameraInit = true;
        computeDepth = false;

        if (renderFrameBuffer == null) {
            outputBuffer = viewPort.getOutputFrameBuffer();
        }

        Collection<Caps> caps = renderer.getCaps();

        //antialiasing on filters only supported in opengl 3 due to depth read problem
        if (numSamples > 1 && caps.contains(Caps.FrameBufferMultisample)) {
            renderFrameBufferMS = new FrameBuffer(width, height, numSamples);
            if (caps.contains(Caps.OpenGL31)) {
                Texture2D msColor = new Texture2D(width, height, numSamples, Format.RGBA8);
                Texture2D msDepth = new Texture2D(width, height, numSamples, Format.Depth);
                renderFrameBufferMS.setDepthTexture(msDepth);
                renderFrameBufferMS.setColorTexture(msColor);
                filterTexture = msColor;
                depthTexture = msDepth;
            } else {
                renderFrameBufferMS.setDepthBuffer(Format.Depth);
                renderFrameBufferMS.setColorBuffer(Format.RGBA8);
            }
        }

        if (numSamples <= 1 || !caps.contains(Caps.OpenGL31)) {
            renderFrameBuffer = new FrameBuffer(width, height, 1);
            renderFrameBuffer.setDepthBuffer(Format.Depth);
            filterTexture = new Texture2D(width, height, Format.RGBA8);
            renderFrameBuffer.setColorTexture(filterTexture);
        }

        for (Iterator<Filter> it = filters.iterator(); it.hasNext();) {
            Filter filter = it.next();
            initFilter(filter, vp);
        }

        if (renderFrameBufferMS != null) {
            viewPort.setOutputFrameBuffer(renderFrameBufferMS);
        } else {
            viewPort.setOutputFrameBuffer(renderFrameBuffer);
        }
    }

    /**
     * return the number of samples for antialiasing
     * @return numSamples
     */
    public int getNumSamples() {
        return numSamples;
    }

    /**
     *
     * Removes all the filters from this processor
     */
    public void removeAllFilters() {
        filters.clear();
        updateLastFilterIndex();
    }

    /**
     * Sets the number of samples for antialiasing
     * @param numSamples the number of Samples
     */
    public void setNumSamples(int numSamples) {
        if (numSamples <= 0) {
            throw new IllegalArgumentException("numSamples must be > 0");
        }

        this.numSamples = numSamples;
    }

    /**
     * Sets the asset manager for this processor
     * @param assetManager
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(numSamples, "numSamples", 0);
        oc.writeSavableArrayList((ArrayList) filters, "filters", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        numSamples = ic.readInt("numSamples", 0);
        filters = ic.readSavableArrayList("filters", null);
        for (Filter filter : filters) {
            filter.setProcessor(this);
            setFilterState(filter, filter.isEnabled());
        }
        assetManager = im.getAssetManager();
    }

    /**
     * For internal use only<br>
     * returns the depth texture of the scene
     * @return 
     */    
    public Texture2D getDepthTexture() {
        return depthTexture;
    }

    /**
     * For internal use only<br>
     * returns the rendered texture of the scene
     * @return 
     */
    public Texture2D getFilterTexture() {
        return filterTexture;
    }
}
