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

package com.jme3.scene.debug;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class SkeletonWire extends Mesh {

    private int numConnections = 0;
    private Skeleton skeleton;

    private void countConnections(Bone bone){
        for (Bone child : bone.getChildren()){
            numConnections ++;
            countConnections(child);
        }
    }

    private void writeConnections(ShortBuffer indexBuf, Bone bone){
        for (Bone child : bone.getChildren()){
            // write myself
            indexBuf.put( (short) skeleton.getBoneIndex(bone) );
            // write the child
            indexBuf.put( (short) skeleton.getBoneIndex(child) );

            writeConnections(indexBuf, child);
        }
    }

    public SkeletonWire(Skeleton skeleton){
        this.skeleton = skeleton;
        for (Bone bone : skeleton.getRoots())
            countConnections(bone);

        setMode(Mode.Lines);

        VertexBuffer pb = new VertexBuffer(Type.Position);
        FloatBuffer fpb = BufferUtils.createFloatBuffer(skeleton.getBoneCount() * 3);
        pb.setupData(Usage.Stream, 3, Format.Float, fpb);
        setBuffer(pb);

        VertexBuffer ib = new VertexBuffer(Type.Index);
        ShortBuffer sib = BufferUtils.createShortBuffer(numConnections * 2);
        ib.setupData(Usage.Static, 2, Format.UnsignedShort, sib);
        setBuffer(ib);

        for (Bone bone : skeleton.getRoots())
            writeConnections(sib, bone);
        sib.flip();

        updateCounts();
    }

    public void updateGeometry(){
        VertexBuffer vb = getBuffer(Type.Position);
        FloatBuffer posBuf = getFloatBuffer(Type.Position);
        posBuf.clear();
        for (int i = 0; i < skeleton.getBoneCount(); i++){
            Bone bone = skeleton.getBone(i);
            Vector3f bonePos = bone.getModelSpacePosition();

            posBuf.put(bonePos.getX()).put(bonePos.getY()).put(bonePos.getZ());
        }
        posBuf.flip();
        vb.updateData(posBuf);

        updateBound();
    }
}
