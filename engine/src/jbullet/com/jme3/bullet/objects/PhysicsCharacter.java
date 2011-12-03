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

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.dispatch.PairCachingGhostObject;
import com.bulletphysics.collision.shapes.ConvexShape;
import com.bulletphysics.dynamics.character.KinematicCharacterController;
import com.bulletphysics.linearmath.Transform;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * Basic Bullet Character
 * @author normenhansen
 */
public class PhysicsCharacter extends PhysicsCollisionObject {

    protected KinematicCharacterController character;
    protected float stepHeight;
    protected Vector3f walkDirection = new Vector3f();
    protected float fallSpeed = 55.0f;
    protected float jumpSpeed = 10.0f;
    protected int upAxis = 1;
    protected PairCachingGhostObject gObject;
    protected boolean locationDirty = false;
    //TEMP VARIABLES
    protected final Quaternion tmp_inverseWorldRotation = new Quaternion();
    private Transform tempTrans = new Transform(Converter.convert(new Matrix3f()));
    private com.jme3.math.Transform physicsLocation = new com.jme3.math.Transform();
    private javax.vecmath.Vector3f tempVec = new javax.vecmath.Vector3f();

    public PhysicsCharacter() {
    }

    /**
     * @param shape The CollisionShape (no Mesh or CompoundCollisionShapes)
     * @param stepHeight The quantization size for vertical movement
     */
    public PhysicsCharacter(CollisionShape shape, float stepHeight) {
        this.collisionShape = shape;
        if (!(shape.getCShape() instanceof ConvexShape)) {
            throw (new UnsupportedOperationException("Kinematic character nodes cannot have mesh collision shapes"));
        }
        this.stepHeight = stepHeight;
        buildObject();
    }

    protected void buildObject() {
        if (gObject == null) {
            gObject = new PairCachingGhostObject();
        }
        gObject.setCollisionFlags(CollisionFlags.CHARACTER_OBJECT);
        gObject.setCollisionFlags(gObject.getCollisionFlags() & ~CollisionFlags.NO_CONTACT_RESPONSE);
        gObject.setCollisionShape(collisionShape.getCShape());
        gObject.setUserPointer(this);
        character = new KinematicCharacterController(gObject, (ConvexShape) collisionShape.getCShape(), stepHeight);
    }

    /**
     * Sets the location of this physics character
     * @param location
     */
    public void warp(Vector3f location) {
        character.warp(Converter.convert(location, tempVec));
    }

    /**
     * Set the walk direction, works continuously.
     * This should probably be called setPositionIncrementPerSimulatorStep.
     * This is neither a direction nor a velocity, but the amount to
     * increment the position each physics tick. So vector length = accuracy*speed in m/s
     * @param vec the walk direction to set
     */
    public void setWalkDirection(Vector3f vec) {
        walkDirection.set(vec);
        character.setWalkDirection(Converter.convert(walkDirection, tempVec));
    }

    /**
     * @return the currently set walkDirection
     */
    public Vector3f getWalkDirection() {
        return walkDirection;
    }

    public void setUpAxis(int axis) {
        upAxis = axis;
        character.setUpAxis(axis);
    }

    public int getUpAxis() {
        return upAxis;
    }

    public void setFallSpeed(float fallSpeed) {
        this.fallSpeed = fallSpeed;
        character.setFallSpeed(fallSpeed);
    }

    public float getFallSpeed() {
        return fallSpeed;
    }

    public void setJumpSpeed(float jumpSpeed) {
        this.jumpSpeed = jumpSpeed;
        character.setJumpSpeed(jumpSpeed);
    }

    public float getJumpSpeed() {
        return jumpSpeed;
    }

    //does nothing..
//    public void setMaxJumpHeight(float height) {
//        character.setMaxJumpHeight(height);
//    }
    public void setGravity(float value) {
        character.setGravity(value);
    }

    public float getGravity() {
        return character.getGravity();
    }

    public void setMaxSlope(float slopeRadians) {
        character.setMaxSlope(slopeRadians);
    }

    public float getMaxSlope() {
        return character.getMaxSlope();
    }

    public boolean onGround() {
        return character.onGround();
    }

    public void jump() {
        character.jump();
    }

    @Override
    public void setCollisionShape(CollisionShape collisionShape) {
        if (!(collisionShape.getCShape() instanceof ConvexShape)) {
            throw (new UnsupportedOperationException("Kinematic character nodes cannot have mesh collision shapes"));
        }
        super.setCollisionShape(collisionShape);
        if (gObject == null) {
            buildObject();
        }else{
            gObject.setCollisionShape(collisionShape.getCShape());
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
        gObject.getWorldTransform(tempTrans);
        Converter.convert(tempTrans.origin, physicsLocation.getTranslation());
        return trans.set(physicsLocation.getTranslation());
    }

    /**
     * @return the physicsLocation
     */
    public Vector3f getPhysicsLocation() {
        gObject.getWorldTransform(tempTrans);
        Converter.convert(tempTrans.origin, physicsLocation.getTranslation());
        return physicsLocation.getTranslation();
    }

    public void setCcdSweptSphereRadius(float radius) {
        gObject.setCcdSweptSphereRadius(radius);
    }

    public void setCcdMotionThreshold(float threshold) {
        gObject.setCcdMotionThreshold(threshold);
    }

    public float getCcdSweptSphereRadius() {
        return gObject.getCcdSweptSphereRadius();
    }

    public float getCcdMotionThreshold() {
        return gObject.getCcdMotionThreshold();
    }

    public float getCcdSquareMotionThreshold() {
        return gObject.getCcdSquareMotionThreshold();
    }

    /**
     * used internally
     */
    public KinematicCharacterController getControllerId() {
        return character;
    }

    /**
     * used internally
     */
    public PairCachingGhostObject getObjectId() {
        return gObject;
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
        character = new KinematicCharacterController(gObject, (ConvexShape) collisionShape.getCShape(), stepHeight);
        setGravity(capsule.readFloat("gravity", 9.8f * 3));
        setMaxSlope(capsule.readFloat("maxSlope", 1.0f));
        setFallSpeed(capsule.readFloat("fallSpeed", 55.0f));
        setJumpSpeed(capsule.readFloat("jumpSpeed", 10.0f));
        setUpAxis(capsule.readInt("upAxis", 1));
        setCcdMotionThreshold(capsule.readFloat("ccdMotionThreshold", 0));
        setCcdSweptSphereRadius(capsule.readFloat("ccdSweptSphereRadius", 0));
        setPhysicsLocation((Vector3f) capsule.readSavable("physicsLocation", new Vector3f()));
    }
}
