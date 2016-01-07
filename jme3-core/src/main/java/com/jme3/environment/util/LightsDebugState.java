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
package com.jme3.environment.util;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.light.LightProbe;
import com.jme3.light.Light;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A debug state that will display LIght gizmos on screen.
 * Still a wip and for now it only displays light probes.
 * 
 * @author nehon
 */
public class LightsDebugState extends BaseAppState {

    private Node debugNode;
    private final Map<LightProbe, Node> probeMapping = new HashMap<LightProbe, Node>();
    private final List<LightProbe> garbage = new ArrayList<LightProbe>();
    private Geometry debugGeom;
    private Geometry debugBounds;
    private Material debugMaterial;
    private DebugMode debugMode = DebugMode.PrefilteredEnvMap;
    private float probeScale = 1.0f;
    private Spatial scene = null;
    private final List<LightProbe> probes = new ArrayList<LightProbe>();

    /**
     * Debug mode for light probes
     */
    public enum DebugMode {

        /**
         * Displays the prefiltered env maps on the debug sphere
         */
        PrefilteredEnvMap,
        /**
         * displays the Irradiance map on the debug sphere
         */
        IrradianceMap
    }

    @Override
    protected void initialize(Application app) {
        debugNode = new Node("Environment debug Node");
        Sphere s = new Sphere(16, 16, 1);
        debugGeom = new Geometry("debugEnvProbe", s);
        debugMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/reflect.j3md");
        debugGeom.setMaterial(debugMaterial);
        debugBounds = BoundingSphereDebug.createDebugSphere(app.getAssetManager());
        if (scene == null) {
            scene = app.getViewPort().getScenes().get(0);
        }
    }

    @Override
    public void update(float tpf) {
        for (Light light : scene.getWorldLightList()) {
            switch (light.getType()) {

                case Probe:
                    LightProbe probe = (LightProbe) light;
                    probes.add(probe);
                    Node n = probeMapping.get(probe);
                    if (n == null) {
                        n = new Node("DebugProbe");
                        n.attachChild(debugGeom.clone(true));
                        n.attachChild(debugBounds.clone(false));
                        debugNode.attachChild(n);
                        probeMapping.put(probe, n);
                    }
                    Geometry probeGeom = ((Geometry) n.getChild(0));
                    Material m = probeGeom.getMaterial();
                    probeGeom.setLocalScale(probeScale);
                    if (probe.isReady()) {
                        if (debugMode == DebugMode.IrradianceMap) {
                            m.setTexture("CubeMap", probe.getIrradianceMap());
                        } else {
                            m.setTexture("CubeMap", probe.getPrefilteredEnvMap());
                        }
                    }
                    n.setLocalTranslation(probe.getPosition());
                    n.getChild(1).setLocalScale(((BoundingSphere) probe.getBounds()).getRadius());
                    break;
                default:
                    break;
            }
        }
        debugNode.updateLogicalState(tpf);
        debugNode.updateGeometricState();
        cleanProbes();

    }

    /**
     * Set the scenes for wich to render light gizmos.
     * @param scene 
     */
    public void setScene(Spatial scene) {
        this.scene = scene;
    }

    private void cleanProbes() {
        if (probes.size() != probeMapping.size()) {
            for (LightProbe probe : probeMapping.keySet()) {
                if (!probes.contains(probe)) {
                    garbage.add(probe);
                }
            }
            for (LightProbe probe : garbage) {
                probeMapping.remove(probe);
            }
            garbage.clear();
            probes.clear();
        }
    }

    @Override
    public void render(RenderManager rm) {
        rm.renderScene(debugNode, getApplication().getViewPort());
    }

    /**
     * 
     * @see DebugMode
     * @return the debug mode
     */
    public DebugMode getDebugMode() {
        return debugMode;
    }

    /**
     * sets the debug mode
     * @see DebugMode
     * @param debugMode the debug mode
     */
    public void setDebugMode(DebugMode debugMode) {
        this.debugMode = debugMode;

    }

    /**
     * returns the scale of the probe's debug sphere
     * @return 
     */
    public float getProbeScale() {
        return probeScale;
    }

    /**
     * sets the scale of the probe's debug sphere
     * @param probeScale 
     */
    public void setProbeScale(float probeScale) {
        this.probeScale = probeScale;
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

}
