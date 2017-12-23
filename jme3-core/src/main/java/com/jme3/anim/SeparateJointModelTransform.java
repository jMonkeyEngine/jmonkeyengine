package com.jme3.anim;

import com.jme3.anim.util.JointModelTransform;
import com.jme3.export.*;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * This JointModelTransform implementation accumulates model transform in a Transform class
 * This does NOT support proper non uniform scale in the armature hierarchy.
 * But the effect might be useful in some circumstances.
 * Note that this is how the old animation system was working, so you might want to use this
 * if your model has non uniform scale and was migrated from old j3o model.
 */
public class SeparateJointModelTransform implements JointModelTransform {

    private Transform modelTransform = new Transform();

    @Override
    public void updateModelTransform(Transform localTransform, Joint parent) {
        modelTransform.set(localTransform);
        if (parent != null) {
            modelTransform.combineWithParent(parent.getModelTransform());
        }
    }

    public void getOffsetTransform(Matrix4f outTransform, Matrix4f inverseModelBindMatrix) {
        modelTransform.toTransformMatrix(outTransform).mult(inverseModelBindMatrix, outTransform);
    }

    @Override
    public void applyBindPose(Transform localTransform, Matrix4f inverseModelBindMatrix, Joint parent) {
        localTransform.fromTransformMatrix(inverseModelBindMatrix);
        localTransform.invert(); //model transform
        if (parent != null) {
            localTransform.combineWithParent(parent.getModelTransform().invert());
        }
    }

    @Override
    public Transform getModelTransform() {
        return modelTransform;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(modelTransform, "modelTransform", new Transform());
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        modelTransform = (Transform) ic.readSavable("modelTransform", new Transform());
    }

    @Override
    public Object jmeClone() {
        try {
            SeparateJointModelTransform clone = (SeparateJointModelTransform) super.clone();
            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
        modelTransform = modelTransform.clone();
    }
}
