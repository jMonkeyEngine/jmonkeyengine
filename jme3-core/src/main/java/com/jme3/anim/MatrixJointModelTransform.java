package com.jme3.anim;

import com.jme3.anim.util.JointModelTransform;
import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * This JointModelTransform implementation accumulate joints transforms in a Matrix4f to properly
 * support non uniform scaling in an armature hierarchy
 */
public class MatrixJointModelTransform implements JointModelTransform {

    private Matrix4f modelTransformMatrix = new Matrix4f();
    private Transform modelTransform = new Transform();

    @Override
    public void updateModelTransform(Transform localTransform, Joint parent) {
        localTransform.toTransformMatrix(modelTransformMatrix);
        if (parent != null) {
            ((MatrixJointModelTransform) parent.getJointModelTransform()).getModelTransformMatrix().mult(modelTransformMatrix, modelTransformMatrix);
        }
        modelTransform.fromTransformMatrix(modelTransformMatrix);
    }

    public void getOffsetTransform(Matrix4f outTransform, Matrix4f inverseModelBindMatrix) {
        outTransform.set(modelTransformMatrix).mult(inverseModelBindMatrix, outTransform);
    }

    @Override
    public void applyBindPose(Transform localTransform, Matrix4f inverseModelBindMatrix, Joint parent) {
        modelTransformMatrix.set(inverseModelBindMatrix).invertLocal(); // model transform = model bind
        if (parent != null) {
            ((MatrixJointModelTransform) parent.getJointModelTransform()).getModelTransformMatrix().invert().mult(modelTransformMatrix, modelTransformMatrix);
        }
        localTransform.fromTransformMatrix(modelTransformMatrix);
    }

    public Matrix4f getModelTransformMatrix() {
        return modelTransformMatrix;
    }

    @Override
    public Transform getModelTransform() {
        return modelTransform;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(modelTransformMatrix, "modelTransformMatrix", new Matrix4f());
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        modelTransformMatrix = (Matrix4f) ic.readSavable("modelTransformMatrix", new Matrix4f());
        modelTransform.fromTransformMatrix(modelTransformMatrix);
    }

    @Override
    public Object jmeClone() {
        try {
            MatrixJointModelTransform clone = (MatrixJointModelTransform) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        modelTransformMatrix = modelTransformMatrix.clone();
    }
}
