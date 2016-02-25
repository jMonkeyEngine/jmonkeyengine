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
package com.jme3.environment;

import com.jme3.light.LightProbe;
import com.jme3.environment.generation.JobProgressListener;
import com.jme3.environment.generation.PrefilteredEnvMapFaceGenerator;
import com.jme3.environment.generation.IrradianceMapGenerator;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.app.Application;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * This Factory allows to create LightProbes within a scene given an EnvironmentCamera.
 * 
 * Since the process can be long, you can provide a JobProgressListener that 
 * will be notified of the ongoing generation process when calling the makeProbe method.
 * 
 * The process is the folowing : 
 * 1. Create an EnvironmentCamera
 * 2. give it a position in the scene
 * 3. call {@link LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Node)}
 * 4. add the created LightProbe to a node with the {@link Node#addLight(com.jme3.light.Light) } method.
 * 
 * Optionally for step 3 call {@link LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Node, com.jme3.environment.generation.JobProgressListener) }
 * with a {@link JobProgressListener} to be notified of the progress of the generation process.
 * 
 * The generation will be split in several threads for faster generation. 
 * 
 * This class is entirely thread safe and can be called from any thread. 
 * 
 * Note that in case you are using a {@link JobProgressListener} all the its 
 * method will be called inside and app.enqueu callable.
 * This means that it's completely safe to modify the scenegraph within the 
 * Listener method, but also means that the even will be delayed until next update loop.
 * 
 * @see EnvironmentCamera
 * @author bouquet
 */
public class LightProbeFactory {

    /**
     * Creates a LightProbe with the giver EnvironmentCamera in the given scene.
     * 
     * Note that this is an assynchronous process that will run on multiple threads.
     * The process is thread safe.
     * The created lightProbe will only be marked as ready when the rendering process is done.
     * 
     * If you want to monitor the process use {@link LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Node, com.jme3.environment.generation.JobProgressListener) }
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
     * Note that this is an assynchronous process that will run on multiple threads.
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
     * @param listener the listener of the genration progress.
     * @return the created LightProbe
     */
    public static LightProbe makeProbe(final EnvironmentCamera envCam, Spatial scene, final JobProgressListener<LightProbe> listener) {
        final LightProbe probe = new LightProbe();
        probe.setPosition(envCam.getPosition());
        probe.setIrradianceMap(EnvMapUtils.createIrradianceMap(envCam.getSize(), envCam.getImageFormat()));
        probe.setPrefilteredMap(EnvMapUtils.createPrefilteredEnvMap(envCam.getSize(), envCam.getImageFormat()));
        envCam.snapshot(scene, new JobProgressAdapter<TextureCubeMap>() {

            @Override
            public void done(TextureCubeMap map) {
                generatePbrMaps(map, probe, envCam.getApplication(), listener);
            }
        });
        return probe;
    }
    
     /**
     * Updates a LightProbe with the giver EnvironmentCamera in the given scene.
     * 
     * Note that this is an assynchronous process that will run on multiple threads.
     * The process is thread safe.
     * The created lightProbe will only be marked as ready when the rendering process is done.
     *      
     * The JobProgressListener will be notified of the progress of the generation. 
     * Note that you can also use a {@link JobProgressAdapter}. 
     *      
     * @see LightProbe
     * @see EnvironmentCamera
     * @see JobProgressListener
     * 
     * @param probe the Light probe to update
     * @param envCam the EnvironmentCamera
     * @param scene the Scene
     * @param listener the listener of the genration progress.
     * @return the created LightProbe
     */
    public static LightProbe updateProbe(final LightProbe probe, final EnvironmentCamera envCam, Spatial scene, final JobProgressListener<LightProbe> listener) {
        
        envCam.setPosition(probe.getPosition());
        
        probe.setReady(false);
        
        if(probe.getIrradianceMap() != null) {
            probe.getIrradianceMap().getImage().dispose();
            probe.getPrefilteredEnvMap().getImage().dispose();
        }
        
        probe.setIrradianceMap(EnvMapUtils.createIrradianceMap(envCam.getSize(), envCam.getImageFormat()));
        probe.setPrefilteredMap(EnvMapUtils.createPrefilteredEnvMap(envCam.getSize(), envCam.getImageFormat()));
        
        
        envCam.snapshot(scene, new JobProgressAdapter<TextureCubeMap>() {

            @Override
            public void done(TextureCubeMap map) {
                generatePbrMaps(map, probe, envCam.getApplication(), listener);
            }
        });
        return probe;
    }

    /**
     * Internally called to generate the maps.
     * This method will spawn 7 thread (one for the IrradianceMap, and one for each face of the prefiltered env map).
     * Those threads will be executed in a ScheduledThreadPoolExecutor that will be shutdown when the genration is done.
     * 
     * @param envMap the raw env map rendered by the env camera
     * @param probe the LigthProbe to generate maps for
     * @param app the Application
     * @param listener a progress listener. (can be null if no progress reporting is needed)
     */
    private static void generatePbrMaps(TextureCubeMap envMap, final LightProbe probe, Application app, final JobProgressListener<LightProbe> listener) {
        IrradianceMapGenerator irrMapGenerator;
        PrefilteredEnvMapFaceGenerator[] pemGenerators = new PrefilteredEnvMapFaceGenerator[6];

        final JobState jobState = new JobState(new ScheduledThreadPoolExecutor(7));

        irrMapGenerator = new IrradianceMapGenerator(app, new JobListener(listener, jobState, probe, 6));
        int size = envMap.getImage().getWidth();
        irrMapGenerator.setGenerationParam(EnvMapUtils.duplicateCubeMap(envMap), size, EnvMapUtils.FixSeamsMethod.Wrap, probe.getIrradianceMap());

        jobState.executor.execute(irrMapGenerator);

        for (int i = 0; i < pemGenerators.length; i++) {
            pemGenerators[i] = new PrefilteredEnvMapFaceGenerator(app, i, new JobListener(listener, jobState, probe, i));
            pemGenerators[i].setGenerationParam(EnvMapUtils.duplicateCubeMap(envMap), size, EnvMapUtils.FixSeamsMethod.Wrap, probe.getPrefilteredEnvMap());
            jobState.executor.execute(pemGenerators[i]);
        }
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
            for (double progres : progress) {
                mean += progres;
            }
            return mean / 7f;
        }
    }

    /**
     * An inner JobProgressListener to controll the genration process and properly clean up when it's done
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
                probe.setReady(true);
                if (globalListener != null) {
                    globalListener.done(probe);
                }
                jobState.executor.shutdownNow();
            }
        }
    }
}
