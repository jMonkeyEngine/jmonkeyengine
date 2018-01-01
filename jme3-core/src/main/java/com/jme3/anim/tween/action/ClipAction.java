package com.jme3.anim.tween.action;

import com.jme3.anim.AnimClip;
import com.jme3.anim.TransformTrack;
import com.jme3.anim.tween.AbstractTween;
import com.jme3.anim.util.HasLocalTransform;
import com.jme3.math.Transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClipAction extends BlendableAction {

    private AnimClip clip;
    private Transform transform = new Transform();

    public ClipAction(AnimClip clip) {
        this.clip = clip;
        length = clip.getLength();
    }

    @Override
    public void doInterpolate(double t) {
        TransformTrack[] tracks = clip.getTracks();
        for (TransformTrack track : tracks) {
            HasLocalTransform target = track.getTarget();
            transform.set(target.getLocalTransform());
            track.getTransformAtTime(t, transform);

            if (collectTransformDelegate != null) {
                collectTransformDelegate.collectTransform(target, transform, weight, this);
            } else {
                this.collectTransform(target, transform, getTransitionWeight(), this);
            }
        }
    }

    public void reset() {

    }

    @Override
    public Collection<HasLocalTransform> getTargets() {
        List<HasLocalTransform> targets = new ArrayList<>(clip.getTracks().length);
        for (TransformTrack track : clip.getTracks()) {
            targets.add(track.getTarget());
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


}
