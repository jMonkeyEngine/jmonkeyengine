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
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.light.LightProbe;
import com.jme3.math.Vector3f;
import com.jme3.texture.TextureCubeMap;
import java.util.concurrent.Callable;


/**
 * Generates the Irradiance map for PBR. This job can be launched from a separate
 * thread.
 * <p>
 * This is not used as we use spherical harmonics directly now, but we may need this code again at some point
 *
 * @author Nehon
 */
public class IrradianceSphericalHarmonicsGenerator extends RunnableWithProgress {

    private TextureCubeMap sourceMap;
    private LightProbe store;
    private final Application app;

    /**
     * Creates an Irradiance map generator. The app is needed to enqueue the
     * call to the EnvironmentCamera when the generation is done, so that this
     * process is thread safe.
     *
     * @param app      the Application
     * @param listener to monitor progress (alias created)
     */
    public IrradianceSphericalHarmonicsGenerator(Application app, JobProgressListener<Integer> listener) {
        super(listener);
        this.app = app;
    }

    /**
     * Fills all the generation parameters
     *
     * @param sourceMap the source cube map
     *                  {@link com.jme3.environment.util.EnvMapUtils.FixSeamsMethod}
     * @param store     The cube map to store the result in.
     */
    public void setGenerationParam(TextureCubeMap sourceMap, LightProbe store) {
        this.sourceMap = sourceMap;

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
            Vector3f[] shCoefficients = EnvMapUtils.getSphericalHarmonicsCoefficents(sourceMap);
            EnvMapUtils.prepareShCoefs(shCoefficients);
            store.setShCoeffs(shCoefficients);

        } catch (Exception e) {
            e.printStackTrace();
        }
        app.enqueue(new Callable<Void>() {

            @Override
            @SuppressWarnings("unchecked")
            public Void call() throws Exception {
                listener.done(6);
                return null;
            }
        });
    }

}
