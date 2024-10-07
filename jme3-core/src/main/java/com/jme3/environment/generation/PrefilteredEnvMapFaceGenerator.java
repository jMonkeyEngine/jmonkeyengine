/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.app.Application;
import com.jme3.environment.util.CubeMapWrapper;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.math.*;
import com.jme3.texture.TextureCubeMap;

import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static com.jme3.environment.util.EnvMapUtils.*;
import static com.jme3.math.FastMath.abs;
import static com.jme3.math.FastMath.sqrt;

/**
 * Generates one face of the prefiltered environment map for PBR. This job can
 * be launched from a separate thread.
 * <p>
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
    private EnvMapUtils.GenerationType genType;
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
     * @param app      the Application
     * @param face     the face to generate
     * @param listener to monitor progress (alias created)
     */
    public PrefilteredEnvMapFaceGenerator(Application app, int face, JobProgressListener<Integer> listener) {
        super(listener);
        this.app = app;
        this.face = face;
    }


    /**
     * Fills all the generation parameters
     *
     * @param sourceMap      the source cube map
     * @param targetMapSize  the size of the generated map (width or height in
     *                       pixel)
     * @param fixSeamsMethod the method used to fix seams as described in
     *                       {@link com.jme3.environment.util.EnvMapUtils.FixSeamsMethod}
     * @param genType        select Fast or HighQuality
     * @param store          The cube map to store the result in.
     */
    public void setGenerationParam(TextureCubeMap sourceMap, int targetMapSize, EnvMapUtils.FixSeamsMethod fixSeamsMethod, EnvMapUtils.GenerationType genType, TextureCubeMap store) {
        this.sourceMap = sourceMap;
        this.targetMapSize = targetMapSize;
        this.fixSeamsMethod = fixSeamsMethod;
        this.store = store;
        this.genType = genType;
        init();
    }

    private void init() {
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
            @SuppressWarnings("unchecked")
            public Void call() throws Exception {
                listener.done(face);
                return null;
            }
        });
    }

    /**
     * Generates the prefiltered env map (used for image based specular
     * lighting) With the GGX/Schlick brdf
     * {@link EnvMapUtils#getSphericalHarmonicsCoefficents(com.jme3.texture.TextureCubeMap)}
     * Note that the output cube map is in RGBA8 format.
     *
     * @param sourceEnvMap
     * @param targetMapSize  the size of the irradiance map to generate
     * @param store
     * @param fixSeamsMethod the method to fix seams
     * @return The irradiance cube map for the given coefficients
     */
    private TextureCubeMap generatePrefilteredEnvMap(TextureCubeMap sourceEnvMap, int targetMapSize, EnvMapUtils.FixSeamsMethod fixSeamsMethod, TextureCubeMap store) {
        try {
            TextureCubeMap pem = store;

            int nbMipMap = store.getImage().getMipMapSizes().length;

            setEnd(nbMipMap);

            if (!sourceEnvMap.getImage().hasMipmaps() || sourceEnvMap.getImage().getMipMapSizes().length < nbMipMap) {
                throw new IllegalArgumentException("The input cube map must have at least " + nbMipMap + "mip maps");
            }

            CubeMapWrapper sourceWrapper = new CubeMapWrapper(sourceEnvMap);
            CubeMapWrapper targetWrapper = new CubeMapWrapper(pem);

            Vector3f texelVect = new Vector3f();
            Vector3f color = new Vector3f();
            ColorRGBA outColor = new ColorRGBA();
            int targetMipMapSize = targetMapSize;
            for (int mipLevel = 0; mipLevel < nbMipMap; mipLevel++) {
                float roughness = getRoughnessFromMip(mipLevel, nbMipMap);
                int nbSamples = getSampleFromMip(mipLevel, nbMipMap);

                for (int y = 0; y < targetMipMapSize; y++) {
                    for (int x = 0; x < targetMipMapSize; x++) {
                        color.set(0, 0, 0);
                        getVectorFromCubemapFaceTexCoord(x, y, targetMipMapSize, face, texelVect, fixSeamsMethod);
                        prefilterEnvMapTexel(sourceWrapper, roughness, texelVect, nbSamples, mipLevel, color);

                        outColor.set(Math.max(color.x, 0.0001f), Math.max(color.y, 0.0001f), Math.max(color.z, 0.0001f), 1);
                        targetWrapper.setPixel(x, y, face, mipLevel, outColor);

                    }
                }
                targetMipMapSize /= 2;
                progress();
            }

            return pem;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Vector3f prefilterEnvMapTexel(CubeMapWrapper envMapReader, float roughness, Vector3f N, int numSamples, int mipLevel, Vector3f store) {

        Vector3f prefilteredColor = store;
        float totalWeight = 0.0f;

        int nbRotations = 1;
        if (genType == GenerationType.HighQuality) {
            nbRotations = numSamples == 1 ? 1 : 18;
        }

        float rad = 2f * FastMath.PI / nbRotations;
        // offset rotation to avoid sampling pattern
        float gi = (float) (FastMath.abs(N.z + N.x) * 256.0);
        float offset = rad * (FastMath.cos((gi * 0.5f) % (2f * FastMath.PI)) * 0.5f + 0.5f);

        // a = roughness² and a2 = a²
        float a2 = roughness * roughness;
        a2 *= a2;

        //Computing tangent frame
        Vector3f upVector = Vector3f.UNIT_X;
        if (abs(N.z) < 0.999) {
            upVector = Vector3f.UNIT_Y;
        }
        Vector3f tangentX = tmp1.set(upVector).crossLocal(N).normalizeLocal();
        Vector3f tangentY = tmp2.set(N).crossLocal(tangentX);

        // https://placeholderart.wordpress.com/2015/07/28/implementation-notes-runtime-environment-map-filtering-for-image-based-lighting/
        // in local space view == normal == 0,0,1
        Vector3f V = new Vector3f(0, 0, 1);

        Vector3f lWorld = new Vector3f();
        for (int i = 0; i < numSamples; i++) {
            Xi = getHammersleyPoint(i, numSamples, Xi);
            H = importanceSampleGGX(Xi, a2, H);
            H.normalizeLocal();
            float VoH = H.z;
            Vector3f L = H.multLocal(VoH * 2f).subtractLocal(V);
            float NoL = L.z;

            float computedMipLevel = mipLevel;
            if (mipLevel != 0) {
                computedMipLevel = computeMipLevel(roughness, numSamples, this.targetMapSize, VoH);
            }

            toWorld(L, N, tangentX, tangentY, lWorld);
            totalWeight += samplePixel(envMapReader, lWorld, NoL, computedMipLevel, prefilteredColor);

            for (int j = 1; j < nbRotations; j++) {
                rotateDirection(offset + j * rad, L, lWorld);
                L.set(lWorld);
                toWorld(L, N, tangentX, tangentY, lWorld);
                totalWeight += samplePixel(envMapReader, lWorld, NoL, computedMipLevel, prefilteredColor);
            }

        }
        if (totalWeight > 0) {
            prefilteredColor.divideLocal(totalWeight);
        }

        return prefilteredColor;
    }

    private float samplePixel(CubeMapWrapper envMapReader, Vector3f lWorld, float NoL, float computedMipLevel, Vector3f store) {

        if (NoL <= 0) {
            return 0;
        }
        envMapReader.getPixel(lWorld, computedMipLevel, c);
        store.setX(store.x + c.r * NoL);
        store.setY(store.y + c.g * NoL);
        store.setZ(store.z + c.b * NoL);

        return NoL;
    }

    private void toWorld(Vector3f L, Vector3f N, Vector3f tangentX, Vector3f tangentY, Vector3f store) {
        store.set(tangentX).multLocal(L.x);
        tmp.set(tangentY).multLocal(L.y);
        store.addLocal(tmp);
        tmp.set(N).multLocal(L.z);
        store.addLocal(tmp);
    }

    private float computeMipLevel(float roughness, int numSamples, float size, float voH) {
        // H[2] is NoH in local space
        // adds 1.e-5 to avoid ggx / 0.0
        float NoH = voH + 1E-5f;

        // Probability Distribution Function
        float Pdf = ggx(NoH, roughness) * NoH / (4.0f * voH);

        // Solid angle represented by this sample
        float omegaS = 1.0f / (numSamples * Pdf);

        // Solid angle covered by 1 pixel with 6 faces that are EnvMapSize X EnvMapSize
        float omegaP = 4.0f * FastMath.PI / (6.0f * size * size);

        // Original paper suggest biasing the mip to improve the results
        float mipBias = 1.0f; // I tested that the result is better with bias 1
        double maxLod = Math.log(size) / Math.log(2f);
        double log2 = Math.log(omegaS / omegaP) / Math.log(2);
        return Math.min(Math.max(0.5f * (float) log2 + mipBias, 0.0f), (float) maxLod);
    }


    private float ggx(float NoH, float alpha) {
        // use GGX / Trowbridge-Reitz, same as Disney and Unreal 4
        // cf http://blog.selfshadow.com/publications/s2013-shading-course/karis/s2013_pbs_epic_notes_v2.pdf p3
        float tmp = alpha / (NoH * NoH * (alpha * alpha - 1.0f) + 1.0f);
        return tmp * tmp * (1f / FastMath.PI);
    }

    private Vector3f rotateDirection(float angle, Vector3f l, Vector3f store) {
        float s, c, t;

        s = FastMath.sin(angle);
        c = FastMath.cos(angle);
        t = 1.f - c;

        store.x = l.x * c + l.y * s;
        store.y = -l.x * s + l.y * c;
        store.z = l.z * (t + c);
        return store;
    }

    /**
     * Computes GGX half vector in local space
     *
     * @param xi (not null)
     * @param a2 fourth power of roughness
     * @param store caller-provided storage
     * @return either store or a new vector (not null)
     */
    public Vector3f importanceSampleGGX(Vector4f xi, float a2, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }

        float cosTheta = sqrt((1f - xi.x) / (1f + (a2 - 1f) * xi.x));
        float sinTheta = sqrt(1f - cosTheta * cosTheta);

        float sinThetaCosPhi = sinTheta * xi.z;//xi.z is cos(phi)
        float sinThetaSinPhi = sinTheta * xi.w;//xi.w is sin(phi)

        store.x = sinThetaCosPhi;
        store.y = sinThetaSinPhi;
        store.z = cosTheta;

        return store;
    }

}
