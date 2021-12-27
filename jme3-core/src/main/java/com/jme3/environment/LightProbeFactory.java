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
package com.jme3.environment;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.environment.generation.*;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.light.LightProbe;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Creates LightProbes within a scene, given an EnvironmentCamera.
 * 
 * Since this process can take a long time, you can provide a JobProgressListener that
 * will be notified of the ongoing generation process when calling the makeProbe method.
 * 
 * The process is as follows: 
 * 1. Create an EnvironmentCamera
 * 2. give it a position in the scene
 * 3. call {@link LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Spatial)}
 * 4. add the created LightProbe to a node with the {@link Node#addLight(com.jme3.light.Light) } method.
 * 
 * Optionally for step 3 call
 * {@link #makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Spatial, com.jme3.environment.generation.JobProgressListener)}
 * with a {@link JobProgressListener} to be notified of the progress of the generation process.
 * 
 * The generation will be split in several threads for faster generation. 
 * 
 * This class is entirely thread safe and can be called from any thread. 
 * 
 * Note that in case you are using a {@link JobProgressListener}, all its
 * methods will be called inside an app.enqueue callable.
 * This means that it's completely safe to modify the scenegraph within the 
 * Listener method, but also means that the event will be delayed until next update loop.
 * 
 * @see EnvironmentCamera
 * @author bouquet
 */
public class LightProbeFactory {

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private LightProbeFactory() {
    }

    /**
     * Creates a LightProbe with the giver EnvironmentCamera in the given scene.
     * 
     * Note that this is an asynchronous process that will run on multiple threads.
     * The process is thread safe.
     * The created lightProbe will only be marked as ready when the rendering process is done.
     * 
     * If you want to monitor the process use
     * {@link #makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Spatial, com.jme3.environment.generation.JobProgressListener)}
     * 
     *
     * 
     * @see LightProbe
     * @see EnvironmentCamera
     * @param envCam the EnvironmentCamera
     * @param scene the Scene
     * @return the created LightProbe
     */
    public static LightProbe makeProbe(final EnvironmentCamera envCam, Spatial scene) {
        return makeProbe(envCam, scene, null);
    }

    /**
     * Creates a LightProbe with the giver EnvironmentCamera in the given scene.
     * 
     * Note that this is an asynchronous process that will run on multiple threads.
     * The process is thread safe.
     * The created lightProbe will only be marked as ready when the rendering process is done.
     *      
     * The JobProgressListener will be notified of the progress of the generation. 
     * Note that you can also use a {@link JobProgressAdapter}. 
     * 
     * @see LightProbe
     * @see EnvironmentCamera
     * @see JobProgressListener
     
     * @param envCam the EnvironmentCamera
     * @param scene the Scene
     * @param genType Fast or HighQuality. Fast may be ok for many types of environment, but you may need high quality when an environment map has very high lighting values.
     * @param listener the listener of the generation progress.
     * @return the created LightProbe
     */
    public static LightProbe makeProbe(final EnvironmentCamera envCam, Spatial scene, final EnvMapUtils.GenerationType genType, final JobProgressListener<LightProbe> listener) {
        final LightProbe probe = new LightProbe();
        probe.setPosition(envCam.getPosition());
        probe.setPrefilteredMap(EnvMapUtils.createPrefilteredEnvMap(envCam.getSize(), envCam.getImageFormat()));
        envCam.snapshot(scene, new JobProgressAdapter<TextureCubeMap>() {

            @Override
            public void done(TextureCubeMap map) {
                generatePbrMaps(map, probe, envCam.getApplication(), genType, listener);
            }
        });
        return probe;
    }

    public static LightProbe makeProbe(final EnvironmentCamera envCam, Spatial scene, final JobProgressListener<LightProbe> listener) {
        return makeProbe(envCam, scene, EnvMapUtils.GenerationType.Fast, listener);
    }

    /**
     * Updates a LightProbe with the given EnvironmentCamera in the given scene.
     * <p>
     * Note that this is an asynchronous process that will run on multiple threads.
     * The process is thread safe.
     * The created lightProbe will only be marked as ready when the rendering process is done.
     * <p>
     * The JobProgressListener will be notified of the progress of the generation.
     * Note that you can also use a {@link JobProgressAdapter}.
     *
     * @param probe    the Light probe to update
     * @param envCam   the EnvironmentCamera
     * @param scene    the Scene
     * @param genType  Fast or HighQuality. Fast may be ok for many types of environment, but you may need high quality when an environment map has very high lighting values.
     * @param listener the listener of the generation progress.
     * @return the created LightProbe
     * @see LightProbe
     * @see EnvironmentCamera
     * @see JobProgressListener
     */
    public static LightProbe updateProbe(final LightProbe probe, final EnvironmentCamera envCam, Spatial scene, final EnvMapUtils.GenerationType genType, final JobProgressListener<LightProbe> listener) {
        
        envCam.setPosition(probe.getPosition());
        
        probe.setReady(false);

        if (probe.getPrefilteredEnvMap() != null) {
            probe.getPrefilteredEnvMap().getImage().dispose();
        }

        probe.setPrefilteredMap(EnvMapUtils.createPrefilteredEnvMap(envCam.getSize(), envCam.getImageFormat()));

        envCam.snapshot(scene, new JobProgressAdapter<TextureCubeMap>() {

            @Override
            public void done(TextureCubeMap map) {
                generatePbrMaps(map, probe, envCam.getApplication(), genType, listener);
            }
        });
        return probe;
    }

    public static LightProbe updateProbe(final LightProbe probe, final EnvironmentCamera envCam, Spatial scene, final JobProgressListener<LightProbe> listener) {
        return updateProbe(probe, envCam, scene, EnvMapUtils.GenerationType.Fast, listener);
    }

    /**
     * Internally called to generate the maps.
     * This method will spawn 7 thread (one for the Irradiance spherical harmonics generator, and one for each face of the prefiltered env map).
     * Those threads will be executed in a ScheduledThreadPoolExecutor that will be shutdown when the generation is done.
     *
     * @param envMap the raw env map rendered by the env camera
     * @param probe the LightProbe to generate maps for
     * @param app the Application
     * @param listener a progress listener. (can be null if no progress reporting is needed)
     */
    private static void generatePbrMaps(TextureCubeMap envMap, final LightProbe probe, Application app, EnvMapUtils.GenerationType genType, final JobProgressListener<LightProbe> listener) {
        IrradianceSphericalHarmonicsGenerator irrShGenerator;
        PrefilteredEnvMapFaceGenerator[] pemGenerators = new PrefilteredEnvMapFaceGenerator[6];

        final JobState jobState = new JobState(new ScheduledThreadPoolExecutor(7));

        irrShGenerator = new IrradianceSphericalHarmonicsGenerator(app, new JobListener(listener, jobState, probe, 6));
        int size = envMap.getImage().getWidth();
        irrShGenerator.setGenerationParam(EnvMapUtils.duplicateCubeMap(envMap), probe);

        jobState.executor.execute(irrShGenerator);

        for (int i = 0; i < pemGenerators.length; i++) {
            pemGenerators[i] = new PrefilteredEnvMapFaceGenerator(app, i, new JobListener(listener, jobState, probe, i));
            pemGenerators[i].setGenerationParam(EnvMapUtils.duplicateCubeMap(envMap), size, EnvMapUtils.FixSeamsMethod.None, genType, probe.getPrefilteredEnvMap());
            jobState.executor.execute(pemGenerators[i]);
        }
    }

    /**
     * For debugging purposes only.
     * Will return a Node meant to be added to a GUI presenting the 2 cube maps in a cross pattern with all the mip maps.
     *
     * @param manager the asset manager
     * @param probe the LightProbe to be debugged (not null)
     * @return a debug node
     */
    public static Node getDebugGui(AssetManager manager, LightProbe probe) {
        if (!probe.isReady()) {
            throw new IllegalStateException(
                    "The LightProbe is not ready yet, please test isReady().");
        }

        Node debugNode = new Node("debug gui probe");
        Node debugPfemCm = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(probe.getPrefilteredEnvMap(), manager);
        debugNode.attachChild(debugPfemCm);
        debugPfemCm.setLocalTranslation(520, 0, 0);

        return debugNode;
    }

    /**
     * An inner class to keep the state of a generation process
     */
    private static class JobState {

        double progress[] = new double[7];
        boolean done[] = new boolean[7];
        ScheduledThreadPoolExecutor executor;
        boolean started = false;

        public JobState(ScheduledThreadPoolExecutor executor) {
            this.executor = executor;
        }

        boolean isDone() {
            for (boolean d : done) {
                if (d == false) {
                    return false;
                }
            }
            return true;
        }

        float getProgress() {
            float mean = 0;
            for (double faceProgress : progress) {
                mean += faceProgress;
            }
            return mean / 7f;
        }
    }

    /**
     * An inner JobProgressListener to control the generation process and properly clean up when it's done
     */
    private static class JobListener extends JobProgressAdapter<Integer> {

        JobProgressListener<LightProbe> globalListener;
        JobState jobState;
        LightProbe probe;

        int index;

        public JobListener(JobProgressListener<LightProbe> globalListener, JobState jobState, LightProbe probe, int index) {
            this.globalListener = globalListener;
            this.jobState = jobState;
            this.probe = probe;
            this.index = index;
        }

        @Override
        public void start() {
            if (globalListener != null && !jobState.started) {
                jobState.started = true;
                globalListener.start();
            }
        }

        @Override
        public void progress(double value) {
            jobState.progress[index] = value;
            if (globalListener != null) {
                globalListener.progress(jobState.getProgress());
            }
        }

        @Override
        public void done(Integer result) {
            if (globalListener != null) {
                if (result < 6) {
                    globalListener.step("Prefiltered env map face " + result + " generated");
                } else {
                    globalListener.step("Irradiance map generated");

                }
            }

            jobState.done[index] = true;
            if (jobState.isDone()) {
                probe.setNbMipMaps(probe.getPrefilteredEnvMap().getImage().getMipMapSizes().length);
                probe.setReady(true);
                if (globalListener != null) {
                    globalListener.done(probe);
                }
                jobState.executor.shutdownNow();
            }
        }
    }
}
