/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.opengl.GL4;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.util.clone.Cloner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * See {@link SdsmDirectionalLightShadowFilter}
 */
public class SdsmDirectionalLightShadowRenderer extends AbstractShadowRenderer {

    private DirectionalLight light;
    private final Matrix4f lightViewMatrix = new Matrix4f();
    private Camera[] shadowCameras;
    private boolean[] shadowCameraEnabled;

    private SdsmFitter sdsmFitter;
    private SdsmFitter.SplitFitResult lastFit;
    private Texture depthTexture;

    private boolean glInitialized = false;

    /**
     * Expansion factor for fitted shadow frustums.
     * Larger values reduce shadow pop-in but may waste shadow map resolution.
     */
    private float fitExpansionFactor = 1.0f;

    /**
     * Tolerance for reusing old fit results when camera hasn't moved much.
     * Reduce to eliminate screen-tearing artifacts when rapidly moving or rotating camera, at the cost of lower framerate caused by waiting for SDSM to complete.
     */
    private float fitFrameDelayTolerance = 0.05f;

    /**
     * Used for serialization. Do not use.
     *
     * @see #SdsmDirectionalLightShadowRenderer(AssetManager, int, int)
     */
    protected SdsmDirectionalLightShadowRenderer() {
        super();
    }

    /**
     * Creates an SDSM directional light shadow renderer.
     * You likely should not use this directly, as it requires an SdsmDirectionalLightShadowFilter.
     */
    public SdsmDirectionalLightShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbShadowMaps) {
        super(assetManager, shadowMapSize, nbShadowMaps);
        init(nbShadowMaps, shadowMapSize);
    }

    private void init(int splitCount, int shadowMapSize) {
        if (splitCount < 1 || splitCount > 4) {
            throw new IllegalArgumentException("Number of splits must be between 1 and 4. Given value: " + splitCount);
        }
        this.nbShadowMaps = splitCount;

        shadowCameras = new Camera[this.nbShadowMaps];
        shadowCameraEnabled = new boolean[this.nbShadowMaps];

        for (int i = 0; i < this.nbShadowMaps; i++) {
            shadowCameras[i] = new Camera(shadowMapSize, shadowMapSize);
            shadowCameras[i].setParallelProjection(true);
            shadowCameraEnabled[i] = false;
        }

        needsfallBackMaterial = true;
    }

    /**
     * Initializes the GL interfaces for compute shader operations.
     * Called on first frame when RenderManager is available.
     */
    private void initGL() {
        if (glInitialized) {
            return;
        }

        Renderer renderer = renderManager.getRenderer();
        if (!(renderer instanceof GLRenderer)) {
            throw new UnsupportedOperationException("SdsmDirectionalLightShadowRenderer requires GLRenderer");
        }

        GLRenderer glRenderer = (GLRenderer) renderer;


        GL4 gl4 = glRenderer.getGl4();

        if (gl4 == null) {
            throw new UnsupportedOperationException("SDSM shadows require OpenGL 4.3 or higher");
        }

        sdsmFitter = new SdsmFitter(gl4, renderer, assetManager);
        glInitialized = true;

    }

    /**
     * Returns the light used to cast shadows.
     */
    public DirectionalLight getLight() {
        return light;
    }

    /**
     * Sets the light to use for casting shadows.
     */
    public void setLight(DirectionalLight light) {
        this.light = light;
        if (light != null) {
            generateLightViewMatrix();
        }
    }

    public void setDepthTexture(Texture depthTexture) {
        this.depthTexture = depthTexture;
    }

    /**
     * Gets the fit expansion factor.
     *
     * @return the expansion factor
     */
    public float getFitExpansionFactor() {
        return fitExpansionFactor;
    }

    /**
     * Sets the expansion factor for fitted shadow frustums.
     * <p>
     * A value of 1.0 uses the exact computed bounds.
     * Larger values (e.g., 1.05) add some margin to reduce artifacts
     * from frame delay or precision issues.
     *
     * @param fitExpansionFactor the expansion factor (default 1.0)
     */
    public void setFitExpansionFactor(float fitExpansionFactor) {
        this.fitExpansionFactor = fitExpansionFactor;
    }

    /**
     * Gets the frame delay tolerance for reusing old fit results.
     *
     * @return the tolerance value
     */
    public float getFitFrameDelayTolerance() {
        return fitFrameDelayTolerance;
    }

    /**
     * Sets the tolerance for reusing old fit results.
     * <p>
     * When the camera hasn't moved significantly (within this tolerance),
     * old fit results can be reused to avoid GPU stalls.
     *
     * @param fitFrameDelayTolerance the tolerance (default 0.05)
     */
    public void setFitFrameDelayTolerance(float fitFrameDelayTolerance) {
        this.fitFrameDelayTolerance = fitFrameDelayTolerance;
    }

    private void generateLightViewMatrix() {
        Vector3f lightDir = light.getDirection();
        Vector3f up = Math.abs(lightDir.y) < 0.9f ? Vector3f.UNIT_Y : Vector3f.UNIT_X;
        Vector3f right = lightDir.cross(up).normalizeLocal();
        Vector3f actualUp = right.cross(lightDir).normalizeLocal();

        lightViewMatrix.set(
                right.x, right.y, right.z, 0f,
                actualUp.x, actualUp.y, actualUp.z, 0f,
                lightDir.x, lightDir.y, lightDir.z, 0f,
                0f, 0f, 0f, 1f
        );
    }

    @Override
    protected void initFrustumCam() {}

    @Override
    protected void updateShadowCams(Camera viewCam) {
        if (!glInitialized) {
            initGL();
        }

        if (!tryFitShadowCams(viewCam)) {
            skipPostPass = true;
        }
    }

    private boolean tryFitShadowCams(Camera viewCam) {
        if (depthTexture == null || light == null) {
            return false;
        }

        Vector3f lightDir = light.getDirection();
        if(lightDir.x != lightViewMatrix.m30 || lightDir.y != lightViewMatrix.m31 ||  lightDir.z != lightViewMatrix.m32) {
            generateLightViewMatrix();
        }

        // Compute camera-to-light transformation matrix
        Matrix4f invViewProj = viewCam.getViewProjectionMatrix().invert();
        Matrix4f cameraToLight = lightViewMatrix.mult(invViewProj, invViewProj);

        // Submit fit request to GPU
        sdsmFitter.fit(
                depthTexture,
                nbShadowMaps,
                cameraToLight,
                viewCam.getFrustumNear(),
                viewCam.getFrustumFar()
        );

        // Try to get result without blocking
        SdsmFitter.SplitFitResult fitCallResult = sdsmFitter.getResult(false);

        // If no result yet, try to reuse old fit or wait
        if (fitCallResult == null) {
            fitCallResult = lastFit;
            while (fitCallResult == null || !isOldFitAcceptable(fitCallResult, cameraToLight)) {
                fitCallResult = sdsmFitter.getResult(true);
            }
        }

        lastFit = fitCallResult;
        SdsmFitter.SplitFit fitResult = fitCallResult.result;

        if (fitResult != null) {
            for (int splitIndex = 0; splitIndex < nbShadowMaps; splitIndex++) {
                shadowCameraEnabled[splitIndex] = false;

                SdsmFitter.SplitBounds bounds = fitResult.splits.get(splitIndex);
                if (bounds == null) {
                    continue;
                }

                Camera cam = shadowCameras[splitIndex];

                float centerX = (bounds.minX + bounds.maxX) / 2f;
                float centerY = (bounds.minY + bounds.maxY) / 2f;

                // Position in light space
                Vector3f lightSpacePos = new Vector3f(centerX, centerY, bounds.minZ);

                // Transform back to world space
                Matrix4f invLightView = lightViewMatrix.invert();
                Vector3f worldPos = invLightView.mult(lightSpacePos);

                cam.setLocation(worldPos);
                // Use the same up vector that was used to compute the light view matrix
                // Row 1 of lightViewMatrix contains the actualUp vector (Y axis in light space)
                Vector3f actualUp = new Vector3f(lightViewMatrix.m10, lightViewMatrix.m11, lightViewMatrix.m12);
                cam.lookAtDirection(light.getDirection(), actualUp);

                float width = (bounds.maxX - bounds.minX) * fitExpansionFactor;
                float height = (bounds.maxY - bounds.minY) * fitExpansionFactor;
                float far = (bounds.maxZ - bounds.minZ) * fitExpansionFactor;

                if (width <= 0f || height <= 0f || far <= 0f) {
                    continue; //Skip updating this particular shadowcam, it likely doesn't have any samples or is degenerate.
                }

                cam.setFrustum(
                        -100f, //This will usually help out with clipping problems, where the shadow camera is positioned such that it would clip out a vertex that might cast a shadow.
                        far,
                        -width / 2f,
                        width / 2f,
                        height / 2f,
                        -height / 2f
                );

                shadowCameraEnabled[splitIndex] = true;
                if(Float.isNaN(cam.getViewProjectionMatrix().m00)){
                    throw new IllegalStateException("Invalid shadow projection detected");
                }
            }
            return true;
        }

        return false;
    }

    private boolean isOldFitAcceptable(SdsmFitter.SplitFitResult fit, Matrix4f newCameraToLight) {
        return fit.parameters.cameraToLight.isSimilar(newCameraToLight, fitFrameDelayTolerance);
    }

    @Override
    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders) {
        if (shadowCameraEnabled[shadowMapIndex]) {
            Camera camera = shadowCameras[shadowMapIndex];
            for (Spatial scene : viewPort.getScenes()) {
                ShadowUtil.getGeometriesInCamFrustum(scene, camera, ShadowMode.Cast, shadowMapOccluders);
            }
        }
        return shadowMapOccluders;
    }

    @Override
    protected void getReceivers(GeometryList lightReceivers) { throw new RuntimeException("Only filter mode is implemented for SDSM"); }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCameras[shadowMapIndex];
    }

    @Override
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        if (shadowCameraEnabled[shadowMapIndex]) {
            createDebugFrustum(shadowCameras[shadowMapIndex], shadowMapIndex);
        }
    }

    private Spatial cameraFrustumDebug = null;
    private List<Spatial> shadowMapFrustumDebug = null;
    public void displayAllDebugFrustums() {
        if (cameraFrustumDebug != null) {
            cameraFrustumDebug.removeFromParent();
        }
        if (shadowMapFrustumDebug != null) {
            for (Spatial s : shadowMapFrustumDebug) {
                s.removeFromParent();
            }
        }

        cameraFrustumDebug = createDebugFrustum(viewPort.getCamera(), 4);
        shadowMapFrustumDebug = new ArrayList<>();
        for (int i = 0; i < nbShadowMaps; i++) {
            if (shadowCameraEnabled[i]) {
                shadowMapFrustumDebug.add(createDebugFrustum(shadowCameras[i], i));
            }
        }
    }

    private Geometry createDebugFrustum(Camera camera, int shadowMapColor) {
        Vector3f[] points = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            points[i] = new Vector3f();
        }
        ShadowUtil.updateFrustumPoints2(camera, points);
        Geometry geom = createFrustum(points, shadowMapColor);
        geom.getMaterial().getAdditionalRenderState().setLineWidth(5f);
        geom.getMaterial().getAdditionalRenderState().setDepthWrite(false);
        ((Node) viewPort.getScenes().get(0)).attachChild(geom);
        return geom;
    }

    @Override
    protected void setMaterialParameters(Material material) {
        Vector2f[] splits = getSplits();
        material.setParam("Splits", VarType.Vector2Array, splits);
        material.setVector3("LightDir", light == null ? new Vector3f() : light.getDirection());
    }

    private Vector2f[] getSplits() {
        Vector2f[] result = new Vector2f[3];
        for (int i = 0; i < 3; i++) {
            result[i] = new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        }

        if (lastFit != null && lastFit.result != null) {
            for (int split = 0; split < nbShadowMaps - 1; split++) {
                if (split < lastFit.result.cascadeStarts.size()) {
                    SdsmFitter.SplitInfo splitInfo = lastFit.result.cascadeStarts.get(split);
                    result[split].set(splitInfo.start, splitInfo.end);
                }
            }
        }
        return result;
    }

    @Override
    protected void clearMaterialParameters(Material material) {
        material.clearParam("Splits");
        material.clearParam("LightDir");
    }

    @Override
    protected void setPostShadowParams() {
        setMaterialParameters(postshadowMat);
        postshadowMat.setParam("LightViewProjectionMatrices", VarType.Matrix4Array, lightViewProjectionsMatrices);
        for (int j = 0; j < nbShadowMaps; j++) {
            postshadowMat.setTexture(shadowMapStringCache[j], shadowMaps[j]);
        }
        if (fadeInfo != null) {
            postshadowMat.setVector2("FadeInfo", fadeInfo);
        }
        postshadowMat.setBoolean("BackfaceShadows", renderBackFacesShadows);
    }

    @Override
    protected boolean checkCulling(Camera viewCam) {
        // Directional lights are always visible
        return true;
    }

    /**
     * Cleans up GPU resources used by the SDSM fitter.
     */
    public void cleanup() {
        if (sdsmFitter != null) {
            sdsmFitter.cleanup();
        }
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        light = cloner.clone(light);
        init(nbShadowMaps, (int) shadowMapSize);
        super.cloneFields(cloner, original);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        light = (DirectionalLight) ic.readSavable("light", null);
        fitExpansionFactor = ic.readFloat("fitExpansionFactor", 1.0f);
        fitFrameDelayTolerance = ic.readFloat("fitFrameDelayTolerance", 0.05f);
        init(nbShadowMaps, (int) shadowMapSize);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(light, "light", null);
        oc.write(fitExpansionFactor, "fitExpansionFactor", 1.0f);
        oc.write(fitFrameDelayTolerance, "fitFrameDelayTolerance", 0.05f);
    }

}