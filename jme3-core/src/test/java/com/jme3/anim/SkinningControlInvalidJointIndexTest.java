/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.anim;

import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for malformed joint indices during software skinning.
 */
public class SkinningControlInvalidJointIndexTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testUnusedOutOfRangeJointIndex(boolean includeTangents) {
        Joint root = new Joint("root");
        Armature armature = new Armature(new Joint[]{root});
        armature.saveBindPose();

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(1f, 2f, 3f));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(0f, 0f, 1f));
        if (includeTangents) {
            mesh.setBuffer(Type.Tangent, 4, BufferUtils.createFloatBuffer(1f, 0f, 0f, 1f));
        }
        mesh.setBuffer(Type.BoneIndex, 4, BufferUtils.createByteBuffer(new byte[]{0, 1, 0, 0}));
        mesh.setBuffer(Type.BoneWeight, 4, BufferUtils.createFloatBuffer(1f, 0f, 0f, 0f));
        mesh.setMaxNumWeights(2);
        mesh.generateBindPose();

        Geometry geometry = new Geometry("skinned", mesh);
        TestSkinningControl control = new TestSkinningControl(armature);
        control.setHardwareSkinningPreferred(false);
        geometry.addControl(control);

        Assertions.assertDoesNotThrow(control::renderOnce);

        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();
        Assertions.assertEquals(1f, positions.get(), 1e-6f);
        Assertions.assertEquals(2f, positions.get(), 1e-6f);
        Assertions.assertEquals(3f, positions.get(), 1e-6f);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void testOutOfRangeJointIndexWithWeightDoesNotAbortSoftwareSkinning(boolean includeTangents) {
        Joint root = new Joint("root");
        Armature armature = new Armature(new Joint[]{root});
        armature.saveBindPose();

        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(1f, 2f, 3f));
        mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(0f, 0f, 1f));
        if (includeTangents) {
            mesh.setBuffer(Type.Tangent, 4, BufferUtils.createFloatBuffer(1f, 0f, 0f, 1f));
        }
        mesh.setBuffer(Type.BoneIndex, 4, BufferUtils.createByteBuffer(new byte[]{0, 1, 0, 0}));
        mesh.setBuffer(Type.BoneWeight, 4, BufferUtils.createFloatBuffer(0.5f, 0.5f, 0f, 0f));
        mesh.setMaxNumWeights(2);
        mesh.generateBindPose();

        Geometry geometry = new Geometry("skinned", mesh);
        TestSkinningControl control = new TestSkinningControl(armature);
        control.setHardwareSkinningPreferred(false);
        geometry.addControl(control);

        Assertions.assertDoesNotThrow(control::renderOnce);

        FloatBuffer positions = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
        positions.rewind();
        Assertions.assertEquals(0.5f, positions.get(), 1e-6f);
        Assertions.assertEquals(1f, positions.get(), 1e-6f);
        Assertions.assertEquals(1.5f, positions.get(), 1e-6f);
    }

    private static class TestSkinningControl extends SkinningControl {

        TestSkinningControl(Armature armature) {
            super(armature);
        }

        void renderOnce() {
            controlRender(null, null);
        }
    }
}
