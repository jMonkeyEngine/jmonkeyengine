/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
package com.jme3.environment.util;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * A debug state that visualizes different types of lights in the scene with gizmos.
 * This state is useful for debugging light positions, ranges, and other properties.
 *
 * @author nehon
 * @author capdevon
 */
public class LightsDebugState extends BaseAppState {

    private static final String PROBE_GEOMETRY_NAME = "DebugProbeGeometry";
    private static final String PROBE_BOUNDS_NAME = "DebugProbeBounds";
    private static final String SPOT_LIGHT_INNER_RADIUS_NAME = "SpotLightInnerRadius";
    private static final String SPOT_LIGHT_OUTER_RADIUS_NAME = "SpotLightOuterRadius";
    private static final String SPOT_LIGHT_RADIUS_NAME = "RadiusNode";
    private static final String POINT_LIGHT_RADIUS_NAME = "PointLightRadius";
    private static final String ARROW_NAME = "Arrow";

    // The scene whose lights will be debugged
    private Spatial scene;
    private ViewPort viewPort;
    private Node debugNode;
    private final Map<Light, Spatial> lightGizmoMap = new WeakHashMap<>();
    private final ArrayDeque<Light> lightDeque = new ArrayDeque<>();
    private Predicate<Light> lightFilter = x -> true; // Identity Function

    private AssetManager assetManager;
    private Material debugMaterial;

    private float lightProbeScale = 1.0f;
    private final ColorRGBA debugColor = ColorRGBA.DarkGray;
    private final Quaternion tempRotation = new Quaternion();

    @Override
    protected void initialize(Application app) {

        this.assetManager = app.getAssetManager();
        viewPort = app.getRenderManager().createMainView("EnvDebugView", app.getCamera());
        debugNode = new Node("LightsDebugNode");

        debugMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        debugMaterial.setColor("Color", debugColor);
        debugMaterial.getAdditionalRenderState().setWireframe(true);

        if (scene == null) {
            scene = app.getViewPort().getScenes().get(0);
        }
    }

    private Spatial createBulb() {
        Quad q = new Quad(0.5f, 0.5f);
        Geometry lightBulb = new Geometry("LightBulb", q);
        lightBulb.move(-q.getHeight() / 2f, -q.getWidth() / 2f, 0);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("Common/Textures/lightbulb32.png");
        mat.setTexture("ColorMap", tex);
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        lightBulb.setMaterial(mat);
        lightBulb.setQueueBucket(RenderQueue.Bucket.Transparent);

        Node billboard = new Node("Billboard");
        billboard.addControl(new BillboardControl());
        billboard.attachChild(lightBulb);

        return billboard;
    }

    private Geometry createRadiusShape(String name, float dashSize) {
        Geometry radius = Circle.createShape(assetManager, name);
        Material mat = radius.getMaterial();
        mat.setColor("Color", debugColor);
        mat.setFloat("DashSize", dashSize);
        return radius;
    }

    private Spatial createPointGizmo() {
        Node gizmo = new Node("PointLightNode");
        gizmo.attachChild(createBulb());

        Geometry radius = new Geometry(POINT_LIGHT_RADIUS_NAME, new BoundingSphereDebug());
        radius.setMaterial(debugMaterial);
        gizmo.attachChild(radius);

        return gizmo;
    }

    private Spatial createDirectionalGizmo() {
        Node gizmo = new Node("DirectionalLightNode");
        gizmo.move(0, 5, 0);
        gizmo.attachChild(createBulb());

        Geometry arrow = new Geometry("Arrow", new Arrow(Vector3f.UNIT_Z.mult(5f)));
        arrow.setMaterial(debugMaterial);
        gizmo.attachChild(arrow);

        return gizmo;
    }

    private Spatial createSpotGizmo() {
        Node gizmo = new Node("SpotLightNode");
        gizmo.attachChild(createBulb());

        Node radiusNode = new Node(SPOT_LIGHT_RADIUS_NAME);
        gizmo.attachChild(radiusNode);

        Geometry inRadius = createRadiusShape(SPOT_LIGHT_INNER_RADIUS_NAME, 0.8f); // 0.875f);
        radiusNode.attachChild(inRadius);

        Geometry outRadius = createRadiusShape(SPOT_LIGHT_OUTER_RADIUS_NAME, 0.325f); // 0.125f);
        radiusNode.attachChild(outRadius);

        Geometry arrow = new Geometry(ARROW_NAME, new Arrow(Vector3f.UNIT_Z));
        arrow.setMaterial(debugMaterial);
        gizmo.attachChild(arrow);

        return gizmo;
    }

    private Spatial createLightProbeGizmo() {
        Node gizmo = new Node("LightProbeNode");

        Sphere sphere = new Sphere(16, 16, lightProbeScale);
        Geometry probeGeom = new Geometry(PROBE_GEOMETRY_NAME, sphere);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/reflect.j3md");
        probeGeom.setMaterial(mat);
        gizmo.attachChild(probeGeom);

        Geometry probeBounds = BoundingSphereDebug.createDebugSphere(assetManager);
        probeBounds.setName(PROBE_BOUNDS_NAME);
        gizmo.attachChild(probeBounds);

        return gizmo;
    }

    /**
     * Updates the light gizmos based on the current state of lights in the scene.
     * This method is called every frame when the state is enabled.
     *
     * @param tpf The time per frame.
     */
    @Override
    public void update(float tpf) {
        updateLightGizmos(scene);
        debugNode.updateLogicalState(tpf);
        cleanUpRemovedLights();
    }

    /**
     * Renders the debug gizmos onto the screen.
     *
     * @param rm The render manager.
     */
    @Override
    public void render(RenderManager rm) {
        debugNode.updateGeometricState();
        rm.renderScene(debugNode, viewPort);
    }

    /**
     * Recursively traverses the scene graph to find and update light gizmos.
     * New gizmos are created for new lights, and existing gizmos are updated.
     *
     * @param spatial The current spatial to process for lights.
     */
    private void updateLightGizmos(Spatial spatial) {
        // Add or update gizmos for lights attached to the current spatial
        for (Light light : spatial.getLocalLightList()) {
            if (!lightFilter.test(light)) {
                continue;
            }

            lightDeque.add(light);
            Spatial gizmo = lightGizmoMap.get(light);

            if (gizmo == null) {
                gizmo = createLightGizmo(light);
                if (gizmo != null) {
                    debugNode.attachChild(gizmo);
                    lightGizmoMap.put(light, gizmo);
                    updateGizmoProperties(light, gizmo); // Set initial properties
                }
            } else {
                updateGizmoProperties(light, gizmo);
            }
        }

        // Recursively call for children if it's a Node
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                updateLightGizmos(child);
            }
        }
    }

    /**
     * Creates a new gizmo spatial for a given light based on its type.
     *
     * @param light The light for which to create a gizmo.
     * @return A spatial representing the gizmo, or null if the light type is not supported.
     */
    private Spatial createLightGizmo(Light light) {
        switch (light.getType()) {
            case Probe:
                return createLightProbeGizmo();
            case Point:
                return createPointGizmo();
            case Directional:
                return createDirectionalGizmo();
            case Spot:
                return createSpotGizmo();
            default:
                // Unsupported light type
                return null;
        }
    }

    /**
     * Updates the visual properties and position of a light gizmo based on its corresponding light.
     *
     * @param light The light whose properties are used for updating the gizmo.
     * @param gizmo The spatial representing the light gizmo.
     */
    private void updateGizmoProperties(Light light, Spatial gizmo) {
        Node lightNode = (Node) gizmo;

        switch (light.getType()) {
            case Probe:
                LightProbe probe = (LightProbe) light;
                Geometry probeGeom = (Geometry) lightNode.getChild(PROBE_GEOMETRY_NAME);
                Geometry probeBounds = (Geometry) lightNode.getChild(PROBE_BOUNDS_NAME);

                // Update texture if probe is ready
                if (probe.isReady()) {
                    Material mat = probeGeom.getMaterial();
                    if (mat.getTextureParam("CubeMap") == null) {
                        mat.setTexture("CubeMap", probe.getPrefilteredEnvMap());
                    }
                }
                probeGeom.setLocalScale(lightProbeScale);
                probeBounds.setLocalScale(probe.getArea().getRadius());
                gizmo.setLocalTranslation(probe.getPosition());
                break;

            case Point:
                PointLight pl = (PointLight) light;
                Geometry radius = (Geometry) lightNode.getChild(POINT_LIGHT_RADIUS_NAME);
                radius.setLocalScale(pl.getRadius());
                gizmo.setLocalTranslation(pl.getPosition());
                break;

            case Spot:
                SpotLight sl = (SpotLight) light;
                gizmo.setLocalTranslation(sl.getPosition());

                tempRotation.lookAt(sl.getDirection(), Vector3f.UNIT_Y);
                gizmo.setLocalRotation(tempRotation);

                float spotRange = sl.getSpotRange();
                float innerAngle = sl.getSpotInnerAngle();
                float outerAngle = sl.getSpotOuterAngle();
                float innerRadius = spotRange * FastMath.tan(innerAngle);
                float outerRadius = spotRange * FastMath.tan(outerAngle);

                lightNode.getChild(SPOT_LIGHT_INNER_RADIUS_NAME).setLocalScale(innerRadius);
                lightNode.getChild(SPOT_LIGHT_OUTER_RADIUS_NAME).setLocalScale(outerRadius);
                lightNode.getChild(SPOT_LIGHT_RADIUS_NAME).setLocalTranslation(0, 0, spotRange);
                lightNode.getChild(ARROW_NAME).setLocalScale(spotRange);
                break;

            case Directional:
                DirectionalLight dl = (DirectionalLight) light;
                tempRotation.lookAt(dl.getDirection(), Vector3f.UNIT_Y);
                gizmo.setLocalRotation(tempRotation);
                break;

            default:
                // Unsupported light type
                break;
        }
    }

    /**
     * Cleans up gizmos for lights that have been removed from the scene.
     */
    private void cleanUpRemovedLights() {

        Iterator<Map.Entry<Light, Spatial>> iterator = lightGizmoMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Light, Spatial> entry = iterator.next();
            Light light = entry.getKey();

            if (!lightDeque.contains(light)) {
                Spatial gizmo = entry.getValue();
                gizmo.removeFromParent();
                iterator.remove();
            }
        }

        lightDeque.clear();
    }

    /**
     * Sets the scene for which to render light gizmos.
     * If no scene is set, it defaults to the first scene in the viewport.
     *
     * @param scene The root of the desired scene.
     */
    public void setScene(Spatial scene) {
        this.scene = scene;
        // Clear existing gizmos when the scene changes to avoid displaying gizmos from the old scene
        debugNode.detachAllChildren();
        lightGizmoMap.clear();
        lightDeque.clear();
    }

    /**
     * Returns the current scale of the light probe's debug sphere.
     *
     * @return The scale factor.
     */
    public float getLightProbeScale() {
        return lightProbeScale;
    }

    /**
     * Sets the scale of the light probe's debug sphere.
     *
     * @param scale The scale factor (default is 1.0).
     */
    public void setLightProbeScale(float scale) {
        this.lightProbeScale = scale;
    }

    /**
     * Sets a filter to control which lights are displayed by the debug state.
     * By default, no filter is applied, meaning all lights are displayed.
     *
     * @param lightFilter A {@link Predicate} that tests a {@link Light} object.
     */
    public void setLightFilter(Predicate<Light> lightFilter) {
        this.lightFilter = lightFilter;
    }

    /**
     * Cleans up resources when the app state is detached.
     *
     * @param app The application instance.
     */
    @Override
    protected void cleanup(Application app) {
        debugNode.detachAllChildren();
        lightGizmoMap.clear();
        lightDeque.clear();
        debugMaterial = null;
        app.getRenderManager().removeMainView(viewPort);
    }

    @Override
    protected void onEnable() {
        viewPort.attachScene(debugNode);
    }

    @Override
    protected void onDisable() {
        viewPort.detachScene(debugNode);
    }

}
