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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
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
 * PssmShadow renderer use Parrallel Split Shadow Mapping technique (pssm)<br>
 * It splits the view frustum in several parts and compute a shadow map for each
 * one.<br> splits are distributed so that the closer they are from the camera,
 * the smaller they are to maximize the resolution used of the shadow map.<br>
 * This result in a better quality shadow than standard shadow mapping.<br> for
 * more informations on this read this <a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a><br>
 * <p/>
 * @author Rémy Bouquet aka Nehon
 * @deprecated use {@link DirectionalLightShadowRenderer}
 */
@Deprecated
public class PssmShadowRenderer implements SceneProcessor {

    private AppProfiler prof;

    /**
     * <code>FilterMode</code> specifies how shadows are filtered
     * @deprecated use {@link EdgeFilteringMode}
     */
    @Deprecated
    public enum FilterMode{

        /**
         * Shadows are not filtered. Nearest sample is used, causing in blocky
         * shadows.
         */
        Nearest,
        /**
         * Bilinear filtering is used. Has the potential of being hardware
         * accelerated on some GPUs
         */
        Bilinear,
        /**
         * Dither-based sampling is used, very cheap but can look bad at low
         * resolutions.
         */
        Dither,
        /**
         * 4x4 percentage-closer filtering is used. Shadows will be smoother at
         * the cost of performance
         */
        PCF4,
        /**
         * 8x8 percentage-closer filtering is used. Shadows will be smoother at
         * the cost of performance
         */
        PCFPOISSON,
        /**
         * 8x8 percentage-closer filtering is used. Shadows will be smoother at
         * the cost of performance
         */
        PCF8
    }

    /**
     * Specifies the shadow comparison mode
     * @deprecated use {@link CompareMode}
     */
    @Deprecated
    public enum CompareMode {

        /**
         * Shadow depth comparisons are done by using shader code
         */
        Software,
        /**
         * Shadow depth comparisons are done by using the GPU's dedicated
         * shadowing pipeline.
         */
        Hardware;
    }
    protected int nbSplits = 3;
    protected float shadowMapSize;
    protected float lambda = 0.65f;
    protected float shadowIntensity = 0.7f;
    protected float zFarOverride = 0;
    protected RenderManager renderManager;
    protected ViewPort viewPort;
    protected FrameBuffer[] shadowFB;
    protected Texture2D[] shadowMaps;
    protected Texture2D dummyTex;
    protected Camera shadowCam;
    protected Material preshadowMat;
    protected Material postshadowMat;
    protected GeometryList splitOccluders = new GeometryList(new OpaqueComparator());
    protected Matrix4f[] lightViewProjectionsMatrices;
    protected ColorRGBA splits;
    protected float[] splitsArray;
    protected boolean noOccluders = false;
    protected Vector3f direction = new Vector3f();
    protected AssetManager assetManager;
    protected boolean debug = false;
    protected float edgesThickness = 1.0f;
    protected FilterMode filterMode;
    protected CompareMode compareMode;
    protected Picture[] dispPic;
    protected Vector3f[] points = new Vector3f[8];
    protected boolean flushQueues = true;
    // define if the fallback material should be used.
    protected boolean needsfallBackMaterial = false;
    //Name of the post material technique
    protected String postTechniqueName = "PostShadow";
    //flags to know when to change params in the materials
    protected boolean applyHWShadows = true;
    protected boolean applyFilterMode = true;
    protected boolean applyPCFEdge = true;
    protected boolean applyShadowIntensity = true;
    //a list of material of the post shadow queue geometries.
    protected List<Material> matCache = new ArrayList<Material>();
    //Holding the info for fading shadows in the far distance 
    protected Vector2f fadeInfo;
    protected float fadeLength;
    protected boolean applyFadeInfo = false;

    protected GeometryList lightReceivers = new GeometryList(new OpaqueComparator());
    
    /**
     * Create a PSSM Shadow Renderer More info on the technique at <a
     * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     *
     * @param manager the application asset manager
     * @param size the size of the rendered shadowmaps (512,1024,2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps
     * the more quality, the less fps).
     * @param nbSplits the number of shadow maps rendered (the more shadow maps
     * the more quality, the less fps).
     */
    public PssmShadowRenderer(AssetManager manager, int size, int nbSplits) {
        this(manager, size, nbSplits, new Material(manager, "Common/MatDefs/Shadow/PostShadow.j3md"));
    }

    /**
     * Create a PSSM Shadow Renderer More info on the technique at <a
     * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     *
     * @param manager the application asset manager
     * @param size the size of the rendered shadowmaps (512,1024,2048, etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps
     * the more quality, the less fps).
     * @param postShadowMat the material used for post shadows if you need to
     * override it
     */
    protected PssmShadowRenderer(AssetManager manager, int size, int nbSplits, Material postShadowMat) {

        this.postshadowMat = postShadowMat;
        assetManager = manager;
        nbSplits = Math.max(Math.min(nbSplits, 4), 1);
        this.nbSplits = nbSplits;
        shadowMapSize = size;

        shadowFB = new FrameBuffer[nbSplits];
        shadowMaps = new Texture2D[nbSplits];
        dispPic = new Picture[nbSplits];
        lightViewProjectionsMatrices = new Matrix4f[nbSplits];
        splits = new ColorRGBA();
        splitsArray = new float[nbSplits + 1];

        //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
        dummyTex = new Texture2D(size, size, Format.RGBA8);

        preshadowMat = new Material(manager, "Common/MatDefs/Shadow/PreShadow.j3md");
        postshadowMat.setFloat("ShadowMapSize", size);

        for (int i = 0; i < nbSplits; i++) {
            lightViewProjectionsMatrices[i] = new Matrix4f();
            shadowFB[i] = new FrameBuffer(size, size, 1);
            shadowMaps[i] = new Texture2D(size, size, Format.Depth);

            shadowFB[i].setDepthTexture(shadowMaps[i]);

            //DO NOT COMMENT THIS (it prevent the OSX incomplete read buffer crash)
            shadowFB[i].setColorTexture(dummyTex);

            postshadowMat.setTexture("ShadowMap" + i, shadowMaps[i]);

            //quads for debuging purpose
            dispPic[i] = new Picture("Picture" + i);
            dispPic[i].setTexture(manager, shadowMaps[i], false);
        }

        setCompareMode(CompareMode.Hardware);
        setFilterMode(FilterMode.Bilinear);
        setShadowIntensity(0.7f);

        shadowCam = new Camera(size, size);
        shadowCam.setParallelProjection(true);

        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

    }

    /**
     * Sets the filtering mode for shadow edges see {@link FilterMode} for more
     * info
     *
     * @param filterMode
     */
    final public void setFilterMode(FilterMode filterMode) {
        if (filterMode == null) {
            throw new NullPointerException();
        }

        if (this.filterMode == filterMode) {
            return;
        }

        this.filterMode = filterMode;
        postshadowMat.setInt("FilterMode", filterMode.ordinal());
        postshadowMat.setFloat("PCFEdge", edgesThickness);
        if (compareMode == CompareMode.Hardware) {
            for (Texture2D shadowMap : shadowMaps) {
                if (filterMode == FilterMode.Bilinear) {
                    shadowMap.setMagFilter(MagFilter.Bilinear);
                    shadowMap.setMinFilter(MinFilter.BilinearNoMipMaps);
                } else {
                    shadowMap.setMagFilter(MagFilter.Nearest);
                    shadowMap.setMinFilter(MinFilter.NearestNoMipMaps);
                }
            }
        }
        applyFilterMode = true;
    }

    /**
     * sets the shadow compare mode see {@link CompareMode} for more info
     *
     * @param compareMode
     */
    final public void setCompareMode(CompareMode compareMode) {
        if (compareMode == null) {
            throw new NullPointerException();
        }

        if (this.compareMode == compareMode) {
            return;
        }

        this.compareMode = compareMode;
        for (Texture2D shadowMap : shadowMaps) {
            if (compareMode == CompareMode.Hardware) {
                shadowMap.setShadowCompareMode(ShadowCompareMode.LessOrEqual);
                if (filterMode == FilterMode.Bilinear) {
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
        applyHWShadows = true;
    }

    //debug function that create a displayable frustrum
    private Geometry createFrustum(Vector3f[] pts, int i) {
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
        postTechniqueName = "PostShadow";
    }

    public boolean isInitialized() {
        return viewPort != null;
    }

    /**
     * returns the light direction used by the processor
     *
     * @return
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Sets the light direction to use to compute shadows
     *
     * @param direction
     */
    public void setDirection(Vector3f direction) {
        this.direction.set(direction).normalizeLocal();
    }

    @SuppressWarnings("fallthrough")
    public void postQueue(RenderQueue rq) {
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), ShadowMode.Receive, lightReceivers);
        }

        Camera viewCam = viewPort.getCamera();

        float zFar = zFarOverride;
        if (zFar == 0) {
            zFar = viewCam.getFrustumFar();
        }

        //We prevent computing the frustum points and splits with zeroed or negative near clip value
        float frustumNear = Math.max(viewCam.getFrustumNear(), 0.001f);
        ShadowUtil.updateFrustumPoints(viewCam, frustumNear, zFar, 1.0f, points);

        //shadowCam.setDirection(direction);
        shadowCam.getRotation().lookAt(direction, shadowCam.getUp());
        shadowCam.update();
        shadowCam.updateViewProjection();

        PssmShadowUtil.updateFrustumSplits(splitsArray, frustumNear, zFar, lambda);


        switch (splitsArray.length) {
            case 5:
                splits.a = splitsArray[4];
            case 4:
                splits.b = splitsArray[3];
            case 3:
                splits.g = splitsArray[2];
            case 2:
            case 1:
                splits.r = splitsArray[1];
                break;
        }

        Renderer r = renderManager.getRenderer();
        renderManager.setForcedMaterial(preshadowMat);
        renderManager.setForcedTechnique("PreShadow");

        for (int i = 0; i < nbSplits; i++) {

            // update frustum points based on current camera and split
            ShadowUtil.updateFrustumPoints(viewCam, splitsArray[i], splitsArray[i + 1], 1.0f, points);

            //Updating shadow cam with curent split frustra
            ShadowUtil.updateShadowCamera(viewPort, lightReceivers, shadowCam, points, splitOccluders, shadowMapSize);

            //saving light view projection matrix for this split            
            lightViewProjectionsMatrices[i].set(shadowCam.getViewProjectionMatrix());
            renderManager.setCamera(shadowCam, false);

            if (debugfrustums) {
//                    frustrumFromBound(b.casterBB,ColorRGBA.Blue );
//                    frustrumFromBound(b.receiverBB,ColorRGBA.Green );
//                    frustrumFromBound(b.splitBB,ColorRGBA.Yellow );
                ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, i));
                ShadowUtil.updateFrustumPoints2(shadowCam, points);
                ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, i));

            }

            r.setFrameBuffer(shadowFB[i]);
            r.clearBuffers(true, true, true);

            // render shadow casters to shadow map
            viewPort.getQueue().renderShadowQueue(splitOccluders, renderManager, shadowCam, true);
        }
        debugfrustums = false;

        //restore setting for future rendering
        r.setFrameBuffer(viewPort.getOutputFrameBuffer());
        renderManager.setForcedMaterial(null);
        renderManager.setForcedTechnique(null);
        renderManager.setCamera(viewCam, false);

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
     * For debugging purpose Allow to "snapshot" the current frustrum to the
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
            viewPort.getQueue().renderShadowQueue(lightReceivers, renderManager, cam, true);

            //resetting renderManager settings
            renderManager.setForcedTechnique(null);
            renderManager.setForcedMaterial(null);
            renderManager.setCamera(cam, false);

        }

    }

    private void setMatParams() {

        GeometryList l = lightReceivers;

        //iteration throught all the geometries of the list to gather the materials

        matCache.clear();
        for (int i = 0; i < l.size(); i++) {
            Material mat = l.get(i).getMaterial();
            //checking if the material has the post technique and adding it to the material cache
            if (mat.getMaterialDef().getTechniqueDefs(postTechniqueName) != null) {
                if (!matCache.contains(mat)) {
                    matCache.add(mat);
                }
            } else {
                needsfallBackMaterial = true;
            }
        }

        //iterating through the mat cache and setting the parameters
        for (Material mat : matCache) {
            mat.setColor("Splits", splits);
            mat.setFloat("ShadowMapSize", shadowMapSize);

            for (int j = 0; j < nbSplits; j++) {
                mat.setMatrix4("LightViewProjectionMatrix" + j, lightViewProjectionsMatrices[j]);
            }
            for (int j = 0; j < nbSplits; j++) {
                mat.setTexture("ShadowMap" + j, shadowMaps[j]);
            }
            mat.setBoolean("HardwareShadows", compareMode == CompareMode.Hardware);
            mat.setInt("FilterMode", filterMode.ordinal());
            mat.setFloat("PCFEdge", edgesThickness);
            mat.setFloat("ShadowIntensity", shadowIntensity);

            if (fadeInfo != null) {
                mat.setVector2("FadeInfo", fadeInfo);
            }

        }

        applyHWShadows = false;
        applyFilterMode = false;
        applyPCFEdge = false;
        applyShadowIntensity = false;
        applyFadeInfo = false;

        //At least one material of the receiving geoms does not support the post shadow techniques
        //so we fall back to the forced material solution (transparent shadows won't be supported for these objects)
        if (needsfallBackMaterial) {
            setPostShadowParams();
        }

    }

    protected void setPostShadowParams() {
        postshadowMat.setColor("Splits", splits);
        for (int j = 0; j < nbSplits; j++) {
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
     * returns the lambda parameter see #setLambda(float lambda)
     *
     * @return lambda
     */
    public float getLambda() {
        return lambda;
    }

    /*
     * Adjust the repartition of the different shadow maps in the shadow extend
     * usualy goes from 0.0 to 1.0
     * a low value give a more linear repartition resulting in a constant quality in the shadow over the extends, but near shadows could look very jagged
     * a high value give a more logarithmic repartition resulting in a high quality for near shadows, but the quality quickly decrease over the extend.
     * the default value is set to 0.65f (theoric optimal value).
     * @param lambda the lambda value.
     */
    public void setLambda(float lambda) {
        this.lambda = lambda;
    }

    /**
     * How far the shadows are rendered in the view
     *
     * @see #setShadowZExtend(float zFar)
     * @return shadowZExtend
     */
    public float getShadowZExtend() {
        return zFarOverride;
    }

    /**
     * Set the distance from the eye where the shadows will be rendered default
     * value is dynamically computed to the shadow casters/receivers union bound
     * zFar, capped to view frustum far value.
     *
     * @param zFar the zFar values that override the computed one
     */
    public void setShadowZExtend(float zFar) {
        if (fadeInfo != null) {
            fadeInfo.set(zFar - fadeLength, 1f / fadeLength);
        }
        this.zFarOverride = zFar;

    }

    /**
     * returns the shadow intensity
     *
     * @see #setShadowIntensity(float shadowIntensity)
     * @return shadowIntensity
     */
    public float getShadowIntensity() {
        return shadowIntensity;
    }

    /**
     * Set the shadowIntensity, the value should be between 0 and 1, a 0 value
     * gives a bright and invisible shadow, a 1 value gives a pitch black
     * shadow, default is 0.7
     *
     * @param shadowIntensity the darkness of the shadow
     */
    final public void setShadowIntensity(float shadowIntensity) {
        this.shadowIntensity = shadowIntensity;
        postshadowMat.setFloat("ShadowIntensity", shadowIntensity);
        applyShadowIntensity = true;
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
        applyPCFEdge = true;
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

    /**
     * Define the length over which the shadow will fade out when using a
     * shadowZextend This is useful to make dynamic shadows fade into baked
     * shadows in the distance.
     *
     * @param length the fade length in world units
     */
    public void setShadowZFadeLength(float length) {
        if (length == 0) {
            fadeInfo = null;
            fadeLength = 0;
            postshadowMat.clearParam("FadeInfo");
        } else {
            if (zFarOverride == 0) {
                fadeInfo = new Vector2f(0, 0);
            } else {
                fadeInfo = new Vector2f(zFarOverride - length, 1.0f / length);
            }
            fadeLength = length;
            postshadowMat.setVector2("FadeInfo", fadeInfo);
        }
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

    /**
     * get the length over which the shadow will fade out when using a
     * shadowZextend
     *
     * @return the fade length in world units
     */
    public float getShadowZFadeLength() {
        if (fadeInfo != null) {
            return zFarOverride - fadeInfo.x;
        }
        return 0f;
    }
}
