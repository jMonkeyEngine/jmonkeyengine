/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.bullet.collision;

import com.jme3.animation.Bone;

/**
 *
 * @author Nehon
 */
public interface RagdollCollisionListener {
    
    public void collide(Bone bone, PhysicsCollisionObject object, PhysicsCollisionEvent event);
    
}
