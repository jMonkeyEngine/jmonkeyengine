package com.jme3.anim;

import com.jme3.anim.util.JointModelTransform;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;

/**
 * This JointModelTransform implementation accumulate joints transforms in a Matrix4f to properly
 * support non uniform scaling in an armature hierarchy
 */
public class MatrixJointModelTransform implements JointModelTransform {

    final private Matrix4f modelTransformMatrix = new Matrix4f();
    final private Transform modelTransform = new Transform();

    @Override
    public void updateModelTransform(Transform localTransform, Joint parent) {
        localTransform.toTransformMatrix(modelTransformMatrix);
        if (parent != null) {
            ((MatrixJointModelTransform) parent.getJointModelTransform()).getModelTransformMatrix().mult(modelTransformMatrix, modelTransformMatrix);
        }

    }

    @Override
    public void getOffsetTransform(Matrix4f outTransform, Matrix4f inverseModelBindMatrix) {
        modelTransformMatrix.mult(inverseModelBindMatrix, outTransform);
    }

    @Override
    public void applyBindPose(Transform localTransform, Matrix4f inverseModelBindMatrix, Joint parent) {
        modelTransformMatrix.set(inverseModelBindMatrix).invertLocal(); // model transform = model bind
        if (parent != null) {
            ((MatrixJointModelTransform) parent.getJointModelTransform()).getModelTransformMatrix().invert().mult(modelTransformMatrix, modelTransformMatrix);
        }
        localTransform.fromTransformMatrix(modelTransformMatrix);
    }

    /**
     * Access the model transform.
     *
     * @return the pre-existing instance 
     */
    public Matrix4f getModelTransformMatrix() {
        return modelTransformMatrix;
    }

    @Override
    public Transform getModelTransform() {
        modelTransform.fromTransformMatrix(modelTransformMatrix);
        return modelTransform;
    }
}
