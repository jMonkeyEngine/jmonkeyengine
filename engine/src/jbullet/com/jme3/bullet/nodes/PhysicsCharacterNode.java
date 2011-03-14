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
package com.jme3.bullet.nodes;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import java.io.IOException;

/**
 *
 * @author normenhansen
 * @deprecated in favor of physics Controls
 */
@Deprecated
public class PhysicsCharacterNode extends PhysicsBaseNode {

    public PhysicsCharacterNode() {
    }

    public PhysicsCharacterNode(CollisionShape shape, float stepHeight) {
        collisionObject = new CharacterControl(shape, stepHeight);
        addControl((CharacterControl)collisionObject);
    }

    public PhysicsCharacterNode(Spatial spat, CollisionShape shape, float stepHeight) {
        collisionObject = new CharacterControl(shape, stepHeight);
        addControl((CharacterControl)collisionObject);
        attachChild(spat);
    }

    public void warp(Vector3f location) {
        ((PhysicsCharacter)collisionObject).warp(location);
    }

    @Override
    public void setLocalTransform(Transform t) {
        super.setLocalTransform(t);
        ((CharacterControl)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalTranslation(Vector3f localTranslation) {
        super.setLocalTranslation(localTranslation);
        ((CharacterControl)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalTranslation(float x, float y, float z) {
        super.setLocalTranslation(x, y, z);
        ((CharacterControl)collisionObject).setPhysicsLocation(getWorldTranslation());
    }

    @Override
    public void setLocalRotation(Matrix3f rotation) {
        super.setLocalRotation(rotation);
    }

    @Override
    public void setLocalRotation(Quaternion quaternion) {
        super.setLocalRotation(quaternion);
    }

    /**
     * set the walk direction, works continuously
     * @param vec the walk direction to set
     */
    public void setWalkDirection(Vector3f vec) {
        ((PhysicsCharacter)collisionObject).setWalkDirection(vec);
    }

    public void setUpAxis(int axis) {
        ((PhysicsCharacter)collisionObject).setUpAxis(axis);
    }

    public int getUpAxis() {
        return ((PhysicsCharacter)collisionObject).getUpAxis();
    }

    public void setFallSpeed(float fallSpeed) {
        ((PhysicsCharacter)collisionObject).setFallSpeed(fallSpeed);
    }

    public float getFallSpeed() {
        return ((PhysicsCharacter)collisionObject).getFallSpeed();
    }

    public void setJumpSpeed(float jumpSpeed) {
        ((PhysicsCharacter)collisionObject).setJumpSpeed(jumpSpeed);
    }

    public float getJumpSpeed() {
        return ((PhysicsCharacter)collisionObject).getJumpSpeed();
    }

    public void setGravity(float value) {
        ((PhysicsCharacter)collisionObject).setGravity(value);
    }

    public float getGravity() {
        return ((PhysicsCharacter)collisionObject).getGravity();
    }

    public void setMaxSlope(float slopeRadians) {
        ((PhysicsCharacter)collisionObject).setMaxSlope(slopeRadians);
    }

    public float getMaxSlope() {
        return ((PhysicsCharacter)collisionObject).getMaxSlope();
    }

    public boolean onGround() {
        return ((PhysicsCharacter)collisionObject).onGround();
    }

    public void jump() {
        ((PhysicsCharacter)collisionObject).jump();
    }

    public void setCollisionShape(CollisionShape collisionShape) {
        ((PhysicsCharacter)collisionObject).setCollisionShape(collisionShape);
    }

    public PhysicsCharacter getPhysicsCharacter() {
        return ((PhysicsCharacter)collisionObject);
    }

    public void destroy() {
        ((PhysicsCharacter)collisionObject).destroy();
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
    }
}
