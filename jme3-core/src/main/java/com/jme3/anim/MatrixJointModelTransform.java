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
package com.jme3.anim;

import com.jme3.anim.util.JointModelTransform;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;

/**
 * An implementation of {@link JointModelTransform} that accumulates joint transformations
 * into a {@link Matrix4f}. This approach is particularly useful for correctly handling
 * non-uniform scaling within an armature hierarchy, as {@code Matrix4f} can represent
 * non-uniform scaling directly, unlike {@link Transform}, which typically handles
 * uniform scaling.
 * <p>
 * This class maintains a single {@link Matrix4f} to represent the accumulated
 * model-space transform of the joint it's associated with.
 */
public class MatrixJointModelTransform implements JointModelTransform {

    /**
     * The model-space transform of the joint represented as a Matrix4f.
     * This matrix accumulates the local transform of the joint and the model transform
     * of its parent.
     */
    private final Matrix4f modelTransformMatrix = new Matrix4f();
    /**
     * A temporary Transform instance used for converting the modelTransformMatrix
     * to a Transform object when {@link #getModelTransform()} is called.
     */
    private final Transform modelTransform = new Transform();

    @Override
    public void updateModelTransform(Transform localTransform, Joint parent) {
        localTransform.toTransformMatrix(modelTransformMatrix);
        if (parent != null) {
            MatrixJointModelTransform transform = (MatrixJointModelTransform) parent.getJointModelTransform();
            transform.getModelTransformMatrix().mult(modelTransformMatrix, modelTransformMatrix);
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
            MatrixJointModelTransform transform = (MatrixJointModelTransform) parent.getJointModelTransform();
            transform.getModelTransformMatrix().invert().mult(modelTransformMatrix, modelTransformMatrix);
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
