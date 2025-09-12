/*
 * Copyright (c) 2019-2021 jMonkeyEngine
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
package com.jme3.jbullet.test;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.KinematicRagdollControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The Test Suite to prevent regressions from previously fixed Bullet issues
 * @author Stephen Gold &lt;sgold@sonic.net&gt;
 */
public class PreventBulletIssueRegressions {
    /**
     * Test case for JME issue #889: disabled physics control gets added to a
     * physics space.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testIssue889() throws IllegalAccessException, NoSuchFieldException {
        // throws are added so that we don't have to catch them just to assert them again.
        // If they throw, the Unit Test should fail
        Field f1 = PhysicsSpace.class.getDeclaredField("tickListeners");
        f1.setAccessible(true);
        Field f2 = BetterCharacterControl.class.getDeclaredField("rigidBody");
        f2.setAccessible(true);

        BulletAppState bulletAppState = new BulletAppState();
        bulletAppState.setSpeed(0f);
        bulletAppState.startPhysics(); // so that we don't need an AppStateManager etc
        PhysicsSpace space = bulletAppState.getPhysicsSpace();
        ConcurrentLinkedQueue<PhysicsTickListener> tickListeners = (ConcurrentLinkedQueue<PhysicsTickListener>)f1.get(space);

        float radius = 1f;
        CollisionShape sphere = new SphereCollisionShape(radius);
        CollisionShape box = new BoxCollisionShape(Vector3f.UNIT_XYZ);

        Node rootNode = new Node("RootNode");

        RigidBodyControl rbc = new RigidBodyControl(box);
        rbc.setEnabled(false);
        rbc.setPhysicsSpace(space);
        rootNode.addControl(rbc);

        BetterCharacterControl bcc = new BetterCharacterControl(radius, 4f, 1f);
        bcc.setEnabled(false);
        bcc.setPhysicsSpace(space);
        rootNode.addControl(bcc);
        PhysicsRigidBody bcc_rb = (PhysicsRigidBody)f2.get(bcc);

        GhostControl gc = new GhostControl(sphere);
        gc.setEnabled(false);
        gc.setPhysicsSpace(space);
        rootNode.addControl(gc);

        Assert.assertFalse(space.getRigidBodyList().contains(rbc));
        Assert.assertFalse(tickListeners.contains(bcc));
        Assert.assertFalse(space.getRigidBodyList().contains(bcc_rb));
        Assert.assertFalse(space.getGhostObjectList().contains(gc));
    }

    /**
     * Test case for JME issue #931: RagdollUtils can miss model meshes or use the
     * non-animated ones.
     */
    @Test
    public void testIssue931() {
        Node sinbad = (Node)new DesktopAssetManager(true).loadModel("Models/Sinbad/SinbadOldAnim.j3o");
        Node extender = new Node();
        for (Spatial child : sinbad.getChildren()) {
            extender.attachChild(child);
        }
        sinbad.attachChild(extender);

        //Note: PhysicsRagdollControl is still a WIP, constructor will change
        KinematicRagdollControl ragdoll = new KinematicRagdollControl(0.5f);
        sinbad.addControl(ragdoll);
    }

    /**
     * Test case for JME issue #970: RigidBodyControl doesn't read/write velocities.
     * Clone a body that implements Control by saving and then loading it.
     * */
    @Test
    public void testIssue970() throws IOException {
        // throws are added so that we don't have to catch them just to assert them again.
        // If they throw, the Unit Test should fail
        CollisionShape shape = new SphereCollisionShape(1f);
        RigidBodyControl rbc = new RigidBodyControl(shape, 1f);
        rbc.setAngularVelocity(new Vector3f(0.04f, 0.05f, 0.06f));
        rbc.setLinearVelocity(new Vector3f(0.26f, 0.27f, 0.28f));

        Assert.assertEquals(new Vector3f(0.04f, 0.05f, 0.06f), rbc.getAngularVelocity());
        Assert.assertEquals(new Vector3f(0.26f, 0.27f, 0.28f), rbc.getLinearVelocity());

        // Write/Serialize the RBC
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JmeExporter exporter = BinaryExporter.getInstance();
        exporter.save(rbc, baos);

        // Load/Deserialize the RBC
        JmeImporter importer = BinaryImporter.getInstance();
        RigidBodyControl rbcCopy = (RigidBodyControl)importer.load(new AssetInfo(null, null) {
            @Override
            public InputStream openStream() {
                return new ByteArrayInputStream(baos.toByteArray());
            }
        });

        Assert.assertNotNull(rbcCopy);
        Assert.assertEquals(new Vector3f(0.04f, 0.05f, 0.06f), rbcCopy.getAngularVelocity());
        Assert.assertEquals(new Vector3f(0.26f, 0.27f, 0.28f), rbcCopy.getLinearVelocity());
    }

    /**
     *  Test case for JME issue #1004: RagdollUtils can't handle 16-bit bone indices.
     */
    @Test
    public void testIssue1004() {
        Node sinbad = (Node)new DesktopAssetManager(true).loadModel("Models/Sinbad/SinbadOldAnim.j3o");

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

        sinbad.addControl(new KinematicRagdollControl(0.5f)); // should not throw
    }
}
