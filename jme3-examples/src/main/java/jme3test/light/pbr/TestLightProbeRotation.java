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
package jme3test.light.pbr;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.LightProbe;
import com.jme3.light.OrientedBoxProbeArea;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.debug.WireBox;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.SkyFactory;

/**
 * Manual partner of the TestLightProbeRotation screenshot test, showing why a light probe needs to rotate after it is
 * baked.
 * <p>
 * The viewer stands on a platform that never moves (as a player might stand on a ship's bridge, or a VR user in their
 * room). In front of it, the world (the sky, a big red cube and three spheres from mirror to fully rough) is turned
 * about a point, as if the platform were turning within it. The probe is baked once from the sky and the cube, and
 * follows the world through a {@link LightControl}. The platform is not baked into the probe but is lit by it, so it
 * should pick up the turning world's lighting while it stays still.
 * <p>
 * Switch "probe follows world" off to see the problem this solves: the reflections and lighting, on the spheres and the
 * platform alike, stay where they were while the sky and the cube turn away from them.
 */
public class TestLightProbeRotation extends SimpleApplication {

    private static final float TURN_SPEED = 0.4f;

    private static final Vector3f ROTATION_CENTRE = new Vector3f(0, 0, -10);

    private static final Quaternion BOX_ROTATION_AT_SETUP = new Quaternion().fromAngleAxis(30 * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y);

    /**
     * Large enough to take in the platform whichever way the world is turned.
     */
    private static final Vector3f BOX_EXTENT = new Vector3f(16, 6, 16);

    private static final String TURN_LEFT = "TurnLeft";
    private static final String TURN_RIGHT = "TurnRight";
    private static final String TOGGLE_AUTO_TURN = "ToggleAutoTurn";
    private static final String TOGGLE_FOLLOW = "ToggleFollow";
    private static final String REBAKE = "Rebake";
    private static final String TOGGLE_BOX = "ToggleBox";

    private final Node world = new Node("world");
    private float worldAngle = 0;
    private boolean autoTurn = false;
    private EnvironmentProbeControl probe;
    private LightControl probeFollowsWorld;
    private Geometry boxOutline;
    private BitmapText hud;

    public static void main(String[] args) {
        new TestLightProbeRotation().start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(0, 1.7f, 1.5f));
        cam.lookAt(ROTATION_CENTRE, Vector3f.UNIT_Y);
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(0);

        buildPlatform();
        buildWorld();

        boxOutline = new Geometry("probeBox", new WireBox(BOX_EXTENT.x, BOX_EXTENT.y, BOX_EXTENT.z));
        boxOutline.setMaterial(unshaded(ColorRGBA.Yellow));

        hud = new BitmapText(guiFont);
        guiNode.attachChild(hud);

        inputManager.addMapping(TURN_LEFT, new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping(TURN_RIGHT, new KeyTrigger(KeyInput.KEY_L));
        inputManager.addMapping(TOGGLE_AUTO_TURN, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(TOGGLE_FOLLOW, new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping(REBAKE, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(TOGGLE_BOX, new KeyTrigger(KeyInput.KEY_B));
        inputManager.addListener(turnListener, TURN_LEFT, TURN_RIGHT);
        inputManager.addListener(toggleListener, TOGGLE_AUTO_TURN, TOGGLE_FOLLOW, REBAKE, TOGGLE_BOX);

        updateHud();
    }

    private void buildPlatform() {
        Material platformMaterial = pbr(new ColorRGBA(0.8f, 0.8f, 0.85f, 1), 1f, 0.25f);

        Geometry deck = new Geometry("deck", new Box(3, 0.1f, 3));
        deck.setMaterial(platformMaterial);
        deck.setLocalTranslation(0, -0.1f, 0);
        rootNode.attachChild(deck);

        Geometry rail = new Geometry("rail", new Box(3, 0.05f, 0.05f));
        rail.setMaterial(platformMaterial);
        rail.setLocalTranslation(0, 1, -3);
        rootNode.attachChild(rail);

        for (int i = -1; i <= 1; i++) {
            Geometry post = new Geometry("post" + i, new Box(0.05f, 0.5f, 0.05f));
            post.setMaterial(platformMaterial);
            post.setLocalTranslation(i * 2.95f, 0.5f, -3);
            rootNode.attachChild(post);
        }
    }

    private void buildWorld() {
        world.setLocalTranslation(ROTATION_CENTRE);

        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        world.attachChild(sky);

        Geometry redCube = new Geometry("redCube", new Box(2.5f, 2.5f, 2.5f));
        redCube.setMaterial(unshaded(ColorRGBA.Red));
        redCube.setLocalTranslation(-9, 1.5f, 3);
        world.attachChild(redCube);

        float[] roughnesses = {0.05f, 0.35f, 1f};
        for (int i = 0; i < roughnesses.length; i++) {
            Geometry sphere = new Geometry("sphere" + i, new Sphere(48, 48, 1.2f));
            sphere.setMaterial(pbr(ColorRGBA.White, roughnesses[i] < 1f ? 1f : 0f, roughnesses[i]));
            sphere.setLocalTranslation((i - 1) * 3f, 1, 0);
            world.attachChild(sphere);
        }
        rootNode.attachChild(world);

        probe = new EnvironmentProbeControl(assetManager, 256);
        rootNode.addControl(probe);
        probe.tag(sky);
        probe.tag(redCube);

        Node probeNode = new Node("probeNode");
        probeNode.setLocalTranslation(0, 1, 0);
        probeFollowsWorld = new LightControl(probe);
        probeNode.addControl(probeFollowsWorld);
        world.attachChild(probeNode);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (autoTurn) {
            turnWorld(TURN_SPEED * tpf);
        }
        if (!probeFollowsWorld.isEnabled()) {
            probe.setPosition(world.getWorldTranslation().add(0, 1, 0));
            probe.setRotation(Quaternion.IDENTITY);
        }
        if (probe.getArea() instanceof OrientedBoxProbeArea) {
            OrientedBoxProbeArea box = (OrientedBoxProbeArea) probe.getArea();
            boxOutline.setLocalTranslation(box.getCenter());
            boxOutline.setLocalRotation(box.getRotation());
        }
    }

    private void turnWorld(float angle) {
        worldAngle += angle;
        world.setLocalRotation(new Quaternion().fromAngleAxis(worldAngle, Vector3f.UNIT_Y));
        updateHud();
    }

    private final AnalogListener turnListener = (name, value, tpf) ->
            turnWorld(TURN_SPEED * tpf * (TURN_LEFT.equals(name) ? 1 : -1));

    private final ActionListener toggleListener = (name, isPressed, tpf) -> {
        if (!isPressed) {
            return;
        }
        switch (name) {
            case TOGGLE_AUTO_TURN:
                autoTurn = !autoTurn;
                break;
            case TOGGLE_FOLLOW:
                probeFollowsWorld.setEnabled(!probeFollowsWorld.isEnabled());
                break;
            case REBAKE:
                probe.rebake();
                break;
            case TOGGLE_BOX:
                toggleBox();
                break;
        }
        updateHud();
    };

    /**
     * Switches between a spherical area and a box that, like a room not built on the world axes, is already rotated
     * relative to the world.
     */
    private void toggleBox() {
        if (probe.getAreaType() == LightProbe.AreaType.OrientedBox) {
            probe.setAreaType(LightProbe.AreaType.Spherical);
            probe.getArea().setRadius(Float.MAX_VALUE);
            boxOutline.removeFromParent();
        } else {
            probe.setAreaType(LightProbe.AreaType.OrientedBox);
            OrientedBoxProbeArea box = (OrientedBoxProbeArea) probe.getArea();
            box.setExtent(BOX_EXTENT);
            box.setRotation(probe.getRotation().mult(BOX_ROTATION_AT_SETUP));
            rootNode.attachChild(boxOutline);
        }
    }

    private Material pbr(ColorRGBA colour, float metallic, float roughness) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        material.setColor("BaseColor", colour);
        material.setFloat("Metallic", metallic);
        material.setFloat("Roughness", roughness);
        return material;
    }

    private Material unshaded(ColorRGBA colour) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", colour);
        return material;
    }

    private void updateHud() {
        hud.setText(String.format(
                "World turned: %.0f degrees (the platform never moves)%n"
                        + "J / L: turn the world about the point in front of the platform%n"
                        + "T: keep turning: %s%n"
                        + "F: probe follows world: %s%n"
                        + "R: re-bake the probe (should make no visible difference)%n"
                        + "B: probe area: %s%n"
                        + "Drag the mouse to look around",
                worldAngle * FastMath.RAD_TO_DEG,
                autoTurn ? "on" : "off",
                probeFollowsWorld.isEnabled() ? "on" : "off (lighting ignores the turn)",
                probe.getAreaType() == LightProbe.AreaType.OrientedBox ? "box (yellow, rotated 30 degrees at setup)" : "spherical"));
        hud.setLocalTranslation(10, cam.getHeight() - 10, 0);
    }
}
