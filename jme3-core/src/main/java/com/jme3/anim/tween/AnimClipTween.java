package com.jme3.anim.tween;

import com.jme3.anim.AnimClip;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.anim.util.Weighted;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Transform;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;

public class AnimClipTween implements Tween, Weighted, JmeCloneable {

    private AnimClip clip;
    private Transform transform = new Transform();
    private float weight = 1f;
    private Action parentAction;

    public AnimClipTween() {
    }

    public AnimClipTween(AnimClip clip) {
        this.clip = clip;
    }

    @Override
    public double getLength() {
        return clip.getLength();
    }

    @Override
    public boolean interpolate(double t) {
        // Sanity check the inputs
        if (t < 0) {
            return true;
        }
        if (parentAction != null) {
            weight = parentAction.getWeightForTween(this);
        }
        if (weight == 0) {
            //weight is 0 let's not interpolate
            return t < clip.getLength();
        }
        TransformTrack[] tracks = clip.getTracks();
        for (TransformTrack track : tracks) {
            HasLocalTransform target = track.getTarget();
            transform.set(target.getLocalTransform());
            track.getTransformAtTime(t, transform);

            if (weight == 1f) {
                target.setLocalTransform(transform);
            } else {
                Transform tr = target.getLocalTransform();
                tr.interpolateTransforms(tr, transform, weight);
                target.setLocalTransform(tr);
            }
        }
        return t < clip.getLength();
    }


    @Override
    public Object jmeClone() {
        try {
            AnimClipTween clone = (AnimClipTween) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        clip = cloner.clone(clip);
    }

    @Override
    public void setParentAction(Action action) {
        this.parentAction = action;
    }
}
