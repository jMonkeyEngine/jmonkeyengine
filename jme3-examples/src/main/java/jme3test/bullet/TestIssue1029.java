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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;

/**
 * Test case for JME issue #1029: sphere-sphere collisions not reported.
 * <p>
 * If successful, the app will terminate normally, without a RuntimeException.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class TestIssue1029
        extends SimpleApplication
        implements PhysicsCollisionListener {

    private double elapsedSeconds = 0.0;

    public static void main(String[] arguments) {
        Application application = new TestIssue1029();
        application.start();
    }

    @Override
    public void simpleInitApp() {
        BulletAppState bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        bulletAppState.setDebugEnabled(true);

        PhysicsSpace physicsSpace = bulletAppState.getPhysicsSpace();
        physicsSpace.addCollisionListener(this);

        CollisionShape shape;
        shape = new SphereCollisionShape(1f);
        //shape = new BoxCollisionShape(new Vector3f(1f, 1f, 1f));

        PhysicsRigidBody staticBody = new PhysicsRigidBody(shape, 0f);
        physicsSpace.add(staticBody);

        PhysicsGhostObject ghost = new PhysicsGhostObject(shape);
        physicsSpace.add(ghost);
    }

    @Override
    public void simpleUpdate(float tpf) {
        super.simpleUpdate(tpf);

        elapsedSeconds += tpf;
        if (elapsedSeconds > 1.0) {
            throw new RuntimeException("No collisions reported!");
        }
    }

    @Override
    public void collision(PhysicsCollisionEvent event) {
        Class aClass = event.getObjectA().getCollisionShape().getClass();
        String aShape = aClass.getSimpleName().replace("CollisionShape", "");
        Class bClass = event.getObjectB().getCollisionShape().getClass();
        String bShape = bClass.getSimpleName().replace("CollisionShape", "");

        System.out.printf("%s-%s collision reported at t = %f sec%n",
                aShape, bShape, elapsedSeconds);
        stop();
    }
}
