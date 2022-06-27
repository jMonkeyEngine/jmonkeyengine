package com.jme3.anim;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An AnimationMask to select joints from a single Armature.
 * 
 * @author capdevon
 */
public class AvatarMask implements AnimationMask {

    private static final Logger logger = Logger.getLogger(AvatarMask.class.getName());

    private final BitSet affectedJoints;
    private final Armature armature;

    /**
     * Instantiate a mask that affects no joints.
     *
     * @param armature
     */
    public AvatarMask(Armature armature) {
        this.armature = armature;
        this.affectedJoints = new BitSet(armature.getJointCount());
        logger.log(Level.INFO, "Joint count: {0}", armature.getJointCount());
    }

    /**
     * Add all the bones of the model's armature to be influenced by this
     * animation mask.
     *
     * @return AvatarMask
     */
    public AvatarMask addAllJoints() {
        int numJoints = armature.getJointCount();
        affectedJoints.set(0, numJoints);
        return this;
    }

    /**
     * Add joints to be influenced by this animation mask.
     *
     * @param jointNames
     * @return AvatarMask
     */
    public AvatarMask addJoints(String...jointNames) {
        for (String jointName: jointNames) {
            Joint joint = findJoint(jointName);
            affectedJoints.set(joint.getId());
        }
        return this;
    }

    private Joint findJoint(String jointName) {
        Joint joint = armature.getJoint(jointName);
        if (joint == null) {
            throw new IllegalArgumentException("Cannot find joint " + jointName);
        }
        return joint;
    }

    /**
     * Add a joint and all its sub armature joints to be influenced by this
     * animation mask.
     *
     * @param jointName the starting point (may be null, unaffected)
     * @return AvatarMask
     */
    public AvatarMask addFromJoint(String jointName) {
        Joint joint = findJoint(jointName);
        addFromJoint(joint);
        return this;
    }

    private void addFromJoint(Joint joint) {
        affectedJoints.set(joint.getId());
        for (Joint j: joint.getChildren()) {
            addFromJoint(j);
        }
    }

    /**
     * Remove a joint and all its sub armature joints to be influenced by this
     * animation mask.
     *
     * @param jointName the starting point (may be null, unaffected)
     * @return AvatarMask
     */
    public AvatarMask removeFromJoint(String jointName) {
        Joint joint = findJoint(jointName);
        removeFromJoint(joint);
        return this;
    }

    private void removeFromJoint(Joint joint) {
        affectedJoints.clear(joint.getId());
        for (Joint j: joint.getChildren()) {
            removeFromJoint(j);
        }
    }

    /**
     * Add the specified Joint and all its ancestors.
     *
     * @param jointName the starting point (may be null, unaffected)
     * @return AvatarMask
     */
    public AvatarMask addAncestors(String jointName) {
        Joint joint = findJoint(jointName);
        addAncestors(joint);
        return this;
    }

    private void addAncestors(Joint start) {
        for (Joint joint = start; joint != null; joint = joint.getParent()) {
            affectedJoints.set(joint.getId());
        }
    }

    /**
     * Remove the specified Joint and all its ancestors.
     *
     * @param jointName the starting point (may be null, unaffected)
     * @return AvatarMask
     */
    public AvatarMask removeAncestors(String jointName) {
        Joint joint = findJoint(jointName);
        removeAncestors(joint);
        return this;
    }

    private void removeAncestors(Joint start) {
        for (Joint joint = start; joint != null; joint = joint.getParent()) {
            affectedJoints.clear(joint.getId());
        }
    }

    /**
     * Remove the named joints.
     *
     * @param jointNames the names of the joints to be removed
     * @return AvatarMask
     */
    public AvatarMask removeJoints(String...jointNames) {
        for (String jointName: jointNames) {
            Joint joint = findJoint(jointName);
            affectedJoints.clear(joint.getId());
        }

        return this;
    }
    
    /**
     * Get the list of joints affected by this animation mask.
     *
     * @return
     */
    public List<Joint> getAffectedJoints() {
        List<Joint> lst = new ArrayList<>();
        for (Joint joint : armature.getJointList()) {
            if (contains(joint)) {
                lst.add(joint);
            }
        }
        return lst;
    }

    @Override
    public boolean contains(Object target) {
        Joint joint = (Joint) target;
        return affectedJoints.get(joint.getId());
    }

}
