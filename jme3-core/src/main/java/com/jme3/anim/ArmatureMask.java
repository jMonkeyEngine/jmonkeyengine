/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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

import java.util.BitSet;

public class ArmatureMask implements AnimationMask {

    private BitSet affectedJoints = new BitSet();

    @Override
    public boolean contains(Object target) {
        return affectedJoints.get(((Joint) target).getId());
    }

    public static ArmatureMask createMask(Armature armature, String fromJoint) {
        ArmatureMask mask = new ArmatureMask();
        mask.addFromJoint(armature, fromJoint);
        return mask;
    }

    public static ArmatureMask createMask(Armature armature, String... joints) {
        ArmatureMask mask = new ArmatureMask();
        mask.addBones(armature, joints);
        for (String joint : joints) {
            mask.affectedJoints.set(armature.getJoint(joint).getId());
        }
        return mask;
    }

    /**
     * Add joints to be influenced by this animation mask.
     */
    public void addBones(Armature armature, String... jointNames) {
        for (String jointName : jointNames) {
            Joint joint = findJoint(armature, jointName);
            affectedJoints.set(joint.getId());
        }
    }

    private Joint findJoint(Armature armature, String jointName) {
        Joint joint = armature.getJoint(jointName);
        if (joint == null) {
            throw new IllegalArgumentException("Cannot find joint " + jointName);
        }
        return joint;
    }

    /**
     * Add a joint and all its sub armature joints to be influenced by this animation mask.
     */
    public void addFromJoint(Armature armature, String jointName) {
        Joint joint = findJoint(armature, jointName);
        recurseAddJoint(joint);
    }

    private void recurseAddJoint(Joint joint) {
        affectedJoints.set(joint.getId());
        for (Joint j : joint.getChildren()) {
            recurseAddJoint(j);
        }
    }

}
