/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.bullet.objects;

import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic Bullet Character
 * @author normenhansen
 */
public class PhysicsCharacter extends PhysicsCollisionObject {

    protected long characterId = 0;
    protected float stepHeight;
    protected Vector3f walkDirection = new Vector3f();
    protected float fallSpeed = 55.0f;
    protected float jumpSpeed = 10.0f;
    protected int upAxis = 1;
    protected boolean locationDirty = false;
    //TEMP VARIABLES
    protected final Quaternion tmp_inverseWorldRotation = new Quaternion();

    public PhysicsCharacter() {
    }

    /**
     * @param shape The CollisionShape (no Mesh or CompoundCollisionShapes)
     * @param stepHeight The quantization size for vertical movement
     */
    public PhysicsCharacter(CollisionShape shape, float stepHeight) {
        this.collisionShape = shape;
//        if (shape instanceof MeshCollisionShape || shape instanceof CompoundCollisionShape) {
//            throw (new UnsupportedOperationException("Kinematic character nodes cannot have mesh or compound collision shapes"));
//        }
        this.stepHeight = stepHeight;
        buildObject();
    }

    protected void buildObject() {
        if (objectId == 0) {
            objectId = createGhostObject();
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Creating GhostObject {0}", Long.toHexString(objectId));
            initUserPointer();
        }
        setCharacterFlags(objectId);
        attachCollisionShape(objectId, collisionShape.getObjectId());
        if (characterId != 0) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Clearing Character {0}", Long.toHexString(objectId));
            finalizeNativeCharacter(characterId);
        }
        characterId = createCharacterObject(objectId, collisionShape.getObjectId(), stepHeight);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Creating Character {0}", Long.toHexString(characterId));
    }

    private native long createGhostObject();

    private native void setCharacterFlags(long objectId);

    private native long createCharacterObject(long objectId, long shapeId, float stepHeight);

    /**
     * Sets the location of this physics character
     * @param location
     */
    public void warp(Vector3f location) {
        warp(characterId, location);
    }

    private native void warp(long characterId, Vector3f location);

    /**
     * Set the walk direction, works continuously.
     * This should probably be called setPositionIncrementPerSimulatorStep.
     * This is neither a direction nor a velocity, but the amount to
     * increment the position each physics tick. So vector length = accuracy*speed in m/s
     * @param vec the walk direction to set
     */
    public void setWalkDirection(Vector3f vec) {
        walkDirection.set(vec);
        setWalkDirection(characterId, vec);
    }

    private native void setWalkDirection(long characterId, Vector3f vec);

    /**
     * @return the currently set walkDirection
     */
    public Vector3f getWalkDirection() {
        return walkDirection;
    }

    public void setUpAxis(int axis) {
        upAxis = axis;
        setUpAxis(characterId, axis);
    }

    private native void setUpAxis(long characterId, int axis);

    public int getUpAxis() {
        return upAxis;
    }

    public void setFallSpeed(float fallSpeed) {
        this.fallSpeed = fallSpeed;
        setFallSpeed(characterId, fallSpeed);
    }

    private native void setFallSpeed(long characterId, float fallSpeed);

    public float getFallSpeed() {
        return fallSpeed;
    }

    public void setJumpSpeed(float jumpSpeed) {
        this.jumpSpeed = jumpSpeed;
        setJumpSpeed(characterId, jumpSpeed);
    }

    private native void setJumpSpeed(long characterId, float jumpSpeed);

    public float getJumpSpeed() {
        return jumpSpeed;
    }

    public void setGravity(float value) {
        setGravity(characterId, value);
    }

    private native void setGravity(long characterId, float gravity);

    public float getGravity() {
        return getGravity(characterId);
    }

    private native float getGravity(long characterId);

    public void setMaxSlope(float slopeRadians) {
        setMaxSlope(characterId, slopeRadians);
    }

    private native void setMaxSlope(long characterId, float slopeRadians);

    public float getMaxSlope() {
        return getMaxSlope(characterId);
    }

    private native float getMaxSlope(long characterId);

    public boolean onGround() {
        return onGround(characterId);
    }

    private native boolean onGround(long characterId);

    public void jump() {
        jump(characterId);
    }

    private native void jump(long characterId);

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
//        if (!(collisionShape.getObjectId() instanceof ConvexShape)) {
//            throw (new UnsupportedOperationException("Kinematic character nodes cannot have mesh collision shapes"));
//        }
        super.setCollisionShape(collisionShape);
        if (objectId == 0) {
            buildObject();
        } else {
            attachCollisionShape(objectId, collisionShape.getObjectId());
        }
    }

    /**
     * Set the physics location (same as warp())
     * @param location the location of the actual physics object
     */
    public void setPhysicsLocation(Vector3f location) {
        warp(location);
    }

    /**
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation(Vector3f trans) {
        if (trans == null) {
            trans = new Vector3f();
        }
        getPhysicsLocation(objectId, trans);
        return trans;
    }

    private native void getPhysicsLocation(long objectId, Vector3f vec);

    /**
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation() {
        return getPhysicsLocation(null);
    }

    public void setCcdSweptSphereRadius(float radius) {
        setCcdSweptSphereRadius(objectId, radius);
    }

    private native void setCcdSweptSphereRadius(long objectId, float radius);

    public void setCcdMotionThreshold(float threshold) {
        setCcdMotionThreshold(objectId, threshold);
    }

    private native void setCcdMotionThreshold(long objectId, float threshold);

    public float getCcdSweptSphereRadius() {
        return getCcdSweptSphereRadius(objectId);
    }

    private native float getCcdSweptSphereRadius(long objectId);

    public float getCcdMotionThreshold() {
        return getCcdMotionThreshold(objectId);
    }

    private native float getCcdMotionThreshold(long objectId);

    public float getCcdSquareMotionThreshold() {
        return getCcdSquareMotionThreshold(objectId);
    }

    private native float getCcdSquareMotionThreshold(long objectId);

    /**
     * used internally
     */
    public long getControllerId() {
        return characterId;
    }

    public void destroy() {
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(stepHeight, "stepHeight", 1.0f);
        capsule.write(getGravity(), "gravity", 9.8f * 3);
        capsule.write(getMaxSlope(), "maxSlope", 1.0f);
        capsule.write(fallSpeed, "fallSpeed", 55.0f);
        capsule.write(jumpSpeed, "jumpSpeed", 10.0f);
        capsule.write(upAxis, "upAxis", 1);
        capsule.write(getCcdMotionThreshold(), "ccdMotionThreshold", 0);
        capsule.write(getCcdSweptSphereRadius(), "ccdSweptSphereRadius", 0);
        capsule.write(getPhysicsLocation(new Vector3f()), "physicsLocation", new Vector3f());
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        stepHeight = capsule.readFloat("stepHeight", 1.0f);
        buildObject();
        setGravity(capsule.readFloat("gravity", 9.8f * 3));
        setMaxSlope(capsule.readFloat("maxSlope", 1.0f));
        setFallSpeed(capsule.readFloat("fallSpeed", 55.0f));
        setJumpSpeed(capsule.readFloat("jumpSpeed", 10.0f));
        setUpAxis(capsule.readInt("upAxis", 1));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        finalizeNativeCharacter(characterId);
    }

    private native void finalizeNativeCharacter(long characterId);
}
