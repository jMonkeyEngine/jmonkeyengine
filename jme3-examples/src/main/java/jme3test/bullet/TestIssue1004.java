/*
 * Copyright (c) 2019 jMonkeyEngine
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
package jme3test.bullet;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import java.nio.ByteBuffer;

/**
 * Test case for JME issue #1004: RagdollUtils can't handle 16-bit bone indices.
 * <p>
 * If successful, no exception will be thrown.
 */
public class TestIssue1004 extends SimpleApplication {
    // *************************************************************************
    // new methods exposed

    public static void main(String[] args) {
        TestIssue1004 app = new TestIssue1004();
        app.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    @Override
    public void simpleInitApp() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        String sinbadPath = "Models/Sinbad/Sinbad.mesh.xml";
        Node sinbad = (Node) assetManager.loadModel(sinbadPath);

        Geometry geometry = (Geometry) sinbad.getChild(0);
        Mesh mesh = geometry.getMesh();
        VertexBuffer.Type bufferType = VertexBuffer.Type.BoneIndex;
        VertexBuffer vertexBuffer = mesh.getBuffer(bufferType);

        // Remove the existing bone-index buffer.
        mesh.getBufferList().remove(vertexBuffer);
        mesh.getBuffers().remove(bufferType.ordinal());

        // Copy the 8-bit bone indices to 16-bit indices.
        ByteBuffer oldBuffer = (ByteBuffer) vertexBuffer.getDataReadOnly();
        int numComponents = oldBuffer.limit();
        oldBuffer.rewind();
        short[] shortArray = new short[numComponents];
        for (int index = 0; oldBuffer.hasRemaining(); ++index) {
            shortArray[index] = oldBuffer.get();
        }

        // Add the 16-bit bone indices to the mesh.
        mesh.setBuffer(bufferType, 4, shortArray);

        KinematicRagdollControl ragdoll = new KinematicRagdollControl(0.5f);
        sinbad.addControl(ragdoll);

        stop();
    }
}
