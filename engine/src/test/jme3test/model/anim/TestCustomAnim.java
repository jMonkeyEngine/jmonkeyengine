/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.shape.Box;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class TestCustomAnim extends SimpleApplication {

    private Bone bone;
    private Skeleton skeleton;
    private Quaternion rotation = new Quaternion();

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

        // Setup bone weight buffer
        FloatBuffer weights = FloatBuffer.allocate( box.getVertexCount() * 4 );
        VertexBuffer weightsBuf = new VertexBuffer(Type.BoneWeight);
        weightsBuf.setupData(Usage.CpuOnly, 4, Format.Float, weights);
        box.setBuffer(weightsBuf);

        // Setup bone index buffer
        ByteBuffer indices = ByteBuffer.allocate( box.getVertexCount() * 4 );
        VertexBuffer indicesBuf = new VertexBuffer(Type.BoneIndex);
        indicesBuf.setupData(Usage.CpuOnly, 4, Format.UnsignedByte, indices);
        box.setBuffer(indicesBuf);

        // Create bind pose buffers
        box.generateBindPose(true);

        // Create skeleton
        bone = new Bone("root");
        bone.setBindTransforms(Vector3f.ZERO, Quaternion.IDENTITY, Vector3f.UNIT_XYZ);
        bone.setUserControl(true);
        skeleton = new Skeleton(new Bone[]{ bone });

        // Assign all verticies to bone 0 with weight 1
        for (int i = 0; i < box.getVertexCount() * 4; i += 4){
            // assign vertex to bone index 0
            indices.array()[i+0] = 0;
            indices.array()[i+1] = 0;
            indices.array()[i+2] = 0;
            indices.array()[i+3] = 0;

            // set weight to 1 only for first entry
            weights.array()[i+0] = 1;
            weights.array()[i+1] = 0;
            weights.array()[i+2] = 0;
            weights.array()[i+3] = 0;
        }

        // Maximum number of weights per bone is 1
        box.setMaxNumWeights(1);

        // Create model
        Geometry geom = new Geometry("box", box);
        geom.setMaterial(assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m"));
        Node model = new Node("model");
        model.attachChild(geom);

        // Create skeleton control
        SkeletonControl skeletonControl = new SkeletonControl(skeleton);
        model.addControl(skeletonControl);

        rootNode.attachChild(model);
    }

    @Override
    public void simpleUpdate(float tpf){
        // Rotate around X axis
        Quaternion rotate = new Quaternion();
        rotate.fromAngleAxis(tpf, Vector3f.UNIT_X);

        // Combine rotation with previous
        rotation.multLocal(rotate);

        // Set new rotation into bone
        bone.setUserTransforms(Vector3f.ZERO, rotation, Vector3f.UNIT_XYZ);

        // After changing skeleton transforms, must update world data
        skeleton.updateWorldVectors();
    }

}
