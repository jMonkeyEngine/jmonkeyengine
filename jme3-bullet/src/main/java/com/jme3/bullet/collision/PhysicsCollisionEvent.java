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
package com.jme3.bullet.collision;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.EventObject;

/**
 * Describe a collision in the physics world.
 * <p>
 * Do not retain this object, as it will be reused after the collision() method
 * returns. Copy any data you need during the collide() method.
 *
 * @author normenhansen
 */
public class PhysicsCollisionEvent extends EventObject {

    /**
     * type value to indicate a new event
     */
    public static final int TYPE_ADDED = 0;
    /**
     * type value to indicate an event that has been added to a PhysicsSpace
     * queue
     */
    public static final int TYPE_PROCESSED = 1;
    /**
     * type value to indicate a cleaned/destroyed event
     */
    public static final int TYPE_DESTROYED = 2;
    /**
     * type value that indicates the event's status
     */
    private int type;
    /**
     * 1st involved object
     */
    private PhysicsCollisionObject nodeA;
    /**
     * 2nd involved object
     */
    private PhysicsCollisionObject nodeB;
    /**
     * Bullet identifier of the btManifoldPoint
     */
    private long manifoldPointObjectId = 0;

    /**
     * Instantiate a collision event.
     *
     * @param type event type (0=added/1=processed/2=destroyed)
     * @param nodeA 1st involved object (alias created)
     * @param nodeB 2nd involved object (alias created)
     * @param manifoldPointObjectId Bullet identifier of the btManifoldPoint
     */
    public PhysicsCollisionEvent(int type, PhysicsCollisionObject nodeA, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        super(nodeA);
        this.type = type;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.manifoldPointObjectId = manifoldPointObjectId;
    }

    /**
     * Destroy this event.
     */
    public void clean() {
        source = null;
        this.type = 0;
        this.nodeA = null;
        this.nodeB = null;
        this.manifoldPointObjectId = 0;
    }

    /**
     * Reuse this event.
     *
     * @param type event type (added/processed/destroyed)
     * @param source 1st involved object (alias created)
     * @param nodeB 2nd involved object (alias created)
     * @param manifoldPointObjectId Bullet identifier
     */
    public void refactor(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        this.source = source;
        this.type = type;
        this.nodeA = source;
        this.nodeB = nodeB;
        this.manifoldPointObjectId = manifoldPointObjectId;
    }

    /**
     * Read the type of event.
     *
     * @return added/processed/destroyed
     */
    public int getType() {
        return type;
    }

    /**
     * Access the user object of collision object A, provided it's a Spatial.
     *
     * @return the pre-existing Spatial, or null if none
     */
    public Spatial getNodeA() {
        if (nodeA.getUserObject() instanceof Spatial) {
            return (Spatial) nodeA.getUserObject();
        }
        return null;
    }

    /**
     * Access the user object of collision object B, provided it's a Spatial.
     *
     * @return the pre-existing Spatial, or null if none
     */
    public Spatial getNodeB() {
        if (nodeB.getUserObject() instanceof Spatial) {
            return (Spatial) nodeB.getUserObject();
        }
        return null;
    }

    /**
     * Access collision object A.
     *
     * @return the pre-existing object (not null)
     */
    public PhysicsCollisionObject getObjectA() {
        return nodeA;
    }

    /**
     * Access collision object B.
     *
     * @return the pre-existing object (not null)
     */
    public PhysicsCollisionObject getObjectB() {
        return nodeB;
    }

    /**
     * Read the collision's applied impulse.
     *
     * @return impulse
     */
    public float getAppliedImpulse() {
        return getAppliedImpulse(manifoldPointObjectId);
    }
    private native float getAppliedImpulse(long manifoldPointObjectId);

    /**
     * Read the collision's applied lateral impulse #1.
     *
     * @return impulse
     */
    public float getAppliedImpulseLateral1() {
        return getAppliedImpulseLateral1(manifoldPointObjectId);
    }
    private native float getAppliedImpulseLateral1(long manifoldPointObjectId);

    /**
     * Read the collision's applied lateral impulse #2.
     *
     * @return impulse
     */
    public float getAppliedImpulseLateral2() {
        return getAppliedImpulseLateral2(manifoldPointObjectId);
    }
    private native float getAppliedImpulseLateral2(long manifoldPointObjectId);

    /**
     * Read the collision's combined friction.
     *
     * @return friction
     */
    public float getCombinedFriction() {
        return getCombinedFriction(manifoldPointObjectId);
    }
    private native float getCombinedFriction(long manifoldPointObjectId);

    /**
     * Read the collision's combined restitution.
     *
     * @return restitution
     */
    public float getCombinedRestitution() {
        return getCombinedRestitution(manifoldPointObjectId);
    }
    private native float getCombinedRestitution(long manifoldPointObjectId);

    /**
     * Read the collision's distance #1.
     *
     * @return distance
     */
    public float getDistance1() {
        return getDistance1(manifoldPointObjectId);
    }
    private native float getDistance1(long manifoldPointObjectId);

    /**
     * Read the collision's index 0.
     *
     * @return index
     */
    public int getIndex0() {
        return getIndex0(manifoldPointObjectId);
    }
    private native int getIndex0(long manifoldPointObjectId);

    /**
     * Read the collision's index 1.
     *
     * @return index
     */
    public int getIndex1() {
        return getIndex1(manifoldPointObjectId);
    }
    private native int getIndex1(long manifoldPointObjectId);

    /**
     * Copy the collision's lateral friction direction #1.
     *
     * @return a new vector (not null)
     */
    public Vector3f getLateralFrictionDir1() {
        return getLateralFrictionDir1(new Vector3f());
    }

    /**
     * Copy the collision's lateral friction direction #1.
     *
     * @param lateralFrictionDir1 storage for the result (not null, modified)
     * @return direction vector (not null)
     */
    public Vector3f getLateralFrictionDir1(Vector3f lateralFrictionDir1) {
        getLateralFrictionDir1(manifoldPointObjectId, lateralFrictionDir1);
        return lateralFrictionDir1;
    }
    private native void getLateralFrictionDir1(long manifoldPointObjectId, Vector3f lateralFrictionDir1);

    /**
     * Copy the collision's lateral friction direction #2.
     *
     * @return a new vector
     */
    public Vector3f getLateralFrictionDir2() {
        return getLateralFrictionDir2(new Vector3f());
    }

    /**
     * Copy the collision's lateral friction direction #2.
     *
     * @param lateralFrictionDir2 storage for the result (not null, modified)
     * @return direction vector (not null)
     */
    public Vector3f getLateralFrictionDir2(Vector3f lateralFrictionDir2) {
        getLateralFrictionDir2(manifoldPointObjectId, lateralFrictionDir2);
        return lateralFrictionDir2;
    }
    private native void getLateralFrictionDir2(long manifoldPointObjectId, Vector3f lateralFrictionDir2);

    /**
     * Test whether the collision's lateral friction is initialized.
     *
     * @return true if initialized, otherwise false
     */
    public boolean isLateralFrictionInitialized() {
        return isLateralFrictionInitialized(manifoldPointObjectId);
    }
    private native boolean isLateralFrictionInitialized(long manifoldPointObjectId);

    /**
     * Read the collision's lifetime.
     *
     * @return lifetime
     */
    public int getLifeTime() {
        return getLifeTime(manifoldPointObjectId);
    }
    private native int getLifeTime(long manifoldPointObjectId);

    /**
     * Copy the collision's location in the local coordinates of object A.
     *
     * @return a new location vector (in local coordinates, not null)
     */
    public Vector3f getLocalPointA() {
        return getLocalPointA(new Vector3f());
    }
    
    /**
     * Copy the collision's location in the local coordinates of object A.
     *
     * @param localPointA storage for the result (not null, modified)
     * @return a location vector (in local coordinates, not null)
     */
    public Vector3f getLocalPointA(Vector3f localPointA) {
        getLocalPointA(manifoldPointObjectId, localPointA);
        return localPointA;
    }
    private native void getLocalPointA(long manifoldPointObjectId, Vector3f localPointA);

    /**
     * Copy the collision's location in the local coordinates of object B.
     *
     * @return a new location vector (in local coordinates, not null)
     */
    public Vector3f getLocalPointB() {
        return getLocalPointB(new Vector3f());
    }
    
    /**
     * Copy the collision's location in the local coordinates of object B.
     *
     * @param localPointB storage for the result (not null, modified)
     * @return a location vector (in local coordinates, not null)
     */
    public Vector3f getLocalPointB(Vector3f localPointB) {
        getLocalPointB(manifoldPointObjectId, localPointB);
        return localPointB;
    }
    private native void getLocalPointB(long manifoldPointObjectId, Vector3f localPointB);

    /**
     * Copy the collision's normal on object B.
     *
     * @return a new normal vector (in physics-space coordinates, not null)
     */
    public Vector3f getNormalWorldOnB() {
        return getNormalWorldOnB(new Vector3f());
    }

    /**
     * Copy the collision's normal on object B.
     *
     * @param normalWorldOnB storage for the result (not null, modified)
     * @return a normal vector (in physics-space coordinates, not null)
     */
    public Vector3f getNormalWorldOnB(Vector3f normalWorldOnB) {
        getNormalWorldOnB(manifoldPointObjectId, normalWorldOnB);
        return normalWorldOnB;
    }
    private native void getNormalWorldOnB(long manifoldPointObjectId, Vector3f normalWorldOnB);

    /**
     * Read part identifier 0.
     *
     * @return identifier
     */
    public int getPartId0() {
        return getPartId0(manifoldPointObjectId);
    }
    private native int getPartId0(long manifoldPointObjectId);

    /**
     * Read part identifier 1.
     *
     * @return identifier
     */
    public int getPartId1() {
        return getPartId1(manifoldPointObjectId);
    }

    private native int getPartId1(long manifoldPointObjectId);

    /**
     * Copy the collision's location.
     *
     * @return a new vector (in physics-space coordinates, not null)
     */
    public Vector3f getPositionWorldOnA() {
        return getPositionWorldOnA(new Vector3f());
    }

    /**
     * Copy the collision's location.
     *
     * @param positionWorldOnA storage for the result (not null, modified)
     * @return a location vector (in physics-space coordinates, not null)
     */
    public Vector3f getPositionWorldOnA(Vector3f positionWorldOnA) {
        getPositionWorldOnA(manifoldPointObjectId, positionWorldOnA);
        return positionWorldOnA;
    }
    private native void getPositionWorldOnA(long manifoldPointObjectId, Vector3f positionWorldOnA);

    /**
     * Copy the collision's location.
     *
     * @return a new location vector (in physics-space coordinates, not null)
     */
    public Vector3f getPositionWorldOnB() {
        return getPositionWorldOnB(new Vector3f());
    }

    /**
     * Copy the collision's location.
     *
     * @param positionWorldOnB storage for the result (not null, modified)
     * @return a location vector (in physics-space coordinates, not null)
     */
    public Vector3f getPositionWorldOnB(Vector3f positionWorldOnB) {
        getPositionWorldOnB(manifoldPointObjectId, positionWorldOnB);
        return positionWorldOnB;
    }
    private native void getPositionWorldOnB(long manifoldPointObjectId, Vector3f positionWorldOnB);

//    public Object getUserPersistentData() {
//        return userPersistentData;
//    }
}
