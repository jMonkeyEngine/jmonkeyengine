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
package com.jme3.bullet.collision;

import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import java.util.EventObject;

/**
 * A CollisionEvent stores all information about a collision in the PhysicsWorld.
 * Do not store this Object, as it will be reused after the collision() method has been called.
 * Get/reference all data you need in the collide method.
 * @author normenhansen
 */
public class PhysicsCollisionEvent extends EventObject {

    public static final int TYPE_ADDED = 0;
    public static final int TYPE_PROCESSED = 1;
    public static final int TYPE_DESTROYED = 2;
    private int type;
    private PhysicsCollisionObject nodeA;
    private PhysicsCollisionObject nodeB;
    private long manifoldPointObjectId = 0;

    public PhysicsCollisionEvent(int type, PhysicsCollisionObject nodeA, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        super(nodeA);
        this.manifoldPointObjectId = manifoldPointObjectId;
    }
    
    /**
     * used by event factory, called when event is destroyed
     */
    public void clean() {
        source = null;
        this.type = 0;
        this.nodeA = null;
        this.nodeB = null;
        this.manifoldPointObjectId = 0;
    }

    /**
     * used by event factory, called when event reused
     */
    public void refactor(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, long manifoldPointObjectId) {
        this.source = source;
        this.type = type;
        this.nodeA = source;
        this.nodeB = nodeB;
        this.manifoldPointObjectId = manifoldPointObjectId;
    }

    public int getType() {
        return type;
    }

    /**
     * @return A Spatial if the UserObject of the PhysicsCollisionObject is a Spatial
     */
    public Spatial getNodeA() {
        if (nodeA.getUserObject() instanceof Spatial) {
            return (Spatial) nodeA.getUserObject();
        }
        return null;
    }

    /**
     * @return A Spatial if the UserObject of the PhysicsCollisionObject is a Spatial
     */
    public Spatial getNodeB() {
        if (nodeB.getUserObject() instanceof Spatial) {
            return (Spatial) nodeB.getUserObject();
        }
        return null;
    }

    public PhysicsCollisionObject getObjectA() {
        return nodeA;
    }

    public PhysicsCollisionObject getObjectB() {
        return nodeB;
    }

    public float getAppliedImpulse() {
        return getAppliedImpulse(manifoldPointObjectId);
    }
    private native float getAppliedImpulse(long manifoldPointObjectId);

    public float getAppliedImpulseLateral1() {
        return getAppliedImpulseLateral1(manifoldPointObjectId);
    }
    private native float getAppliedImpulseLateral1(long manifoldPointObjectId);

    public float getAppliedImpulseLateral2() {
        return getAppliedImpulseLateral2(manifoldPointObjectId);
    }
    private native float getAppliedImpulseLateral2(long manifoldPointObjectId);

    public float getCombinedFriction() {
        return getCombinedFriction(manifoldPointObjectId);
    }
    private native float getCombinedFriction(long manifoldPointObjectId);

    public float getCombinedRestitution() {
        return getCombinedRestitution(manifoldPointObjectId);
    }
    private native float getCombinedRestitution(long manifoldPointObjectId);

    public float getDistance1() {
        return getDistance1(manifoldPointObjectId);
    }
    private native float getDistance1(long manifoldPointObjectId);

    public int getIndex0() {
        return getIndex0(manifoldPointObjectId);
    }
    private native int getIndex0(long manifoldPointObjectId);

    public int getIndex1() {
        return getIndex1(manifoldPointObjectId);
    }
    private native int getIndex1(long manifoldPointObjectId);

    public Vector3f getLateralFrictionDir1() {
        return getLateralFrictionDir1(new Vector3f());
    }

    public Vector3f getLateralFrictionDir1(Vector3f lateralFrictionDir1) {
        getLateralFrictionDir1(manifoldPointObjectId, lateralFrictionDir1);
        return lateralFrictionDir1;
    }
    private native void getLateralFrictionDir1(long manifoldPointObjectId, Vector3f lateralFrictionDir1);

    public Vector3f getLateralFrictionDir2() {
        return getLateralFrictionDir2(new Vector3f());
    }

    public Vector3f getLateralFrictionDir2(Vector3f lateralFrictionDir2) {
        getLateralFrictionDir2(manifoldPointObjectId, lateralFrictionDir2);
        return lateralFrictionDir2;
    }
    private native void getLateralFrictionDir2(long manifoldPointObjectId, Vector3f lateralFrictionDir2);

    public boolean isLateralFrictionInitialized() {
        return isLateralFrictionInitialized(manifoldPointObjectId);
    }
    private native boolean isLateralFrictionInitialized(long manifoldPointObjectId);

    public int getLifeTime() {
        return getLifeTime(manifoldPointObjectId);
    }
    private native int getLifeTime(long manifoldPointObjectId);

    public Vector3f getLocalPointA() {
        return getLocalPointA(new Vector3f());
    }
    
    public Vector3f getLocalPointA(Vector3f localPointA) {
        getLocalPointA(manifoldPointObjectId, localPointA);
        return localPointA;
    }
    private native void getLocalPointA(long manifoldPointObjectId, Vector3f localPointA);

    public Vector3f getLocalPointB() {
        return getLocalPointB(new Vector3f());
    }
    
    public Vector3f getLocalPointB(Vector3f localPointB) {
        getLocalPointB(manifoldPointObjectId, localPointB);
        return localPointB;
    }
    private native void getLocalPointB(long manifoldPointObjectId, Vector3f localPointB);

    public Vector3f getNormalWorldOnB() {
        return getNormalWorldOnB(new Vector3f());
    }

    public Vector3f getNormalWorldOnB(Vector3f normalWorldOnB) {
        getNormalWorldOnB(manifoldPointObjectId, normalWorldOnB);
        return normalWorldOnB;
    }
    private native void getNormalWorldOnB(long manifoldPointObjectId, Vector3f normalWorldOnB);

    public int getPartId0() {
        return getPartId0(manifoldPointObjectId);
    }
    private native int getPartId0(long manifoldPointObjectId);

    public int getPartId1() {
        return getPartId1(manifoldPointObjectId);
    }

    private native int getPartId1(long manifoldPointObjectId);

    public Vector3f getPositionWorldOnA() {
        return getPositionWorldOnA(new Vector3f());
    }

    public Vector3f getPositionWorldOnA(Vector3f positionWorldOnA) {
        getPositionWorldOnA(positionWorldOnA);
        return positionWorldOnA;
    }
    private native void getPositionWorldOnA(long manifoldPointObjectId, Vector3f positionWorldOnA);

    public Vector3f getPositionWorldOnB() {
        return getPositionWorldOnB(new Vector3f());
    }

    public Vector3f getPositionWorldOnB(Vector3f positionWorldOnB) {
        getPositionWorldOnB(manifoldPointObjectId, positionWorldOnB);
        return positionWorldOnB;
    }
    private native void getPositionWorldOnB(long manifoldPointObjectId, Vector3f positionWorldOnB);

//    public Object getUserPersistentData() {
//        return userPersistentData;
//    }
}
