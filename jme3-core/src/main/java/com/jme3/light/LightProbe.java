/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.bounding.*;
import com.jme3.environment.EnvironmentCamera;
import com.jme3.environment.LightProbeFactory;
import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.TempVars;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A LightProbe is not exactly a light. It holds environment map information used for Image Based Lighting.
 * This is used for indirect lighting in the Physically Based Rendering pipeline.
 *
 * A light probe has a position in world space. This is the position from where the Environment Map are rendered.
 * There are two environment data structure  held by the LightProbe :
 * - The irradiance spherical harmonics factors (used for indirect diffuse lighting in the PBR pipeline).
 * - The prefiltered environment map (used for indirect specular lighting and reflection in the PBE pipeline).
 * Note that when instantiating the LightProbe, both of those structures are null.
 * To compute them see
 * {@link com.jme3.environment.LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Spatial)}
 * and {@link EnvironmentCamera}.
 *
 * The light probe has an area of effect centered on its position. It can have a Spherical area or an Oriented Box area
 *
 * A LightProbe will only be taken into account when it's marked as ready and enabled.
 * A light probe is ready when it has valid environment map data set.
 * Note that you should never call setReady yourself.
 *
 * @see LightProbeFactory
 * @see EnvironmentCamera
 * @author nehon
 */
public class LightProbe extends Light implements Savable {

    private static final Logger logger = Logger.getLogger(LightProbe.class.getName());
    public static final Matrix4f FALLBACK_MATRIX = new Matrix4f(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1);

    private Vector3f[] shCoefficients;
    private TextureCubeMap prefilteredEnvMap;
    private ProbeArea area = new SphereProbeArea(Vector3f.ZERO, 1.0f);
    private boolean ready = false;
    private Vector3f position = new Vector3f();
    private int nbMipMaps;

    public enum AreaType{
        Spherical,
        OrientedBox
    }

    /**
     * Empty constructor used for serialization.
     * You should never call it, use 
     * {@link com.jme3.environment.LightProbeFactory#makeProbe(com.jme3.environment.EnvironmentCamera, com.jme3.scene.Spatial)}
     * instead.
     */
    public LightProbe() {
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
     * @param prefilteredEnvMap the prefiltered environment map
     */
    public void setPrefilteredMap(TextureCubeMap prefilteredEnvMap) {
        this.prefilteredEnvMap = prefilteredEnvMap;
    }

    /**
     * Returns the data to send to the shader.
     * This is a column major matrix that is not a classic transform matrix, it's laid out in a particular way
     //   3x3 rot mat|
     //      0  1  2 |  3
     // 0 | ax bx cx | px | )
     // 1 | ay by cy | py | probe position
     // 2 | az bz cz | pz | )
     // --|----------|
     // 3 | sx sy sz   sp |    1/probe radius + nbMipMaps
     //    --scale--
     * <p>
     * (ax, ay, az) is the pitch rotation axis
     * (bx, by, bz) is the yaw rotation axis
     * (cx, cy, cz) is the roll rotation axis
     * Like in a standard 3x3 rotation matrix.
     * It's also the valid rotation matrix of the probe in world space.
     * Note that for the Spherical Probe area this part is a 3x3 identity matrix.
     * <p>
     * (px, py, pz) is the position of the center of the probe in world space
     * Like in a valid 4x4 transform matrix.
     * <p>
     * (sx, sy, sy) is the extent of the probe ( the scale )
     * In a standard transform matrix, the scale is applied to the rotation matrix part.
     * In the shader, we need the rotation and the scale to be separated. Doing so avoids extracting
     * the scale from a classic transform matrix in the shader.
     * <p>
     * (sp) is a special entry, it contains the packed number of mip maps of the probe and the inverse radius for the probe.
     * since the inverse radius in lower than 1, it's packed in the decimal part of the float.
     * The number of mip maps is packed in the integer part of the float.
     * (ie: for 6 mip maps and a radius of 3, sp= 6.3333333)
     * <p>
     * The radius is obvious for a SphereProbeArea,
     * but in the case of an OrientedBoxProbeArea it's the max of the extent vector's components.
     *
     * @return the pre-existing matrix
     */
    public Matrix4f getUniformMatrix(){

        Matrix4f mat = area.getUniformMatrix();

        // setting the (sp) entry of the matrix
        mat.m33 = nbMipMaps + 1f / area.getRadius();

        return mat;
    }


    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(shCoefficients, "shCoeffs", null);
        oc.write(prefilteredEnvMap, "prefilteredEnvMap", null);
        oc.write(position, "position", null);
        oc.write(area, "area", new SphereProbeArea(Vector3f.ZERO, 1.0f));
        oc.write(ready, "ready", false);
        oc.write(nbMipMaps, "nbMipMaps", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);

        prefilteredEnvMap = (TextureCubeMap) ic.readSavable("prefilteredEnvMap", null);
        position = (Vector3f) ic.readSavable("position", null);
        area = (ProbeArea)ic.readSavable("area", null);
        if(area == null) {
            // retro compat
            BoundingSphere bounds = (BoundingSphere) ic.readSavable("bounds", new BoundingSphere(1.0f, Vector3f.ZERO));
            area = new SphereProbeArea(bounds.getCenter(), bounds.getRadius());
        }
        area.setCenter(position);
        nbMipMaps = ic.readInt("nbMipMaps", 0);
        ready = ic.readBoolean("ready", false);

        Savable[] coeffs = ic.readSavableArray("shCoeffs", null);
        if (coeffs == null) {
            ready = false;
            logger.log(Level.WARNING, "LightProbe is missing parameters, it should be recomputed. Please use lightProbeFactory.updateProbe()");
        } else {
            shCoefficients = new Vector3f[coeffs.length];
            for (int i = 0; i < coeffs.length; i++) {
                shCoefficients[i] = (Vector3f) coeffs[i];
            }
        }
    }


    /**
     * returns the bounding volume of this LightProbe
     * @return a bounding volume.
     * @deprecated use {@link LightProbe#getArea()}
     */
    @Deprecated
    public BoundingVolume getBounds() {
        return new BoundingSphere(area.getRadius(), ((SphereProbeArea)area).getCenter());
    }

    public ProbeArea getArea() {
        return area;
    }

    public void setAreaType(AreaType type){
        switch (type){
            case Spherical:
                area = new SphereProbeArea(Vector3f.ZERO, 1.0f);
                break;
            case OrientedBox:
                area = new OrientedBoxProbeArea(new Transform());
                break;
        }
        area.setCenter(position);
    }

    public AreaType getAreaType(){
        if(area instanceof SphereProbeArea){
            return AreaType.Spherical;
        }
        return AreaType.OrientedBox;
    }

    /**
     * return true if the LightProbe is ready, meaning the Environment maps have
     * been loaded or rendered and are ready to be used by a material
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

    public Vector3f[] getShCoeffs() {
        return shCoefficients;
    }

    public void setShCoeffs(Vector3f[] shCoefficients) {
        this.shCoefficients = shCoefficients;
    }

    /**
     * Returns the position of the LightProbe in world space
     * @return the world-space position
     */
    public Vector3f getPosition() {
        return position;
    }

    /**
     * Sets the position of the LightProbe in world space
     * @param position the world-space position
     */
    public void setPosition(Vector3f position) {
        this.position.set(position);
        area.setCenter(position);
    }

    public int getNbMipMaps() {
        return nbMipMaps;
    }

    public void setNbMipMaps(int nbMipMaps) {
        this.nbMipMaps = nbMipMaps;
    }

    @Override
    public boolean intersectsBox(BoundingBox box, TempVars vars) {
        return area.intersectsBox(box, vars);
    }

    @Override
    public boolean intersectsFrustum(Camera camera, TempVars vars) {
        return area.intersectsFrustum(camera, vars);
    }

    @Override
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars) {
        return area.intersectsSphere(sphere, vars);
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
        return "Light Probe : " + name + " at " + position + " / " + area;
    }


}
