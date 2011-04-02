/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

public class Listener {

    private Vector3f location;
    private Vector3f velocity;
    private Quaternion rotation;
    private float volume = 1;
    private AudioRenderer renderer;

    public Listener(){
        location = new Vector3f();
        velocity = new Vector3f();
        rotation = new Quaternion();
    }
    
    public Listener(Listener source){
        location = source.location.clone();
        velocity = source.velocity.clone();
        rotation = source.rotation.clone();
        volume = source.volume;
    }

    public void setRenderer(AudioRenderer renderer){
        this.renderer = renderer;
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        if (renderer != null)
            renderer.updateListenerParam(this, ListenerParam.Volume);
    }
    
    public Vector3f getLocation() {
        return location;
    }

    public Quaternion getRotation() {
        return rotation;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public Vector3f getLeft(){
        return rotation.getRotationColumn(0);
    }

    public Vector3f getUp(){
        return rotation.getRotationColumn(1);
    }

    public Vector3f getDirection(){
        return rotation.getRotationColumn(2);
    }
    
    public void setLocation(Vector3f location) {
        this.location.set(location);
        if (renderer != null)
            renderer.updateListenerParam(this, ListenerParam.Position);
    }

    public void setRotation(Quaternion rotation) {
        this.rotation.set(rotation);
        if (renderer != null)
            renderer.updateListenerParam(this, ListenerParam.Rotation);
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
        if (renderer != null)
            renderer.updateListenerParam(this, ListenerParam.Velocity);
    }
}
