/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.anim;

import com.jme3.export.*;
import com.jme3.math.*;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

/**
 * Contains a list of transforms and times for each keyframe.
 *
 * @author Rémy Bouquet
 */
public final class JointTrack extends TransformTrack implements JmeCloneable, Savable {

    private Joint target;

    /**
     * Serialization-only. Do not use.
     */
    public JointTrack() {
        super();
    }

    /**
     * Creates a bone track for the given bone index
     *
     * @param target       The Joint target of this track
     * @param times        a float array with the time of each frame
     * @param translations the translation of the bone for each frame
     * @param rotations    the rotation of the bone for each frame
     * @param scales       the scale of the bone for each frame
     */
    public JointTrack(Joint target, float[] times, Vector3f[] translations, Quaternion[] rotations, Vector3f[] scales) {
        super(times, translations, rotations, scales);
        this.target = target;
    }

    @Override
    public boolean interpolate(double t) {
        setDefaultTransform(target.getLocalTransform());
        boolean running = super.interpolate(t);
        Transform transform = getInterpolatedTransform();
        target.setLocalTransform(transform);
        return running;
    }

    public void setTarget(Joint target) {
        this.target = target;
    }

    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Error cloning", e);
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        this.target = cloner.clone(target);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(target, "target", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        target = (Joint) ic.readSavable("target", null);
    }

}
