/*
 * Copyright (c) 2023 jMonkeyEngine
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
import com.jme3.scene.control.UpdateControl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests selected methods of the Spatial class.
 *
 * @author Stephen Gold
 */
public class SpatialTest {

    /**
     * Tests addControlAt() with a duplicate Control.
     */
    @Test
    public void addControlAtDuplicate() {
        Spatial testSpatial = new Node("testSpatial");
        UpdateControl control1 = new UpdateControl();
        testSpatial.addControlAt(0, control1);
        assertThrows(IllegalStateException.class, () -> testSpatial.addControlAt(1, control1));
    }

    /**
     * Tests addControlAt() with a negative index.
     */
    @Test
    public void addControlAtNegativeIndex() {
        Spatial testSpatial = new Node("testSpatial");
        UpdateControl control1 = new UpdateControl();
        assertThrows(IndexOutOfBoundsException.class, () -> testSpatial.addControlAt(-1, control1));
    }

    /**
     * Tests addControlAt() with a null argument.
     */
    @Test
    public void addControlAtNullControl() {
        Spatial testSpatial = new Node("testSpatial");
        assertThrows(IllegalArgumentException.class, () -> testSpatial.addControlAt(0, null));
    }

    /**
     * Tests addControlAt() with an out-of-range positive index.
     */
    @Test
    public void addControlAtOutOfRange() {
        Spatial testSpatial = new Node("testSpatial");
        UpdateControl control1 = new UpdateControl();
        assertThrows(IndexOutOfBoundsException.class, () -> testSpatial.addControlAt(1, control1));
    }

    /**
     * Tests typical uses of addControlAt().
     */
    @Test
    public void testAddControlAt() {
        Spatial testSpatial = new Node("testSpatial");

        // Add to an empty list.
        UpdateControl control1 = new UpdateControl();
        testSpatial.addControlAt(0, control1);

        assertEquals(1, testSpatial.getNumControls());
        assertEquals(control1, testSpatial.getControl(0));
        assertEquals(testSpatial, control1.getSpatial());

        // Add at the end of a non-empty list.
        UpdateControl control2 = new UpdateControl();
        testSpatial.addControlAt(1, control2);

        assertEquals(2, testSpatial.getNumControls());
        assertEquals(control1, testSpatial.getControl(0));
        assertEquals(control2, testSpatial.getControl(1));
        assertEquals(testSpatial, control1.getSpatial());
        assertEquals(testSpatial, control2.getSpatial());

        // Add at the beginning of a non-empty list.
        UpdateControl control0 = new UpdateControl();
        testSpatial.addControlAt(0, control0);

        assertEquals(3, testSpatial.getNumControls());
        assertEquals(control0, testSpatial.getControl(0));
        assertEquals(control1, testSpatial.getControl(1));
        assertEquals(control2, testSpatial.getControl(2));
        assertEquals(testSpatial, control0.getSpatial());
        assertEquals(testSpatial, control1.getSpatial());
        assertEquals(testSpatial, control2.getSpatial());
    }

    @Test
    public void testTransferToOtherNode(){
        Node nodeA = new Node("nodeA");
        Node nodeB = new Node("nodeB");
        Node testNode=new Node("testNode");
        nodeA.setLocalTranslation(-1,0,0);
        nodeB.setLocalTranslation(1,0,0);
        nodeB.rotate(0,90* FastMath.DEG_TO_RAD,0);
        testNode.setLocalTranslation(1,0,0);
        nodeA.attachChild(testNode);
        Vector3f worldTranslation = testNode.getWorldTranslation().clone();
        Quaternion worldRotation = testNode.getWorldRotation().clone();

        assertTrue(worldTranslation.isSimilar(testNode.getWorldTranslation(),1e-6f));
        assertTrue(worldRotation.isSimilar(testNode.getWorldRotation(),1e-6f));

        nodeB.attachChild(testNode);

        assertFalse(worldTranslation.isSimilar(testNode.getWorldTranslation(),1e-6f));
        assertFalse(worldRotation.isSimilar(testNode.getWorldRotation(),1e-6f));

        testNode.setLocalTranslation(nodeB.worldToLocal(worldTranslation,null));
        assertTrue(worldTranslation.isSimilar(testNode.getWorldTranslation(),1e-6f));

        testNode.setLocalRotation(nodeB.worldToLocal(worldRotation,null));
        System.out.println(testNode.getWorldRotation());
        assertTrue(worldRotation.isSimilar(testNode.getWorldRotation(),1e-6f));
    }
}
