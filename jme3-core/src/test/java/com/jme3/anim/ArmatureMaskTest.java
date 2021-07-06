/*
 * Copyright (c) 2021 jMonkeyEngine
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
package com.jme3.anim;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test constructors and modification methods of the ArmatureMask class.
 */
public class ArmatureMaskTest {

    final private Joint j0 = createJoint("j0", 0);         // leaf
    final private Joint j1 = createJoint("j1", 1);         // leaf
    final private Joint j2 = createJoint("j2", 2, j0, j1);
    final private Joint j3 = createJoint("j3", 3, j2);     // root
    final private Joint j4 = createJoint("j4", 4);         // leaf
    final private Joint j5 = createJoint("j5", 5, j4);     // root
    final private Joint j6 = createJoint("j6", 6);         // root and leaf
    final private Joint[] jointList = {j0, j1, j2, j3, j4, j5, j6};
    final private Armature arm = new Armature(jointList);

    private Joint createJoint(String name, int id, Joint... children) {
        Joint result = new Joint(name);
        result.setId(id);
        for (Joint child : children) {
            result.addChild(child);
        }
        return result;
    }

    /**
     * Test various ways to instantiate a mask that affects all joints.
     */
    @Test
    public void testMaskAll() {
        ArmatureMask[] maskArray = new ArmatureMask[5];
        maskArray[0] = new ArmatureMask(arm);
        maskArray[1] = ArmatureMask.createMask(arm,
                "j0", "j1", "j2", "j3", "j4", "j5", "j6");

        maskArray[2] = ArmatureMask.createMask(arm, "j3");
        maskArray[2].addFromJoint(arm, "j5");
        maskArray[2].addFromJoint(arm, "j6");

        maskArray[3] = ArmatureMask.createMask(arm, "j3")
                .addAncestors(j4)
                .addAncestors(j6);

        maskArray[4] = ArmatureMask.createMask(arm, "j3");
        maskArray[4].addBones(arm, "j4", "j5", "j6");

        for (ArmatureMask testMask : maskArray) {
            for (Joint testJoint : jointList) {
                Assert.assertTrue(testMask.contains(testJoint));
            }
        }
    }

    /**
     * Instantiate masks that affect no joints.
     */
    @Test
    public void testMaskNone() {
        ArmatureMask[] maskArray = new ArmatureMask[4];
        maskArray[0] = new ArmatureMask();
        maskArray[1] = ArmatureMask.createMask(arm);

        maskArray[2] = ArmatureMask.createMask(arm, "j2")
                .removeAncestors(j0)
                .removeAncestors(j1);

        maskArray[3] = ArmatureMask.createMask(arm, "j0", "j1")
                .removeJoints(arm, "j0", "j1");

        for (ArmatureMask testMask : maskArray) {
            for (Joint testJoint : jointList) {
                Assert.assertFalse(testMask.contains(testJoint));
            }
        }
    }

    /**
     * Instantiate masks that affect only j1 and j2.
     */
    @Test
    public void testMask12() {
        ArmatureMask[] maskArray = new ArmatureMask[4];
        maskArray[0] = new ArmatureMask();
        maskArray[0].addBones(arm, "j1", "j2");

        maskArray[1] = ArmatureMask.createMask(arm, "j3")
                .removeJoints(arm, "j0", "j3");

        maskArray[2] = new ArmatureMask()
                .addAncestors(j1)
                .removeAncestors(j3);

        ArmatureMask mask0 = ArmatureMask.createMask(arm, "j0");
        maskArray[3] = ArmatureMask.createMask(arm, "j2")
                .remove(mask0);

        for (ArmatureMask testMask : maskArray) {
            for (Joint testJoint : jointList) {
                if (testJoint == j1 || testJoint == j2) {
                    Assert.assertTrue(testMask.contains(testJoint));
                } else {
                    Assert.assertFalse(testMask.contains(testJoint));
                }
            }
        }
    }
}
