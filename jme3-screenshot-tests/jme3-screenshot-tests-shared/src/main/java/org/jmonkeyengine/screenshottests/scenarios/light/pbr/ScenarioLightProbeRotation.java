/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.scenarios.light.pbr;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.light.LightProbe;
import com.jme3.light.OrientedBoxProbeArea;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.SkyFactory;
import org.jmonkeyengine.screenshottests.testframework.Scenario;
import org.jmonkeyengine.screenshottests.testframework.ScreenshotTest;

/**
 * Light probes that are baked and then rotated (see {@link LightProbe#setRotation(Quaternion)}).
 * <p>
 * A row of spheres, from mirror-like to fully rough, is lit only by a probe baked from the sky and a big red cube off to
 * one side, which makes the rotation obvious in the reflections. The equivalence tests bake the probe, then turn the
 * whole world (sky, cube, spheres and probe) and the camera together by the same rotation.
 * That must look exactly like never having turned anything, so each is a multi-scenario test whose scenarios must
 * render identically. If the probe's rotation were ignored the reflections would stay put while everything else
 * turned, and the scenarios would differ. The re-bake scenario captures the probe again after it has been turned,
 * which must give the same result (the capture is taken in the probe's rotated space, not the world's).
 */
public class ScenarioLightProbeRotation {

    private static final int BAKE_SIZE = 256;

    /**
     * The world is turned on this frame, after the probe has been baked (it bakes on the first frame rendered).
     */
    private static final int ROTATE_ON_FRAME = 3;

    private static final int REBAKE_ON_FRAME = 4;

    private static final int SCREENSHOT_FRAME = 6;

    /**
     * Re-baking a turned probe samples the sky through turned capture cameras, landing on texels at a slightly
     * different alignment, so the re-bake scenario differs from the others by a little noise (about 0.05% of pixels on
     * the Android emulator). A real rotation error would move the reflections by tens of degrees, far beyond this.
     */
    private static final float ALLOWED_DIFFERENT_PIXEL_RATIO = 0.001f;

    private static final Quaternion WORLD_ROTATION = new Quaternion().fromAngleAxis(50 * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y);

    private static final Quaternion BOX_ROTATION_AT_SETUP = new Quaternion().fromAngleAxis(30 * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y);

    private enum RotationMethod {
        /**
         * Everything is screenshotted as it is initially set up.
         */
        NONE,
        /**
         * Rotates the probe directly (using its setRotation method), the world and the camera
         * (which if all work should have no visible effect vs "none")
         */
        SET_ROTATION,
        /**
         * Attaches a LightControl to the light probe then rotates the camera and world (which leads to the probe being
         * rotated via the control) - (which if all work should have no visible effect vs "none")
         */
        LIGHT_CONTROL,
        /**
         * Attaches a LightControl to the light probe then rotates the camera and world (which leads to the probe being
         * rotated via the control) additionally rebaking the scene after the rotation - (which if all work should have no visible effect vs "none")
         */
        LIGHT_CONTROL_THEN_REBAKE
    }

    /**
     * A spherical probe (as baked by default) turned after baking.
     */
    public static ScreenshotTest testSphericalProbeRotation() {
        return new ScreenshotTest(
                new Scenario("Unrotated", probeScene(false, RotationMethod.NONE)),
                new Scenario("BakedThenRotatedWithSetRotation", probeScene(false, RotationMethod.SET_ROTATION)),
                new Scenario("BakedThenRotatedWithLightControl", probeScene(false, RotationMethod.LIGHT_CONTROL)),
                new Scenario("RotatedThenRebaked", probeScene(false, RotationMethod.LIGHT_CONTROL_THEN_REBAKE))
        ).setFramesToTakeScreenshotsOn(SCREENSHOT_FRAME)
                .setAllowedDifferentPixelRatio(ALLOWED_DIFFERENT_PIXEL_RATIO);
    }

    /**
     * An oriented box probe whose box was already rotated at setup (like a room not built on the world axes), turned
     * after baking. The box must keep its setup rotation and be turned on top of it, or the parallax correction would
     * no longer line up and the scenarios would differ.
     */
    public static ScreenshotTest testOrientedBoxProbeRotation() {
        return new ScreenshotTest(
                new Scenario("Unrotated", probeScene(true, RotationMethod.NONE)),
                new Scenario("BakedThenRotatedWithSetRotation", probeScene(true, RotationMethod.SET_ROTATION)),
                new Scenario("BakedThenRotatedWithLightControl", probeScene(true, RotationMethod.LIGHT_CONTROL)),
                new Scenario("RotatedThenRebaked", probeScene(true, RotationMethod.LIGHT_CONTROL_THEN_REBAKE))
        ).setFramesToTakeScreenshotsOn(SCREENSHOT_FRAME)
                .setAllowedDifferentPixelRatio(ALLOWED_DIFFERENT_PIXEL_RATIO);
    }

    /**
     * Only the probe is turned, so its lighting and reflections no longer match the visible sky. This shows what the
     * rotation does on its own.
     */
    public static ScreenshotTest testProbeRotatedAgainstSky() {
        return new ScreenshotTest(new BaseAppState() {
            private int frame = 0;
            private EnvironmentProbeControl probe;

            @Override
            protected void initialize(Application app) {
                probe = buildScene((SimpleApplication) app, new Node("world"), false);
            }

            @Override
            public void update(float tpf) {
                frame++;
                if (frame == ROTATE_ON_FRAME) {
                    probe.setRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
                }
            }

            @Override protected void cleanup(Application app) {}
            @Override protected void onEnable() {}
            @Override protected void onDisable() {}
        }).setFramesToTakeScreenshotsOn(SCREENSHOT_FRAME);
    }

    private static BaseAppState probeScene(boolean orientedBox, RotationMethod rotationMethod) {
        return new BaseAppState() {
            private int frame = 0;
            private final Node world = new Node("world");
            private EnvironmentProbeControl probe;

            @Override
            protected void initialize(Application app) {
                probe = buildScene((SimpleApplication) app, world, orientedBox);
                if (rotationMethod == RotationMethod.LIGHT_CONTROL || rotationMethod == RotationMethod.LIGHT_CONTROL_THEN_REBAKE) {
                    Node probeNode = new Node("probeNode");
                    probeNode.addControl(new LightControl(probe));
                    world.attachChild(probeNode);
                }
            }

            @Override
            public void update(float tpf) {
                frame++;
                if (frame == ROTATE_ON_FRAME && rotationMethod != RotationMethod.NONE) {
                    world.setLocalRotation(WORLD_ROTATION);
                    if (rotationMethod == RotationMethod.SET_ROTATION) {
                        probe.setRotation(WORLD_ROTATION);
                    }
                    Camera cam = getApplication().getCamera();
                    cam.setLocation(WORLD_ROTATION.mult(cam.getLocation()));
                    cam.setRotation(WORLD_ROTATION.mult(cam.getRotation()));
                }
                if (frame == REBAKE_ON_FRAME && rotationMethod == RotationMethod.LIGHT_CONTROL_THEN_REBAKE) {
                    probe.rebake();
                }
            }

            @Override protected void cleanup(Application app) {}
            @Override protected void onEnable() {}
            @Override protected void onDisable() {}
        };
    }

    /**
     * Builds the sky, red cube and spheres under the world node and a probe, at the origin, baked from the sky and cube.
     */
    private static EnvironmentProbeControl buildScene(SimpleApplication app, Node world, boolean orientedBox) {
        AssetManager assetManager = app.getAssetManager();
        Node rootNode = app.getRootNode();

        Camera cam = app.getCamera();
        cam.setLocation(new Vector3f(0, 2.5f, 9));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        world.attachChild(sky);

        Geometry redCube = new Geometry("redCube", new Box(3.5f, 3.5f, 3.5f));
        Material redMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        redMaterial.setColor("Color", ColorRGBA.Red);
        redCube.setMaterial(redMaterial);
        redCube.setLocalTranslation(-7.5f, 1.5f, 8);
        world.attachChild(redCube);

        float[] roughnesses = {0.05f, 0.35f, 1f};
        for (int i = 0; i < roughnesses.length; i++) {
            Geometry sphere = new Geometry("sphere" + i, new Sphere(48, 48, 1.2f));
            Material material = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
            material.setColor("BaseColor", ColorRGBA.White);
            material.setFloat("Metallic", roughnesses[i] < 1f ? 1f : 0f);
            material.setFloat("Roughness", roughnesses[i]);
            sphere.setMaterial(material);
            sphere.setLocalTranslation((i - 1) * 3f, 0, 0);
            world.attachChild(sphere);
        }
        rootNode.attachChild(world);

        EnvironmentProbeControl probe = new EnvironmentProbeControl(assetManager, BAKE_SIZE);
        if (orientedBox) {
            probe.setAreaType(LightProbe.AreaType.OrientedBox);
            OrientedBoxProbeArea box = (OrientedBoxProbeArea) probe.getArea();
            box.setExtent(new Vector3f(7, 4, 5));
            box.setRotation(BOX_ROTATION_AT_SETUP);
        }
        rootNode.addControl(probe);
        probe.tag(sky);
        probe.tag(redCube);
        return probe;
    }
}
