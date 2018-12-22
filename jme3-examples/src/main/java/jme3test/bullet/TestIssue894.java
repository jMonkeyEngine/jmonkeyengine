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

/**
 * Test case for JME issue #894: SliderJoint.setRestitutionOrthoLin() sets wrong
 * joint parameter. The bug existed in Native Bullet only.
 * <p>
 * If successful, no exception will be thrown during initialization.
 */
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.joints.SliderJoint;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

public class TestIssue894 extends SimpleApplication {

    public static void main(String[] args) {
        TestIssue894 app = new TestIssue894();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        float radius = 1f;
        CollisionShape sphere = new SphereCollisionShape(radius);

        PhysicsRigidBody rb1 = new PhysicsRigidBody(sphere);
        PhysicsRigidBody rb2 = new PhysicsRigidBody(sphere);
        rb2.setPhysicsLocation(new Vector3f(4f, 0f, 0f));

        SliderJoint joint = new SliderJoint(rb1, rb2,
                new Vector3f(2f, 0f, 0f),
                new Vector3f(-2f, 0f, 0f), true);

        joint.setLowerAngLimit(-0.01f);
        joint.setLowerLinLimit(-0.02f);
        joint.setUpperAngLimit(0.01f);
        joint.setUpperLinLimit(0.02f);

        joint.setDampingDirAng(0.03f);
        joint.setDampingDirLin(0.04f);
        joint.setDampingLimAng(0.05f);
        joint.setDampingLimLin(0.06f);
        joint.setDampingOrthoAng(0.07f);
        joint.setDampingOrthoLin(0.08f);

        joint.setRestitutionDirAng(0.09f);
        joint.setRestitutionDirLin(0.10f);
        joint.setRestitutionLimAng(0.11f);
        joint.setRestitutionLimLin(0.12f);
        joint.setRestitutionOrthoAng(0.13f);
        joint.setRestitutionOrthoLin(0.14f);

        joint.setSoftnessDirAng(0.15f);
        joint.setSoftnessDirLin(0.16f);
        joint.setSoftnessLimAng(0.17f);
        joint.setSoftnessLimLin(0.18f);
        joint.setSoftnessOrthoAng(0.19f);
        joint.setSoftnessOrthoLin(0.20f);

        joint.setMaxAngMotorForce(0.21f);
        joint.setMaxLinMotorForce(0.22f);

        joint.setTargetAngMotorVelocity(0.23f);
        joint.setTargetLinMotorVelocity(0.24f);

        RuntimeException e = new RuntimeException();

        if (joint.getLowerAngLimit() != -0.01f) {
            throw new RuntimeException();
        }
        if (joint.getLowerLinLimit() != -0.02f) {
            throw new RuntimeException();
        }
        if (joint.getUpperAngLimit() != 0.01f) {
            throw new RuntimeException();
        }
        if (joint.getUpperLinLimit() != 0.02f) {
            throw new RuntimeException();
        }

        if (joint.getDampingDirAng() != 0.03f) {
            throw new RuntimeException();
        }
        if (joint.getDampingDirLin() != 0.04f) {
            throw new RuntimeException();
        }
        if (joint.getDampingLimAng() != 0.05f) {
            throw new RuntimeException();
        }
        if (joint.getDampingLimLin() != 0.06f) {
            throw new RuntimeException();
        }
        if (joint.getDampingOrthoAng() != 0.07f) {
            throw new RuntimeException();
        }
        if (joint.getDampingOrthoLin() != 0.08f) {
            throw new RuntimeException();
        }

        if (joint.getRestitutionDirAng() != 0.09f) {
            throw new RuntimeException();
        }
        if (joint.getRestitutionDirLin() != 0.10f) {
            throw new RuntimeException();
        }
        if (joint.getRestitutionLimAng() != 0.11f) {
            throw new RuntimeException();
        }
        if (joint.getRestitutionLimLin() != 0.12f) {
            throw new RuntimeException();
        }
        if (joint.getRestitutionOrthoAng() != 0.13f) {
            throw new RuntimeException();
        }
        if (joint.getRestitutionOrthoLin() != 0.14f) {
            throw new RuntimeException();
        }

        if (joint.getSoftnessDirAng() != 0.15f) {
            throw new RuntimeException();
        }
        if (joint.getSoftnessDirLin() != 0.16f) {
            throw new RuntimeException();
        }
        if (joint.getSoftnessLimAng() != 0.17f) {
            throw new RuntimeException();
        }
        if (joint.getSoftnessLimLin() != 0.18f) {
            throw new RuntimeException();
        }
        if (joint.getSoftnessOrthoAng() != 0.19f) {
            throw new RuntimeException();
        }
        if (joint.getSoftnessOrthoLin() != 0.20f) {
            throw new RuntimeException();
        }

        if (joint.getMaxAngMotorForce() != 0.21f) {
            throw new RuntimeException();
        }
        if (joint.getMaxLinMotorForce() != 0.22f) {
            throw new RuntimeException();
        }

        if (joint.getTargetAngMotorVelocity() != 0.23f) {
            throw new RuntimeException();
        }
        if (joint.getTargetLinMotorVelocity() != 0.24f) {
            throw new RuntimeException();
        }
    }
}
