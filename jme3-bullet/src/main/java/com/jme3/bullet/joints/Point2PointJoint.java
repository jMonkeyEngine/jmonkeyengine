/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.bullet.joints;

import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A joint based on Bullet's btPoint2PointConstraint.
 * <p>
 * <i>From the Bullet manual:</i><br>
 * Point to point constraint limits the translation so that the local pivot
 * points of 2 rigidbodies match in worldspace. A chain of rigidbodies can be
 * connected using this constraint.
 *
 * @author normenhansen
 */
public class Point2PointJoint extends PhysicsJoint {

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected Point2PointJoint() {
    }

    /**
     * Instantiate a Point2PointJoint. To be effective, the joint must be added
     * to a physics space.
     *
     * @param nodeA the 1st body connected by the joint (not null, alias
     * created)
     * @param nodeB the 2nd body connected by the joint (not null, alias
     * created)
     * @param pivotA the local offset of the connection point in node A (not
     * null, alias created)
     * @param pivotB the local offset of the connection point in node B (not
     * null, alias created)
     */
    public Point2PointJoint(PhysicsRigidBody nodeA, PhysicsRigidBody nodeB, Vector3f pivotA, Vector3f pivotB) {
        super(nodeA, nodeB, pivotA, pivotB);
        createJoint();
    }

    /**
     * Alter the joint's damping.
     *
     * @param value the desired viscous damping ratio (0&rarr;no damping,
     * 1&rarr;critically damped, default=1)
     */
    public void setDamping(float value) {
        setDamping(objectId, value);
    }

    private native void setDamping(long objectId, float value);

    /**
     * Alter the joint's impulse clamp.
     *
     * @param value the desired impulse clamp value (default=0)
     */
    public void setImpulseClamp(float value) {
        setImpulseClamp(objectId, value);
    }

    private native void setImpulseClamp(long objectId, float value);

    /**
     * Alter the joint's tau value.
     *
     * @param value the desired tau value (default=0.3)
     */
    public void setTau(float value) {
        setTau(objectId, value);
    }

    private native void setTau(long objectId, float value);

    /**
     * Read the joint's damping ratio.
     *
     * @return the viscous damping ratio (0&rarr;no damping, 1&rarr;critically
     * damped)
     */
    public float getDamping() {
        return getDamping(objectId);
    }

    private native float getDamping(long objectId);

    /**
     * Read the joint's impulse clamp.
     *
     * @return the clamp value
     */
    public float getImpulseClamp() {
        return getImpulseClamp(objectId);
    }

    private native float getImpulseClamp(long objectId);

    /**
     * Read the joint's tau value.
     *
     * @return the tau value
     */
    public float getTau() {
        return getTau(objectId);
    }

    private native float getTau(long objectId);

    /**
     * Serialize this joint, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule cap = ex.getCapsule(this);
        cap.write(getDamping(), "damping", 1.0f);
        cap.write(getTau(), "tau", 0.3f);
        cap.write(getImpulseClamp(), "impulseClamp", 0f);
    }

    /**
     * De-serialize this joint, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        createJoint();
        InputCapsule cap = im.getCapsule(this);
        setDamping(cap.readFloat("damping", 1.0f));
        setDamping(cap.readFloat("tau", 0.3f));
        setDamping(cap.readFloat("impulseClamp", 0f));
    }

    /**
     * Create the configured joint in Bullet.
     */
    protected void createJoint() {
        objectId = createJoint(nodeA.getObjectId(), nodeB.getObjectId(), pivotA, pivotB);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Joint {0}", Long.toHexString(objectId));
    }

    private native long createJoint(long objectIdA, long objectIdB, Vector3f pivotA, Vector3f pivotB);
}
