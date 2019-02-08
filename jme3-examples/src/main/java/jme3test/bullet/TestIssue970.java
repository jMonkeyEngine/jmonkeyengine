/*
 * Copyright (c) 2018 jMonkeyEngine
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
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.JmeExporter;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import java.io.File;
import java.io.IOException;

/**
 * Test case for JME issue #970: RigidBodyControl doesn't read/write velocities.
 *
 * If successful, no AssertionError will be thrown.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue970 extends SimpleApplication {

    private int fileIndex = 0;

    public static void main(String[] args) {
        TestIssue970 app = new TestIssue970();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        assetManager.registerLocator(".", FileLocator.class);

        CollisionShape shape = new SphereCollisionShape(1f);
        RigidBodyControl rbc = new RigidBodyControl(shape, 1f);
        setParameters(rbc);
        verifyParameters(rbc);
        RigidBodyControl rbcCopy = (RigidBodyControl) saveThenLoad(rbc);
        verifyParameters(rbcCopy);

        stop();
    }

    /**
     * Clone a body that implements Control by saving and then loading it.
     *
     * @param sgc the body/control to copy (not null, unaffected)
     * @return a new body/control
     */
    private PhysicsRigidBody saveThenLoad(PhysicsRigidBody body) {
        Control sgc = (Control) body;
        Node savedNode = new Node();
        /*
         * Add the Control to the Node without altering its physics transform.
         */
        Vector3f pl = body.getPhysicsLocation(null);
        Matrix3f pr = body.getPhysicsRotationMatrix(null);
        savedNode.addControl(sgc);
        body.setPhysicsLocation(pl);
        body.setPhysicsRotation(pr);

        String fileName = String.format("tmp%d.j3o", ++fileIndex);
        File file = new File(fileName);

        JmeExporter exporter = BinaryExporter.getInstance();
        try {
            exporter.save(savedNode, file);
        } catch (IOException exception) {
            assert false;
        }

        ModelKey key = new ModelKey(fileName);
        Spatial loadedNode = new Node();
        try {
            loadedNode = assetManager.loadAsset(key);
        } catch (AssetNotFoundException e) {
            assert false;
        }
        file.delete();
        Control loadedSgc = loadedNode.getControl(0);

        return (PhysicsRigidBody) loadedSgc;
    }

    private void setParameters(PhysicsRigidBody body) {
        body.setAngularVelocity(new Vector3f(0.04f, 0.05f, 0.06f));
        body.setLinearVelocity(new Vector3f(0.26f, 0.27f, 0.28f));
    }

    private void verifyParameters(PhysicsRigidBody body) {
        Vector3f w = body.getAngularVelocity();
        assert w.x == 0.04f : w;
        assert w.y == 0.05f : w;
        assert w.z == 0.06f : w;

        Vector3f v = body.getLinearVelocity();
        assert v.x == 0.26f : v;
        assert v.y == 0.27f : v;
        assert v.z == 0.28f : v;
    }
}
