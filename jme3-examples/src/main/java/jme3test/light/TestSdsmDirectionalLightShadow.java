/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package jme3test.light;


import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.ZipLocator;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.SdsmDirectionalLightShadowFilter;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;

import java.io.File;


/**
 * Test application for SDSM (Sample Distribution Shadow Mapping).
 */
public class TestSdsmDirectionalLightShadow extends SimpleApplication implements ActionListener {

    private static final int[] SHADOW_MAP_SIZES = {256, 512, 1024, 2048, 4096};
    private int shadowMapSizeIndex = 2;  // Start at 1024
    private int numSplits = 2;

    private DirectionalLight sun;
    private FilterPostProcessor fpp;

    private Filter activeFilter;
    private SdsmDirectionalLightShadowFilter sdsmFilter;
    private DirectionalLightShadowFilter traditionalFilter;

    private boolean useSdsm = true;

    // Light direction parameters (in radians)
    private float lightElevation = 1.32f;
    private float lightAzimuth = FastMath.QUARTER_PI;

    private BitmapText statusText;

    public static void main(String[] args) {
        TestSdsmDirectionalLightShadow app = new TestSdsmDirectionalLightShadow();
        AppSettings settings = new AppSettings(true);
        settings.setGraphicsDebug(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        setupCamera();
        buildScene();
        setupLighting();
        setupShadows();
        setupUI();
        setupInputs();
    }

    private void setupCamera() {
        // Start at origin looking along +X
        cam.setLocation(new Vector3f(0, 5f, 0));
        flyCam.setMoveSpeed(20);
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);
        //Note that for any specific scene, the actual frustum sizing matters a lot for non-SDSM results.
        //Sometimes values that make the frustums match the usual scene depths will result in pretty good splits
        //without SDSM! But then, the creator has to tune for that specific scene.
        // If they just use a general frustum, results will be awful.
        // Most users will probably not even know about this and want a frustum that shows things really far away and things closer than 1 meter to the camera.
        //So what's fair to show off, really?
        //(And if a user looks really closely at a shadow on a wall or something, SDSM is always going to win.)
        cam.setFrustumPerspective(60f, cam.getAspect(), 0.01f, 500f);
    }

    private void buildScene() {
        // Add reference objects at origin for orientation
        addReferenceObjects();

        // Load Sponza scene from zip - need to extract to temp file since ZipLocator needs filesystem path
        File f = new File("jme3-examples/sponza.zip");
        if(!f.exists()){
            System.out.println("Sponza demo not found. Note that SDSM is most effective with interior environments.");
        } else {
            assetManager.registerLocator(f.getAbsolutePath(), ZipLocator.class);
            Spatial sponza = assetManager.loadModel("NewSponza_Main_glTF_003.gltf");
            sponza.setShadowMode(ShadowMode.CastAndReceive);
            sponza.getLocalLightList().clear();

            rootNode.attachChild(sponza);

            // Light probe for PBR materials
            LightProbe probe = (LightProbe) assetManager.loadAsset("lightprobe.j3o");
            probe.getArea().setRadius(Float.POSITIVE_INFINITY);
            probe.setPosition(new Vector3f(0f,0f,0f));
            rootNode.addLight(probe);
        }
    }

    private void addReferenceObjects() {
        Material red = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        red.setBoolean("UseMaterialColors", true);
        red.setColor("Diffuse", ColorRGBA.Red);
        red.setColor("Ambient", ColorRGBA.Red.mult(0.3f));

        Material green = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        green.setBoolean("UseMaterialColors", true);
        green.setColor("Diffuse", ColorRGBA.Green);
        green.setColor("Ambient", ColorRGBA.Green.mult(0.3f));

        Material blue = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        blue.setBoolean("UseMaterialColors", true);
        blue.setColor("Diffuse", ColorRGBA.Blue);
        blue.setColor("Ambient", ColorRGBA.Blue.mult(0.3f));

        Material white = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        white.setBoolean("UseMaterialColors", true);
        white.setColor("Diffuse", ColorRGBA.White);
        white.setColor("Ambient", ColorRGBA.White.mult(0.3f));


        Material brown = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        brown.setBoolean("UseMaterialColors", true);
        brown.setColor("Diffuse", ColorRGBA.Brown);
        brown.setColor("Ambient", ColorRGBA.Brown.mult(0.3f));

        // Origin sphere (white)
        Geometry origin = new Geometry("Origin", new Sphere(16, 16, 1f));
        origin.setMaterial(white);
        origin.setLocalTranslation(0, 0, 0);
        origin.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(origin);

        // X axis marker (red) at +10
        Geometry xMarker = new Geometry("X+", new Box(1f, 1f, 1f));
        xMarker.setMaterial(red);
        xMarker.setLocalTranslation(10, 0, 0);
        xMarker.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(xMarker);

        // Y axis marker (green) at +10
        Geometry yMarker = new Geometry("Y+", new Box(1f, 1f, 1f));
        yMarker.setMaterial(green);
        yMarker.setLocalTranslation(0, 10, 0);
        yMarker.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(yMarker);

        // Z axis marker (blue) at +10
        Geometry zMarker = new Geometry("Z+", new Box(1f, 1f, 1f));
        zMarker.setMaterial(blue);
        zMarker.setLocalTranslation(0, 0, 10);
        zMarker.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(zMarker);

        // Ground plane
        Geometry ground = new Geometry("Ground", new Box(50f, 0.1f, 50f));
        ground.setMaterial(brown);
        ground.setLocalTranslation(0, -1f, 0);
        ground.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(ground);
    }

    private void setupLighting() {
        sun = new DirectionalLight();
        updateLightDirection();
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun);

        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
        rootNode.addLight(ambient);

        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
    }

    /**
     * Updates the light direction based on elevation and azimuth angles.
     * Elevation: 0 = horizon, PI/2 = straight down (noon)
     * Azimuth: rotation around the Y axis
     */
    private void updateLightDirection() {
        // Compute direction from spherical coordinates
        // The light points DOWN toward the scene, so we negate Y
        float cosElev = FastMath.cos(lightElevation);
        float sinElev = FastMath.sin(lightElevation);
        float cosAz = FastMath.cos(lightAzimuth);
        float sinAz = FastMath.sin(lightAzimuth);

        // Direction vector (pointing from sun toward scene)
        Vector3f dir = new Vector3f(
                cosElev * sinAz,   // X component
                -sinElev,          // Y component (negative = pointing down)
                cosElev * cosAz    // Z component
        );
        sun.setDirection(dir.normalizeLocal());
        if(sdsmFilter != null) { sdsmFilter.setLight(sun); }
        if(traditionalFilter != null) { traditionalFilter.setLight(sun); }
    }

    private void setupShadows() {
        fpp = new FilterPostProcessor(assetManager);

        setActiveFilter(useSdsm);

        viewPort.addProcessor(fpp);
    }

    private void setActiveFilter(boolean isSdsm){
        if(activeFilter != null){ fpp.removeFilter(activeFilter); }
        int shadowMapSize = SHADOW_MAP_SIZES[shadowMapSizeIndex];
        if(isSdsm){
            // SDSM shadow filter (requires OpenGL 4.3)
            sdsmFilter = new SdsmDirectionalLightShadowFilter(assetManager, shadowMapSize, numSplits);
            sdsmFilter.setLight(sun);
            sdsmFilter.setShadowIntensity(0.7f);
            sdsmFilter.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
            activeFilter = sdsmFilter;
            traditionalFilter = null;
        } else {
            // Traditional shadow filter for comparison
            traditionalFilter = new DirectionalLightShadowFilter(assetManager, shadowMapSize, numSplits);
            traditionalFilter.setLight(sun);
            traditionalFilter.setLambda(0.55f);
            traditionalFilter.setShadowIntensity(0.7f);
            traditionalFilter.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
            this.activeFilter = traditionalFilter;
            sdsmFilter = null;
        }
        fpp.addFilter(activeFilter);
    }

    private void setupUI() {
        statusText = new BitmapText(guiFont);
        statusText.setSize(guiFont.getCharSet().getRenderedSize() * 0.8f);
        statusText.setLocalTranslation(10, cam.getHeight() - 10, 0);
        guiNode.attachChild(statusText);
        updateStatusText();
    }

    private void updateStatusText() {
        StringBuilder sb = new StringBuilder();
        sb.append("SDSM Shadow Test (Requires OpenGL 4.3)\n");
        sb.append("---------------------------------------\n");

        if (useSdsm) {
            sb.append("Mode: SDSM (Sample Distribution Shadow Mapping)\n");
        } else {
            sb.append("Mode: Traditional (Lambda-based splits)\n");
        }

        sb.append(String.format("Shadow Map Size: %d  |  Splits: %d\n",
                SHADOW_MAP_SIZES[shadowMapSizeIndex], numSplits));
        sb.append(String.format("Light: Elevation %.0f deg  |  Azimuth %.0f deg\n",
                lightElevation * FastMath.RAD_TO_DEG, lightAzimuth * FastMath.RAD_TO_DEG));

        sb.append("\n");
        sb.append("Controls:\n");
        sb.append("  T - Toggle SDSM / Traditional\n");
        sb.append("  1-4 - Set number of splits\n");
        sb.append("  -/+ - Change shadow map size\n");
        sb.append("  Numpad 8/5 - Light elevation\n");
        sb.append("  Numpad 4/6 - Light rotation\n");
        sb.append("  X - Show shadow frustum debug\n");
        sb.append("  C - Restart display\n");

        statusText.setText(sb.toString());
    }

    private void setupInputs() {
        inputManager.addMapping("toggleMode", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("splits1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("splits2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("splits3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("splits4", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("sizeUp", new KeyTrigger(KeyInput.KEY_EQUALS));
        inputManager.addMapping("sizeDown", new KeyTrigger(KeyInput.KEY_MINUS));
        inputManager.addMapping("debug", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("restartDisplay", new KeyTrigger(KeyInput.KEY_C));

        inputManager.addMapping("elevUp", new KeyTrigger(KeyInput.KEY_NUMPAD8));
        inputManager.addMapping("elevDown", new KeyTrigger(KeyInput.KEY_NUMPAD5));
        inputManager.addMapping("azimLeft", new KeyTrigger(KeyInput.KEY_NUMPAD4));
        inputManager.addMapping("azimRight", new KeyTrigger(KeyInput.KEY_NUMPAD6));

        inputManager.addListener(this,
                "toggleMode", "splits1", "splits2", "splits3", "splits4",
                "sizeUp", "sizeDown", "debug", "restartDisplay",
                "elevUp", "elevDown", "azimLeft", "azimRight");
    }

    private boolean elevUp, elevDown, azimLeft, azimRight;

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        // Track light direction key states
        switch (name) {
            case "elevUp": elevUp = isPressed; return;
            case "elevDown": elevDown = isPressed; return;
            case "azimLeft": azimLeft = isPressed; return;
            case "azimRight": azimRight = isPressed; return;
            default: break;
        }

        // Other keys only on press
        if (!isPressed) {
            return;
        }

        switch (name) {
            case "toggleMode":
                useSdsm = !useSdsm;
                setActiveFilter(useSdsm);
                updateStatusText();
                break;

            case "splits1":
            case "splits2":
            case "splits3":
            case "splits4":
                int newSplits = Integer.parseInt(name.substring(6));
                if (newSplits != numSplits) {
                    numSplits = newSplits;
                    setActiveFilter(useSdsm);
                    updateStatusText();
                }
                break;

            case "sizeUp":
                if (shadowMapSizeIndex < SHADOW_MAP_SIZES.length - 1) {
                    shadowMapSizeIndex++;
                    setActiveFilter(useSdsm);
                    updateStatusText();
                }
                break;

            case "sizeDown":
                if (shadowMapSizeIndex > 0) {
                    shadowMapSizeIndex--;
                    setActiveFilter(useSdsm);
                    updateStatusText();
                }
                break;

            case "debug":
                if (useSdsm) {
                    sdsmFilter.displayAllFrustums();
                } else {
                    traditionalFilter.displayFrustum();
                }
                break;
            case "restartDisplay":
                (context).restart();
            default:
                break;
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        boolean changed = false;

        // Adjust elevation (clamped between 5 degrees and 90 degrees)
        if (elevUp) {
            lightElevation = Math.min(FastMath.PI, lightElevation + tpf);
            changed = true;
        }
        if (elevDown) {
            lightElevation = Math.max(0f, lightElevation - tpf);
            changed = true;
        }

        // Adjust azimuth (wraps around)
        if (azimLeft) {
            lightAzimuth -= tpf;
            changed = true;
        }
        if (azimRight) {
            lightAzimuth += tpf;
            changed = true;
        }

        if (changed) {
            updateLightDirection();
            updateStatusText();
        }
    }
}