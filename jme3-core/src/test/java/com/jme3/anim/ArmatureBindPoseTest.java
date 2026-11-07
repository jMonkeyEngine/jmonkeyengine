/*
 * Copyright (c) 2026 jMonkeyEngine
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
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for saving and restoring an armature's bind pose (issue 2383).
 */
public class ArmatureBindPoseTest {

    @ParameterizedTest
    @ValueSource(classes = {SeparateJointModelTransform.class, MatrixJointModelTransform.class})
    public void testRepeatedBindPoseWithUnitScale(Class<? extends JointModelTransform> transformClass) {
        checkRepeatedBindPose(transformClass, new Vector3f(1f, 1f, 1f));
    }

    @ParameterizedTest
    @ValueSource(classes = {SeparateJointModelTransform.class, MatrixJointModelTransform.class})
    public void testRepeatedBindPoseWithNonUniformScale(Class<? extends JointModelTransform> transformClass) {
        checkRepeatedBindPose(transformClass, new Vector3f(0.9f, 1.1f, 1f));
    }

    @ParameterizedTest
    @ValueSource(classes = {SeparateJointModelTransform.class, MatrixJointModelTransform.class})
    public void testSavedBindPoseHasIdentitySkinning(Class<? extends JointModelTransform> transformClass) {
        Armature armature = createArmature(transformClass, new Vector3f(0.9f, 1.1f, 1f));
        armature.saveBindPose();
        for (Matrix4f skinningMatrix : armature.computeSkinningMatrices()) {
            assertMatrixEquals(Matrix4f.IDENTITY, skinningMatrix, 0.00001f);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {SeparateJointModelTransform.class, MatrixJointModelTransform.class})
    public void testSaveReplacesPreviousBindMatrix(Class<? extends JointModelTransform> transformClass) {
        Armature armature = createArmature(transformClass, new Vector3f(1f, 1f, 1f));
        // A previous inversion must not affect the affine row of the new bind matrix.
        armature.getJoint(0).getInverseModelBindMatrix().m33 = 2f;
        armature.saveBindPose();
        assertMatrixEquals(Matrix4f.IDENTITY, armature.computeSkinningMatrices()[0], 0.00001f);
    }

    private static void checkRepeatedBindPose(
            Class<? extends JointModelTransform> transformClass, Vector3f scale) {
        Armature armature = createArmature(transformClass, scale);
        Transform[] expected = new Transform[armature.getJointCount()];
        for (int i = 0; i < expected.length; ++i) {
            expected[i] = armature.getJoint(i).getLocalTransform().clone();
        }
        armature.saveBindPose();

        // Repeatedly applying and saving an unchanged pose must not distort it.
        for (int iteration = 0; iteration < 1000; ++iteration) {
            armature.applyBindPose();
            armature.saveBindPose();
        }
        for (int i = 0; i < expected.length; ++i) {
            Matrix4f actual = armature.getJoint(i).getLocalTransform().toTransformMatrix();
            assertMatrixEquals(expected[i].toTransformMatrix(), actual, 0.001f);
        }
    }

    private static Armature createArmature(
            Class<? extends JointModelTransform> transformClass, Vector3f scale) {
        Joint[] joints = new Joint[8];
        for (int i = 0; i < joints.length; ++i) {
            Joint joint = new Joint("joint" + i);
            joint.setLocalTranslation(new Vector3f(0.1f, 0.7f, -0.2f));
            joint.setLocalRotation(new Quaternion().fromAngles(0.3f, -0.7f, 1.1f));
            joint.setLocalScale(scale);
            joints[i] = joint;
            if (i > 0) {
                joints[i - 1].addChild(joint);
            }
        }
        Armature armature = new Armature(joints);
        armature.setModelTransformClass(transformClass);
        return armature;
    }

    private static void assertMatrixEquals(Matrix4f expected, Matrix4f actual, float tolerance) {
        for (int row = 0; row < 4; ++row) {
            for (int column = 0; column < 4; ++column) {
                Assertions.assertEquals(expected.get(row, column), actual.get(row, column), tolerance,
                        "matrix element [" + row + ", " + column + "]");
            }
        }
    }
}
