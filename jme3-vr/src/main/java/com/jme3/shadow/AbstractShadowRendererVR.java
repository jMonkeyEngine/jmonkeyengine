package com.jme3.shadow;

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

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
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
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.shadow.CompareMode;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract shadow renderer that holds commons feature to have for a shadow
 * renderer.
 *
 * @author Rémy Bouquet aka Nehon
 * @author reden - phr00t - https://github.com/phr00t
 * @author Julien Seinturier - (c) 2016 - JOrigin project - <a href="http://www.jorigin.org">http:/www.jorigin.org</a>
 */
public abstract class AbstractShadowRendererVR implements SceneProcessor, Savable {

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
    protected AssetManager assetManager;
    protected boolean debug = false;
    protected float edgesThickness = 1.0f;
    protected EdgeFilteringMode edgeFilteringMode = EdgeFilteringMode.Bilinear;
    protected CompareMode shadowCompareMode = CompareMode.Hardware;
    protected Picture[] dispPic;
    protected RenderState forcedRenderState = new RenderState();
    protected Boolean renderBackFacesShadows;

    protected AppProfiler profiler = null;
    
    /**
     * true if the fallback material should be used, otherwise false
     */
    protected boolean needsfallBackMaterial = false;
    /**
     * name of the post material technique
     */
    protected String postTechniqueName = "PostShadow";
    /**
     * list of materials for post shadow queue geometries
     */
    protected List<Material> matCache = new ArrayList<Material>();
    protected GeometryList lightReceivers = new GeometryList(new OpaqueComparator());
    protected GeometryList shadowMapOccluders = new GeometryList(new OpaqueComparator());
    private String[] shadowMapStringCache;
    private String[] lightViewStringCache;
    /**
     * fade shadows at distance
     */
    protected float zFarOverride = 0;
    protected Vector2f fadeInfo;
    protected float fadeLength;
    protected Camera frustumCam;
    /**
     * true to skip the post pass when there are no shadow casters
     */
    protected boolean skipPostPass;
    
    /**
     * used for serialization
     */
    protected AbstractShadowRendererVR(){        
    }    
    
    /**
     * Create an abstract shadow renderer. Subclasses invoke this constructor.
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadow maps (512,1024,2048,
     * etc...)
     * @param nbShadowMaps the number of shadow maps rendered (the more shadow
     * maps the more quality, the fewer fps).
     */
    protected AbstractShadowRendererVR(AssetManager assetManager, int shadowMapSize, int nbShadowMaps) {

        this.assetManager = assetManager;
        this.nbShadowMaps = nbShadowMaps;
        this.shadowMapSize = shadowMapSize;
        init(assetManager, nbShadowMaps, shadowMapSize);

    }

    private void init(AssetManager assetManager, int nbShadowMaps, int shadowMapSize) {
        this.postshadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PostShadow.j3md");
        shadowFB = new FrameBuffer[nbShadowMaps];
        shadowMaps = new Texture2D[nbShadowMaps];
        dispPic = new Picture[nbShadowMaps];
        lightViewProjectionsMatrices = new Matrix4f[nbShadowMaps];
        shadowMapStringCache = new String[nbShadowMaps];
        lightViewStringCache = new String[nbShadowMaps];

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
            shadowMapStringCache[i] = "ShadowMap" + i; 
            lightViewStringCache[i] = "LightViewProjectionMatrix" + i;

            postshadowMat.setTexture(shadowMapStringCache[i], shadowMaps[i]);

            //quads for debuging purpose
            dispPic[i] = new Picture("Picture" + i);
            dispPic[i].setTexture(assetManager, shadowMaps[i], false);
        }

        setShadowCompareMode(shadowCompareMode);
        setEdgeFilteringMode(edgeFilteringMode);
        setShadowIntensity(shadowIntensity);
        initForcedRenderState();
    }

    protected void initForcedRenderState() {
        forcedRenderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        forcedRenderState.setColorWrite(false);
        forcedRenderState.setDepthWrite(true);
        forcedRenderState.setDepthTest(true);
    }

    /**
     * set the post shadow material for this renderer
     *
     * @param postShadowMat
     */
    protected final void setPostShadowMaterial(Material postShadowMat) {
        this.postshadowMat = postShadowMat;
        postshadowMat.setFloat("ShadowMapSize", shadowMapSize);
        for (int i = 0; i < nbShadowMaps; i++) {
            postshadowMat.setTexture(shadowMapStringCache[i], shadowMaps[i]);
        }
        setShadowCompareMode(shadowCompareMode);
        setEdgeFilteringMode(edgeFilteringMode);
        setShadowIntensity(shadowIntensity);
    }

    /**
     * Sets the filtering mode for shadow edges. See {@link EdgeFilteringMode}
     * for more info.
     *
     * @param filterMode the desired filter mode (not null)
     */
    final public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
        if (filterMode == null) {
            throw new NullPointerException();
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
     * Get the edge filtering mode.
     * @return the edge filtering mode.
     */
    public EdgeFilteringMode getEdgeFilteringMode() {
        return edgeFilteringMode;
    }

    /**
     * Sets the shadow compare mode. See {@link CompareMode} for more info.
     *
     * @param compareMode the desired compare mode (not null)
     */
    final public void setShadowCompareMode(CompareMode compareMode) {
        if (compareMode == null) {
            throw new IllegalArgumentException("Shadow compare mode cannot be null");
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

    /**
     * debug function to create a visible frustum
     */
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

    /**
     * Initialize this shadow renderer prior to its first update.
     *
     * @param rm the render manager
     * @param vp the viewport
     */
    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        viewPort = vp;
        postTechniqueName = "PostShadow";
        if(zFarOverride>0 && frustumCam == null){
            initFrustumCam();
        }
    }
    
    /**
     * delegates the initialization of the frustum cam to child renderers
     */
    protected abstract void initFrustumCam();

    /**
     * Test whether this shadow renderer has been initialized.
     *
     * @return true if initialized, otherwise false
     */
    public boolean isInitialized() {
        return viewPort != null;
    }

    /**
     * Invoked once per frame to update the shadow cams according to the light
     * view.
     * 
     * @param viewCam the scene cam
     */
    protected abstract void updateShadowCams(Camera viewCam);

    /**
     * Returns a subclass-specific geometryList containing the occluders to be
     * rendered in the shadow map
     *
     * @param shadowMapIndex the index of the shadow map being rendered
     * @param shadowMapOccluders the list of occluders
     * @return
     */
    protected abstract GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders);

    /**
     * return the shadow camera to use for rendering the shadow map according
     * the given index
     *
     * @param shadowMapIndex the index of the shadow map being rendered
     * @return the shadowCam
     */
    protected abstract Camera getShadowCam(int shadowMapIndex);

	@Override
	public void setProfiler(AppProfiler profiler) {
		this.profiler = profiler;
	}
    
    /**
     * responsible for displaying the frustum of the shadow cam for debug
     * purpose
     *
     * @param shadowMapIndex
     */
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
    }

    @SuppressWarnings("fallthrough")
    public void postQueue(RenderQueue rq) {
        lightReceivers.clear();
        skipPostPass = false;
        if ( !checkCulling(viewPort.getCamera()) ) {
            skipPostPass = true;
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
                renderShadowMap(shadowMapIndex);

            }

        debugfrustums = false;

        //restore setting for future rendering
        r.setFrameBuffer(viewPort.getOutputFrameBuffer());
        renderManager.setForcedMaterial(null);
        renderManager.setForcedTechnique(null);
        renderManager.setCamera(viewPort.getCamera(), false);
        
    }

    protected void renderShadowMap(int shadowMapIndex) {
        shadowMapOccluders = getOccludersToRender(shadowMapIndex, shadowMapOccluders);
        Camera shadowCam = getShadowCam(shadowMapIndex);

        //saving light view projection matrix for this split            
        lightViewProjectionsMatrices[shadowMapIndex].set(shadowCam.getViewProjectionMatrix());
        renderManager.setCamera(shadowCam, false);

        renderManager.getRenderer().setFrameBuffer(shadowFB[shadowMapIndex]);
        renderManager.getRenderer().clearBuffers(true, true, true);
        renderManager.setForcedRenderState(forcedRenderState);

        // render shadow casters to shadow map
        viewPort.getQueue().renderShadowQueue(shadowMapOccluders, renderManager, shadowCam, true);
        renderManager.setForcedRenderState(null);
    }
    boolean debugfrustums = false;

    /**
     * Force the frustum to be displayed.
     */
    public void displayFrustum() {
        debugfrustums = true;
    }

    /**
     * For debugging purposes, display depth shadow maps.
     */
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
     * For debugging purposes, "snapshot" the current frustum to the scene.
     */
    public void displayDebug() {
        debug = true;
    }

    protected abstract void getReceivers(GeometryList lightReceivers);

    public void postFrame(FrameBuffer out) {
        if (skipPostPass) {
            return;
        }
        if (debug) {
            displayShadowMap(renderManager.getRenderer());
        }
        
        getReceivers(lightReceivers);

        if (lightReceivers.size() != 0) {
            //setting params to recieving geometry list
            setMatParams(lightReceivers);

            Camera cam = viewPort.getCamera();
            //some materials in the scene does not have a post shadow technique so we're using the fall back material
            if (needsfallBackMaterial) {
                renderManager.setForcedMaterial(postshadowMat);
            }

            //forcing the post shadow technique and render state
            renderManager.setForcedTechnique(postTechniqueName);

            //rendering the post shadow pass
            viewPort.getQueue().renderShadowQueue(lightReceivers, renderManager, cam, false);

            //resetting renderManager settings
            renderManager.setForcedTechnique(null);
            renderManager.setForcedMaterial(null);
            renderManager.setCamera(cam, false);
            
            //clearing the params in case there are some other shadow renderers
            clearMatParams();
        }
    }
    
    /**
     * This method is called once per frame and is responsible for clearing any
     * material parameters that subclasses may need to clear on the post material.
     *
     * @param material the material that was used for the post shadow pass     
     */
    protected abstract void clearMaterialParameters(Material material);    
    
    private void clearMatParams(){
        for (Material mat : matCache) {
         
            //clearing only necessary params, the others may be set by other 
            //renderers 
            //Note that j start at 1 because other shadow renderers will have 
            //at least 1 shadow map and will set it on each frame anyway.
            for (int j = 1; j < nbShadowMaps; j++) {
                mat.clearParam(lightViewStringCache[j]);
            }
            for (int j = 1; j < nbShadowMaps; j++) {
                mat.clearParam(shadowMapStringCache[j]);
            }
            mat.clearParam("FadeInfo");
            clearMaterialParameters(mat);
        }        
        //No need to clear the postShadowMat params as the instance is locale to each renderer       
    }

    /**
     * This method is called once per frame and is responsible for setting any
     * material parameters that subclasses may need to set on the post material.
     *
     * @param material the material to use for the post shadow pass
     */
    protected abstract void setMaterialParameters(Material material);

    private void setMatParams(GeometryList l) {
        //iteration throught all the geometries of the list to gather the materials

        buildMatCache(l);

        //iterating through the mat cache and setting the parameters
        for (Material mat : matCache) {

            mat.setFloat("ShadowMapSize", shadowMapSize);

            for (int j = 0; j < nbShadowMaps; j++) {
                mat.setMatrix4(lightViewStringCache[j], lightViewProjectionsMatrices[j]);
            }
            for (int j = 0; j < nbShadowMaps; j++) {
                mat.setTexture(shadowMapStringCache[j], shadowMaps[j]);
            }
            mat.setBoolean("HardwareShadows", shadowCompareMode == CompareMode.Hardware);
            mat.setInt("FilterMode", edgeFilteringMode.getMaterialParamValue());
            mat.setFloat("PCFEdge", edgesThickness);
            mat.setFloat("ShadowIntensity", shadowIntensity);
            if (fadeInfo != null) {
               mat.setVector2("FadeInfo", fadeInfo);
            }
            if(renderBackFacesShadows != null){
                mat.setBoolean("BackfaceShadows", renderBackFacesShadows);
            }

            setMaterialParameters(mat);
        }

        //At least one material of the receiving geoms does not support the post shadow techniques
        //so we fall back to the forced material solution (transparent shadows won't be supported for these objects)
        if (needsfallBackMaterial) {
            setPostShadowParams();
        }

    }

    private void buildMatCache(GeometryList l) {
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
    }

    /**
     * for internal use only
     */
    protected void setPostShadowParams() {
        setMaterialParameters(postshadowMat);
        for (int j = 0; j < nbShadowMaps; j++) {
            postshadowMat.setMatrix4(lightViewStringCache[j], lightViewProjectionsMatrices[j]);
            postshadowMat.setTexture(shadowMapStringCache[j], shadowMaps[j]);
        }
        if (fadeInfo != null) {
            postshadowMat.setVector2("FadeInfo", fadeInfo);
        }
        if(renderBackFacesShadows != null){
            postshadowMat.setBoolean("BackfaceShadows", renderBackFacesShadows);
        }
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
        this.zFarOverride = zFar;        
        if(zFarOverride == 0){
            fadeInfo = null;
            frustumCam = null;
        }else{
            if (fadeInfo != null) {
                fadeInfo.set(zFarOverride - fadeLength, 1f / fadeLength);
            }
            if(frustumCam == null && viewPort != null){
                initFrustumCam();
            }
        }
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
    
    /**
     * returns true if the light source bounding box is in the view frustum
     * @return 
     */
    protected abstract boolean checkCulling(Camera viewCam);
    
    public void preFrame(float tpf) {           
    }

    public void cleanup() {
    }

    public void reshape(ViewPort vp, int w, int h) {
    }

    /**
     * Returns the shadow intensity.
     *
     * @see #setShadowIntensity(float shadowIntensity)
     * @return shadowIntensity
     */
    public float getShadowIntensity() {
        return shadowIntensity;
    }

    /**
     * Set the shadowIntensity. The value should be between 0 and 1. A 0 value
     * gives a bright and invisible shadow, a 1 value gives a pitch black
     * shadow. The default is 0.7
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
     * @param edgesThickness the shadow edges thickness.
     */
    public void setEdgesThickness(int edgesThickness) {
        this.edgesThickness = Math.max(1, Math.min(edgesThickness, 10));
        this.edgesThickness *= 0.1f;
        postshadowMat.setFloat("PCFEdge", edgesThickness);
    }

    /**
     * This method does nothing now and is kept only for backward compatibility.
     * @return <code>false</code>
     * @deprecated This method does nothing now and is kept only for backward compatibility.
     */
    @Deprecated
    public boolean isFlushQueues() { return false; }

    /**
     * This method does nothing now and is kept only for backward compatibility.
     * @param flushQueues any boolean.
     * @deprecated This method does nothing now and is kept only for backward compatibility.
     */
    @Deprecated
    public void setFlushQueues(boolean flushQueues) {}


    /**
     * Returns the pre shadows pass render state.
     * use it to adjust the RenderState parameters of the pre shadow pass.
     * Note that this will be overridden if the preShadow technique in the material has a ForcedRenderState
     * @return the pre shadow render state.
     */
    public RenderState getPreShadowForcedRenderState() {
        return forcedRenderState;
    }

    /**
     * Set to true if you want back faces shadows on geometries.
     * Note that back faces shadows will be blended over dark lighten areas and may produce overly dark lighting.
     *
     * Also note that setting this parameter will override this parameter for ALL materials in the scene.
     * You can alternatively change this parameter on a single material using {@link Material#setBoolean(String, boolean)}
     *
     * This also will automatically adjust the faceCullMode and the PolyOffset of the pre shadow pass.
     * You can modify them by using {@link #getPreShadowForcedRenderState()}
     *
     * @param renderBackFacesShadows true or false.
     */
    public void setRenderBackFacesShadows(Boolean renderBackFacesShadows) {
        this.renderBackFacesShadows = renderBackFacesShadows;
        if(renderBackFacesShadows) {
            getPreShadowForcedRenderState().setPolyOffset(5, 3);
            getPreShadowForcedRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        }else{
            getPreShadowForcedRenderState().setPolyOffset(0, 0);
            getPreShadowForcedRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
        }
    }

    /**
     * if this processor renders back faces shadows
     * @return true if this processor renders back faces shadows
     */
    public boolean isRenderBackFacesShadows() {
        return renderBackFacesShadows != null?renderBackFacesShadows:false;
    }

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = (InputCapsule) im.getCapsule(this);
        assetManager = im.getAssetManager();
        nbShadowMaps = ic.readInt("nbShadowMaps", 1);
        shadowMapSize = ic.readFloat("shadowMapSize", 0f);
        shadowIntensity = ic.readFloat("shadowIntensity", 0.7f);
        edgeFilteringMode = ic.readEnum("edgeFilteringMode", EdgeFilteringMode.class, EdgeFilteringMode.Bilinear);
        shadowCompareMode = ic.readEnum("shadowCompareMode", CompareMode.class, CompareMode.Hardware);
        init(assetManager, nbShadowMaps, (int) shadowMapSize);
        edgesThickness = ic.readFloat("edgesThickness", 1.0f);
        postshadowMat.setFloat("PCFEdge", edgesThickness);

    }

    /**
     * Serialize this instance, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = (OutputCapsule) ex.getCapsule(this);
        oc.write(nbShadowMaps, "nbShadowMaps", 1);
        oc.write(shadowMapSize, "shadowMapSize", 0);
        oc.write(shadowIntensity, "shadowIntensity", 0.7f);
        oc.write(edgeFilteringMode, "edgeFilteringMode", EdgeFilteringMode.Bilinear);
        oc.write(shadowCompareMode, "shadowCompareMode", CompareMode.Hardware);
        oc.write(edgesThickness, "edgesThickness", 1.0f);
    }
}
