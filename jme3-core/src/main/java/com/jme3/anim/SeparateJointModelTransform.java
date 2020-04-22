/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
