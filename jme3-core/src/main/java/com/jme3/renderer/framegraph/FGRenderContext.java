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
package com.jme3.renderer.framegraph;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.util.function.Predicate;

/**
 * In order to be compatible with existing logic, FGRenderContext is currently just a local proxy, and may gradually replace the existing state machine manager in the future.
 * @author JohnKkk
 */
public class FGRenderContext {
    
    private final RenderManager renderManager;
    private ViewPort viewPort;
    private AppProfiler profiler;
    private final CameraSize camSize = new CameraSize();
    private float tpf;
    private Geometry screen;
    private final Material transferMat;
    
    private String forcedTechnique;
    private Material forcedMat;
    private FrameBuffer frameBuffer;
    private GeometryRenderHandler geomRender;
    private Predicate<Geometry> geomFilter;
    private RenderState renderState;

    public FGRenderContext(AssetManager assetManager, RenderManager renderManager) {
        this.renderManager = renderManager;
        transferMat = new Material(assetManager, "Common/MatDefs/ShadingCommon/TextureTransfer.j3md");
        //transferMat.getAdditionalRenderState().setDepthFunc(RenderState.TestFunction.Less);
    }
    
    public void update(ViewPort vp, AppProfiler profiler, float tpf) {
        this.viewPort = vp;
        this.profiler = profiler;
        this.tpf = tpf;
        if (viewPort == null) {
            throw new NullPointerException("ViewPort cannot be null.");
        }
        camSize.update(viewPort.getCamera());
    }
    
    /**
     * Saves the current render settings.
     */
    public void pushRenderSettings() {
        forcedTechnique = renderManager.getForcedTechnique();
        forcedMat = renderManager.getForcedMaterial();
        frameBuffer = renderManager.getRenderer().getCurrentFrameBuffer();
        geomRender = renderManager.getGeometryRenderHandler();
        geomFilter = renderManager.getRenderFilter();
        renderState = renderManager.getForcedRenderState();
    }
    /**
     * Applies saved render settings.
     */
    public void popRenderSettings() {
        renderManager.setForcedTechnique(forcedTechnique);
        renderManager.setForcedMaterial(forcedMat);
        renderManager.getRenderer().setFrameBuffer(frameBuffer);
        renderManager.setGeometryRenderHandler(geomRender);
        renderManager.setRenderFilter(geomFilter);
        renderManager.setForcedRenderState(renderState);
        renderManager.getRenderer().setDepthRange(0, 1);
        if (viewPort.isClearColor()) {
            renderManager.getRenderer().setBackgroundColor(viewPort.getBackgroundColor());
        }
    }
    
    public void renderViewPortQueue(RenderQueue.Bucket bucket, boolean clear) {
        viewPort.getQueue().renderQueue(bucket, renderManager, viewPort.getCamera(), clear);
    }
    public void renderFullscreen(Material mat) {
        if (screen == null) {
            Mesh mesh = new Mesh();
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(
                0, 0, 0,
                1, 0, 0,
                0, 1, 0,
                1, 1, 0
            ));
            mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(
                0, 1, 2,
                1, 3, 2
            ));
            mesh.updateBound();
            mesh.updateCounts();
            mesh.setStatic();
            screen = new Geometry("Screen", mesh);
        }
        screen.setMaterial(mat);
        screen.updateGeometricState();
        renderManager.renderGeometry(screen);
    }
    public void transferTextures(Texture2D color, Texture2D depth) {
        transferTextures(color, depth, BlendMode.Off);
    }
    public void transferTextures(Texture2D color, Texture2D depth, BlendMode blend) {
        boolean writeDepth = depth != null;
        if (color != null || writeDepth) {
            transferMat.setTexture("ColorMap", color);
            transferMat.setTexture("DepthMap", depth);
            transferMat.getAdditionalRenderState().setDepthTest(writeDepth);
            transferMat.getAdditionalRenderState().setDepthWrite(writeDepth);
            //transferMat.getAdditionalRenderState().setBlendMode(BlendMode.Off);
            System.out.println("blend="+blend);
            transferMat.getAdditionalRenderState().setBlendMode(blend);
            //transferMat.setTransparent(blend == BlendMode.Alpha
            //        || blend == BlendMode.AlphaAdditive || blend == BlendMode.AlphaSumA);
            //System.out.println("blendmode="+transferMat.getAdditionalRenderState().getBlendMode());
            //System.out.println("alpha="+transferMat.isTransparent());
            renderFullscreen(transferMat);
        }
    }
    public FrameBuffer.FrameBufferTextureTarget createTextureTarget(Texture tex) {
        return FrameBuffer.FrameBufferTarget.newTarget(tex);
    }
    
    public void setFrameBuffer(FrameBuffer fbo) {
        renderManager.getRenderer().setFrameBuffer(fbo);
    }
    public void setFrameBuffer(FrameBuffer fbo, boolean clearColor, boolean clearDepth, boolean clearStencil) {
        setFrameBuffer(fbo);
        renderManager.getRenderer().clearBuffers(clearColor, clearDepth, clearStencil);
    }
    
    public RenderManager getRenderManager() {
        return renderManager;
    }
    public ViewPort getViewPort() {
        return viewPort;
    }
    public AppProfiler getProfiler() {
        return profiler;
    }
    public CameraSize getCameraSize() {
        return camSize;
    }
    public Renderer getRenderer() {
        return renderManager.getRenderer();
    }
    public RenderQueue getRenderQueue() {
        if (viewPort != null) {
            return viewPort.getQueue();
        } else {
            return null;
        }
    }
    public float getTpf() {
        return tpf;
    }
    public int getWidth() {
        return camSize.getWidth();
    }
    public int getHeight() {
        return camSize.getHeight();
    }
    
    public boolean isProfilerAvailable() {
        return profiler != null;
    }
    
}
