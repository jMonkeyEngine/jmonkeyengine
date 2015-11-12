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
import com.jme3.math.Vector3f;
import com.jme3.texture.TextureCubeMap;
import static com.jme3.environment.util.EnvMapUtils.shBandFactor;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

/**
 *
 * Generates the Irrafiance map for PBR. This job can be lauched from a separate
 * thread.
 *
 * TODO there is a lot of duplicate code here with the EnvMapUtils.
 *
 * @author Nehon
 */
//TODO there is a lot of duplicate code here with the EnvMapUtils. We should, 
//either leverage the code from the util class either remove it and only allow 
//parallel generation using this runnable.
public class IrradianceMapGenerator extends RunnableWithProgress {

    private int targetMapSize;
    private EnvMapUtils.FixSeamsMethod fixSeamsMethod;
    private TextureCubeMap sourceMap;
    private TextureCubeMap store;
    private final Application app;

    /**
     * Creates an Irradiance map generator. The app is needed to enqueue the
     * call to the EnvironmentCamera when the generation is done, so that this
     * process is thread safe.
     *
     * @param app the Application
     * @param listener
     */
    public IrradianceMapGenerator(Application app, JobProgressListener<Integer> listener) {
        super(listener);
        this.app = app;
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
        try {
            Vector3f[] shCoeffs = EnvMapUtils.getSphericalHarmonicsCoefficents(sourceMap);
            store = generateIrradianceMap(shCoeffs, targetMapSize, fixSeamsMethod, store);
        } catch (Exception e) {
            e.printStackTrace();
        }
        app.enqueue(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                listener.done(6);
                return null;
            }
        });
    }

    /**
     * Generates the Irradiance map (used for image based difuse lighting) from
     * Spherical Harmonics coefficients previously computed with
     * {@link EnvMapUtils#getSphericalHarmonicsCoefficents(com.jme3.texture.TextureCubeMap)}
     *
     * @param shCoeffs the SH coeffs
     * @param targetMapSize the size of the irradiance map to generate
     * @param fixSeamsMethod the method to fix seams
     * @param store
     * @return The irradiance cube map for the given coefficients
     */
    public TextureCubeMap generateIrradianceMap(Vector3f[] shCoeffs, int targetMapSize, EnvMapUtils.FixSeamsMethod fixSeamsMethod, TextureCubeMap store) {
        TextureCubeMap irrCubeMap = store;

        setEnd(6 + 6);
        for (int i = 0; i < 6; i++) {
            ByteBuffer buf = BufferUtils.createByteBuffer(targetMapSize * targetMapSize * store.getImage().getFormat().getBitsPerPixel() / 8);
            irrCubeMap.getImage().setData(i, buf);
            progress();
        }

        Vector3f texelVect = new Vector3f();
        ColorRGBA color = new ColorRGBA(ColorRGBA.Black);
        float[] shDir = new float[9];
        CubeMapWrapper envMapWriter = new CubeMapWrapper(irrCubeMap);
        for (int face = 0; face < 6; face++) {

            for (int y = 0; y < targetMapSize; y++) {
                for (int x = 0; x < targetMapSize; x++) {
                    EnvMapUtils.getVectorFromCubemapFaceTexCoord(x, y, targetMapSize, face, texelVect, fixSeamsMethod);
                    EnvMapUtils.evalShBasis(texelVect, shDir);
                    color.set(0, 0, 0, 0);
                    for (int i = 0; i < EnvMapUtils.NUM_SH_COEFFICIENT; i++) {
                        color.set(color.r + shCoeffs[i].x * shDir[i] * shBandFactor[i],
                                color.g + shCoeffs[i].y * shDir[i] * shBandFactor[i],
                                color.b + shCoeffs[i].z * shDir[i] * shBandFactor[i],
                                1.0f);
                    }

                    //clamping the color because very low value close to zero produce artifacts
                    color.r = Math.max(0.0001f, color.r);
                    color.g = Math.max(0.0001f, color.g);
                    color.b = Math.max(0.0001f, color.b);

                    envMapWriter.setPixel(x, y, face, color);
                    
                }
            }
            progress();
        }
        return irrCubeMap;
    }

}
