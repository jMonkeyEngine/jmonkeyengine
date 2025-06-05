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
package com.jme3.scene.control;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * `LightControl` synchronizes the world transformation (position and/or
 * direction) of a `Light` with its attached `Spatial`. This control allows
 * a light to follow a spatial or vice-versa, depending on the chosen
 * {@link ControlDirection}.
 * <p>
 * This is particularly useful for attaching lights to animated characters,
 * moving vehicles, or dynamically controlled objects.
 * </p>
 *
 * @author Tim
 * @author Markil 3
 */
public class LightControl extends AbstractControl {

    public enum ControlDirection {
        /**
         * Means, that the Light's transform is "copied"
         * to the Transform of the Spatial.
         */
        LightToSpatial,
        /**
         * Means, that the Spatial's transform is "copied"
         * to the Transform of the light.
         */
        SpatialToLight
    }

    /**
     * Represents the local axis of the spatial (X, Y, or Z) to be used
     * for determining the light's direction when `ControlDirection` is
     * `SpatialToLight`.
     */
    public enum Axis {
        X, Y, Z
    }

    /**
     * Represents the direction (positive or negative) along the chosen axis.
     */
    public enum Direction {
        Positive, Negative
    }

    private Light light;
    private ControlDirection controlDir = ControlDirection.SpatialToLight;
    private Axis axisRotation = Axis.Z;
    private Direction axisDirection = Direction.Positive;

    /**
     * For serialization only. Do not use.
     */
    public LightControl() {
    }

    /**
     * Creates a new `LightControl` that synchronizes the light's transform to the spatial.
     *
     * @param light The light to be synced.
     * @throws IllegalArgumentException if the light type is not supported
     * (only Point, Directional, and Spot lights are supported).
     */
    public LightControl(Light light) {
        validateSupportedLightType(light);
        this.light = light;
    }

    /**
     * Creates a new `LightControl` with a specified synchronization direction.
     *
     * @param light The light to be synced.
     * @param controlDir The direction of synchronization (SpatialToLight or LightToSpatial).
     * @throws IllegalArgumentException if the light type is not supported
     * (only Point, Directional, and Spot lights are supported).
     */
    public LightControl(Light light, ControlDirection controlDir) {
        validateSupportedLightType(light);
        this.light = light;
        this.controlDir = controlDir;
    }

    /**
     * Creates a new `LightControl` with a specified
     * axis of rotation, and axis direction.
     *
     * @param light The light to be synced.
     * @param axisRotation The spatial's local axis to be used as the light's forward direction
     * when synchronizing Spatial to Light.
     * @param axisDirection The direction along the chosen axis.
     * @throws IllegalArgumentException if the light type is not supported
     * (only Point, Directional, and Spot lights are supported).
     */
    public LightControl(Light light, Axis axisRotation, Direction axisDirection) {
        validateSupportedLightType(light);
        this.light = light;
        this.axisRotation = axisRotation;
        this.axisDirection = axisDirection;
    }

    public Light getLight() {
        return light;
    }

    public void setLight(Light light) {
        validateSupportedLightType(light);
        this.light = light;
    }

    public ControlDirection getControlDir() {
        return controlDir;
    }

    public void setControlDir(ControlDirection controlDir) {
        this.controlDir = controlDir;
    }

    public Axis getAxisRotation() {
        return axisRotation;
    }

    public void setAxisRotation(Axis axisRotation) {
        this.axisRotation = axisRotation;
    }

    public Direction getAxisDirection() {
        return axisDirection;
    }

    public void setAxisDirection(Direction axisDirection) {
        this.axisDirection = axisDirection;
    }

    private void validateSupportedLightType(Light light) {
        switch (light.getType()) {
            case Point:
            case Directional:
            case Spot:
                // These types are supported, validation passes.
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported Light type: " + light.getType());
        }
    }

    @Override
    protected void controlUpdate(float tpf) {
        switch (controlDir) {
            case SpatialToLight:
                spatialToLight(light);
                break;
            case LightToSpatial:
                lightToSpatial(light);
                break;
        }
    }

    /**
     * Updates the light's position and/or direction to match the spatial's
     * world transformation.
     *
     * @param light The light whose properties will be set.
     */
    private void spatialToLight(Light light) {
        TempVars vars = TempVars.get();

        final Vector3f worldPosition = vars.vect1;
        worldPosition.set(spatial.getWorldTranslation());

        final Vector3f lightDirection = vars.vect2;
        spatial.getWorldRotation().getRotationColumn(axisRotation.ordinal(), lightDirection);
        if (axisDirection == Direction.Negative) {
            lightDirection.negateLocal();
        }

        if (light instanceof PointLight) {
            ((PointLight) light).setPosition(worldPosition);

        } else if (light instanceof DirectionalLight) {
            ((DirectionalLight) light).setDirection(lightDirection);

        } else if (light instanceof SpotLight) {
            SpotLight sl = (SpotLight) light;
            sl.setPosition(worldPosition);
            sl.setDirection(lightDirection);
        }
        vars.release();
    }

    /**
     * Updates the spatial's local transformation (position and/or rotation)
     * to match the light's world transformation.
     *
     * @param light The light from which properties will be read.
     */
    private void lightToSpatial(Light light) {
        TempVars vars = TempVars.get();
        Vector3f translation = vars.vect1;
        Vector3f direction = vars.vect2;
        Quaternion rotation = vars.quat1;
        boolean rotateSpatial = false;
        boolean translateSpatial = false;

        if (light instanceof PointLight) {
            PointLight pl = (PointLight) light;
            translation.set(pl.getPosition());
            translateSpatial = true;

        } else if (light instanceof DirectionalLight) {
            DirectionalLight dl = (DirectionalLight) light;
            direction.set(dl.getDirection()).negateLocal();
            rotateSpatial = true;

        } else if (light instanceof SpotLight) {
            SpotLight sl = (SpotLight) light;
            translation.set(sl.getPosition());
            direction.set(sl.getDirection()).negateLocal();
            translateSpatial = true;
            rotateSpatial = true;
        }

        // Transform light's world properties to spatial's parent's local space
        if (spatial.getParent() != null) {
            // Get inverse of parent's world matrix
            spatial.getParent().getLocalToWorldMatrix(vars.tempMat4).invertLocal();
            vars.tempMat4.rotateVect(translation);
            vars.tempMat4.translateVect(translation);
            vars.tempMat4.rotateVect(direction);
        }

        // Apply transformed properties to spatial's local transformation
        if (rotateSpatial) {
            rotation.lookAt(direction, Vector3f.UNIT_Y).normalizeLocal();
            spatial.setLocalRotation(rotation);
        }
        if (translateSpatial) {
            spatial.setLocalTranslation(translation);
        }
        vars.release();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing to do
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        super.cloneFields(cloner, original);
        light = cloner.clone(light);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        controlDir = ic.readEnum("controlDir", ControlDirection.class, ControlDirection.SpatialToLight);
        light = (Light) ic.readSavable("light", null);
        axisRotation = ic.readEnum("axisRotation", Axis.class, Axis.Z);
        axisDirection = ic.readEnum("axisDirection", Direction.class, Direction.Positive);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(controlDir, "controlDir", ControlDirection.SpatialToLight);
        oc.write(light, "light", null);
        oc.write(axisRotation, "axisRotation", Axis.Z);
        oc.write(axisDirection, "axisDirection", Direction.Positive);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "[light=" + light +
                ", controlDir=" + controlDir +
                ", axisRotation=" + axisRotation +
                ", axisDirection=" + axisDirection +
                ", enabled=" + enabled +
                ", spatial=" + spatial +
                "]";
    }
}
