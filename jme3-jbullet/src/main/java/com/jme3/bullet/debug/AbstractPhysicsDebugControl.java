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
package com.jme3.bullet.debug;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;

/**
 * The abstract base class for physics-debug controls (such as
 * BulletRigidBodyDebugControl) used to visualize individual collision objects
 * and joints.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public abstract class AbstractPhysicsDebugControl extends AbstractControl {

    private final Quaternion tmp_inverseWorldRotation = new Quaternion();
    /**
     * the app state that this control serves
     */
    protected final BulletDebugAppState debugAppState;

    /**
     * Instantiate an enabled control to serve the specified debug app state.
     *
     * @param debugAppState which app state (not null, alias created)
     */
    public AbstractPhysicsDebugControl(BulletDebugAppState debugAppState) {
        this.debugAppState = debugAppState;
    }

    /**
     * This is called on the physics thread for debug controls
     */
    @Override
    protected abstract void controlUpdate(float tpf);

    /**
     * Apply the specified location and orientation to the controlled spatial.
     *
     * @param worldLocation location vector (in physics-space coordinates, not
     * null, unaffected)
     * @param worldRotation orientation (in physics-space coordinates, not null,
     * unaffected)
     */
    protected void applyPhysicsTransform(Vector3f worldLocation, Quaternion worldRotation) {
        applyPhysicsTransform(worldLocation, worldRotation, this.spatial);
    }

    /**
     * Apply the specified location and orientation to the specified spatial.
     *
     * @param worldLocation location vector (in physics-space coordinates, not
     * null, unaffected)
     * @param worldRotation orientation (in physics-space coordinates, not null,
     * unaffected)
     * @param spatial where to apply (may be null)
     */
    protected void applyPhysicsTransform(Vector3f worldLocation, Quaternion worldRotation, Spatial spatial) {
        if (spatial != null) {
            Vector3f localLocation = spatial.getLocalTranslation();
            Quaternion localRotationQuat = spatial.getLocalRotation();
            if (spatial.getParent() != null) {
                localLocation.set(worldLocation).subtractLocal(spatial.getParent().getWorldTranslation());
                localLocation.divideLocal(spatial.getParent().getWorldScale());
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().multLocal(localLocation);
                localRotationQuat.set(worldRotation);
                tmp_inverseWorldRotation.set(spatial.getParent().getWorldRotation()).inverseLocal().mult(localRotationQuat, localRotationQuat);
                spatial.setLocalTranslation(localLocation);
                spatial.setLocalRotation(localRotationQuat);
            } else {
                spatial.setLocalTranslation(worldLocation);
                spatial.setLocalRotation(worldRotation);
            }
        }

    }
}
