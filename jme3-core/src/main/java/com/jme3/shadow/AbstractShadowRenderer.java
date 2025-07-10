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
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.light.LightFilter;
import com.jme3.light.NullLightFilter;
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
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An abstract shadow renderer that provides common features for shadow rendering.
 *
 * @author Rémy Bouquet aka Nehon
 */
public abstract class AbstractShadowRenderer implements SceneProcessor, Savable, JmeCloneable {

    protected static final Logger logger = Logger.getLogger(AbstractShadowRenderer.class.getName());
    private static final LightFilter NULL_LIGHT_FILTER = new NullLightFilter();

    // The number of shadow maps to render.
    protected int nbShadowMaps = 1;
    // The resolution (width and height) of each shadow map.
    protected float shadowMapSize;
    // The intensity of the shadows, ranging from 0.0 (fully transparent) to 1.0 (fully opaque).
    protected float shadowIntensity = 0.7f;
    // The RenderManager instance used for rendering operations.
    protected RenderManager renderManager;
    // The ViewPort associated with this shadow renderer.
    protected ViewPort viewPort;
    // Array of frame buffers used for rendering shadow maps.
    protected FrameBuffer[] shadowFB;
    // Array of 2D textures representing the generated shadow maps.
    protected Texture2D[] shadowMaps;
    // A dummy texture used to prevent read-buffer crashes on certain platforms (e.g., OSX).
    protected Texture2D dummyTex;
    // Material used for the pre-shadow pass (rendering occluders into the shadow map).
    protected Material preshadowMat;
    // Material used for the post-shadow pass (applying shadows to the scene).
    protected Material postshadowMat;
    // Array of light view projection matrices for each shadow map.
    protected Matrix4f[] lightViewProjectionsMatrices;
    // The AssetManager instance used to load assets.
    protected AssetManager assetManager;
    // Flag indicating whether debug visualizations (e.g., shadow maps) should be displayed.
    protected boolean debug = false;
    // The thickness of shadow edges, influencing PCF (Percentage-Closer Filtering). Value is in tenths of a pixel.
    protected float edgesThickness = 1.0f;
    // The filtering mode applied to shadow edges.
    protected EdgeFilteringMode edgeFilteringMode = EdgeFilteringMode.Bilinear;
    // The shadow comparison mode (hardware or software).
    protected CompareMode shadowCompareMode = CompareMode.Hardware;
    // Array of Picture objects used for debugging to display shadow maps.
    protected Picture[] dispPic;
    // Forced RenderState used during the pre-shadow pass to render occluders.
    protected RenderState forcedRenderState = new RenderState();
    // Flag indicating whether back faces should cast shadows.
    protected boolean renderBackFacesShadows = true;
    // The application profiler for performance monitoring.
    protected AppProfiler prof;
    // Flag indicating whether shadow frustums should be displayed for debugging.
    protected boolean debugfrustums = false;
    // True if a fallback material should be used for post-shadow rendering, otherwise false.
    // This occurs if some scene materials do not support the post-shadow technique.
    protected boolean needsfallBackMaterial = false;
    // The name of the technique to use for the post-shadow material.
    protected String postTechniqueName = "PostShadow";
    // A cache of materials found on geometries in the post-shadow queue.
    protected List<Material> matCache = new ArrayList<>();
    // List of geometries that receive shadows.
    protected GeometryList lightReceivers = new GeometryList(new OpaqueComparator());
    // List of geometries that cast shadows (occluders).
    protected GeometryList shadowMapOccluders = new GeometryList(new OpaqueComparator());
    // Internal cache for shadow map uniform names (e.g., "ShadowMap0", "ShadowMap1").
    private String[] shadowMapStringCache;
    // nternal cache for light view projection matrix uniform names (e.g., "LightViewProjectionMatrix0").
    private String[] lightViewStringCache;
    // The distance at which shadows start to fade out. A value of 0 means no override.
    protected float zFarOverride = 0;
    // Vector containing information about shadow fading (start distance, inverse fade length).
    protected Vector2f fadeInfo;
    // The length over which shadows fade out.
    protected float fadeLength;
    // A camera used to define the frustum for shadow rendering, especially when `zFarOverride` is used.
    protected Camera frustumCam;
    // True to skip the post pass when there are no shadow casters.
    protected boolean skipPostPass;

    /**
     * For serialization only. Do not use.
     */
    protected AbstractShadowRenderer() {
    }

    /**
     * Creates an  AbstractShadowRenderer. Subclasses invoke this constructor.
     *
     * @param assetManager The application's asset manager.
     * @param shadowMapSize The size of the rendered shadow maps (e.g., 512, 1024, 2048).
     * @param nbShadowMaps The number of shadow maps to render (1 to 4). More maps
     * improve quality but can reduce performance.
     */
    protected AbstractShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbShadowMaps) {
        this.assetManager = assetManager;
        this.shadowMapSize = shadowMapSize;
        this.nbShadowMaps = nbShadowMaps;
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

        //DO NOT COMMENT THIS (it prevents the OSX incomplete read-buffer crash)
        dummyTex = new Texture2D(shadowMapSize, shadowMapSize, Format.RGBA8);

        preshadowMat = new Material(assetManager, "Common/MatDefs/Shadow/PreShadow.j3md");
        postshadowMat.setFloat("ShadowMapSize", shadowMapSize);

        for (int i = 0; i < nbShadowMaps; i++) {
            lightViewProjectionsMatrices[i] = new Matrix4f();
            shadowFB[i] = new FrameBuffer(shadowMapSize, shadowMapSize, 1);
            shadowMaps[i] = new Texture2D(shadowMapSize, shadowMapSize, Format.Depth);

            shadowFB[i].setDepthTarget(FrameBufferTarget.newTarget(shadowMaps[i]));

            //DO NOT COMMENT THIS (it prevents the OSX incomplete read-buffer crash)
            shadowFB[i].addColorTarget(FrameBufferTarget.newTarget(dummyTex));
            shadowMapStringCache[i] = "ShadowMap" + i;
            lightViewStringCache[i] = "LightViewProjectionMatrix" + i;

            postshadowMat.setTexture(shadowMapStringCache[i], shadowMaps[i]);

            //quads for debugging purposes
            dispPic[i] = new Picture("Picture" + i);
            dispPic[i].setTexture(assetManager, shadowMaps[i], false);
        }

        setShadowCompareMode(shadowCompareMode);
        setEdgeFilteringMode(edgeFilteringMode);
        setShadowIntensity(shadowIntensity);
        initForcedRenderState();
        setRenderBackFacesShadows(isRenderBackFacesShadows());
    }

    protected void initForcedRenderState() {
        forcedRenderState.setFaceCullMode(RenderState.FaceCullMode.Front);
        forcedRenderState.setColorWrite(false);
        forcedRenderState.setDepthWrite(true);
        forcedRenderState.setDepthTest(true);
    }

    /**
     * Sets the post-shadow material for this renderer. This material is used to apply
     * the shadows to the main scene.
     *
     * @param postShadowMat The desired Material instance to use (alias created).
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
     * Sets the filtering mode for shadow edges. This affects the smoothness of
     * shadow boundaries.
     *
     * @param filterMode The desired filtering mode (cannot be null). See {@link EdgeFilteringMode}
     * for available options.
     */
    final public void setEdgeFilteringMode(EdgeFilteringMode filterMode) {
        if (filterMode == null) {
            throw new IllegalArgumentException("filterMode cannot be null");
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
     * Returns the currently edge filtering mode for shadows.
     *
     * @return The current {@link EdgeFilteringMode} enum value.
     * @see EdgeFilteringMode
     */
    public EdgeFilteringMode getEdgeFilteringMode() {
        return edgeFilteringMode;
    }

    /**
     * Sets the shadow comparison mode. This determines how shadow map values are
     * compared to generate shadows.
     *
     * @param compareMode The desired compare mode (cannot be null). See {@link CompareMode}
     * for available options.
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
     * Returns the currently shadow comparison mode.
     *
     * @return The current {@link CompareMode} enum value.
     * @see CompareMode
     */
    public CompareMode getShadowCompareMode() {
        return shadowCompareMode;
    }

    /**
     * Debug function to create a visible wireframe frustum. This is useful for
     * visualizing the shadow camera's view.
     *
     * @param pts Optional storage for vertex positions. If null, a new array will be created.
     * @param i The index, used to assign a color to the frustum for differentiation (e.g., for multiple shadow maps).
     * @return A new {@link Geometry} representing the wireframe frustum.
     */
    protected Geometry createFrustum(Vector3f[] pts, int i) {
        WireFrustum frustum = new WireFrustum(pts);
        Geometry geo = new Geometry("WireFrustum" + i, frustum);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        geo.setMaterial(mat);
        geo.setCullHint(Spatial.CullHint.Never);
        geo.setShadowMode(ShadowMode.Off);

        switch (i) {
            case 0:
                mat.setColor("Color", ColorRGBA.Pink);
                break;
            case 1:
                mat.setColor("Color", ColorRGBA.Red);
                break;
            case 2:
                mat.setColor("Color", ColorRGBA.Green);
                break;
            case 3:
                mat.setColor("Color", ColorRGBA.Blue);
                break;
            default:
                mat.setColor("Color", ColorRGBA.White);
                break;
        }

        geo.updateGeometricState();
        return geo;
    }

    /**
     * Initialize this shadow renderer prior to its first update.
     *
     * @param rm the render manager
     * @param vp the viewport
     */
    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        renderManager = rm;
        viewPort = vp;
        postTechniqueName = "PostShadow";
        if (zFarOverride > 0 && frustumCam == null) {
            initFrustumCam();
        }
    }

    /**
     * Delegates the initialization of the frustum camera to child renderers.
     * This camera defines the view for calculating shadow frustums.
     */
    protected abstract void initFrustumCam();

    /**
     * Test whether this shadow renderer has been initialized.
     *
     * @return true if initialized, otherwise false
     */
    @Override
    public boolean isInitialized() {
        return viewPort != null;
    }

    /**
     * Invoked once per frame to update the shadow cameras according to the light view.
     * Subclasses must implement this method to define how shadow cameras are positioned
     * and oriented.
     *
     * @param viewCam The main scene camera.
     */
    protected abstract void updateShadowCams(Camera viewCam);

    /**
     * Returns a subclass-specific {@link GeometryList} containing the occluders
     * that should be rendered into the shadow map.
     *
     * @param shadowMapIndex The index of the shadow map being rendered.
     * @param shadowMapOccluders An existing {@link GeometryList} that can be reused or populated.
     * @return A {@link GeometryList} containing the geometries that cast shadows for the given map.
     */
    protected abstract GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders);

    /**
     * Returns the shadow camera to use for rendering the shadow map according to the given index.
     * Subclasses must implement this to provide the correct camera for each shadow map.
     *
     * @param shadowMapIndex The index of the shadow map being rendered.
     * @return The {@link Camera} instance representing the shadow's viewpoint.
     */
    protected abstract Camera getShadowCam(int shadowMapIndex);

    /**
     * Responsible for displaying the frustum of the shadow camera for debugging purposes.
     * Subclasses can override this method to provide specific debug visualizations.
     *
     * @param shadowMapIndex The index of the shadow map for which to display the frustum.
     */
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        // Default implementation does nothing.
    }

    @Override
    public void postQueue(RenderQueue rq) {
        lightReceivers.clear();
        skipPostPass = false;
        if (!checkCulling(viewPort.getCamera())) {
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

        // render shadow casters to shadow map and disables the light filter
        LightFilter tmpLightFilter = renderManager.getLightFilter();
        renderManager.setLightFilter(NULL_LIGHT_FILTER);
        viewPort.getQueue().renderShadowQueue(shadowMapOccluders, renderManager, shadowCam, true);
        renderManager.setLightFilter(tmpLightFilter);
        renderManager.setForcedRenderState(null);
    }

    /**
     * Enables debugging of shadow frustums, making them visible in the scene.
     * Call this before {@link #postQueue(RenderQueue)} to see the frustums.
     */
    public void displayFrustum() {
        debugfrustums = true;
    }

    /**
     * For debugging purposes, displays the depth shadow maps on screen as Picture quads.
     *
     * @param r The current {@link Renderer} (ignored).
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
     * For debugging purposes, "snapshots" the current state of the shadow maps
     * and displays them on screen.
     */
    public void displayDebug() {
        debug = true;
    }

    /**
     * Populates the provided {@link GeometryList} with geometries that are considered
     * shadow receivers. Subclasses must implement this method.
     *
     * @param lightReceivers The {@link GeometryList} to populate with shadow-receiving geometries.
     */
    protected abstract void getReceivers(GeometryList lightReceivers);

    @Override
    public void postFrame(FrameBuffer out) {
        if (skipPostPass) {
            return;
        }
        if (debug) {
            displayShadowMap(renderManager.getRenderer());
        }

        getReceivers(lightReceivers);

        if (lightReceivers.size() != 0) {
            //setting params to receiving geometry list
            setMatParams(lightReceivers);

            Camera cam = viewPort.getCamera();
            // Some materials in the scene do not have a post shadow technique, so we're using the fallback material.
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
     * material parameters that subclasses may have set on the post-shadow material.
     * This ensures that parameters from previous frames or other renderers do not
     * interfere.
     *
     * @param material The material that was used for the post-shadow pass.
     */
    protected abstract void clearMaterialParameters(Material material);

    /**
     * Clears common material parameters set by this renderer on materials in the cache.
     * This is done to avoid interference with other shadow renderers or subsequent frames.
     */
    private void clearMatParams() {
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

    /**
     * Iterates through the given {@link GeometryList} to gather unique materials
     * and sets common shadow-related parameters on them.
     *
     * @param list The {@link GeometryList} containing geometries whose materials need parameters set.
     */
    private void setMatParams(GeometryList list) {
        //iterate through all the geometries of the list to gather the materials

        buildMatCache(list);

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
            mat.setBoolean("BackfaceShadows", renderBackFacesShadows);

            if (fadeInfo != null) {
                mat.setVector2("FadeInfo", fadeInfo);
            }

            setMaterialParameters(mat);
        }

        // At least one material of the receiving geoms does not support the post shadow techniques,
        // so we fall back to the forced material solution. (Transparent shadows won't be supported for these objects.)
        if (needsfallBackMaterial) {
            setPostShadowParams();
        }
    }

    /**
     * Builds a cache of unique materials from the provided {@link GeometryList}
     * that support the post-shadow technique. If any material does not support
     * it, the `needsfallBackMaterial` flag is set.
     *
     * @param list The {@link GeometryList} to extract materials from.
     */
    private void buildMatCache(GeometryList list) {
        matCache.clear();
        for (int i = 0; i < list.size(); i++) {
            Material mat = list.get(i).getMaterial();
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
     * For internal use only. Sets the common shadow parameters on the internal
     * post-shadow material. This is used when a fallback material is needed.
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
        postshadowMat.setBoolean("BackfaceShadows", renderBackFacesShadows);
    }

    /**
     * Returns the maximum distance from the eye where shadows are rendered.
     * A value of 0 indicates that the distance is dynamically computed based on scene bounds.
     *
     * @return The shadow Z-extend distance in world units.
     * @see #setShadowZExtend(float zFar)
     */
    public float getShadowZExtend() {
        return zFarOverride;
    }

    /**
     * Sets the distance from the camera where shadows will be rendered.
     * By default (0), this value is dynamically computed based on the union bound
     * of shadow casters and receivers, capped by the view frustum's far value.
     * Setting a positive value overrides this dynamic computation.
     *
     * @param zFar The zFar value that overrides the computed one. Set to 0 to use dynamic computation.
     */
    public void setShadowZExtend(float zFar) {
        this.zFarOverride = zFar;
        if (zFarOverride == 0) {
            fadeInfo = null;
            frustumCam = null;
        } else {
            if (fadeInfo != null) {
                fadeInfo.set(zFarOverride - fadeLength, 1f / fadeLength);
            }
            if (frustumCam == null && viewPort != null) {
                initFrustumCam();
            }
        }
    }

    /**
     * Defines the length over which the shadow will fade out when using a
     * custom `shadowZextend`. This is useful for smoothly transitioning
     * dynamic shadows into baked shadows or for preventing abrupt shadow cut-offs.
     *
     * @param length The fade length in world units. Set to 0 to disable fading.
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
     * Returns the length over which the shadow will fade out when using a
     * custom `shadowZextend`.
     *
     * @return The fade length in world units. Returns 0 if no fading is applied.
     */
    public float getShadowZFadeLength() {
        if (fadeInfo != null) {
            return zFarOverride - fadeInfo.x;
        }
        return 0f;
    }

    /**
     * Abstract method to check if the light source's bounding box is within the view frustum
     * of the given camera. This is used for culling to avoid unnecessary shadow computations.
     *
     * @param viewCam A {@link Camera} to define the view frustum against which to check.
     * @return True if the light source's bounding box is in the view frustum, otherwise false.
     */
    protected abstract boolean checkCulling(Camera viewCam);

    @Override
    public void preFrame(float tpf) {
        // no-op
    }

    @Override
    public void cleanup() {
        // no-op
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
        // no-op
    }

    /**
     * Returns the current shadow intensity.
     *
     * @return The shadow intensity value, ranging from 0.0 to 1.0.
     * @see #setShadowIntensity(float shadowIntensity)
     */
    public float getShadowIntensity() {
        return shadowIntensity;
    }

    /**
     * Sets the shadow intensity. This value controls the darkness of the shadows.
     * A value of 0.0 results in bright, almost invisible shadows, while 1.0 creates
     * pitch-black shadows. The default value is 0.7.
     *
     * @param shadowIntensity The desired darkness of the shadow, a float between 0.0 and 1.0.
     */
    final public void setShadowIntensity(float shadowIntensity) {
        this.shadowIntensity = shadowIntensity;
        postshadowMat.setFloat("ShadowIntensity", shadowIntensity);
    }

    /**
     * Returns the configured shadow edges thickness. The value is returned
     * as an integer representing tenths of a pixel (e.g., 10 for 1.0 pixel).
     *
     * @return The edges thickness in tenths of a pixel.
     * @see #setEdgesThickness(int edgesThickness)
     */
    public int getEdgesThickness() {
        return (int) (edgesThickness * 10);
    }

    /**
     * Returns the number of shadow maps currently rendered by this processor.
     *
     * @return The count of shadow maps.
     */
    public int getNumShadowMaps() {
        return nbShadowMaps;
    }

    /**
     * Returns the size (width and height) of each shadow map rendered by this processor.
     *
     * @return The resolution of a single shadow map in pixels.
     */
    public int getShadowMapSize() {
        return (int) shadowMapSize;
    }

    /**
     * Sets the shadow edges thickness. This parameter influences the
     * smoothness of shadow edges, particularly with PCF (Percentage-Closer Filtering).
     * Setting lower values can help reduce jagged artifacts.
     *
     * @param edgesThickness The desired thickness in tenths of a pixel (e.g., 10 for 1.0 pixel).
     * The value is clamped between 1 and 10. Default is 10.
     */
    public void setEdgesThickness(int edgesThickness) {
        this.edgesThickness = Math.max(1, Math.min(edgesThickness, 10));
        this.edgesThickness *= 0.1f;
        postshadowMat.setFloat("PCFEdge", this.edgesThickness);
    }

    /**
     *  isFlushQueues does nothing now and is kept only for backward compatibility
     *
     * @return false
     */
    @Deprecated
    public boolean isFlushQueues() {
        return false;
    }

    /**
     * Returns the {@link RenderState} that is forced during the pre-shadow pass.
     * You can use this to adjust the rendering parameters for geometries that cast shadows.
     * Note that this will be overridden if the "PreShadow" technique in the material definition
     * has its own `ForcedRenderState`.
     *
     * @return The {@link RenderState} applied to the pre-shadow pass.
     */
    public RenderState getPreShadowForcedRenderState() {
        return forcedRenderState;
    }

    /**
     * Sets whether back faces of geometries should cast shadows.
     * When enabled, shadows cast by the back side of an object can appear.
     * Be aware that back face shadows can sometimes lead to overly dark lighting
     * when blended with existing dark areas.
     *
     * <p>Setting this parameter will globally override this setting for ALL materials
     * in the scene for the shadow pass. Alternatively, you can control this on
     * individual materials using {@link Material#setBoolean(String, boolean)}
     * with the "BackfaceShadows" parameter.
     *
     * <p>This method also automatically adjusts the {@link RenderState.FaceCullMode}
     * and {@link RenderState#setPolyOffset(float, float)} of the pre-shadow pass
     * to accommodate back face rendering. You can further modify these
     * using {@link #getPreShadowForcedRenderState()}.
     *
     * @param renderBackFacesShadows True to enable back face shadows, false to disable.
     */
    public void setRenderBackFacesShadows(boolean renderBackFacesShadows) {
        this.renderBackFacesShadows = renderBackFacesShadows;
        if (renderBackFacesShadows) {
            getPreShadowForcedRenderState().setPolyOffset(5, 3);
            getPreShadowForcedRenderState().setFaceCullMode(RenderState.FaceCullMode.Back);
        } else {
            getPreShadowForcedRenderState().setPolyOffset(0, 0);
            getPreShadowForcedRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
        }
    }

    /**
     * Checks if this shadow processor is configured to render shadows from back faces.
     *
     * @return True if back face shadows are enabled, false otherwise.
     */
    public boolean isRenderBackFacesShadows() {
        return renderBackFacesShadows;
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        forcedRenderState = cloner.clone(forcedRenderState);
        init(assetManager, nbShadowMaps, (int) shadowMapSize);
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
        this.prof = profiler;
    }

    /**
     * De-serialize this instance, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
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
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(nbShadowMaps, "nbShadowMaps", 1);
        oc.write(shadowMapSize, "shadowMapSize", 0);
        oc.write(shadowIntensity, "shadowIntensity", 0.7f);
        oc.write(edgeFilteringMode, "edgeFilteringMode", EdgeFilteringMode.Bilinear);
        oc.write(shadowCompareMode, "shadowCompareMode", CompareMode.Hardware);
        oc.write(edgesThickness, "edgesThickness", 1.0f);
    }
}
