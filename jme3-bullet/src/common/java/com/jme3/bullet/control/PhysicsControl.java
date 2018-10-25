/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package com.jme3.bullet.control;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.scene.control.Control;

/**
 * An interface for a scene-graph control that links a physics object to a
 * Spatial.
 * <p>
 * This interface is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public interface PhysicsControl extends Control {

    /**
     * If enabled, add this control's physics object to the specified physics
     * space. In not enabled, alter where the object would be added. The object
     * is removed from any other space it's currently in.
     *
     * @param space where to add, or null to simply remove
     */
    public void setPhysicsSpace(PhysicsSpace space);

    /**
     * Access the physics space to which the object is (or would be) added.
     *
     * @return the pre-existing space, or null for none
     */
    public PhysicsSpace getPhysicsSpace();

    /**
     * Enable or disable this control.
     * <p>
     * The physics object is removed from its physics space when the control is
     * disabled. When the control is enabled again, the physics object is moved
     * to the current location of the spatial and then added to the physics
     * space.
     *
     * @param state true&rarr;enable the control, false&rarr;disable it
     */
    public void setEnabled(boolean state);

    /**
     * Test whether this control is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled();
}
