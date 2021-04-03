package com.jme3.anim;

import java.util.BitSet;

public class ArmatureMask implements AnimationMask {

    final private BitSet affectedJoints = new BitSet();

    /**
     * Instantiate a mask that affects no joints.
     */
    public ArmatureMask() {
        // do nothing
    }

    /**
     * Instantiate a mask that affects all joints in the specified Armature.
     *
     * @param armature the Armature containing the joints (not null, unaffected)
     */
    public ArmatureMask(Armature armature) {
        int numJoints = armature.getJointCount();
        affectedJoints.set(0, numJoints);
    }

    /**
     * Remove all joints affected by the specified ArmatureMask.
     *
     * @param removeMask the set of joints to remove (not null, unaffected)
     * @return this
     */
    public ArmatureMask remove(ArmatureMask removeMask) {
        BitSet removeBits = removeMask.getAffectedJoints();
        affectedJoints.andNot(removeBits);

        return this;
    }

    private BitSet getAffectedJoints() {
        return affectedJoints;
    }

    /**
     * Remove the named joints.
     *
     * @param armature the Armature containing the joints (not null, unaffected)
     * @param jointNames the names of the joints to be removed
     * @return this
     */
    public ArmatureMask removeJoints(Armature armature, String... jointNames) {
        for (String jointName : jointNames) {
            Joint joint = findJoint(armature, jointName);
            int jointId = joint.getId();
            affectedJoints.clear(jointId);
        }

        return this;
    }

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

    /**
     * Add the specified Joint and all its ancestors.
     *
     * @param start the starting point (may be null, unaffected)
     * @return this
     */
    public ArmatureMask addAncestors(Joint start) {
        for (Joint cur = start; cur != null; cur = cur.getParent()) {
            int jointId = cur.getId();
            affectedJoints.set(jointId);
        }

        return this;
    }

    /**
     * Remove the specified Joint and all its ancestors.
     *
     * @param start the starting point (may be null, unaffected)
     * @return this
     */
    public ArmatureMask removeAncestors(Joint start) {
        for (Joint cur = start; cur != null; cur = cur.getParent()) {
            int jointId = cur.getId();
            affectedJoints.clear(jointId);
        }

        return this;
    }
}
