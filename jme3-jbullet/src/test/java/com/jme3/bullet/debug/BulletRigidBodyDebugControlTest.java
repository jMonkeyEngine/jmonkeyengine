/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.bullet.debug;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for physics debug geometry updates.
 */
public class BulletRigidBodyDebugControlTest {

    @Test
    public void updatesDebugGeometryWhenCompoundChildIsRemoved() {
        CompoundCollisionShape compound = new CompoundCollisionShape();
        BoxCollisionShape removedShape = new BoxCollisionShape(Vector3f.UNIT_XYZ);
        BoxCollisionShape retainedShape = new BoxCollisionShape(Vector3f.UNIT_XYZ);
        compound.addChildShape(removedShape, new Vector3f(-1f, 0f, 0f));
        compound.addChildShape(retainedShape, new Vector3f(1f, 0f, 0f));

        TestDebugControl control = createControl(compound);

        Node initialDebugShape = (Node) control.geom;
        Assertions.assertEquals(2, initialDebugShape.getQuantity());

        control.updateControl();
        Assertions.assertSame(initialDebugShape, control.geom,
                "unchanged compound shapes should reuse their debug geometry");

        compound.removeChildShape(removedShape);
        control.updateControl();

        Assertions.assertNotSame(initialDebugShape, control.geom,
                "changing compound children should regenerate debug geometry");
        Assertions.assertEquals(1, ((Node) control.geom).getQuantity());
    }

    @Test
    public void updatesDebugGeometryWhenCompoundChildIsReplaced() {
        CompoundCollisionShape compound = new CompoundCollisionShape();
        BoxCollisionShape removedShape = new BoxCollisionShape(Vector3f.UNIT_XYZ);
        BoxCollisionShape retainedShape = new BoxCollisionShape(Vector3f.UNIT_XYZ);
        compound.addChildShape(removedShape, new Vector3f(-1f, 0f, 0f));
        compound.addChildShape(retainedShape, new Vector3f(1f, 0f, 0f));

        TestDebugControl control = createControl(compound);
        Node initialDebugShape = (Node) control.geom;

        compound.removeChildShape(removedShape);
        compound.addChildShape(new BoxCollisionShape(Vector3f.UNIT_XYZ),
                new Vector3f(-1f, 0f, 0f));
        control.updateControl();

        Assertions.assertNotSame(initialDebugShape, control.geom,
                "replacing a child should regenerate debug geometry even when the count "
                + "is unchanged");
        Assertions.assertEquals(2, ((Node) control.geom).getQuantity());
    }

    @Test
    public void updatesDebugGeometryWhenCompoundChildLocationChangesInPlace() {
        CompoundCollisionShape compound = createSingleChildCompound();
        TestDebugControl control = createControl(compound);
        Node initialDebugShape = (Node) control.geom;
        ChildCollisionShape child = compound.getChildren().get(0);

        child.location.x += 2f;
        control.updateControl();

        Assertions.assertNotSame(initialDebugShape, control.geom);
        Geometry debugChild = (Geometry) ((Node) control.geom).getChild(0);
        Assertions.assertEquals(child.location, debugChild.getLocalTranslation());
    }

    @Test
    public void updatesDebugGeometryWhenCompoundChildRotationChangesInPlace() {
        CompoundCollisionShape compound = createSingleChildCompound();
        TestDebugControl control = createControl(compound);
        Node initialDebugShape = (Node) control.geom;
        ChildCollisionShape child = compound.getChildren().get(0);

        child.rotation.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
        control.updateControl();

        Assertions.assertNotSame(initialDebugShape, control.geom);
    }

    @Test
    public void updatesDebugGeometryWhenCompoundChildShapeChangesInPlace() {
        CompoundCollisionShape compound = createSingleChildCompound();
        TestDebugControl control = createControl(compound);
        Node initialDebugShape = (Node) control.geom;
        Geometry initialDebugChild = (Geometry) initialDebugShape.getChild(0);
        ChildCollisionShape child = compound.getChildren().get(0);

        child.shape = new BoxCollisionShape(new Vector3f(2f, 2f, 2f));
        control.updateControl();

        Assertions.assertNotSame(initialDebugShape, control.geom);
        Geometry updatedDebugChild = (Geometry) ((Node) control.geom).getChild(0);
        Assertions.assertNotSame(initialDebugChild.getMesh(), updatedDebugChild.getMesh());
    }

    private static CompoundCollisionShape createSingleChildCompound() {
        CompoundCollisionShape compound = new CompoundCollisionShape();
        compound.addChildShape(new BoxCollisionShape(Vector3f.UNIT_XYZ),
                new Vector3f(1f, 0f, 0f));
        return compound;
    }

    private static TestDebugControl createControl(CompoundCollisionShape compound) {
        PhysicsRigidBody body = new PhysicsRigidBody(compound, 1f);
        BulletDebugAppState debugAppState = new BulletDebugAppState(new PhysicsSpace());
        debugAppState.DEBUG_BLUE = new Material();
        debugAppState.DEBUG_MAGENTA = debugAppState.DEBUG_BLUE;
        TestDebugControl control = new TestDebugControl(debugAppState, body);
        Node bodyNode = new Node("body");
        bodyNode.addControl(control);
        return control;
    }

    private static final class TestDebugControl extends BulletRigidBodyDebugControl {

        TestDebugControl(BulletDebugAppState debugAppState, PhysicsRigidBody body) {
            super(debugAppState, body);
        }

        void updateControl() {
            controlUpdate(0f);
        }
    }
}
