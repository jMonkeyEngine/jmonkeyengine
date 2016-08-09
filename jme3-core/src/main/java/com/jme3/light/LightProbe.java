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
package com.jme3.light;

import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.environment.util.EnvMapUtils;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * A LightProbe is not exactly a light. It holds environment map information used for Image Based Lighting.
 * This is used for indirect lighting in the Physically Based Rendering pipeline.
 * 
 * A light probe has a position in world space. This is the position from where the Environment Map are rendered.
 * There are two environment maps held by the LightProbe :
 * - The irradiance map (used for indirect diffuse lighting in the PBR pipeline).
 * - The prefiltered environment map (used for indirect specular lighting and reflection in the PBE pipeline).
 * Note that when instanciating the LightProbe, both those maps are null. 
 * To render them see {@link LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Node)}
 * and {@link EnvironmentCamera}.
 * 
 * The light probe has an area of effect that is a bounding volume centered on its position. (for now only Bounding spheres are supported).
 * 
 * A LightProbe will only be taken into account when it's marked as ready. 
 * A light probe is ready when it has valid environment map data set.
 * Note that you should never call setReady yourself.
 *
 * @see LightProbeFactory
 * @see EnvironmentCamera
 * @author nehon
 */
public class LightProbe extends Light implements Savable {

    private TextureCubeMap irradianceMap;
    private TextureCubeMap prefilteredEnvMap;
    private BoundingVolume bounds = new BoundingSphere(1.0f, Vector3f.ZERO);
    private boolean ready = false;
    private Vector3f position = new Vector3f();
    private Node debugNode;

    /**
     * Empty constructor used for serialization. 
     * You should never call it, use {@link LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Node)} instead
     */
    public LightProbe() {        
    }

    /**
     * returns the irradiance map texture of this Light probe.
     * Note that this Texture may not have image data yet if the LightProbe is not ready
     * @return the irradiance map 
     */
    public TextureCubeMap getIrradianceMap() {
        return irradianceMap;
    }

    /**
     * Sets the irradiance map
     * @param irradianceMap the irradiance map
     */
    public void setIrradianceMap(TextureCubeMap irradianceMap) {
        this.irradianceMap = irradianceMap;
    }

    /**
     * returns the prefiltered environment map texture of this light probe
     * Note that this Texture may not have image data yet if the LightProbe is not ready
     * @return the prefiltered environment map
     */
    public TextureCubeMap getPrefilteredEnvMap() {
        return prefilteredEnvMap;
    }

    /**
     * Sets the prefiltered environment map 
     * @param prefileteredEnvMap the prefiltered environment map 
     */
    public void setPrefilteredMap(TextureCubeMap prefileteredEnvMap) {
        this.prefilteredEnvMap = prefileteredEnvMap;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(irradianceMap, "irradianceMap", null);
        oc.write(prefilteredEnvMap, "prefilteredEnvMap", null);
        oc.write(position, "position", null);
        oc.write(bounds, "bounds", new BoundingSphere(1.0f, Vector3f.ZERO));
        oc.write(ready, "ready", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        irradianceMap = (TextureCubeMap) ic.readSavable("irradianceMap", null);
        prefilteredEnvMap = (TextureCubeMap) ic.readSavable("prefilteredEnvMap", null);
        position = (Vector3f) ic.readSavable("position", this);
        bounds = (BoundingVolume) ic.readSavable("bounds", new BoundingSphere(1.0f, Vector3f.ZERO));
        ready = ic.readBoolean("ready", false);
    }

    /**
     * returns the bounding volume of this LightProbe
     * @return a bounding volume.
     */
    public BoundingVolume getBounds() {
        return bounds;
    }
    
    /**
     * Sets the bounds of this LightProbe
     * Note that for now only BoundingSphere is supported and this method will 
     * throw an UnsupportedOperationException with any other BoundingVolume type
     * @param bounds the bounds of the LightProbe
     */
    public void setBounds(BoundingVolume bounds) {
        if( bounds.getType()!= BoundingVolume.Type.Sphere){
            throw new UnsupportedOperationException("For not only BoundingSphere are suported for LightProbe");
        }
        this.bounds = bounds;
    }

    /**
     * return true if the LightProbe is ready, meaning the Environment maps have
     * been loaded or rnedered and are ready to be used by a material
     * @return the LightProbe ready state
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Don't call this method directly.
     * It's meant to be called by additional systems that will load or render
     * the Environment maps of the LightProbe
     * @param ready the ready state of the LightProbe.
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /**
     * For debuging porpose only
     * Will return a Node meant to be added to a GUI presenting the 2 cube maps in a cross pattern with all the mip maps.
     * 
     * @param manager the asset manager
     * @return a debug node
     */
    public Node getDebugGui(AssetManager manager) {
        if (!ready) {
            throw new UnsupportedOperationException("This EnvProbeis not ready yet, try to test isReady()");
        }
        if (debugNode == null) {
            debugNode = new Node("debug gui probe");
            Node debugPfemCm = EnvMapUtils.getCubeMapCrossDebugViewWithMipMaps(getPrefilteredEnvMap(), manager);
            Node debugIrrCm = EnvMapUtils.getCubeMapCrossDebugView(getIrradianceMap(), manager);

            debugNode.attachChild(debugIrrCm);
            debugNode.attachChild(debugPfemCm);
            debugPfemCm.setLocalTranslation(520, 0, 0);
        }

        return debugNode;
    }

    /**
     * Returns the position of the LightProbe in world space
     * @return the wolrd space position
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Sets the position of the LightProbe in world space
     * @param position the wolrd space position
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
        getBounds().setCenter(position);
    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        return getBounds().intersectsBoundingBox(box);
    }

    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        return camera.contains(bounds) != Camera.FrustumIntersect.Outside;
    }

    @Override
    protected void computeLastDistance(Spatial owner) {
        if (owner.getWorldBound() != null) {
            BoundingVolume bv = owner.getWorldBound();
            lastDistance = bv.distanceSquaredTo(position);
        } else {
            lastDistance = owner.getWorldTranslation().distanceSquared(position);
        }
    }

    @Override
    public Type getType() {
        return Type.Probe;
    }

    @Override
    public String toString() {
        return "Light Probe : " + name + " at " + position + " / " + bounds;
    }

    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        return getBounds().intersectsSphere(sphere);
    }
    
    

}
