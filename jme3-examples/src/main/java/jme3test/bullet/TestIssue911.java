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
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;

/**
 * Test case for JME issue #911: PhysicsRigidBody sleeping threshold setters
 * have unexpected side effects. The bug existed in Native Bullet only.
 * <p>
 * If successful, no exception will be thrown.
 */
public class TestIssue911 extends SimpleApplication {
    // *************************************************************************
    // new methods exposed

    public static void main(String[] args) {
        TestIssue911 app = new TestIssue911();
        app.start();
    }
    // *************************************************************************
    // SimpleApplication methods

    @Override
    public void simpleInitApp() {
        CollisionShape capsule = new SphereCollisionShape(1f);
        PhysicsRigidBody body = new PhysicsRigidBody(capsule, 1f);
        assert body.getAngularSleepingThreshold() == 1f;
        assert body.getLinearSleepingThreshold() == 0.8f;

        body.setAngularSleepingThreshold(0.03f);

        assert body.getAngularSleepingThreshold() == 0.03f;
        float lst = body.getLinearSleepingThreshold();
        assert lst == 0.8f : lst; // fails, actual value is 1f

        body.setLinearSleepingThreshold(0.17f);

        float ast = body.getAngularSleepingThreshold();
        assert ast == 0.03f : ast; // fails, actual value is 1f

        stop();
    }
}
