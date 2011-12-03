/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.control.Control;

/**
 *
 * @author normenhansen
 */
public interface PhysicsControl extends Control {

    public void setPhysicsSpace(PhysicsSpace space);

    public PhysicsSpace getPhysicsSpace();

    /**
     * The physics object is removed from the physics space when the control
     * is disabled. When the control is enabled  again the physics object is
     * moved to the current location of the spatial and then added to the physics
     * space. This allows disabling/enabling physics to move the spatial freely.
     * @param state
     */
    public void setEnabled(boolean state);
}
