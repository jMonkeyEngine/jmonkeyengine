package com.jme3.anim.util;

import com.jme3.anim.Joint;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;

/**
 * Implementations of this interface holds accumulated model transform of a Joint.
 * Implementation might choose different accumulation strategy.
 */
public interface JointModelTransform {

    /**
     * Update the joint's transform in model space.
     *
     * @param localTransform the joint's local transform (not null, unaffected)
     * @param parent the joint's parent, or null for a root joint
     */
    void updateModelTransform(Transform localTransform, Joint parent);

    /**
     * Determine the joint's skinning transform.
     *
     * @param outTransform storage for the result (modified if not null)
     * @param inverseModelBindMatrix the joint's inverse model bind matrix (not
     * null, unaffected)
     */
    void getOffsetTransform(Matrix4f outTransform, Matrix4f inverseModelBindMatrix);

    /**
     * Configure joint's local transform for bind pose.
     *
     * @param localTransform the joint's local transform (not null, unaffected)
     * @param inverseModelBindMatrix the joint's inverse model bind matrix (not
     * null, unaffected)
     * @param parent the joint's parent, or null for a root joint
     */
    void applyBindPose(Transform localTransform, Matrix4f inverseModelBindMatrix, Joint parent);

    /**
     * Determine the joint's transform in model space.
     *
     * @return a new instance or a pre-existing one
     */
    Transform getModelTransform();
}
