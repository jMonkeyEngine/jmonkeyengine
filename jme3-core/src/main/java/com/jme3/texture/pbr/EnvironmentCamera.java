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
package com.jme3.texture.pbr;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureCubeMap;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A 360 camera that can capture a cube map of a scene, and then generate the 
 * Prefiltered Environment cube Map and the Irradiance cube Map needed for PBE 
 * indirect lighting
 * 
 * @author Nehon
 */
public class EnvironmentCamera extends BaseAppState {
    
    private final static Logger log = Logger.getLogger(EnvironmentCamera.class.getName());

    private static Vector3f[] axisX = new Vector3f[6];
    private static Vector3f[] axisY = new Vector3f[6];
    private static Vector3f[] axisZ = new Vector3f[6];
    private Image.Format imageFormat = Image.Format.RGB16F;
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(7);

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
    private boolean snapshotRequested = false;
    private Node scene;
    private int size = 128;
    private TextureCubeMap irradianceMap;
    private TextureCubeMap prefilteredEnvMap;
    private Runnable generationCallback;
    private TextureCubeMap map ;
    
    // Generation states
    private boolean irrMapGenerated = false;
    private boolean pemGenerated = false;
    private int faceGenerated = 0;
    private long time = 0;
    private boolean generating;
    private Node debugPfemCm;
    private Node debugIrrCm;
    private IrradianceMapGenerator irrMapGenerator;
    private final PrefilteredEnvMapGenerator[] pemGenerators = new PrefilteredEnvMapGenerator[6];

    /**
     * Creates an EnvironmentCamera with a size of 128
     */
    public EnvironmentCamera() {
    }

    /**
     * Creates an EnvironmentCamera with the given size.
     * @param size the size of the resulting texture.
     */
    public EnvironmentCamera(int size) {
        this.size = size;
    }

    /**
     * Creates an EnvironmentCamera with the given size, and the given position
     * @param size the size of the resulting texture.
     * @param position the position of the camera.
     */
    public EnvironmentCamera(int size, Vector3f position) {
        this.size = size;
        this.position = position;
    }

    /**
     * Creates an EnvironmentCamera with the given size, and the given position
     * @param size the size of the resulting texture, and the given ImageFormat.
     * @param position the position of the camera.
     * @param imageFormat the ImageFormat to use for the resulting texture.
     */
    public EnvironmentCamera(int size, Vector3f position, Image.Format imageFormat) {
        this.size = size;
        this.position = position;
        this.imageFormat = imageFormat;
    }

    /**
     * Takes a snapshot of the surrounding scene.
     * @param scene the scene to snapshot.
     * @param onDone a callback to call when the snapshot is done.
     */
    public void snapshot(Node scene, Runnable onDone) {
        snapshotRequested = true;
        this.generationCallback = onDone;
        this.scene = scene;
        if (viewports != null) {
            for (ViewPort viewPort : viewports) {
                viewPort.clearScenes();
                viewPort.attachScene(scene);
            }
        }
    }
    
    /**
     * Takes a snapshot of the surrounding scene.
     * @param scene the scene to snapshot.
     */
    public void snapshot(Node scene) {
        snapshot(scene, null);
    }
    
    @Override
    public void render(RenderManager renderManager) {
        if (snapshotRequested) {
            time = System.currentTimeMillis();
            snapshotRequested = false;
            for (int i = 0; i < 6; i++) {
                renderManager.renderViewPort(viewports[i], 0.16f);
                renderManager.getRenderer().readFrameBufferWithFormat(framebuffers[i], buffers[i], imageFormat);
                //renderManager.getRenderer().readFrameBuffer(framebuffers[i], buffers[i]);
                images[i] = new Image(imageFormat, size, size, buffers[i], ColorSpace.Linear);
            }

             map = EnvMapUtils.makeCubeMap(images[0], images[1], images[2], images[3], images[4], images[5], imageFormat);

            
            irrMapGenerator = new IrradianceMapGenerator(getApplication());
            irrMapGenerator.setGenerationParam(EnvMapUtils.duplicateCubeMap(map), size, EnvMapUtils.FixSeamsMethod.Wrap, irradianceMap);
            generating = true;           
            executor.execute(irrMapGenerator);
            
            int nbMipMap = (int) (Math.log(size) / Math.log(2) - 1);
            CubeMapWrapper targetWrapper = new CubeMapWrapper(prefilteredEnvMap);
            targetWrapper.initMipMaps(nbMipMap);

            for (int i = 0; i < pemGenerators.length; i++) {
                pemGenerators[i] = new PrefilteredEnvMapGenerator(getApplication(), i);
                pemGenerators[i].setGenerationParam(EnvMapUtils.duplicateCubeMap(map), size, EnvMapUtils.FixSeamsMethod.Wrap, prefilteredEnvMap);
                executor.execute(pemGenerators[i]);
            }

        }
    }

    /**
     * Called when the irradiance map is done being generated
     * @param irrMap 
     */
    protected void doneIrradianceMap(TextureCubeMap irrMap) {
        irradianceMap = irrMap;
        irrMapGenerated = true;
    }

    /**
     * Called when the PEm was generated for the given face
     * @param face the face of the cube map
     */
    protected void donePemForFace(int face) {
        faceGenerated++;
        if (faceGenerated == 6) {
            pemGenerated = true;
        }
    }  

    //TODO add a way to plug in a progress reporter that would be external
    @Override
    public void update(float tpf) {
        if (generating) {
            double progress = 0;
            progress += irrMapGenerator.getProgress();
            for (PrefilteredEnvMapGenerator pemGenerator : pemGenerators) {
                progress += pemGenerator.getProgress();
            }
            progress /= 7;
            log.log(Level.INFO, "progress : {0}%", progress * 100);
        }
        if (pemGenerated && irrMapGenerated && generating) {
            generating = false;
            long time2 = System.currentTimeMillis();
            log.log(Level.INFO, "generated in {0} ms", (time2 - time));
            if(generationCallback != null){
                generationCallback.run();
            }
        }
    }
    
    /**
     * Displays or cycles through the generated maps.
     */
    public void toggleDebug() {
        if (debugPfemCm == null) {
            debugPfemCm = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(prefilteredEnvMap, getApplication().getAssetManager());
            debugPfemCm.setLocalTranslation(getApplication().getGuiViewPort().getCamera().getWidth() - 532, 20, 0);
        }
        if (debugIrrCm == null) {
            debugIrrCm = EnvMapUtils.getCubeMapCrossDebugView(irradianceMap, getApplication().getAssetManager());
            debugIrrCm.setLocalTranslation(getApplication().getGuiViewPort().getCamera().getWidth() - 532, 20, 0);
        }

        if (debugIrrCm.getParent() != null) {
            debugIrrCm.removeFromParent();
            ((Node) (getApplication().getGuiViewPort().getScenes().get(0))).attachChild(debugPfemCm);

        } else if (debugPfemCm.getParent() != null) {
            debugPfemCm.removeFromParent();
        } else {
            ((Node) (getApplication().getGuiViewPort().getScenes().get(0))).attachChild(debugIrrCm);
        }

    }

    /**
     * Sets the camera position.
     * @param position 
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
        if (viewports != null) {
            for (ViewPort viewPort : viewports) {
                viewPort.getCamera().setLocation(position);
            }
        }
    }

    /**
     * initialize the Irradiancemap
     */
    private void initIrradianceMap() {

        irradianceMap = new TextureCubeMap(size, size, imageFormat);
        irradianceMap.setMagFilter(Texture.MagFilter.Bilinear);
        irradianceMap.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        irradianceMap.getImage().setColorSpace(ColorSpace.Linear);

    }

    /**
     * initialize the pem map
     */
    private void initPrefilteredEnvMap() {

        prefilteredEnvMap = new TextureCubeMap(size, size, imageFormat);
        prefilteredEnvMap.setMagFilter(Texture.MagFilter.Bilinear);
        prefilteredEnvMap.setMinFilter(Texture.MinFilter.Trilinear);
        prefilteredEnvMap.getImage().setColorSpace(ColorSpace.Linear);

    }

    /**
     * returns the irradiance map
     * @return 
     */
    public TextureCubeMap getIrradianceMap() {
        return irradianceMap;
    }
    
    /**
     * returns the pem map
     * @return 
     */
    public TextureCubeMap getPrefilteredEnvMap() {
        return prefilteredEnvMap;
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
            buffers[i] = BufferUtils.createByteBuffer(size * size * imageFormat.getBitsPerPixel() / 8);
        }
        initIrradianceMap();
        initPrefilteredEnvMap();
    }

    @Override
    protected void cleanup(Application app) {
        this.backGroundColor = null;
        for (FrameBuffer frameBuffer : framebuffers) {
            app.getRenderManager().getRenderer().deleteFrameBuffer(frameBuffer);
        }
        for (Image image : images) {
            app.getRenderManager().getRenderer().deleteImage(image);
        }

        executor.shutdownNow();
    }

    /**
     * returns the images format used for the generated maps.
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
     * @param name
     * @param offCamera
     * @return 
     */
    protected final ViewPort createOffViewPort(String name, Camera offCamera) {
        ViewPort offView = new ViewPort(name, offCamera);
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(backGroundColor);
        if (scene != null) {
            offView.attachScene(scene);
        }
        return offView;
    }

    /**
     * create an offscreen frame buffer.
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
}
