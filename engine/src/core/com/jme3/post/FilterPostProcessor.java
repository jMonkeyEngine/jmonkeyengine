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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
    private int lastFilterIndex = -1;

    /**
     * Create a FilterProcessor constructor
     * @param assetManager the Asset Manager
     */
    public FilterPostProcessor(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Don't use this constructor use FilterPostProcessor(AssetManager assetManager)
     */
    public FilterPostProcessor() {
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
        
        if (isInitialized()) {
            initFilter(filter, viewPort);
        }
        if(filter.isEnabled()){
            lastFilterIndex = filters.size() - 1;
        }
    }

    public void removeFilter(Filter filter) {
        for (Iterator<Filter> it = filters.iterator(); it.hasNext();) {
            if (it.next() == filter) {
                it.remove();
            }
        }
        filter.cleanup(renderer);
        updateLastFilterIndex();
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        renderer = rm.getRenderer();
        viewPort = vp;
        fsQuad = new Picture("filter full screen quad");

        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    private void initFilter(Filter filter, ViewPort vp) {
        filter.init(this, assetManager, renderManager, vp, width, height);
        if (filter.isRequiresDepthTexture()) {
            if (!computeDepth && renderFrameBuffer != null) {
                depthTexture = new Texture2D(width, height, Format.Depth24);
                renderFrameBuffer.setDepthTexture(depthTexture);
            }
            computeDepth = true;
            filter.getMaterial().setTexture("DepthTexture", depthTexture);
        }
    }

    private void renderProcessing(Renderer r, FrameBuffer buff, Material mat) {
        if (buff == null) {
            fsQuad.setWidth(width);
            fsQuad.setHeight(height);
            filterCam.resize(width, height, true);
        } else {
            fsQuad.setWidth(buff.getWidth());
            fsQuad.setHeight(buff.getHeight());
            filterCam.resize(buff.getWidth(), buff.getHeight(), true);
        }
        fsQuad.setMaterial(mat);
        fsQuad.updateGeometricState();
        renderManager.setCamera(filterCam, true);
        r.setFrameBuffer(buff);
        r.clearBuffers(true, true, true);
        renderManager.renderGeometry(fsQuad);
    }

    public boolean isInitialized() {
        return viewPort != null;
    }

    public void postQueue(RenderQueue rq) {
        for (Iterator<Filter> it = filters.iterator(); it.hasNext();) {
            Filter filter = it.next();
            if (filter.isEnabled()) {
                filter.preRender(renderManager, viewPort);
            }
        }
    }

    public void renderFilterChain(Renderer r) {
        Texture2D tex = filterTexture;
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

                Material mat = filter.getMaterial();
                if (msDepth && filter.isRequiresDepthTexture()) {
                    mat.setInt("NumSamplesDepth", depthTexture.getImage().getMultiSamples());
                }

                mat.setTexture("Texture", tex);
                if (tex.getImage().getMultiSamples() > 1) {
                    mat.setInt("NumSamples", tex.getImage().getMultiSamples());
                } else {
                    mat.clearParam("NumSamples");
                }

                FrameBuffer buff = outputBuffer;
                if (i != lastFilterIndex) {
                    buff = filter.getRenderFrameBuffer();
                    tex = filter.getRenderedTexture();
                }
                renderProcessing(r, buff, mat);
            }
        }
    }

    public void postFrame(FrameBuffer out) {
        //Added this to fix the issue where the filter were not rendered when an object in the scene had a DepthWrite to false. (particles for example)
        //there should be a better way...
     //   renderer.applyRenderState(RenderState.DEFAULT);
        if (renderFrameBufferMS != null && !renderer.getCaps().contains(Caps.OpenGL31)) {
            renderer.copyFrameBuffer(renderFrameBufferMS, renderFrameBuffer);
        }
        renderFilterChain(renderer);
    }

    public void preFrame(float tpf) {
        if (filters.isEmpty() || lastFilterIndex == -1) {
            viewPort.setOutputFrameBuffer(outputBuffer);
        } else {
            if (renderFrameBufferMS != null) {
                viewPort.setOutputFrameBuffer(renderFrameBufferMS);
            } else {
                viewPort.setOutputFrameBuffer(renderFrameBuffer);
            }
        }
        for (Iterator<Filter> it = filters.iterator(); it.hasNext();) {
            Filter filter = it.next();
            if (filter.isEnabled()) {
                filter.preFrame(tpf);
            }
        }
    }

    protected void setFilterState(Filter filter, boolean enabled) {
        if (filters.contains(filter)) {
            filter.enabled = enabled;
            updateLastFilterIndex();
        }
    }

    private void updateLastFilterIndex() {
        lastFilterIndex = -1;
        for (int i = filters.size() - 1; i >= 0 && lastFilterIndex == -1; i--) {
            if (filters.get(i).isEnabled()) {
                lastFilterIndex = i;
                return;
            }
        }

    }

    public void cleanup() {
        if (viewPort != null) {
            viewPort.setOutputFrameBuffer(outputBuffer);
            viewPort = null;
        }
    }

    public void reshape(ViewPort vp, int w, int h) {

        width = Math.max(1, w);
        height = Math.max(1, h);
        vp.getCamera().resize(width, height, true);
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
                //   samplePositions = ((LwjglRenderer) renderer).getFrameBufferSamplePositions(renderFrameBufferMS);
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

    /**
     * Writes the processor
     * @param ex
     * @throws IOException
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(numSamples, "numSamples", 0);
        oc.writeSavableArrayList((ArrayList) filters, "filters", null);
    }

    /**
     * Reads the processor
     * @param im
     * @throws IOException
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        numSamples = ic.readInt("numSamples", 0);
        filters = ic.readSavableArrayList("filters", null);
    }
}
