/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

package jme3test.model.anim;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.shape.Box;

public class TestCustomAnim extends SimpleApplication {

    private Joint bone;
    private Armature armature;
    final private Quaternion rotation = new Quaternion();

    public static void main(String[] args) {
        TestCustomAnim app = new TestCustomAnim();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        AmbientLight al = new AmbientLight();
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(Vector3f.UNIT_XYZ.negate());
        rootNode.addLight(dl);

        Box box = new Box(1, 1, 1);

        VertexBuffer weightsHW = new VertexBuffer(Type.HWBoneWeight);
        VertexBuffer indicesHW = new VertexBuffer(Type.HWBoneIndex);
        indicesHW.setUsage(Usage.CpuOnly);
        weightsHW.setUsage(Usage.CpuOnly);
        box.setBuffer(weightsHW);
        box.setBuffer(indicesHW);

        // Setup bone weight buffer
        FloatBuffer weights = FloatBuffer.allocate(box.getVertexCount() * 4);
        VertexBuffer weightsBuf = new VertexBuffer(Type.BoneWeight);
        weightsBuf.setupData(Usage.CpuOnly, 4, Format.Float, weights);
        box.setBuffer(weightsBuf);

        // Setup bone index buffer
        ByteBuffer indices = ByteBuffer.allocate(box.getVertexCount() * 4);
        VertexBuffer indicesBuf = new VertexBuffer(Type.BoneIndex);
        indicesBuf.setupData(Usage.CpuOnly, 4, Format.UnsignedByte, indices);
        box.setBuffer(indicesBuf);

        // Create bind pose buffers
        box.generateBindPose();

        // Create skeleton
        bone = new Joint("root");
        bone.setLocalTransform(new Transform(Vector3f.ZERO, Quaternion.IDENTITY, Vector3f.UNIT_XYZ));
        armature = new Armature(new Joint[] { bone });

        // Assign all vertices to bone 0 with weight 1
        for (int i = 0; i < box.getVertexCount() * 4; i += 4) {
            // assign vertex to bone index 0
            indices.array()[i + 0] = 0;
            indices.array()[i + 1] = 0;
            indices.array()[i + 2] = 0;
            indices.array()[i + 3] = 0;

            // set weight to 1 only for first entry
            weights.array()[i + 0] = 1;
            weights.array()[i + 1] = 0;
            weights.array()[i + 2] = 0;
            weights.array()[i + 3] = 0;
        }

        // Maximum number of weights per bone is 1
        box.setMaxNumWeights(1);

        // Create model
        Geometry geom = new Geometry("box", box);
        geom.setMaterial(assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m"));
        Node model = new Node("model");
        model.attachChild(geom);

        // Create skeleton control
        SkinningControl skinningControl = new SkinningControl(armature);
        model.addControl(skinningControl);

        rootNode.attachChild(model);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Rotate around X axis
        Quaternion rotate = new Quaternion();
        rotate.fromAngleAxis(tpf, Vector3f.UNIT_X);

        // Combine rotation with previous
        rotation.multLocal(rotate);

        // Set new rotation into bone
        bone.setLocalTransform(new Transform(Vector3f.ZERO, rotation, Vector3f.UNIT_XYZ));

        // After changing skeleton transforms, must update world data
        armature.update();
    }

}
