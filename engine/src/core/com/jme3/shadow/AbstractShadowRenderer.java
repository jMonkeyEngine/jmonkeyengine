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
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.List;

/**
 * abstract shadow renderer that holds commons feature to have for a shadow renderer
 * @author Rémy Bouquet aka Nehon
 */
public abstract class AbstractShadowRenderer implements SceneProcessor {

    protected int nbShadowMaps = 1;
    protected float shadowMapSize;
    protected float shadowIntensity = 0.7f;
    protected RenderManager renderManager;
    protected ViewPort viewPort;
    protected FrameBuffer[] shadowFB;
    protected Texture2D[] shadowMaps;
    protected Texture2D dummyTex;
    protected Material preshadowMat;
    protected Material postshadowMat;
    protected Matrix4f[] lightViewProjectionsMatrices;
    protected boolean noOccluders = false;
    protected AssetManager assetManager;
    protected boolean debug = false;
    protected float edgesThickness = 1.0f;
    protected EdgeFilteringMode edgeFilteringMode;
    protected CompareMode shadowCompareMode;
    protected Picture[] dispPic;
    protected boolean flushQueues = true;
    // define if the fallback material should be used.
    protected boolean needsfallBackMaterial = false;
    //Name of the post material technique
    protected String postTechniqueName = "PostShadow";
    //flags to know when to change params in the materials
    //a list of material of the post shadow queue geometries.
    protected List<Material> matCache = new ArrayList<Material>();

    /**
     * Create an abstract shadow renderer, this is to be called in extending classes
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     * @param nbShadowMaps the number of shadow maps rendered (the more shadow
     * maps the more quality, the less fps).
     */
    protected AbstractShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbShadowMaps) {

        this.assetManager = assetManager;
        this.postshadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PostShadow.j3md");
        this.nbShadowMaps = nbShadowMaps;
        this.shadowMapSize = shadowMapSize;
        shadowFB = new FrameBuffer[nbShadowMaps];
        shadowMaps = new Texture2D[nbShadowMaps];
        dispPic = new Picture[nbShadowMaps];
        lightViewProjectionsMatrices = new Matrix4f[nbShadowMaps];

        //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
        dummyTex = new Texture2D(shadowMapSize, shadowMapSize, Format.RGBA8);

        preshadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PreShadow.j3md");
        postshadowMat.setFloat("ShadowMapSize", shadowMapSize);

        for (int i = 0; i < nbShadowMaps; i++) {
            lightViewProjectionsMatrices[i] = new Matrix4f();
            shadowFB[i] = new FrameBuffer(shadowMapSize, shadowMapSize, 1);
            shadowMaps[i] = new Texture2D(shadowMapSize, shadowMapSize, Format.Depth);

            shadowFB[i].setDepthTexture(shadowMaps[i]);

            //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
            shadowFB[i].setColorTexture(dummyTex);

            postshadowMat.setTexture("ShadowMap" + i, shadowMaps[i]);

            //quads for debuging purpose
            dispPic[i] = new Picture("Picture" + i);
            dispPic[i].setTexture(assetManager, shadowMaps[i], false);
        }

        setShadowCompareMode(CompareMode.Hardware);
        setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
        setShadowIntensity(0.7f);

    }

    /**
     * set the post shadow material for this renderer
     * @param postShadowMat 
     */
    protected final void setPostShadowMaterial(Material postShadowMat) {
        this.postshadowMat = postShadowMat;
        postshadowMat.setFloat("ShadowMapSize", shadowMapSize);
        for (int i = 0; i < nbShadowMaps; i++) {
            postshadowMat.setTexture("ShadowMap" + i, shadowMaps[i]);
        }
        setShadowCompareMode(shadowCompareMode);
        setEdgeFilteringMode(edgeFilteringMode);
        setShadowIntensity(shadowIntensity);
    }

    /**
     * Sets the filtering mode for shadow edges see {@link FilterMode} for more
     * info
     *
     * @param filterMode
     */
    final public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
        if (filterMode == null) {
            throw new NullPointerException();
        }

        if (this.edgeFilteringMode == filterMode) {
            return;
        }

        this.edgeFilteringMode = filterMode;
        postshadowMat.setInt("FilterMode", filterMode.getMaterialParamValue());
        postshadowMat.setFloat("PCFEdge", edgesThickness);
        if (shadowCompareMode == CompareMode.Hardware) {
            for (Texture2D shadowMap : shadowMaps) {
                if (filterMode == EdgeFilteringMode.Bilinear) {
                    shadowMap.setMagFilter(MagFilter.Bilinear);
                    shadowMap.setMinFilter(MinFilter.BilinearNoMipMaps);
                } else {
                    shadowMap.setMagFilter(MagFilter.Nearest);
                    shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
                }
            }
        }
    }

    /**
     * returns the the edge filtering mode
     *
     * @see EdgeFilteringMode
     * @return
     */
    public EdgeFilteringMode getEdgeFilteringMode() {
        return edgeFilteringMode;
    }

    /**
     * sets the shadow compare mode see {@link CompareMode} for more info
     *
     * @param compareMode
     */
    final public void setShadowCompareMode(CompareMode compareMode) {
        if (compareMode == null) {
            throw new NullPointerException();
        }

        if (this.shadowCompareMode == compareMode) {
            return;
        }

        this.shadowCompareMode = compareMode;
        for (Texture2D shadowMap : shadowMaps) {
            if (compareMode == CompareMode.Hardware) {
                shadowMap.setShadowCompareMode(ShadowCompareMode.LessOrEqual);
                if (edgeFilteringMode == EdgeFilteringMode.Bilinear) {
                    shadowMap.setMagFilter(MagFilter.Bilinear);
                    shadowMap.setMinFilter(MinFilter.BilinearNoMipMaps);
                } else {
                    shadowMap.setMagFilter(MagFilter.Nearest);
                    shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
                }
            } else {
                shadowMap.setShadowCompareMode(ShadowCompareMode.Off);
                shadowMap.setMagFilter(MagFilter.Nearest);
                shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
            }
        }
        postshadowMat.setBoolean("HardwareShadows", compareMode == CompareMode.Hardware);
    }

    /**
     * returns the shadow compare mode
     *
     * @see CompareMode
     * @return the shadowCompareMode
     */
    public CompareMode getShadowCompareMode() {
        return shadowCompareMode;
    }

    //debug function that create a displayable frustrum
    protected Geometry createFrustum(Vector3f[] pts, int i) {
        WireFrustum frustum = new WireFrustum(pts);
        Geometry frustumMdl = new Geometry("f", frustum);
        frustumMdl.setCullHint(Spatial.CullHint.Never);
        frustumMdl.setShadowMode(ShadowMode.Off);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        frustumMdl.setMaterial(mat);
        switch (i) {
            case 0:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Pink);
                break;
            case 1:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Red);
                break;
            case 2:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Green);
                break;
            case 3:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.Blue);
                break;
            default:
                frustumMdl.getMaterial().setColor("Color", ColorRGBA.White);
                break;
        }

        frustumMdl.updateGeometricState();
        return frustumMdl;
    }

    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        viewPort = vp;
        //checking for caps to chosse the appropriate post material technique
        if (renderManager.getRenderer().getCaps().contains(Caps.GLSL150)) {
            postTechniqueName = "PostShadow15";
        } else {
            postTechniqueName = "PostShadow";
        }
    }

    public boolean isInitialized() {
        return viewPort != null;
    }

    /**
     * This mehtod is called once per frame.
     * it is responsible for updating the shadow cams according to the light view.
     * @param viewCam the scene cam
     */
    protected abstract void updateShadowCams(Camera viewCam);

    /**
     * this method must return the geomtryList that contains the oclluders to be rendered in the shadow map
     * @param shadowMapIndex the index of the shadow map being rendered
     * @param sceneOccluders the occluders of the whole scene
     * @param sceneReceivers the recievers of the whole scene
     * @return 
     */
    protected abstract GeometryList getOccludersToRender(int shadowMapIndex, GeometryList sceneOccluders, GeometryList sceneReceivers);

    /**
     * return the shadow camera to use for rendering the shadow map according the given index
     * @param shadowMapIndex the index of the shadow map being rendered
     * @return the shadowCam
     */
    protected abstract Camera getShadowCam(int shadowMapIndex);

    /**
     * responsible for displaying the frustum of the shadow cam for debug purpose
     * @param shadowMapIndex 
     */
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
    }

    @SuppressWarnings("fallthrough")
    public void postQueue(RenderQueue rq) {
        GeometryList occluders = rq.getShadowQueueContent(ShadowMode.Cast);
        GeometryList receivers = rq.getShadowQueueContent(ShadowMode.Receive);
        if (receivers.size() == 0 || occluders.size() == 0) {
            return;
        }

        updateShadowCams(viewPort.getCamera());

        Renderer r = renderManager.getRenderer();
        renderManager.setForcedMaterial(preshadowMat);
        renderManager.setForcedTechnique("PreShadow");

        for (int shadowMapIndex = 0; shadowMapIndex < nbShadowMaps; shadowMapIndex++) {

            if (debugfrustums) {
                doDisplayFrustumDebug(shadowMapIndex);
            }
            renderShadowMap(shadowMapIndex, occluders, receivers);

        }

        debugfrustums = false;
        if (flushQueues) {
            occluders.clear();
        }
        //restore setting for future rendering
        r.setFrameBuffer(viewPort.getOutputFrameBuffer());
        renderManager.setForcedMaterial(null);
        renderManager.setForcedTechnique(null);
        renderManager.setCamera(viewPort.getCamera(), false);

    }

    protected void renderShadowMap(int shadowMapIndex, GeometryList occluders, GeometryList receivers) {
        GeometryList mapOccluders = getOccludersToRender(shadowMapIndex, occluders, receivers);
        Camera shadowCam = getShadowCam(shadowMapIndex);

        //saving light view projection matrix for this split            
        lightViewProjectionsMatrices[shadowMapIndex].set(shadowCam.getViewProjectionMatrix());
        renderManager.setCamera(shadowCam, false);

        renderManager.getRenderer().setFrameBuffer(shadowFB[shadowMapIndex]);
        renderManager.getRenderer().clearBuffers(false, true, false);

        // render shadow casters to shadow map
        viewPort.getQueue().renderShadowQueue(mapOccluders, renderManager, shadowCam, true);
    }
    boolean debugfrustums = false;

    public void displayFrustum() {
        debugfrustums = true;
    }

    //debug only : displays depth shadow maps
    protected void displayShadowMap(Renderer r) {
        Camera cam = viewPort.getCamera();
        renderManager.setCamera(cam, true);
        int h = cam.getHeight();
        for (int i = 0; i < dispPic.length; i++) {
            dispPic[i].setPosition((128 * i) + (150 + 64 * (i + 1)), h / 20f);
            dispPic[i].setWidth(128);
            dispPic[i].setHeight(128);
            dispPic[i].updateGeometricState();
            renderManager.renderGeometry(dispPic[i]);
        }
        renderManager.setCamera(cam, false);
    }

    /**
     * For dubuging purpose Allow to "snapshot" the current frustrum to the
     * scene
     */
    public void displayDebug() {
        debug = true;
    }

    public void postFrame(FrameBuffer out) {

        if (debug) {
            displayShadowMap(renderManager.getRenderer());
        }
        if (!noOccluders) {
            //setting params to recieving geometry list
            setMatParams();

            Camera cam = viewPort.getCamera();
            //some materials in the scene does not have a post shadow technique so we're using the fall back material
            if (needsfallBackMaterial) {
                renderManager.setForcedMaterial(postshadowMat);
            }

            //forcing the post shadow technique and render state
            renderManager.setForcedTechnique(postTechniqueName);

            //rendering the post shadow pass
            viewPort.getQueue().renderShadowQueue(ShadowMode.Receive, renderManager, cam, flushQueues);

            //resetting renderManager settings
            renderManager.setForcedTechnique(null);
            renderManager.setForcedMaterial(null);
            renderManager.setCamera(cam, false);

        }

    }

    /**
     * This method is called once per frame and is responsible of setting the material 
     * parameters than sub class may need to set on the post material
     * @param material the materail to use for the post shadow pass
     */
    protected abstract void setMaterialParameters(Material material);

    private void setMatParams() {

        GeometryList l = viewPort.getQueue().getShadowQueueContent(ShadowMode.Receive);

        //iteration throught all the geometries of the list to gather the materials

        matCache.clear();
        for (int i = 0; i < l.size(); i++) {
            Material mat = l.get(i).getMaterial();
            //checking if the material has the post technique and adding it to the material cache
            if (mat.getMaterialDef().getTechniqueDef(postTechniqueName) != null) {
                if (!matCache.contains(mat)) {
                    matCache.add(mat);
                }
            } else {
                needsfallBackMaterial = true;
            }
        }

        //iterating through the mat cache and setting the parameters
        for (Material mat : matCache) {

            mat.setFloat("ShadowMapSize", shadowMapSize);

            for (int j = 0; j < nbShadowMaps; j++) {
                mat.setMatrix4("LightViewProjectionMatrix" + j, lightViewProjectionsMatrices[j]);
            }
            for (int j = 0; j < nbShadowMaps; j++) {
                mat.setTexture("ShadowMap" + j, shadowMaps[j]);
            }
            mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
            mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
            mat.setFloat("PCFEdge", edgesThickness);
            mat.setFloat("ShadowIntensity", shadowIntensity);

            setMaterialParameters(mat);
        }

        //At least one material of the receiving geoms does not support the post shadow techniques
        //so we fall back to the forced material solution (transparent shadows won't be supported for these objects)
        if (needsfallBackMaterial) {
            setPostShadowParams();
        }

    }

    /**
     * for internal use only
     */
    protected void setPostShadowParams() {
        setMaterialParameters(postshadowMat);
        for (int j = 0; j < nbShadowMaps; j++) {
            postshadowMat.setMatrix4("LightViewProjectionMatrix" + j, lightViewProjectionsMatrices[j]);
            postshadowMat.setTexture("ShadowMap" + j, shadowMaps[j]);
        }
    }

    public void preFrame(float tpf) {
    }

    public void cleanup() {
    }

    public void reshape(ViewPort vp, int w, int h) {
    }

    /**
     * returns the shdaow intensity
     *
     * @see #setShadowIntensity(float shadowIntensity)
     * @return shadowIntensity
     */
    public float getShadowIntensity() {
        return shadowIntensity;
    }

    /**
     * Set the shadowIntensity, the value should be between 0 and 1, a 0 value
     * gives a bright and invisilble shadow, a 1 value gives a pitch black
     * shadow, default is 0.7
     *
     * @param shadowIntensity the darkness of the shadow
     */
    final public void setShadowIntensity(float shadowIntensity) {
        this.shadowIntensity = shadowIntensity;
        postshadowMat.setFloat("ShadowIntensity", shadowIntensity);
    }

    /**
     * returns the edges thickness
     *
     * @see #setEdgesThickness(int edgesThickness)
     * @return edgesThickness
     */
    public int getEdgesThickness() {
        return (int) (edgesThickness * 10);
    }

    /**
     * Sets the shadow edges thickness. default is 1, setting it to lower values
     * can help to reduce the jagged effect of the shadow edges
     *
     * @param edgesThickness
     */
    public void setEdgesThickness(int edgesThickness) {
        this.edgesThickness = Math.max(1, Math.min(edgesThickness, 10));
        this.edgesThickness *= 0.1f;
        postshadowMat.setFloat("PCFEdge", edgesThickness);
    }

    /**
     * returns true if the PssmRenderer flushed the shadow queues
     *
     * @return flushQueues
     */
    public boolean isFlushQueues() {
        return flushQueues;
    }

    /**
     * Set this to false if you want to use several PssmRederers to have
     * multiple shadows cast by multiple light sources. Make sure the last
     * PssmRenderer in the stack DO flush the queues, but not the others
     *
     * @param flushQueues
     */
    public void setFlushQueues(boolean flushQueues) {
        this.flushQueues = flushQueues;
    }
}
