package com.jme3.anim;

import com.jme3.anim.util.JointModelTransform;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;

/**
 * This JointModelTransform implementation accumulates model transform in a Transform class
 * This does NOT support proper nonuniform scale in the armature hierarchy.
 * But the effect might be useful in some circumstances.
 * Note that this is how the old animation system was working, so you might want to use this
 * if your model has nonuniform scale and was migrated from an old j3o model.
 */
public class SeparateJointModelTransform implements JointModelTransform {

    final private Transform modelTransform = new Transform();

    @Override
    public void updateModelTransform(Transform localTransform, Joint parent) {
        modelTransform.set(localTransform);
        if (parent != null) {
            modelTransform.combineWithParent(parent.getModelTransform());
        }
    }

    @Override
    public void getOffsetTransform(Matrix4f outTransform, Matrix4f inverseModelBindMatrix) {
        modelTransform.toTransformMatrix(outTransform).mult(inverseModelBindMatrix, outTransform);
    }

    @Override
    public void applyBindPose(Transform localTransform, Matrix4f inverseModelBindMatrix, Joint parent) {
        localTransform.fromTransformMatrix(inverseModelBindMatrix.invert());
        if (parent != null) {
            localTransform.combineWithParent(parent.getModelTransform().invert());
        }
    }

    @Override
    public Transform getModelTransform() {
        return modelTransform;
    }

}
