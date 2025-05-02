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
package com.jme3.audio;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/**
 * Represents the audio listener in the 3D sound scene.
 * The listener defines the point of view from which sound is heard,
 * influencing spatial audio effects like panning and Doppler shift.
 */
public class Listener {

    private final Vector3f location;
    private final Vector3f velocity;
    private final Quaternion rotation;
    private float volume = 1;
    private AudioRenderer renderer;

    /**
     * Constructs a new {@code Listener} with default parameters.
     */
    public Listener() {
        location = new Vector3f();
        velocity = new Vector3f();
        rotation = new Quaternion();
    }

    /**
     * Constructs a new {@code Listener} by copying the properties of another {@code Listener}.
     *
     * @param source The {@code Listener} to copy the properties from.
     */
    public Listener(Listener source) {
        this.location = source.location.clone();
        this.velocity = source.velocity.clone();
        this.rotation = source.rotation.clone();
        this.volume = source.volume;
        this.renderer = source.renderer; // Note: Renderer is also copied
    }

    /**
     * Sets the {@link AudioRenderer} associated with this listener.
     * The renderer is responsible for applying the listener's parameters
     * to the audio output.
     *
     * @param renderer The {@link AudioRenderer} to associate with.
     */
    public void setRenderer(AudioRenderer renderer) {
        this.renderer = renderer;
    }

    /**
     * Gets the current volume of the listener.
     *
     * @return The current volume.
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Sets the volume of the listener.
     * If an {@link AudioRenderer} is set, it will be notified of the volume change.
     *
     * @param volume The new volume.
     */
    public void setVolume(float volume) {
        this.volume = volume;
        updateListenerParam(ListenerParam.Volume);
    }

    /**
     * Gets the current location of the listener in world space.
     *
     * @return The listener's location as a {@link Vector3f}.
     */
    public Vector3f getLocation() {
        return location;
    }

    /**
     * Gets the current rotation of the listener in world space.
     *
     * @return The listener's rotation as a {@link Quaternion}.
     */
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * Gets the current velocity of the listener.
     * This is used for Doppler effect calculations.
     *
     * @return The listener's velocity as a {@link Vector3f}.
     */
    public Vector3f getVelocity() {
        return velocity;
    }

    /**
     * Gets the left direction vector of the listener.
     * This vector is derived from the listener's rotation.
     *
     * @return The listener's left direction as a {@link Vector3f}.
     */
    public Vector3f getLeft() {
        return rotation.getRotationColumn(0);
    }

    /**
     * Gets the up direction vector of the listener.
     * This vector is derived from the listener's rotation.
     *
     * @return The listener's up direction as a {@link Vector3f}.
     */
    public Vector3f getUp() {
        return rotation.getRotationColumn(1);
    }

    /**
     * Gets the forward direction vector of the listener.
     * This vector is derived from the listener's rotation.
     *
     * @return The listener's forward direction.
     */
    public Vector3f getDirection() {
        return rotation.getRotationColumn(2);
    }

    /**
     * Sets the location of the listener in world space.
     * If an {@link AudioRenderer} is set, it will be notified of the position change.
     *
     * @param location The new location of the listener.
     */
    public void setLocation(Vector3f location) {
        this.location.set(location);
        updateListenerParam(ListenerParam.Position);
    }

    /**
     * Sets the rotation of the listener.
     * This defines the listener's orientation in world space.
     * If an {@link AudioRenderer} is set, it will be notified of the rotation change.
     *
     * @param rotation The new rotation of the listener.
     */
    public void setRotation(Quaternion rotation) {
        this.rotation.set(rotation);
        updateListenerParam(ListenerParam.Rotation);
    }

    /**
     * Sets the velocity of the listener.
     * This is used for Doppler effect calculations.
     * If an {@link AudioRenderer} is set, it will be notified of the velocity change.
     *
     * @param velocity The new velocity of the listener.
     */
    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
        updateListenerParam(ListenerParam.Velocity);
    }

    /**
     * Updates the associated {@link AudioRenderer} with the specified listener parameter.
     * This method checks if a renderer is set before attempting to update it.
     *
     * @param param The {@link ListenerParam} to update on the renderer.
     */
    private void updateListenerParam(ListenerParam param) {
        if (renderer != null) {
            renderer.updateListenerParam(this, param);
        }
    }
}
