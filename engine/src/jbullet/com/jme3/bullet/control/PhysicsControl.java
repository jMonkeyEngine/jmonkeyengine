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
}
