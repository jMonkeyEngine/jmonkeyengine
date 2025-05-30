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
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A debug state that will display Light gizmos on screen.
 * Still a wip and for now it only displays light probes.
 *
 * @author nehon
 */
public class LightsDebugState extends BaseAppState {

    private static final String PROBE_GEOMETRY_NAME = "DebugProbeGeometry";
    private static final String PROBE_BOUNDS_NAME = "DebugProbeBounds";

    private Node debugNode;
    private final Map<Light, Spatial> lightGizmoMap = new HashMap<>();
    private final List<Light> lightList = new ArrayList<>();
    private Spatial scene;

    private Geometry baseProbeGeom;
    private Geometry baseProbeBounds;
    private Material lightProbeMaterial;
    private float probeScale = 1.0f;

    @Override
    protected void initialize(Application app) {
        debugNode = new Node("LightsDebugNode");

        Sphere s = new Sphere(16, 16, 0.15f);
        baseProbeGeom = new Geometry(PROBE_GEOMETRY_NAME, s);
        lightProbeMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/reflect.j3md");
        baseProbeGeom.setMaterial(lightProbeMaterial);

        baseProbeBounds = BoundingSphereDebug.createDebugSphere(app.getAssetManager());
        baseProbeBounds.setName(PROBE_BOUNDS_NAME);

        if (scene == null) {
            scene = app.getViewPort().getScenes().get(0);
        }
    }

    @Override
    public void update(float tpf) {
        if (!isEnabled()) {
            return;
        }
        updateLights(scene);
        debugNode.updateLogicalState(tpf);
        debugNode.updateGeometricState();
        cleanProbes();
    }

    public void updateLights(Spatial scene) {
        for (Light light : scene.getWorldLightList()) {
            switch (light.getType()) {

                case Probe:
                    LightProbe probe = (LightProbe) light;
                    lightList.add(probe);
                    Node gizmo = (Node) lightGizmoMap.get(probe);
                    if (gizmo == null) {
                        gizmo = new Node("DebugProbe");
                        gizmo.attachChild(baseProbeGeom.clone(true));
                        gizmo.attachChild(baseProbeBounds.clone(false));
                        debugNode.attachChild(gizmo);
                        lightGizmoMap.put(probe, gizmo);
                    }
                    Geometry probeGeom = (Geometry) gizmo.getChild(PROBE_GEOMETRY_NAME);
                    Geometry probeBounds = (Geometry) gizmo.getChild(PROBE_BOUNDS_NAME);

                    Material mat = probeGeom.getMaterial();
                    if (probe.isReady()) {
                        mat.setTexture("CubeMap", probe.getPrefilteredEnvMap());
                    }

                    probeGeom.setLocalScale(probeScale);
                    probeBounds.setLocalScale(probe.getArea().getRadius());
                    gizmo.setLocalTranslation(probe.getPosition());
                    break;

                case Point:
                case Spot:
                case Directional:
                    // work in progress...
                default:
                    break;
            }
        }
        if (scene instanceof Node) {
            Node n = (Node)scene;
            for (Spatial spatial : n.getChildren()) {
                updateLights(spatial);
            }
        }
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
        lightList.clear();
    }

    private void cleanProbes() {
        Iterator<Map.Entry<Light, Spatial>> iterator = lightGizmoMap.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Light, Spatial> entry = iterator.next();
            Light light = entry.getKey();

            if (!lightList.contains(light)) {
                Spatial gizmoToRemove = entry.getValue();
                gizmoToRemove.removeFromParent();
                iterator.remove();
            }
        }

        lightList.clear();
    }

    @Override
    public void render(RenderManager rm) {
        if (!isEnabled()) {
            return;
        }
        rm.renderScene(debugNode, getApplication().getViewPort());
    }

    /**
     * Returns the current scale of the light probe's debug sphere.
     *
     * @return The scale factor.
     */
    public float getProbeScale() {
        return probeScale;
    }

    /**
     * Sets the scale of the light probe's debug sphere.
     *
     * @param probeScale The scale factor (default is 1.0).
     */
    public void setProbeScale(float probeScale) {
        this.probeScale = probeScale;
    }

    @Override
    protected void cleanup(Application app) {
        debugNode.removeFromParent();
        lightGizmoMap.clear();
        lightList.clear();

        lightProbeMaterial = null;
        baseProbeGeom = null;
        baseProbeBounds = null;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

}
