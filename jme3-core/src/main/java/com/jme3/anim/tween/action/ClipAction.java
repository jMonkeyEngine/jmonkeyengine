/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.anim.tween.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.jme3.anim.AnimClip;
import com.jme3.anim.AnimTrack;
import com.jme3.anim.MorphTrack;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.action.BlendableAction;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;
import com.jme3.scene.Geometry;
import com.jme3.util.clone.Cloner;

public class ClipAction extends BlendableAction {
    
    private AnimClip clip;
    private Transform transform = new Transform();

    public ClipAction(AnimClip clip) {
        this.clip = clip;
        setLength(clip.getLength());
    }

    @Override
    public void doInterpolate(double t) {
        AnimTrack[] tracks = clip.getTracks();
        for (AnimTrack track : tracks) {
            if (track instanceof TransformTrack) {
                TransformTrack tt = (TransformTrack) track;
                if (getMask() != null && !getMask().contains(tt.getTarget())) {
                    continue;
                }
                interpolateTransformTrack(t, tt);
            } else if (track instanceof MorphTrack) {
                interpolateMorphTrack(t, (MorphTrack) track);
            }
        }
    }

    private void interpolateTransformTrack(double t, TransformTrack track) {
        HasLocalTransform target = track.getTarget();
        transform.set(target.getLocalTransform());
        track.getDataAtTime(t, transform);

        if (collectTransformDelegate != null) {
            collectTransformDelegate.collectTransform(target, transform, getWeight(), this);
        } else {
            this.collectTransform(target, transform, getTransitionWeight(), this);
        }
    }

    private void interpolateMorphTrack(double t, MorphTrack track) {
        Geometry target = track.getTarget();
        float[] weights = target.getMorphState();
        track.getDataAtTime(t, weights);
        target.setMorphState(weights);

//        if (collectTransformDelegate != null) {
//            collectTransformDelegate.collectTransform(target, transform, getWeight(), this);
//        } else {
//            this.collectTransform(target, transform, getTransitionWeight(), this);
//        }
    }

    public AnimClip getAnimClip() {
        return clip;
    }

    @Override
    public String toString() {
        return clip.toString();
    }

    @Override
    public Collection<HasLocalTransform> getTargets() {
        List<HasLocalTransform> targets = new ArrayList<>(clip.getTracks().length);
        for (AnimTrack track : clip.getTracks()) {
            if (track instanceof TransformTrack) {
                targets.add(((TransformTrack) track).getTarget());
            }
        }
        return targets;
    }

    @Override
    public void collectTransform(HasLocalTransform target, Transform t, float weight, BlendableAction source) {
        if (weight == 1f) {
            target.setLocalTransform(t);
        } else {
            Transform tr = target.getLocalTransform();
            tr.interpolateTransforms(tr, t, weight);
            target.setLocalTransform(tr);
        }
    }

    /**
     * Create a shallow clone for the JME cloner.
     *
     * @return a new action (not null)
     */
    @Override
    public ClipAction jmeClone() {
        try {
            ClipAction clone = (ClipAction) super.clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned action into a deep-cloned one, using the specified cloner
     * and original to resolve copied fields.
     *
     * @param cloner the cloner that's cloning this action (not null)
     * @param original the action from which this action was shallow-cloned
     * (unused)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        super.cloneFields(cloner, original);
        clip = cloner.clone(clip);
        transform = cloner.clone(transform);
    }
}
