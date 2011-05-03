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
    public final Vector3f localPointA;
    public final Vector3f localPointB;
    public final Vector3f positionWorldOnB;
    public final Vector3f positionWorldOnA;
    public final Vector3f normalWorldOnB;
    public float distance1;
    public float combinedFriction;
    public float combinedRestitution;
    public int partId0;
    public int partId1;
    public int index0;
    public int index1;
    public Object userPersistentData;
    public float appliedImpulse;
    public boolean lateralFrictionInitialized;
    public float appliedImpulseLateral1;
    public float appliedImpulseLateral2;
    public int lifeTime;
    public final Vector3f lateralFrictionDir1;
    public final Vector3f lateralFrictionDir2;

    public PhysicsCollisionEvent(int type, PhysicsCollisionObject nodeA, PhysicsCollisionObject nodeB) {
        this(type, nodeA, nodeB, new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), 0, 0, 0, 0, 0, 0, 0, null, 0, false, 0, 0, 0, new Vector3f(), new Vector3f());
    }
    
    public PhysicsCollisionEvent(int type, PhysicsCollisionObject nodeA, PhysicsCollisionObject nodeB, Vector3f localPointA, Vector3f localPointB, Vector3f positionWorldOnB, Vector3f positionWorldOnA, Vector3f normalWorldOnB, float distance1, float combinedFriction, float combinedRestitution, int partId0, int partId1, int index0, int index1, Object userPersistentData, float appliedImpulse, boolean lateralFrictionInitialized, float appliedImpulseLateral1, float appliedImpulseLateral2, int lifeTime, Vector3f lateralFrictionDir1, Vector3f lateralFrictionDir2) {
        super(nodeA);
        this.type = type;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.localPointA = localPointA;
        this.localPointB = localPointB;
        this.positionWorldOnB = positionWorldOnB;
        this.positionWorldOnA = positionWorldOnA;
        this.normalWorldOnB = normalWorldOnB;
        this.distance1 = distance1;
        this.combinedFriction = combinedFriction;
        this.combinedRestitution = combinedRestitution;
        this.partId0 = partId0;
        this.partId1 = partId1;
        this.index0 = index0;
        this.index1 = index1;
        this.userPersistentData = userPersistentData;
        this.appliedImpulse = appliedImpulse;
        this.lateralFrictionInitialized = lateralFrictionInitialized;
        this.appliedImpulseLateral1 = appliedImpulseLateral1;
        this.appliedImpulseLateral2 = appliedImpulseLateral2;
        this.lifeTime = lifeTime;
        this.lateralFrictionDir1 = lateralFrictionDir1;
        this.lateralFrictionDir2 = lateralFrictionDir2;
    }

    /**
     * used by event factory, called when event is destroyed
     */
    public void clean() {
        source = null;
        this.type = 0;
        this.nodeA = null;
        this.nodeB = null;
//        this.localPointA = null;
//        this.localPointB = null;
//        this.positionWorldOnB = null;
//        this.positionWorldOnA = null;
//        this.normalWorldOnB = null;
//        this.distance1 = null
//        this.combinedFriction = null;
//        this.combinedRestitution = null;
//        this.partId0 = null;
//        this.partId1 = null;
//        this.index0 = null;
//        this.index1 = null;
        this.userPersistentData = null;
//        this.appliedImpulse = null;
//        this.lateralFrictionInitialized = null;
//        this.appliedImpulseLateral1 = null;
//        this.appliedImpulseLateral2 = null;
//        this.lifeTime = null;
//        this.lateralFrictionDir1 = null;
//        this.lateralFrictionDir2 = null;
    }

    /**
     * used by event factory, called when event reused
     */
    public void refactor(int type, PhysicsCollisionObject source, PhysicsCollisionObject nodeB, Vector3f localPointA, Vector3f localPointB, Vector3f positionWorldOnB, Vector3f positionWorldOnA, Vector3f normalWorldOnB, float distance1, float combinedFriction, float combinedRestitution, int partId0, int partId1, int index0, int index1, Object userPersistentData, float appliedImpulse, boolean lateralFrictionInitialized, float appliedImpulseLateral1, float appliedImpulseLateral2, int lifeTime, Vector3f lateralFrictionDir1, Vector3f lateralFrictionDir2) {
        this.source = source;
        this.type = type;
        this.nodeA = source;
        this.nodeB = nodeB;
        this.localPointA.set(localPointA);
        this.localPointB.set(localPointB);
        this.positionWorldOnB.set(positionWorldOnB);
        this.positionWorldOnA.set(positionWorldOnA);
        this.normalWorldOnB.set(normalWorldOnB);
        this.distance1 = distance1;
        this.combinedFriction = combinedFriction;
        this.combinedRestitution = combinedRestitution;
        this.partId0 = partId0;
        this.partId1 = partId1;
        this.index0 = index0;
        this.index1 = index1;
        this.userPersistentData = userPersistentData;
        this.appliedImpulse = appliedImpulse;
        this.lateralFrictionInitialized = lateralFrictionInitialized;
        this.appliedImpulseLateral1 = appliedImpulseLateral1;
        this.appliedImpulseLateral2 = appliedImpulseLateral2;
        this.lifeTime = lifeTime;
        this.lateralFrictionDir1.set(lateralFrictionDir1);
        this.lateralFrictionDir2.set(lateralFrictionDir2);
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
        return appliedImpulse;
    }

    public float getAppliedImpulseLateral1() {
        return appliedImpulseLateral1;
    }

    public float getAppliedImpulseLateral2() {
        return appliedImpulseLateral2;
    }

    public float getCombinedFriction() {
        return combinedFriction;
    }

    public float getCombinedRestitution() {
        return combinedRestitution;
    }

    public float getDistance1() {
        return distance1;
    }

    public int getIndex0() {
        return index0;
    }

    public int getIndex1() {
        return index1;
    }

    public Vector3f getLateralFrictionDir1() {
        return lateralFrictionDir1;
    }

    public Vector3f getLateralFrictionDir2() {
        return lateralFrictionDir2;
    }

    public boolean isLateralFrictionInitialized() {
        return lateralFrictionInitialized;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public Vector3f getLocalPointA() {
        return localPointA;
    }

    public Vector3f getLocalPointB() {
        return localPointB;
    }

    public Vector3f getNormalWorldOnB() {
        return normalWorldOnB;
    }

    public int getPartId0() {
        return partId0;
    }

    public int getPartId1() {
        return partId1;
    }

    public Vector3f getPositionWorldOnA() {
        return positionWorldOnA;
    }

    public Vector3f getPositionWorldOnB() {
        return positionWorldOnB;
    }

    public Object getUserPersistentData() {
        return userPersistentData;
    }
}
