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
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.TextureUnitException;
import com.jme3.renderer.opengl.ComputeShader;
import com.jme3.renderer.opengl.GL4;
import com.jme3.renderer.opengl.GLFence;
import com.jme3.renderer.opengl.ShaderStorageBufferObject;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Compute shader used in SDSM.
 */
public class SdsmFitter {

    private static final String REDUCE_DEPTH_SHADER = "Common/MatDefs/Shadow/Sdsm/ReduceDepth.comp";
    private static final String FIT_FRUSTUMS_SHADER = "Common/MatDefs/Shadow/Sdsm/FitLightFrustums.comp";

    private final GL4 gl4;
    private final Renderer renderer;
    private int maxFrameLag = 3;

    private final ComputeShader depthReduceShader;
    private final ComputeShader fitFrustumsShader;

    private final LinkedList<SdsmResultHolder> resultHoldersInFlight = new LinkedList<>();
    private final LinkedList<SdsmResultHolder> resultHoldersReady = new LinkedList<>();
    private SplitFitResult readyToYield;

    // Initial values for fit frustum SSBO
    // 4 cascades x (minX, minY, maxX, maxY) + 4 x (minZ, maxZ) + globalMin + globalMax + 3 x (splitStart, blendEnd)
    private static final int[] FIT_FRUSTUM_INIT = new int[32];
    static {
        for (int i = 0; i < 4; i++) {
            FIT_FRUSTUM_INIT[i * 4] = -1; //MinX (-1 == maximum UINT value)
            FIT_FRUSTUM_INIT[i * 4 + 1] = -1; //MinY
            FIT_FRUSTUM_INIT[i * 4 + 2] = 0; //MaxX
            FIT_FRUSTUM_INIT[i * 4 + 3] = 0; //MaxY
        }
        for (int i = 0; i < 4; i++) {
            FIT_FRUSTUM_INIT[16 + i * 2] = -1; //MinZ
            FIT_FRUSTUM_INIT[16 + i * 2 + 1] = 0; //MaxZ
        }
        FIT_FRUSTUM_INIT[24] = -1; //Global min
        FIT_FRUSTUM_INIT[25] = 0; //Global max
        // Split starts (3 splits max)
        for (int i = 0; i < 6; i++) {
            FIT_FRUSTUM_INIT[26 + i] = 0;
        }
    }

    /**
     * Parameters used for a fit operation.
     */
    public static class FitParameters {
        public final Matrix4f cameraToLight;
        public final int splitCount;
        public final float cameraNear;
        public final float cameraFar;

        public FitParameters(Matrix4f cameraToLight, int splitCount, float cameraNear, float cameraFar) {
            this.cameraToLight = cameraToLight;
            this.splitCount = splitCount;
            this.cameraNear = cameraNear;
            this.cameraFar = cameraFar;
        }

        @Override
        public String toString() {
            return "FitParameters{" +
                    "cameraToLight=" + cameraToLight +
                    ", splitCount=" + splitCount +
                    ", cameraNear=" + cameraNear +
                    ", cameraFar=" + cameraFar +
                    '}';
        }
    }

    /**
     * Bounds for a single cascade split in light space.
     */
    public static class SplitBounds {
        public final float minX, minY, maxX, maxY;
        public final float minZ, maxZ;

        public SplitBounds(float minX, float minY, float maxX, float maxY, float minZ, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }

        public boolean isValid() {
            return minX != Float.POSITIVE_INFINITY && minY != Float.POSITIVE_INFINITY && minZ != Float.POSITIVE_INFINITY && maxX != Float.NEGATIVE_INFINITY && maxY != Float.NEGATIVE_INFINITY && maxZ != Float.NEGATIVE_INFINITY;
        }

        @Override
        public String toString() {
            return "SplitBounds{" +
                    "minX=" + minX +
                    ", minY=" + minY +
                    ", maxX=" + maxX +
                    ", maxY=" + maxY +
                    ", minZ=" + minZ +
                    ", maxZ=" + maxZ +
                    '}';
        }
    }

    /**
     * Information about where a cascade split starts/ends for blending.
     */
    public static class SplitInfo {
        public final float start;
        public final float end;

        public SplitInfo(float start, float end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toString() {
            return "SplitInfo{" +
                    "start=" + start +
                    ", end=" + end +
                    '}';
        }
    }

    /**
     * Complete fit result for all cascades.
     */
    public static class SplitFit {
        public final List<SplitBounds> splits;
        public final float minDepth;
        public final float maxDepth;
        public final List<SplitInfo> cascadeStarts;

        public SplitFit(List<SplitBounds> splits, float minDepth, float maxDepth, List<SplitInfo> cascadeStarts) {
            this.splits = splits;
            this.minDepth = minDepth;
            this.maxDepth = maxDepth;
            this.cascadeStarts = cascadeStarts;
        }

        @Override
        public String toString() {
            return "SplitFit{" +
                    "splits=" + splits +
                    ", minDepth=" + minDepth +
                    ", maxDepth=" + maxDepth +
                    ", cascadeStarts=" + cascadeStarts +
                    '}';
        }
    }

    /**
     * Result of a fit operation, including parameters and computed fit.
     */
    public static class SplitFitResult {
        public final FitParameters parameters;
        public final SplitFit result;

        public SplitFitResult(FitParameters parameters, SplitFit result) {
            this.parameters = parameters;
            this.result = result;
        }

        @Override
        public String toString() {
            return "SplitFitResult{" +
                    "parameters=" + parameters +
                    ", result=" + result +
                    '}';
        }
    }

    /**
     * Internal holder for in-flight fit operations.
     */
    private class SdsmResultHolder {
        ShaderStorageBufferObject minMaxDepthSsbo;
        ShaderStorageBufferObject fitFrustumSsbo;
        FitParameters parameters;
        GLFence fence;

        SdsmResultHolder() {
            this.minMaxDepthSsbo = new ShaderStorageBufferObject(gl4);
            this.fitFrustumSsbo = new ShaderStorageBufferObject(gl4);
        }

        boolean isReady(boolean wait) {
            if (fence == null) {
                return true;
            }
            int status = gl4.glClientWaitSync(fence, 0, wait ? -1 : 0);
            return status == GL4.GL_ALREADY_SIGNALED || status == GL4.GL_CONDITION_SATISFIED;
        }

        SplitFitResult extract() {
            if (fence != null) {
                gl4.glDeleteSync(fence);
                fence = null;
            }
            SplitFit fit = extractFit();
            return new SplitFitResult(parameters, fit);
        }

        private SplitFit extractFit() {
            int[] uintFit = fitFrustumSsbo.read(32);
            float[] fitResult = new float[32];
            for(int i=0;i<fitResult.length;i++) {
                fitResult[i] = uintFlip(uintFit[i]);
            }

            float minDepth = fitResult[24];
            if (minDepth == Float.POSITIVE_INFINITY) {
                // No real samples found
                return null;
            }
            float maxDepth = fitResult[25];
            if (maxDepth == 0) {
                return null;
            }

            List<SplitBounds> cascadeData = new ArrayList<>();
            for (int idx = 0; idx < parameters.splitCount; idx++) {
                int start = idx * 4;
                int zStart = 16 + idx * 2;
                SplitBounds bounds = new SplitBounds(
                        fitResult[start],
                        fitResult[start + 1],
                        fitResult[start + 2],
                        fitResult[start + 3],
                        fitResult[zStart],
                        fitResult[zStart + 1]
                );
                cascadeData.add(bounds.isValid() ? bounds : null);
            }

            float minDepthView = getProjectionToViewZ(parameters.cameraNear, parameters.cameraFar, minDepth);
            float maxDepthView = getProjectionToViewZ(parameters.cameraNear, parameters.cameraFar, maxDepth);

            List<SplitInfo> cascadeStarts = new ArrayList<>();
            for (int i = 0; i < parameters.splitCount - 1; i++) {
                float splitStart = fitResult[26 + i * 2];
                float splitEnd = fitResult[26 + i * 2 + 1];
                assert !Float.isNaN(splitStart) && !Float.isNaN(splitEnd);
                cascadeStarts.add(new SplitInfo(splitStart, splitEnd));
            }

            return new SplitFit(cascadeData, minDepthView, maxDepthView, cascadeStarts);
        }

        void cleanup() {
            minMaxDepthSsbo.delete();
            fitFrustumSsbo.delete();
            if (fence != null) {
                gl4.glDeleteSync(fence);
            }
        }
    }

    public SdsmFitter(GL4 gl, Renderer renderer, AssetManager assetManager) {
        this.gl4 = gl;
        this.renderer = renderer;

        // Load compute shaders
        String reduceSource = (String)assetManager.loadAsset(REDUCE_DEPTH_SHADER);
        String fitSource = (String)assetManager.loadAsset(FIT_FRUSTUMS_SHADER);

        depthReduceShader = new ComputeShader(gl, reduceSource);
        fitFrustumsShader = new ComputeShader(gl, fitSource);
    }

    /**
     * Initiates an asynchronous fit operation on the given depth texture.
     *
     * @param depthTexture the depth texture to analyze
     * @param splitCount number of cascade splits (1-4)
     * @param cameraToLight transformation matrix from camera clip space to light view space
     * @param cameraNear camera near plane distance
     * @param cameraFar camera far plane distance
     */
    public void fit(Texture depthTexture, int splitCount, Matrix4f cameraToLight,
                    float cameraNear, float cameraFar) {

        SdsmResultHolder holder = getResultHolderForUse();
        holder.parameters = new FitParameters(cameraToLight, splitCount, cameraNear, cameraFar);

        gl4.glMemoryBarrier(GL4.GL_TEXTURE_FETCH_BARRIER_BIT);

        int width = depthTexture.getImage().getWidth();
        int height = depthTexture.getImage().getHeight();
        int xGroups = divRoundUp(width, 32);
        int yGroups = divRoundUp(height, 32);

        if (xGroups < 2) {
            throw new RendererException("Depth texture too small for SDSM fit");
        }

        // Initialize SSBOs
        holder.minMaxDepthSsbo.initialize(new int[]{-1, 0}); // max uint, 0

        // Pass 1: Reduce depth to find min/max
        depthReduceShader.makeActive();
        try {
            renderer.setTexture(0, depthTexture);
        } catch (TextureUnitException e) {
            throw new RendererException(e);
        }
        depthReduceShader.bindShaderStorageBuffer(1, holder.minMaxDepthSsbo);
        depthReduceShader.dispatch(xGroups, yGroups, 1);
        gl4.glMemoryBarrier(GL4.GL_SHADER_STORAGE_BARRIER_BIT);

        // Pass 2: Fit cascade frustums
        holder.fitFrustumSsbo.initialize(FIT_FRUSTUM_INIT);

        fitFrustumsShader.makeActive();
        try {
            renderer.setTexture(0, depthTexture);
        } catch (TextureUnitException e) {
            throw new RendererException(e);
        }
        fitFrustumsShader.bindShaderStorageBuffer(1, holder.minMaxDepthSsbo);
        fitFrustumsShader.bindShaderStorageBuffer(2, holder.fitFrustumSsbo);

        fitFrustumsShader.setUniform(3, cameraToLight);
        fitFrustumsShader.setUniform(4, splitCount);
        fitFrustumsShader.setUniform(5, new Vector2f(cameraNear, cameraFar));
        fitFrustumsShader.setUniform(6, 0.05f);

        fitFrustumsShader.dispatch(xGroups, yGroups, 1);
        gl4.glMemoryBarrier(GL4.GL_SHADER_STORAGE_BARRIER_BIT);

        // Create fence for async readback
        holder.fence = gl4.glFenceSync(GL4.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
        resultHoldersInFlight.add(holder);
    }

    /**
     * Gets the next available fit result.
     *
     * @param wait if true, blocks until a result is available
     * @return the fit result, or null if none available (and wait is false)
     */
    public SplitFitResult getResult(boolean wait) {
        if (readyToYield != null) {
            SplitFitResult result = readyToYield;
            readyToYield = null;
            return result;
        }

        SplitFitResult result = null;
        Iterator<SdsmResultHolder> iter = resultHoldersInFlight.iterator();
        while (iter.hasNext()) {
            SdsmResultHolder next = iter.next();
            boolean mustHaveResult = result == null && wait;
            if (next.isReady(mustHaveResult)) {
                iter.remove();
                result = next.extract();
                resultHoldersReady.add(next);
            } else {
                break;
            }
        }
        if(wait && result == null){
            throw new IllegalStateException();
        }
        return result;
    }

    /**
     * Cleans up GPU resources.
     */
    public void cleanup() {
        for (SdsmResultHolder holder : resultHoldersInFlight) {
            holder.cleanup();
        }
        resultHoldersInFlight.clear();

        for (SdsmResultHolder holder : resultHoldersReady) {
            holder.cleanup();
        }
        resultHoldersReady.clear();

        if (depthReduceShader != null) {
            depthReduceShader.delete();
        }
        if (fitFrustumsShader != null) {
            fitFrustumsShader.delete();
        }
    }

    private SdsmResultHolder getResultHolderForUse() {
        if (!resultHoldersReady.isEmpty()) {
            return resultHoldersReady.removeFirst();
        } else if (resultHoldersInFlight.size() <= maxFrameLag) {
            return new SdsmResultHolder();
        } else {
            SdsmResultHolder next = resultHoldersInFlight.removeFirst();
            next.isReady(true);
            readyToYield = next.extract();
            return next;
        }
    }

    private static float getProjectionToViewZ(float near, float far, float projZPos) {
        float a = far / (far - near);
        float b = far * near / (near - far);
        return b / (projZPos - a);
    }

    private static int divRoundUp(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }

    /**
     * Converts a uint-encoded float back to float.
     * This is the inverse of the floatFlip function in the shader.
     */
    private static float uintFlip(int u) {
        int flipped;
        if ((u & 0x80000000) != 0) {
            flipped = u ^ 0x80000000;  // Was positive, flip sign bit
        } else {
            flipped = ~u;  // Was negative, invert all bits
        }
        return Float.intBitsToFloat(flipped);
    }

    public void setMaxFrameLag(int maxFrameLag) {
        this.maxFrameLag = maxFrameLag;
    }
}