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

import com.jme3.environment.generation.JobProgressListener;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * A 360 camera that can capture a cube map of a scene, and then generate the
 * Prefiltered Environment cube Map and the Irradiance cube Map needed for PBR
 * indirect lighting
 * 
 * @see LightProbeFactory
 * @see LightProbe
 *
 * @author Nehon
 */
public class EnvironmentCamera extends BaseAppState {

    
    private static Vector3f[] axisX = new Vector3f[6];
    private static Vector3f[] axisY = new Vector3f[6];
    private static Vector3f[] axisZ = new Vector3f[6];
    private Image.Format imageFormat = Image.Format.RGB16F;

    //Axis for cameras
    static {
        //PositiveX axis(left, up, direction)
        axisX[0] = Vector3f.UNIT_Z.mult(1f);
        axisY[0] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[0] = Vector3f.UNIT_X.mult(1f);
        //NegativeX
        axisX[1] = Vector3f.UNIT_Z.mult(-1f);
        axisY[1] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[1] = Vector3f.UNIT_X.mult(-1f);
        //PositiveY
        axisX[2] = Vector3f.UNIT_X.mult(-1f);
        axisY[2] = Vector3f.UNIT_Z.mult(1f);
        axisZ[2] = Vector3f.UNIT_Y.mult(1f);
        //NegativeY
        axisX[3] = Vector3f.UNIT_X.mult(-1f);
        axisY[3] = Vector3f.UNIT_Z.mult(-1f);
        axisZ[3] = Vector3f.UNIT_Y.mult(-1f);
        //PositiveZ
        axisX[4] = Vector3f.UNIT_X.mult(-1f);
        axisY[4] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[4] = Vector3f.UNIT_Z;
        //NegativeZ
        axisX[5] = Vector3f.UNIT_X.mult(1f);
        axisY[5] = Vector3f.UNIT_Y.mult(-1f);
        axisZ[5] = Vector3f.UNIT_Z.mult(-1f);

    }
    private Image images[];
    ViewPort[] viewports;
    FrameBuffer[] framebuffers;
    ByteBuffer[] buffers;
    private Vector3f position = new Vector3f();
    private ColorRGBA backGroundColor = null;
    private int size = 128;

    private final List<SnapshotJob> jobs = new ArrayList<SnapshotJob>();

    /**
     * Creates an EnvironmentCamera with a size of 128
     */
    public EnvironmentCamera() {
    }

    /**
     * Creates an EnvironmentCamera with the given size.
     *
     * @param size the size of the resulting texture.
     */
    public EnvironmentCamera(int size) {
        this.size = size;
    }

    /**
     * Creates an EnvironmentCamera with the given size, and the given position
     *
     * @param size the size of the resulting texture.
     * @param position the position of the camera.
     */
    public EnvironmentCamera(int size, Vector3f position) {
        this.size = size;
        this.position.set(position);
    }

    /**
     * Creates an EnvironmentCamera with the given size, and the given position
     *
     * @param size the size of the resulting texture, and the given ImageFormat.
     * @param position the position of the camera.
     * @param imageFormat the ImageFormat to use for the resulting texture.
     */
    public EnvironmentCamera(int size, Vector3f position, Image.Format imageFormat) {
        this.size = size;
        this.position.set(position);
        this.imageFormat = imageFormat;
    }

    /**
     * Takes a snapshot of the surrounding scene.
     *
     * @param scene the scene to snapshot.
     * @param done a callback to call when the snapshot is done.
     */
    public void snapshot(final Spatial scene, final JobProgressListener<TextureCubeMap> done) {
        getApplication().enqueue(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                SnapshotJob job = new SnapshotJob(done, scene);
                jobs.add(job);
                return null;
            }
        });
    }

    @Override
    public void render(RenderManager renderManager) {
        if (!jobs.isEmpty()) {
            SnapshotJob job = jobs.get(0);

            for (int i = 0; i < 6; i++) {
                viewports[i].clearScenes();
                viewports[i].attachScene(job.scene);
                renderManager.renderViewPort(viewports[i], 0.16f);
                buffers[i] = BufferUtils.createByteBuffer(size * size * imageFormat.getBitsPerPixel() / 8);
                renderManager.getRenderer().readFrameBufferWithFormat(framebuffers[i], buffers[i], imageFormat);
                images[i] = new Image(imageFormat, size, size, buffers[i], ColorSpace.Linear);
            }

            TextureCubeMap map = EnvMapUtils.makeCubeMap(images[0], images[1], images[2], images[3], images[4], images[5], imageFormat);

            job.callback.done(map);
            map.getImage().dispose();
            jobs.remove(0);
        }
    }

    public int getSize() {
        return size;
    }

    public Vector3f getPosition() {
        return position;
    }
    
    /**
     * Sets the camera position in world space.
     *
     * @param position the position in world space
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
        if (viewports != null) {
            for (ViewPort viewPort : viewports) {
                viewPort.getCamera().setLocation(position);
            }
        }
    }

    @Override
    protected void initialize(Application app) {
        this.backGroundColor = app.getViewPort().getBackgroundColor();
        Camera[] cameras = new Camera[6];
        viewports = new ViewPort[6];
        framebuffers = new FrameBuffer[6];
        buffers = new ByteBuffer[6];
        Texture2D[] textures = new Texture2D[6];
        images = new Image[6];
        for (int i = 0; i < 6; i++) {
            cameras[i] = createOffCamera(size, position, axisX[i], axisY[i], axisZ[i]);
            viewports[i] = createOffViewPort("EnvView" + i, cameras[i]);
            framebuffers[i] = createOffScreenFrameBuffer(size, viewports[i]);
            textures[i] = new Texture2D(size, size, imageFormat);
            framebuffers[i].setColorTexture(textures[i]);
            
        }

    }

    @Override
    protected void cleanup(Application app) {
        this.backGroundColor = null;
        for (FrameBuffer frameBuffer : framebuffers) {
            frameBuffer.dispose();
            
        }
        for (Image image : images) {
            if( image != null){
                image.dispose();
            }
        }
    }

    /**
     * returns the images format used for the generated maps.
     *
     * @return
     */
    public Image.Format getImageFormat() {
        return imageFormat;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    /**
     * Creates an off camera
     *
     * @param mapSize the size
     * @param worldPos the position
     * @param axisX the x axis
     * @param axisY the y axis
     * @param axisZ tha z axis
     * @return
     */
    protected final Camera createOffCamera(int mapSize, Vector3f worldPos, Vector3f axisX, Vector3f axisY, Vector3f axisZ) {
        Camera offCamera = new Camera(mapSize, mapSize);
        offCamera.setLocation(worldPos);
        offCamera.setAxes(axisX, axisY, axisZ);
        offCamera.setFrustumPerspective(90f, 1f, 1, 1000);
        offCamera.setLocation(position);
        return offCamera;
    }

    /**
     * creates an offsceen VP
     *
     * @param name
     * @param offCamera
     * @return
     */
    protected final ViewPort createOffViewPort(String name, Camera offCamera) {
        ViewPort offView = new ViewPort(name, offCamera);
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(backGroundColor);
        
        return offView;
    }

    /**
     * create an offscreen frame buffer.
     *
     * @param mapSize
     * @param offView
     * @return
     */
    protected final FrameBuffer createOffScreenFrameBuffer(int mapSize, ViewPort offView) {
        // create offscreen framebuffer
        FrameBuffer offBuffer = new FrameBuffer(mapSize, mapSize, 1);
        offBuffer.setDepthBuffer(Image.Format.Depth);
        offView.setOutputFrameBuffer(offBuffer);
        return offBuffer;
    }

    /**
     * An inner class to keep track on a snapshot job.
     */
    private class SnapshotJob {

        JobProgressListener<TextureCubeMap> callback;
        Spatial scene;

        public SnapshotJob(JobProgressListener callback, Spatial scene) {
            this.callback = callback;
            this.scene = scene;
        }

    }

}
