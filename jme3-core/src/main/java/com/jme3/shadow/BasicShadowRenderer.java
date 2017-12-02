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
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 * BasicShadowRenderer uses standard shadow mapping with one map
 * it's useful to render shadows in a small scene, but edges might look a bit jagged.
 * 
 * @author Kirill Vainer
 * @deprecated use {@link DirectionalLightShadowRenderer} with one split.
 */
@Deprecated
public class BasicShadowRenderer implements SceneProcessor {

    private RenderManager renderManager;
    private ViewPort viewPort;
    private FrameBuffer shadowFB;
    private Texture2D shadowMap;
    private Camera shadowCam;
    private Material preshadowMat;
    private Material postshadowMat;
    private Picture dispPic = new Picture("Picture");
    private boolean noOccluders = false;
    private Vector3f[] points = new Vector3f[8];
    private Vector3f direction = new Vector3f();
    protected Texture2D dummyTex;
    private float shadowMapSize;

    protected GeometryList lightReceivers = new GeometryList(new OpaqueComparator());
    protected GeometryList shadowOccluders = new GeometryList(new OpaqueComparator());
    private AppProfiler prof;

    /**
     * Creates a BasicShadowRenderer
     * @param manager the asset manager
     * @param size the size of the shadow map (the map is square)
     */
    public BasicShadowRenderer(AssetManager manager, int size) {
        shadowFB = new FrameBuffer(size, size, 1);
        shadowMap = new Texture2D(size, size, Format.Depth);
        shadowFB.setDepthTexture(shadowMap);
        shadowCam = new Camera(size, size);
        
         //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
        dummyTex = new Texture2D(size, size, Format.RGBA8);        
        shadowFB.setColorTexture(dummyTex);
        shadowMapSize = (float)size;
        preshadowMat = new Material(manager, "Common/MatDefs/Shadow/PreShadow.j3md");
        postshadowMat = new Material(manager, "Common/MatDefs/Shadow/BasicPostShadow.j3md");
        postshadowMat.setTexture("ShadowMap", shadowMap);

        dispPic.setTexture(manager, shadowMap, false);

        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        viewPort = vp;

        reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }

    public boolean isInitialized() {
        return viewPort != null;
    }

    /**
     * returns the light direction used for this processor
     * @return 
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * sets the light direction to use to computs shadows
     * @param direction 
     */
    public void setDirection(Vector3f direction) {
        this.direction.set(direction).normalizeLocal();
    }

    /**
     * debug only
     * @return 
     */
    public Vector3f[] getPoints() {
        return points;
    }

    /**
     * debug only
     * returns the shadow camera 
     * @return 
     */
    public Camera getShadowCamera() {
        return shadowCam;
    }

    public void postQueue(RenderQueue rq) {
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), ShadowMode.Receive, lightReceivers);
        }

        // update frustum points based on current camera
        Camera viewCam = viewPort.getCamera();
        ShadowUtil.updateFrustumPoints(viewCam,
                viewCam.getFrustumNear(),
                viewCam.getFrustumFar(),
                1.0f,
                points);

        Vector3f frustaCenter = new Vector3f();
        for (Vector3f point : points) {
            frustaCenter.addLocal(point);
        }
        frustaCenter.multLocal(1f / 8f);

        // update light direction
        shadowCam.setProjectionMatrix(null);
        shadowCam.setParallelProjection(true);
//        shadowCam.setFrustumPerspective(45, 1, 1, 20);

        shadowCam.lookAtDirection(direction, Vector3f.UNIT_Y);
        shadowCam.update();
        shadowCam.setLocation(frustaCenter);
        shadowCam.update();
        shadowCam.updateViewProjection();

        // render shadow casters to shadow map
        ShadowUtil.updateShadowCamera(viewPort, lightReceivers, shadowCam, points, shadowOccluders, shadowMapSize);
        if (shadowOccluders.size() == 0) {
            noOccluders = true;
            return;
        } else {
            noOccluders = false;
        }            
        
        Renderer r = renderManager.getRenderer();
        renderManager.setCamera(shadowCam, false);
        renderManager.setForcedMaterial(preshadowMat);

        r.setFrameBuffer(shadowFB);
        r.clearBuffers(true, true, true);
        viewPort.getQueue().renderShadowQueue(shadowOccluders, renderManager, shadowCam, true);
        r.setFrameBuffer(viewPort.getOutputFrameBuffer());

        renderManager.setForcedMaterial(null);
        renderManager.setCamera(viewCam, false);
    }

    /**
     * debug only
     * @return 
     */
    public Picture getDisplayPicture() {
        return dispPic;
    }

    public void postFrame(FrameBuffer out) {
        if (!noOccluders) {
            postshadowMat.setMatrix4("LightViewProjectionMatrix", shadowCam.getViewProjectionMatrix());
            renderManager.setForcedMaterial(postshadowMat);
            viewPort.getQueue().renderShadowQueue(lightReceivers, renderManager, viewPort.getCamera(), true);
            renderManager.setForcedMaterial(null);
        }
    }

    public void preFrame(float tpf) {
    }

    public void cleanup() {
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

    public void reshape(ViewPort vp, int w, int h) {
        dispPic.setPosition(w / 20f, h / 20f);
        dispPic.setWidth(w / 5f);
        dispPic.setHeight(h / 5f);
    }
}
