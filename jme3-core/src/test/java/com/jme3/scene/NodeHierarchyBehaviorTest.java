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
package com.jme3.scene;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.junit.Assert;
import org.junit.Test;

/**
 * Behavioral tests for scenegraph hierarchy and transform propagation.
 */
public class NodeHierarchyBehaviorTest {

    private static final float TOL = 1e-5f;

    @Test
    public void testAttachChildAutomaticallyReparents() {
        Node parentA = new Node("parentA");
        Node parentB = new Node("parentB");
        Node child = new Node("child");

        parentA.attachChild(child);
        Assert.assertSame(parentA, child.getParent());

        parentB.attachChild(child);

        Assert.assertSame(parentB, child.getParent());
        Assert.assertEquals(-1, parentA.getChildIndex(child));
        Assert.assertTrue(parentB.getChildIndex(child) >= 0);
    }

    @Test
    public void testWorldTransformPropagationThroughHierarchy() {
        Node root = new Node("root");
        Node child = new Node("child");

        root.setLocalTranslation(10f, 0f, 0f);
        root.setLocalScale(2f);
        root.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z));

        child.setLocalTranslation(1f, 0f, 0f);
        root.attachChild(child);

        root.updateLogicalState(0f);
        root.updateGeometricState();

        Vector3f world = child.getWorldTranslation();
        // child local (1,0,0) rotated by 90deg around Z and scaled by 2 => (0,2,0), then translated by root => (10,2,0)
        Assert.assertEquals(10f, world.x, TOL);
        Assert.assertEquals(2f, world.y, TOL);
        Assert.assertEquals(0f, world.z, TOL);
    }

    @Test
    public void testLocalToWorldAndWorldToLocalRoundTrip() {
        Node root = new Node("root");
        root.setLocalTranslation(3f, -2f, 1f);
        root.setLocalScale(new Vector3f(2f, 3f, 4f));
        root.setLocalRotation(new Quaternion().fromAngleAxis(0.3f, Vector3f.UNIT_Y));

        Node child = new Node("child");
        child.setLocalTranslation(1f, 2f, 3f);
        root.attachChild(child);

        root.updateLogicalState(0f);
        root.updateGeometricState();

        Vector3f localPoint = new Vector3f(0.25f, -0.5f, 1.75f);
        Vector3f worldPoint = child.localToWorld(localPoint, null);
        Vector3f roundTrip = child.worldToLocal(worldPoint, null);

        Assert.assertEquals(localPoint.x, roundTrip.x, TOL);
        Assert.assertEquals(localPoint.y, roundTrip.y, TOL);
        Assert.assertEquals(localPoint.z, roundTrip.z, TOL);
    }

    @Test
    public void testDetachChildPreservesLocalTransform() {
        Node parent = new Node("parent");
        Node child = new Node("child");

        parent.setLocalTranslation(5f, 0f, 0f);
        parent.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
        child.setLocalTranslation(1f, 0f, 0f);

        parent.attachChild(child);
        parent.updateLogicalState(0f);
        parent.updateGeometricState();

        Vector3f localBeforeDetach = child.getLocalTranslation().clone();
        parent.detachChild(child);

        // Detaching must keep local transform unchanged and clear parent.
        Assert.assertNull(child.getParent());
        Assert.assertEquals(localBeforeDetach.x, child.getLocalTranslation().x, TOL);
        Assert.assertEquals(localBeforeDetach.y, child.getLocalTranslation().y, TOL);
        Assert.assertEquals(localBeforeDetach.z, child.getLocalTranslation().z, TOL);
    }

    @Test
    public void testSwapChildrenAffectsTraversalOrder() {
        Node root = new Node("root");
        Node a = new Node("a");
        Node b = new Node("b");

        root.attachChild(a);
        root.attachChild(b);

        SceneGraphIterator itBefore = new SceneGraphIterator(root);
        Assert.assertEquals("root", itBefore.next().getName());
        Assert.assertEquals("a", itBefore.next().getName());

        root.swapChildren(0, 1);

        SceneGraphIterator itAfter = new SceneGraphIterator(root);
        Assert.assertEquals("root", itAfter.next().getName());
        Assert.assertEquals("b", itAfter.next().getName());
    }
}
