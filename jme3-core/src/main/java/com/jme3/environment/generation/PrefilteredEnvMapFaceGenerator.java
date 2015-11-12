/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.environment.generation;

import com.jme3.environment.util.CubeMapWrapper;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import static com.jme3.math.FastMath.abs;
import static com.jme3.math.FastMath.clamp;
import static com.jme3.math.FastMath.pow;
import static com.jme3.math.FastMath.sqrt;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.texture.TextureCubeMap;
import static com.jme3.environment.util.EnvMapUtils.getHammersleyPoint;
import static com.jme3.environment.util.EnvMapUtils.getRoughnessFromMip;
import static com.jme3.environment.util.EnvMapUtils.getSampleFromMip;
import static com.jme3.environment.util.EnvMapUtils.getVectorFromCubemapFaceTexCoord;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Generates one face of the prefiltered environnement map for PBR. This job can
 * be lauched from a separate thread.
 *
 * TODO there is a lot of duplicate code here with the EnvMapUtils.
 *
 * @author Nehon
 */
//TODO there is a lot of duplicate code here with the EnvMapUtils. We should, 
//either leverage the code from the util class either remove it and only allow 
//parallel generation using this runnable.
public class PrefilteredEnvMapFaceGenerator extends RunnableWithProgress {

    private final static Logger log = Logger.getLogger(PrefilteredEnvMapFaceGenerator.class.getName());

    private int targetMapSize;
    private EnvMapUtils.FixSeamsMethod fixSeamsMethod;
    private TextureCubeMap sourceMap;
    private TextureCubeMap store;
    private final Application app;
    private int face = 0;
    Vector4f Xi = new Vector4f();
    Vector3f H = new Vector3f();
    Vector3f tmp = new Vector3f();
    ColorRGBA c = new ColorRGBA();
    Vector3f tmp1 = new Vector3f();
    Vector3f tmp2 = new Vector3f();
    Vector3f tmp3 = new Vector3f();

    /**
     * Creates a pem generator for the given face. The app is needed to enqueue
     * the call to the EnvironmentCamera when the generation is done, so that
     * this process is thread safe.
     *
     * @param app the Application
     * @param face the face to generate
     * @param listener
     */
    public PrefilteredEnvMapFaceGenerator(Application app, int face, JobProgressListener<Integer> listener) {
        super(listener);
        this.app = app;
        this.face = face;
    }

    
    
    /**
     * Fills all the genration parameters
     *
     * @param sourceMap the source cube map
     * @param targetMapSize the size of the generated map (width or height in
     * pixel)
     * @param fixSeamsMethod the method used to fix seams as described here
     * {@link EnvMapUtils.FixSeamsMethod}
     *
     * @param store The cube map to store the result in.
     */
    public void setGenerationParam(TextureCubeMap sourceMap, int targetMapSize, EnvMapUtils.FixSeamsMethod fixSeamsMethod, TextureCubeMap store) {
        this.sourceMap = sourceMap;
        this.targetMapSize = targetMapSize;
        this.fixSeamsMethod = fixSeamsMethod;
        this.store = store;
        init();
    }
    
    private void init(){
         Xi.set(0, 0, 0, 0);
         H.set(0, 0, 0);
         tmp.set(0, 0, 0);
         c.set(1, 1, 1, 1);
         tmp1.set(0, 0, 0);
         tmp2.set(0, 0, 0);
         tmp3.set(0, 0, 0);
         reset();

    }

    @Override
    public void run() {

        app.enqueue(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                listener.start();
                return null;
            }
        });
        store = generatePrefilteredEnvMap(sourceMap, targetMapSize, fixSeamsMethod, store);
        app.enqueue(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                listener.done(face);
                return null;
            }
        });
    }

    /**
     * Generates the prefiltered env map (used for image based specular
     * lighting) With the GGX/Shlick brdf
     * {@link EnvMapUtils#getSphericalHarmonicsCoefficents(com.jme3.texture.TextureCubeMap)}
     * Note that the output cube map is in RGBA8 format.
     *
     * @param sourceEnvMap
     * @param targetMapSize the size of the irradiance map to generate
     * @param store
     * @param fixSeamsMethod the method to fix seams
     * @return The irradiance cube map for the given coefficients
     */
    private TextureCubeMap generatePrefilteredEnvMap(TextureCubeMap sourceEnvMap, int targetMapSize, EnvMapUtils.FixSeamsMethod fixSeamsMethod, TextureCubeMap store) {
        TextureCubeMap pem = store;

        int nbMipMap = (int) (Math.log(targetMapSize) / Math.log(2) - 1);

        setEnd(nbMipMap);
        

        CubeMapWrapper sourceWrapper = new CubeMapWrapper(sourceEnvMap);
        CubeMapWrapper targetWrapper = new CubeMapWrapper(pem);

        Vector3f texelVect = new Vector3f();
        Vector3f color = new Vector3f();
        ColorRGBA outColor = new ColorRGBA();
        for (int mipLevel = 0; mipLevel < nbMipMap; mipLevel++) {
            float roughness = getRoughnessFromMip(mipLevel, nbMipMap);
            int nbSamples = getSampleFromMip(mipLevel, nbMipMap);
            int targetMipMapSize = (int) pow(2, nbMipMap + 1 - mipLevel);

            for (int y = 0; y < targetMipMapSize; y++) {
                for (int x = 0; x < targetMipMapSize; x++) {
                    color.set(0, 0, 0);
                    getVectorFromCubemapFaceTexCoord(x, y, targetMipMapSize, face, texelVect, EnvMapUtils.FixSeamsMethod.Wrap);
                    prefilterEnvMapTexel(sourceWrapper, roughness, texelVect, nbSamples, color);
                    
                    outColor.set(Math.max(color.x, 0.0001f), Math.max(color.y,0.0001f), Math.max(color.z, 0.0001f), 1);
                    log.log(Level.FINE, "coords {0},{1}", new Object[]{x, y});
                    targetWrapper.setPixel(x, y, face, mipLevel, outColor);

                }
            }
            progress();
        }

        return pem;
    }

    private Vector3f prefilterEnvMapTexel(CubeMapWrapper envMapReader, float roughness, Vector3f N, int numSamples, Vector3f store) {

        Vector3f prefilteredColor = store;
        float totalWeight = 0.0f;

        // a = roughness² and a2 = a²
        float a2 = roughness * roughness;
        a2 *= a2;
        a2 *= 10;
        for (int i = 0; i < numSamples; i++) {
            Xi = getHammersleyPoint(i, numSamples, Xi);
            H = importanceSampleGGX(Xi, a2, N, H);

            H.normalizeLocal();
            tmp.set(H);
            float NoH = N.dot(tmp);

            Vector3f L = tmp.multLocal(NoH * 2).subtractLocal(N);
            float NoL = clamp(N.dot(L), 0.0f, 1.0f);
            if (NoL > 0) {
                envMapReader.getPixel(L, c);
                prefilteredColor.setX(prefilteredColor.x + c.r * NoL);
                prefilteredColor.setY(prefilteredColor.y + c.g * NoL);
                prefilteredColor.setZ(prefilteredColor.z + c.b * NoL);

                totalWeight += NoL;
            }
        }

        return prefilteredColor.divideLocal(totalWeight);
    }

    public Vector3f importanceSampleGGX(Vector4f xi, float a2, Vector3f normal, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }

        float cosTheta = sqrt((1f - xi.x) / (1f + (a2 - 1f) * xi.x));
        float sinTheta = sqrt(1f - cosTheta * cosTheta);

        float sinThetaCosPhi = sinTheta * xi.z;//xi.z is cos(phi)
        float sinThetaSinPhi = sinTheta * xi.w;//xi.w is sin(phi)

        Vector3f upVector = Vector3f.UNIT_X;

        if (abs(normal.z) < 0.999) {
            upVector = Vector3f.UNIT_Y;
        }

        Vector3f tangentX = tmp1.set(upVector).crossLocal(normal).normalizeLocal();
        Vector3f tangentY = tmp2.set(normal).crossLocal(tangentX);

        // Tangent to world space
        tangentX.multLocal(sinThetaCosPhi);
        tangentY.multLocal(sinThetaSinPhi);
        tmp3.set(normal).multLocal(cosTheta);

        // Tangent to world space
        store.set(tangentX).addLocal(tangentY).addLocal(tmp3);

        return store;
    }

}
