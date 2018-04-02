package com.jme3.anim;

/**
 * Created by Nehon
 * An AnimationMask is defining a subset of elements on which an animation will be applied.
 * Most used implementation is the ArmatureMask that defines a subset of joints in an Armature.
 */
public interface AnimationMask {

    boolean contains(Object target);

}