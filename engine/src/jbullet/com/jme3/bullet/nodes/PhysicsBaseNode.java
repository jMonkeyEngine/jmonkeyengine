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

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import java.io.IOException;

/**
 * Base class for Physics Nodes (PhysicsNode, PhysicsGhostNode)
 * @author normenhansen
 * @deprecated in favor of physics Controls
 */
@Deprecated
public abstract class PhysicsBaseNode extends Node {

    protected PhysicsCollisionObject collisionObject;
    protected Quaternion tmp_inverseWorldRotation = new Quaternion();

    public void updatePhysicsState() {
    }

    /**
     * Sets a CollisionShape to this physics object, note that the object should
     * not be in the physics space when adding a new collision shape as it is rebuilt
     * on the physics side.
     * @param collisionShape the CollisionShape to set
     */
    public void setCollisionShape(CollisionShape collisionShape) {
        collisionObject.setCollisionShape(collisionShape);
    }

    /**
     * @return the CollisionShape of this PhysicsNode, to be able to reuse it with
     * other physics nodes (increases performance)
     */
    public CollisionShape getCollisionShape() {
        return collisionObject.getCollisionShape();
    }

    /**
     * Returns the collision group for this collision shape
     * @return
     */
    public int getCollisionGroup() {
        return collisionObject.getCollisionGroup();
    }

    /**
     * Sets the collision group number for this physics object. <br>
     * The groups are integer bit masks and some pre-made variables are available in CollisionObject.
     * All physics objects are by default in COLLISION_GROUP_01.<br>
     * Two object will collide when <b>one</b> of the partys has the
     * collisionGroup of the other in its collideWithGroups set.
     * @param collisionGroup the collisionGroup to set
     */
    public void setCollisionGroup(int collisionGroup) {
        collisionObject.setCollisionGroup(collisionGroup);
    }

    /**
     * Add a group that this object will collide with.<br>
     * Two object will collide when <b>one</b> of the partys has the
     * collisionGroup of the other in its collideWithGroups set.<br>
     * @param collisionGroup
     */
    public void addCollideWithGroup(int collisionGroup) {
        collisionObject.addCollideWithGroup(collisionGroup);
    }

    /**
     * Remove a group from the list this object collides with.
     * @param collisionGroup
     */
    public void removeCollideWithGroup(int collisionGroup) {
        collisionObject.removeCollideWithGroup(collisionGroup);
    }

    /**
     * Directly set the bitmask for collision groups that this object collides with.
     * @param collisionGroup
     */
    public void setCollideWithGroups(int collisionGroups) {
        collisionObject.setCollideWithGroups(collisionGroups);
    }

    /**
     * Gets the bitmask of collision groups that this object collides with.
     * @return
     */
    public int getCollideWithGroups() {
        return collisionObject.getCollideWithGroups();
    }

    /**
     * computes the local translation from the parameter translation and sets it as new
     * local translation<br>
     * This should only be called from the physics thread to update the jme spatial
     * @param translation new world translation of this spatial.
     * @return the computed local translation
     */
    public Vector3f setWorldTranslation(Vector3f translation) {
        Vector3f localTranslation = super.getLocalTranslation();
        if (parent != null) {
            localTranslation.set(translation).subtractLocal(parent.getWorldTranslation());
            localTranslation.divideLocal(parent.getWorldScale());
            tmp_inverseWorldRotation.set(parent.getWorldRotation()).inverseLocal().multLocal(localTranslation);
        } else {
            localTranslation.set(translation);
        }
        super.setLocalTranslation(localTranslation);
        return localTranslation;
    }

    /**
     * computes the local rotation from the parameter rot and sets it as new
     * local rotation<br>
     * This should only be called from the physics thread to update the jme spatial
     * @param rot new world rotation of this spatial.
     * @return the computed local rotation
     */
    public Quaternion setWorldRotation(Quaternion rot) {
        Quaternion localRotation = super.getLocalRotation();
        if (parent != null) {
            tmp_inverseWorldRotation.set(parent.getWorldRotation()).inverseLocal().mult(rot, localRotation);
        } else {
            localRotation.set(rot);
        }
        super.setLocalRotation(localRotation);
        return localRotation;
    }

    /**
     * Attaches a visual debug shape of the current collision shape to this physics object<br/>
     * <b>Does not work with detached physics, please switch to PARALLEL or SEQUENTIAL for debugging</b>
     * @param manager AssetManager to load the default wireframe material for the debug shape
     */
    public void attachDebugShape(AssetManager manager) {
        collisionObject.attachDebugShape(manager);
    }

    public void attachDebugShape(Material material) {
        collisionObject.attachDebugShape(material);
    }

    /**
     * Detaches the debug shape
     */
    public void detachDebugShape() {
        collisionObject.detachDebugShape();
    }

    @Override
    public void write(JmeExporter e) throws IOException {
        super.write(e);
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(collisionObject, "collisionObject", null);

    }

    @Override
    public void read(JmeImporter e) throws IOException {
        super.read(e);
        InputCapsule capsule = e.getCapsule(this);
        collisionObject = (PhysicsCollisionObject) capsule.readSavable("collisionObject", null);
    }
}
