/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 */
package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.SkyFactory;

/**
 * Validation scene for the runtime PBR path. It intentionally exercises the
 * real LightProbe + PBRLighting runtime rather than a separate debug shader.
 *
 * Keys:
 * 1 white furnace
 * 2 roughness sweep
 * 3 metallic sweep
 * 4 HDR hotspot
 * 5 dielectric vs metal
 * R rebake probe
 */
public class TestPBRValidation extends SimpleApplication {
    private static final int MODE_WHITE_FURNACE = 1;
    private static final int MODE_ROUGHNESS_SWEEP = 2;
    private static final int MODE_METALLIC_SWEEP = 3;
    private static final int MODE_HDR_HOTSPOT = 4;
    private static final int MODE_DIELECTRIC_VS_METAL = 5;

    private final Node environmentNode = new Node("ValidationEnvironment");
    private final Node sampleNode = new Node("ValidationSamples");
    private EnvironmentProbeControl probe;
    private int mode = MODE_ROUGHNESS_SWEEP;
    private boolean rebakePending;

    public static void main(String[] args) {
        new TestPBRValidation().start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20f);
        cam.setLocation(new Vector3f(0f, 2.5f, 13f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);

        rootNode.attachChild(environmentNode);
        rootNode.attachChild(sampleNode);

        probe = new EnvironmentProbeControl(assetManager, 256);
        rootNode.addControl(probe);

        inputManager.addMapping("Mode1", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Mode2", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Mode3", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("Mode4", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("Mode5", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("Rebake", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(modeListener, "Mode1", "Mode2", "Mode3", "Mode4", "Mode5", "Rebake");

        rebuildScene();
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (rebakePending) {
            probe.rebake();
            rebakePending = false;
        }
    }

    private final ActionListener modeListener = (name, isPressed, tpf) -> {
        if (!isPressed) {
            return;
        }
        if ("Mode1".equals(name)) {
            mode = MODE_WHITE_FURNACE;
            rebuildScene();
        } else if ("Mode2".equals(name)) {
            mode = MODE_ROUGHNESS_SWEEP;
            rebuildScene();
        } else if ("Mode3".equals(name)) {
            mode = MODE_METALLIC_SWEEP;
            rebuildScene();
        } else if ("Mode4".equals(name)) {
            mode = MODE_HDR_HOTSPOT;
            rebuildScene();
        } else if ("Mode5".equals(name)) {
            mode = MODE_DIELECTRIC_VS_METAL;
            rebuildScene();
        } else if ("Rebake".equals(name)) {
            rebakePending = true;
        }
    };

    private void rebuildScene() {
        environmentNode.detachAllChildren();
        sampleNode.detachAllChildren();

        switch (mode) {
            case MODE_WHITE_FURNACE:
                buildWhiteFurnaceEnvironment();
                buildDielectricAndMetalRows(ColorRGBA.White, 0.0f, 1.0f, 6);
                System.out.println("Mode 1: white furnace");
                break;
            case MODE_ROUGHNESS_SWEEP:
                buildHdrSkyEnvironment();
                buildDielectricAndMetalRows(new ColorRGBA(0.95f, 0.0f, 0.0f, 1.0f), 0.0f, 1.0f, 6);
                System.out.println("Mode 2: roughness sweep");
                break;
            case MODE_METALLIC_SWEEP:
                buildHdrSkyEnvironment();
                buildMetallicSweep(new ColorRGBA(1.0f, 0.77f, 0.34f, 1.0f), 0.25f, 6);
                System.out.println("Mode 3: metallic sweep");
                break;
            case MODE_HDR_HOTSPOT:
                buildHdrHotspotEnvironment();
                buildRoughnessSweep(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f), 1.0f, -1.2f, 6);
                buildRoughnessSweep(new ColorRGBA(1.0f, 1.0f, 1.0f, 1.0f), 0.0f, 1.2f, 6);
                System.out.println("Mode 4: HDR hotspot");
                break;
            case MODE_DIELECTRIC_VS_METAL:
            default:
                buildHdrSkyEnvironment();
                buildMaterialComparison();
                System.out.println("Mode 5: dielectric vs metal");
                break;
        }

        rebakePending = true;
        System.out.println("Press 1-5 to switch validation modes, R to rebake.");
    }

    private void buildHdrSkyEnvironment() {
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        environmentNode.attachChild(sky);
        probe.tag(sky);
    }

    private void buildWhiteFurnaceEnvironment() {
        Geometry room = new Geometry("WhiteRoom", new Box(20f, 20f, 20f));
        Material roomMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        roomMat.setColor("Color", ColorRGBA.White);
        roomMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        room.setMaterial(roomMat);
        environmentNode.attachChild(room);
        probe.tag(room);
    }

    private void buildHdrHotspotEnvironment() {
        Geometry room = new Geometry("DarkRoom", new Box(20f, 20f, 20f));
        Material roomMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        roomMat.setColor("Color", new ColorRGBA(0.03f, 0.03f, 0.03f, 1.0f));
        roomMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        room.setMaterial(roomMat);
        environmentNode.attachChild(room);
        probe.tag(room);

        Geometry hotspot = new Geometry("Hotspot", new Sphere(32, 32, 0.8f));
        hotspot.setLocalTranslation(0f, 4f, -5f);
        Material hotspotMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        hotspotMat.setColor("Color", new ColorRGBA(32f, 28f, 18f, 1.0f));
        hotspot.setMaterial(hotspotMat);
        environmentNode.attachChild(hotspot);
        probe.tag(hotspot);
    }

    private void buildDielectricAndMetalRows(ColorRGBA albedo, float startY, float endY, int count) {
        buildRoughnessSweep(albedo, 0.0f, startY, count);
        buildRoughnessSweep(albedo, 1.0f, endY, count);
    }

    private void buildRoughnessSweep(ColorRGBA albedo, float metallic, float y, int count) {
        for (int i = 0; i < count; i++) {
            float roughness = count == 1 ? 0.5f : (float) i / (float) (count - 1);
            Geometry sphere = makePbrSphere("roughness-" + metallic + "-" + i, albedo, metallic, roughness);
            sphere.setLocalTranslation(-5f + i * 2f, y, 0f);
            sampleNode.attachChild(sphere);
        }
    }

    private void buildMetallicSweep(ColorRGBA albedo, float roughness, int count) {
        for (int i = 0; i < count; i++) {
            float metallic = count == 1 ? 0.5f : (float) i / (float) (count - 1);
            Geometry sphere = makePbrSphere("metallic-" + i, albedo, metallic, roughness);
            sphere.setLocalTranslation(-5f + i * 2f, 0f, 0f);
            sampleNode.attachChild(sphere);
        }
    }

    private void buildMaterialComparison() {
        Geometry dielectric = makePbrSphere("dielectric", new ColorRGBA(0.95f, 0.95f, 0.95f, 1.0f), 0.0f, 0.15f);
        dielectric.setLocalTranslation(-2.5f, 1.5f, 0f);
        sampleNode.attachChild(dielectric);

        Geometry metal = makePbrSphere("metal", new ColorRGBA(1.0f, 0.77f, 0.34f, 1.0f), 1.0f, 0.15f);
        metal.setLocalTranslation(2.5f, 1.5f, 0f);
        sampleNode.attachChild(metal);

        buildRoughnessSweep(new ColorRGBA(0.8f, 0.82f, 0.9f, 1.0f), 0.0f, -1.8f, 5);
    }

    private Geometry makePbrSphere(String name, ColorRGBA baseColor, float metallic, float roughness) {
        Geometry sphere = new Geometry(name, new Sphere(48, 48, 0.9f));
        Material material = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        material.setColor("BaseColor", baseColor);
        material.setFloat("Metallic", FastMath.clamp(metallic, 0.0f, 1.0f));
        material.setFloat("Roughness", FastMath.clamp(roughness, 0.0f, 1.0f));
        sphere.setMaterial(material);
        return sphere;
    }
}
