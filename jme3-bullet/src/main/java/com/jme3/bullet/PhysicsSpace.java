/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.bullet;

import com.jme3.app.AppTask;
import com.jme3.bullet.collision.*;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A jbullet-jme physics space with its own btDynamicsWorld.
 *
 * @author normenhansen
 */
public class PhysicsSpace {

    /**
     * message logger for this class
     */
    private static final Logger logger = Logger.getLogger(PhysicsSpace.class.getName());
    /**
     * index of the X axis
     */
    public static final int AXIS_X = 0;
    /**
     * index of the Y axis
     */
    public static final int AXIS_Y = 1;
    /**
     * index of the Z axis
     */
    public static final int AXIS_Z = 2;
    /**
     * Bullet identifier of the physics space. The constructor sets this to a
     * non-zero value.
     */
    private long physicsSpaceId = 0;
    /**
     * first-in/first-out (FIFO) queue of physics tasks for each thread
     */
    private static ThreadLocal<ConcurrentLinkedQueue<AppTask<?>>> pQueueTL =
            new ThreadLocal<ConcurrentLinkedQueue<AppTask<?>>>() {
                @Override
                protected ConcurrentLinkedQueue<AppTask<?>> initialValue() {
                    return new ConcurrentLinkedQueue<AppTask<?>>();
                }
            };
    /**
     * first-in/first-out (FIFO) queue of physics tasks
     */
    private ConcurrentLinkedQueue<AppTask<?>> pQueue = new ConcurrentLinkedQueue<AppTask<?>>();
    /**
     * physics space for each thread
     */
    private static ThreadLocal<PhysicsSpace> physicsSpaceTL = new ThreadLocal<PhysicsSpace>();
    /**
     * copy of type of acceleration structure used
     */
    private BroadphaseType broadphaseType = BroadphaseType.DBVT;
//    private DiscreteDynamicsWorld dynamicsWorld = null;
//    private BroadphaseInterface broadphase;
//    private CollisionDispatcher dispatcher;
//    private ConstraintSolver solver;
//    private DefaultCollisionConfiguration collisionConfiguration;
//    private Map<GhostObject, PhysicsGhostObject> physicsGhostNodes = new ConcurrentHashMap<GhostObject, PhysicsGhostObject>();
    private Map<Long, PhysicsGhostObject> physicsGhostObjects = new ConcurrentHashMap<Long, PhysicsGhostObject>();
    private Map<Long, PhysicsCharacter> physicsCharacters = new ConcurrentHashMap<Long, PhysicsCharacter>();
    private Map<Long, PhysicsRigidBody> physicsBodies = new ConcurrentHashMap<Long, PhysicsRigidBody>();
    private Map<Long, PhysicsJoint> physicsJoints = new ConcurrentHashMap<Long, PhysicsJoint>();
    private Map<Long, PhysicsVehicle> physicsVehicles = new ConcurrentHashMap<Long, PhysicsVehicle>();
    /**
     * list of registered collision listeners
     */
    final private List<PhysicsCollisionListener> collisionListeners
            = new SafeArrayList<>(PhysicsCollisionListener.class);
    /**
     * queue of collision events not yet distributed to listeners
     */
    private ArrayDeque<PhysicsCollisionEvent> collisionEvents = new ArrayDeque<PhysicsCollisionEvent>();
    /**
     * map from collision groups to registered group listeners
     */
    private Map<Integer, PhysicsCollisionGroupListener> collisionGroupListeners = new ConcurrentHashMap<Integer, PhysicsCollisionGroupListener>();
    /**
     * queue of registered tick listeners
     */
    private ConcurrentLinkedQueue<PhysicsTickListener> tickListeners = new ConcurrentLinkedQueue<PhysicsTickListener>();
    private PhysicsCollisionEventFactory eventFactory = new PhysicsCollisionEventFactory();
    /**
     * copy of minimum coordinate values when using AXIS_SWEEP broadphase
     * algorithms
     */
    private Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    /**
     * copy of maximum coordinate values when using AXIS_SWEEP broadphase
     * algorithms
     */
    private Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    /**
     * physics time step (in seconds, &gt;0)
     */
    private float accuracy = 1f / 60f;
    /**
     * maximum number of physics steps per frame (&ge;0, default=4)
     */
    private int maxSubSteps = 4;
    /**
     * flags used in ray tests
     */
    private int rayTestFlags = 1 << 2;
    /**
     * copy of number of iterations used by the contact-and-constraint solver
     * (default=10)
     */
    private int solverNumIterations = 10;

    static {
//        System.loadLibrary("bulletjme");
//        initNativePhysics();
    }

    /**
     * Access the PhysicsSpace <b>running on this thread</b>. For parallel
     * physics, this can be invoked from the OpenGL thread.
     *
     * @return the PhysicsSpace running on this thread
     */
    public static PhysicsSpace getPhysicsSpace() {
        return physicsSpaceTL.get();
    }

    /**
     * Used internally
     *
     * @param space which physics space to simulate on this thread
     */
    public static void setLocalThreadPhysicsSpace(PhysicsSpace space) {
        physicsSpaceTL.set(space);
    }

    /**
     * Instantiate a PhysicsSpace. Must be invoked on the designated physics
     * thread.
     */
    public PhysicsSpace() {
        this(new Vector3f(-10000f, -10000f, -10000f), new Vector3f(10000f, 10000f, 10000f), BroadphaseType.DBVT);
    }

    /**
     * Instantiate a PhysicsSpace. Must be invoked on the designated physics
     * thread.
     */
    public PhysicsSpace(BroadphaseType broadphaseType) {
        this(new Vector3f(-10000f, -10000f, -10000f), new Vector3f(10000f, 10000f, 10000f), broadphaseType);
    }

    /**
     * Instantiate a PhysicsSpace. Must be invoked on the designated physics
     * thread.
     */
    public PhysicsSpace(Vector3f worldMin, Vector3f worldMax) {
        this(worldMin, worldMax, BroadphaseType.AXIS_SWEEP_3);
    }

    /**
     * Instantiate a PhysicsSpace. Must be invoked on the designated physics
     * thread.
     *
     * @param worldMin the desired minimum coordinates values (not null,
     * unaffected, default=-10k,-10k,-10k)
     * @param worldMax the desired minimum coordinates values (not null,
     * unaffected, default=10k,10k,10k)
     * @param broadphaseType which broadphase collision-detection algorithm to
     * use (not null)
     */
    public PhysicsSpace(Vector3f worldMin, Vector3f worldMax, BroadphaseType broadphaseType) {
        this.worldMin.set(worldMin);
        this.worldMax.set(worldMax);
        this.broadphaseType = broadphaseType;
        create();
    }

    /**
     * Must be invoked on the designated physics thread.
     */
    public void create() {
        physicsSpaceId = createPhysicsSpace(worldMin.x, worldMin.y, worldMin.z, worldMax.x, worldMax.y, worldMax.z, broadphaseType.ordinal(), false);
        pQueueTL.set(pQueue);
        physicsSpaceTL.set(this);

//        collisionConfiguration = new DefaultCollisionConfiguration();
//        dispatcher = new CollisionDispatcher(collisionConfiguration);
//        switch (broadphaseType) {
//            case SIMPLE:
//                broadphase = new SimpleBroadphase();
//                break;
//            case AXIS_SWEEP_3:
//                broadphase = new AxisSweep3(Converter.convert(worldMin), Converter.convert(worldMax));
//                break;
//            case AXIS_SWEEP_3_32:
//                broadphase = new AxisSweep3_32(Converter.convert(worldMin), Converter.convert(worldMax));
//                break;
//            case DBVT:
//                broadphase = new DbvtBroadphase();
//                break;
//        }
//
//        solver = new SequentialImpulseConstraintSolver();
//
//        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
//        dynamicsWorld.setGravity(new javax.vecmath.Vector3f(0, -9.81f, 0));
//
//        broadphase.getOverlappingPairCache().setInternalGhostPairCallback(new GhostPairCallback());
//        GImpactCollisionAlgorithm.registerAlgorithm(dispatcher);
//
//        //register filter callback for tick / collision
//        setTickCallback();
//        setContactCallbacks();
//        //register filter callback for collision groups
//        setOverlapFilterCallback();
    }

    private native long createPhysicsSpace(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int broadphaseType, boolean threading);

    /**
     * Callback invoked just before the physics is stepped.
     * <p>
     * This method is invoked from native code.
     *
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    private void preTick_native(float f) {
        AppTask task;
        while((task=pQueue.poll())!=null){
            if(task.isCancelled())continue;
            try{
                task.invoke();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        for (Iterator<PhysicsTickListener> it = tickListeners.iterator(); it.hasNext();) {
            PhysicsTickListener physicsTickCallback = it.next();
            physicsTickCallback.prePhysicsTick(this, f);
        }
    }

    /**
     * Callback invoked just after the physics is stepped.
     * <p>
     * This method is invoked from native code.
     *
     * @param timeStep the time per physics step (in seconds, &ge;0)
     */
    private void postTick_native(float f) {
        for (Iterator<PhysicsTickListener> it = tickListeners.iterator(); it.hasNext();) {
            PhysicsTickListener physicsTickCallback = it.next();
            physicsTickCallback.physicsTick(this, f);
        }
    }

    /**
     * This method is invoked from native code.
     */
    private void addCollision_native() {
    }

    private boolean needCollision_native(PhysicsCollisionObject objectA, PhysicsCollisionObject objectB) {
        return false;
    }

//    private void setOverlapFilterCallback() {
//        OverlapFilterCallback callback = new OverlapFilterCallback() {
//
//            public boolean needBroadphaseCollision(BroadphaseProxy bp, BroadphaseProxy bp1) {
//                boolean collides = (bp.collisionFilterGroup & bp1.collisionFilterMask) != 0;
//                if (collides) {
//                    collides = (bp1.collisionFilterGroup & bp.collisionFilterMask) != 0;
//                }
//                if (collides) {
//                    assert (bp.clientObject instanceof com.bulletphysics.collision.dispatch.CollisionObject && bp1.clientObject instanceof com.bulletphysics.collision.dispatch.CollisionObject);
//                    com.bulletphysics.collision.dispatch.CollisionObject colOb = (com.bulletphysics.collision.dispatch.CollisionObject) bp.clientObject;
//                    com.bulletphysics.collision.dispatch.CollisionObject colOb1 = (com.bulletphysics.collision.dispatch.CollisionObject) bp1.clientObject;
//                    assert (colOb.getUserPointer() != null && colOb1.getUserPointer() != null);
//                    PhysicsCollisionObject collisionObject = (PhysicsCollisionObject) colOb.getUserPointer();
//                    PhysicsCollisionObject collisionObject1 = (PhysicsCollisionObject) colOb1.getUserPointer();
//                    if ((collisionObject.getCollideWithGroups() & collisionObject1.getCollisionGroup()) > 0
//                            || (collisionObject1.getCollideWithGroups() & collisionObject.getCollisionGroup()) > 0) {
//                        PhysicsCollisionGroupListener listener = collisionGroupListeners.get(collisionObject.getCollisionGroup());
//                        PhysicsCollisionGroupListener listener1 = collisionGroupListeners.get(collisionObject1.getCollisionGroup());
//                        if (listener != null) {
//                            return listener.collide(collisionObject, collisionObject1);
//                        } else if (listener1 != null) {
//                            return listener1.collide(collisionObject, collisionObject1);
//                        }
//                        return true;
//                    } else {
//                        return false;
//                    }
//                }
//                return collides;
//            }
//        };
//        dynamicsWorld.getPairCache().setOverlapFilterCallback(callback);
//    }
//    private void setTickCallback() {
//        final PhysicsSpace space = this;
//        InternalTickCallback callback2 = new InternalTickCallback() {
//
//            @Override
//            public void internalTick(DynamicsWorld dw, float f) {
//                //execute task list
//                AppTask task = pQueue.poll();
//                task = pQueue.poll();
//                while (task != null) {
//                    while (task.isCancelled()) {
//                        task = pQueue.poll();
//                    }
//                    try {
//                        task.invoke();
//                    } catch (Exception ex) {
//                        logger.log(Level.SEVERE, null, ex);
//                    }
//                    task = pQueue.poll();
//                }
//                for (Iterator<PhysicsTickListener> it = tickListeners.iterator(); it.hasNext();) {
//                    PhysicsTickListener physicsTickCallback = it.next();
//                    physicsTickCallback.prePhysicsTick(space, f);
//                }
//            }
//        };
//        dynamicsWorld.setPreTickCallback(callback2);
//        InternalTickCallback callback = new InternalTickCallback() {
//
//            @Override
//            public void internalTick(DynamicsWorld dw, float f) {
//                for (Iterator<PhysicsTickListener> it = tickListeners.iterator(); it.hasNext();) {
//                    PhysicsTickListener physicsTickCallback = it.next();
//                    physicsTickCallback.physicsTick(space, f);
//                }
//            }
//        };
//        dynamicsWorld.setInternalTickCallback(callback, this);
//    }
//    private void setContactCallbacks() {
//        BulletGlobals.setContactAddedCallback(new ContactAddedCallback() {
//
//            public boolean contactAdded(ManifoldPoint cp, com.bulletphysics.collision.dispatch.CollisionObject colObj0,
//                    int partId0, int index0, com.bulletphysics.collision.dispatch.CollisionObject colObj1, int partId1,
//                    int index1) {
//                System.out.println("contact added");
//                return true;
//            }
//        });
//
//        BulletGlobals.setContactProcessedCallback(new ContactProcessedCallback() {
//
//            public boolean contactProcessed(ManifoldPoint cp, Object body0, Object body1) {
//                if (body0 instanceof CollisionObject && body1 instanceof CollisionObject) {
//                    PhysicsCollisionObject node = null, node1 = null;
//                    CollisionObject rBody0 = (CollisionObject) body0;
//                    CollisionObject rBody1 = (CollisionObject) body1;
//                    node = (PhysicsCollisionObject) rBody0.getUserPointer();
//                    node1 = (PhysicsCollisionObject) rBody1.getUserPointer();
//                    collisionEvents.add(eventFactory.getEvent(PhysicsCollisionEvent.TYPE_PROCESSED, node, node1, cp));
//                }
//                return true;
//            }
//        });
//
//        BulletGlobals.setContactDestroyedCallback(new ContactDestroyedCallback() {
//
//            public boolean contactDestroyed(Object userPersistentData) {
//                System.out.println("contact destroyed");
//                return true;
//            }
//        });
//    }
    private void addCollisionEvent_native(PhysicsCollisionObject node, PhysicsCollisionObject node1, long manifoldPointObjectId) {
//        System.out.println("addCollisionEvent:"+node.getObjectId()+" "+ node1.getObjectId());
        collisionEvents.add(eventFactory.getEvent(PhysicsCollisionEvent.TYPE_PROCESSED, node, node1, manifoldPointObjectId));
    }
    
    /**
     * This method is invoked from native code.
     */    
    private boolean notifyCollisionGroupListeners_native(PhysicsCollisionObject node, PhysicsCollisionObject node1){
        PhysicsCollisionGroupListener listener = collisionGroupListeners.get(node.getCollisionGroup());
        PhysicsCollisionGroupListener listener1 = collisionGroupListeners.get(node1.getCollisionGroup());
        boolean result = true;
        
        if(listener != null){
            result = listener.collide(node, node1);
        }
        if(listener1 != null && node.getCollisionGroup() != node1.getCollisionGroup()){
            result = listener1.collide(node, node1) && result;
        }
        
        return result;
    }

    /**
     * Update this space. Invoked (by the Bullet app state) once per frame while
     * the app state is attached and enabled.
     *
     * @param time time-per-frame multiplied by speed (in seconds, &ge;0)
     */
    public void update(float time) {
        update(time, maxSubSteps);
    }

    /**
     * Simulate for the specified time interval, using no more than the
     * specified number of steps.
     *
     * @param time the time interval (in seconds, &ge;0)
     * @param maxSteps the maximum number of steps (&ge;1)
     */
    public void update(float time, int maxSteps) {
//        if (getDynamicsWorld() == null) {
//            return;
//        }
        //step simulation
        stepSimulation(physicsSpaceId, time, maxSteps, accuracy);
    }

    private native void stepSimulation(long space, float time, int maxSteps, float accuracy);

    /**
     * Distribute each collision event to all listeners.
     */
    public void distributeEvents() {
        //add collision callbacks
        int clistsize = collisionListeners.size();
        while( collisionEvents.isEmpty() == false ) {
            PhysicsCollisionEvent physicsCollisionEvent = collisionEvents.pop();
            for(int i=0;i<clistsize;i++) {
                collisionListeners.get(i).collision(physicsCollisionEvent);
            }
            //recycle events
            eventFactory.recycle(physicsCollisionEvent);
        }
    }

    /**
     * Enqueue a callable on the currently executing thread.
     *
     * @param <V> the task's result type
     * @param callable the task to be executed
     * @return a new task (not null)
     */
    public static <V> Future<V> enqueueOnThisThread(Callable<V> callable) {
        AppTask<V> task = new AppTask<V>(callable);
        System.out.println("created apptask");
        pQueueTL.get().add(task);
        return task;
    }

    /**
     * Invoke the specified callable during the next physics tick. This is
     * useful for applying forces.
     *
     * @param <V> the return type of the callable
     * @param callable which callable to invoke
     * @return Future object
     */
    public <V> Future<V> enqueue(Callable<V> callable) {
        AppTask<V> task = new AppTask<V>(callable);
        pQueue.add(task);
        return task;
    }

    /**
     * Add the specified object to this space.
     *
     * @param obj the PhysicsControl, Spatial-with-PhysicsControl,
     * PhysicsCollisionObject, or PhysicsJoint to add (not null, modified)
     */
    public void add(Object obj) {
        if (obj instanceof PhysicsControl) {
            ((PhysicsControl) obj).setPhysicsSpace(this);
        } else if (obj instanceof Spatial) {
            Spatial node = (Spatial) obj;
            for (int i = 0; i < node.getNumControls(); i++) {
                if (node.getControl(i) instanceof PhysicsControl) {
                    add(node.getControl(i));
                }
            }
        } else if (obj instanceof PhysicsCollisionObject) {
            addCollisionObject((PhysicsCollisionObject) obj);
        } else if (obj instanceof PhysicsJoint) {
            addJoint((PhysicsJoint) obj);
        } else {
            throw (new UnsupportedOperationException("Cannot add this kind of object to the physics space."));
        }
    }

    /**
     * Add the specified collision object to this space.
     *
     * @param obj the PhysicsCollisionObject to add (not null, modified)
     */
    public void addCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsGhostObject) {
            addGhostObject((PhysicsGhostObject) obj);
        } else if (obj instanceof PhysicsRigidBody) {
            addRigidBody((PhysicsRigidBody) obj);
        } else if (obj instanceof PhysicsVehicle) {
            addRigidBody((PhysicsVehicle) obj);
        } else if (obj instanceof PhysicsCharacter) {
            addCharacter((PhysicsCharacter) obj);
        }
    }

    /**
     * Remove the specified object from this space.
     *
     * @param obj the PhysicsCollisionObject to add, or null (modified)
     */
    public void remove(Object obj) {
        if (obj == null) return;
        if (obj instanceof PhysicsControl) {
            ((PhysicsControl) obj).setPhysicsSpace(null);
        } else if (obj instanceof Spatial) {
            Spatial node = (Spatial) obj;
            for (int i = 0; i < node.getNumControls(); i++) {
                if (node.getControl(i) instanceof PhysicsControl) {
                    remove(node.getControl(i));
                }
            }
        } else if (obj instanceof PhysicsCollisionObject) {
            removeCollisionObject((PhysicsCollisionObject) obj);
        } else if (obj instanceof PhysicsJoint) {
            removeJoint((PhysicsJoint) obj);
        } else {
            throw (new UnsupportedOperationException("Cannot remove this kind of object from the physics space."));
        }
    }

    /**
     * Remove the specified collision object from this space.
     *
     * @param obj the PhysicsControl or Spatial with PhysicsControl to remove
     */
    public void removeCollisionObject(PhysicsCollisionObject obj) {
        if (obj instanceof PhysicsGhostObject) {
            removeGhostObject((PhysicsGhostObject) obj);
        } else if (obj instanceof PhysicsRigidBody) {
            removeRigidBody((PhysicsRigidBody) obj);
        } else if (obj instanceof PhysicsCharacter) {
            removeCharacter((PhysicsCharacter) obj);
        }
    }

    /**
     * Add all collision objects and joints in the specified subtree of the
     * scene graph to this space (e.g. after loading from disk). Note:
     * recursive!
     *
     * @param spatial the root of the subtree (not null)
     */
    public void addAll(Spatial spatial) {
        add(spatial);

        if (spatial.getControl(RigidBodyControl.class) != null) {
            RigidBodyControl physicsNode = spatial.getControl(RigidBodyControl.class);
            //add joints with physicsNode as BodyA
            List<PhysicsJoint> joints = physicsNode.getJoints();
            for (Iterator<PhysicsJoint> it1 = joints.iterator(); it1.hasNext();) {
                PhysicsJoint physicsJoint = it1.next();
                if (physicsNode.equals(physicsJoint.getBodyA())) {
                    //add(physicsJoint.getBodyB());
                    add(physicsJoint);
                }
            }
        }
        //recursion
        if (spatial instanceof Node) {
            List<Spatial> children = ((Node) spatial).getChildren();
            for (Iterator<Spatial> it = children.iterator(); it.hasNext();) {
                Spatial spat = it.next();
                addAll(spat);
            }
        }
    }

    /**
     * Remove all physics controls and joints in the specified subtree of the
     * scene graph from the physics space (e.g. before saving to disk) Note:
     * recursive!
     *
     * @param spatial the root of the subtree (not null)
     */
    public void removeAll(Spatial spatial) {
        if (spatial.getControl(RigidBodyControl.class) != null) {
            RigidBodyControl physicsNode = spatial.getControl(RigidBodyControl.class);
            //remove joints with physicsNode as BodyA
            List<PhysicsJoint> joints = physicsNode.getJoints();
            for (Iterator<PhysicsJoint> it1 = joints.iterator(); it1.hasNext();) {
                PhysicsJoint physicsJoint = it1.next();
                if (physicsNode.equals(physicsJoint.getBodyA())) {
                    removeJoint(physicsJoint);
                    //remove(physicsJoint.getBodyB());
                }
            }
        }
            
        remove(spatial);
        //recursion
        if (spatial instanceof Node) {
            List<Spatial> children = ((Node) spatial).getChildren();
            for (Iterator<Spatial> it = children.iterator(); it.hasNext();) {
                Spatial spat = it.next();
                removeAll(spat);
            }
        }
    }

    private native void addCollisionObject(long space, long id);

    private native void removeCollisionObject(long space, long id);

    private native void addRigidBody(long space, long id);

    private native void removeRigidBody(long space, long id);

    private native void addCharacterObject(long space, long id);

    private native void removeCharacterObject(long space, long id);

    private native void addAction(long space, long id);

    private native void removeAction(long space, long id);

    private native void addVehicle(long space, long id);

    private native void removeVehicle(long space, long id);

    private native void addConstraint(long space, long id);

    private native void addConstraintC(long space, long id, boolean collision);

    private native void removeConstraint(long space, long id);

    private void addGhostObject(PhysicsGhostObject node) {
        if (physicsGhostObjects.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "GhostObject {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsGhostObjects.put(node.getObjectId(), node);
        logger.log(Level.FINE, "Adding ghost object {0} to physics space.", Long.toHexString(node.getObjectId()));
        addCollisionObject(physicsSpaceId, node.getObjectId());
    }

    private void removeGhostObject(PhysicsGhostObject node) {
        if (!physicsGhostObjects.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "GhostObject {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        physicsGhostObjects.remove(node.getObjectId());
        logger.log(Level.FINE, "Removing ghost object {0} from physics space.", Long.toHexString(node.getObjectId()));
        removeCollisionObject(physicsSpaceId, node.getObjectId());
    }

    private void addCharacter(PhysicsCharacter node) {
        if (physicsCharacters.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "Character {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsCharacters.put(node.getObjectId(), node);
        logger.log(Level.FINE, "Adding character {0} to physics space.", Long.toHexString(node.getObjectId()));
        addCharacterObject(physicsSpaceId, node.getObjectId());
        addAction(physicsSpaceId, node.getControllerId());
//        dynamicsWorld.addCollisionObject(node.getObjectId(), CollisionFilterGroups.CHARACTER_FILTER, (short) (CollisionFilterGroups.STATIC_FILTER | CollisionFilterGroups.DEFAULT_FILTER));
//        dynamicsWorld.addAction(node.getControllerId());
    }

    private void removeCharacter(PhysicsCharacter node) {
        if (!physicsCharacters.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "Character {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        physicsCharacters.remove(node.getObjectId());
        logger.log(Level.FINE, "Removing character {0} from physics space.", Long.toHexString(node.getObjectId()));
        removeAction(physicsSpaceId, node.getControllerId());
        removeCharacterObject(physicsSpaceId, node.getObjectId());
//        dynamicsWorld.removeAction(node.getControllerId());
//        dynamicsWorld.removeCollisionObject(node.getObjectId());
    }

    /**
     * NOTE: When a rigid body is added, its gravity gets set to that of the
     * physics space.
     *
     * @param node the body to add (not null, not already in the space)
     */
    private void addRigidBody(PhysicsRigidBody node) {
        if (physicsBodies.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "RigidBody {0} already exists in PhysicsSpace, cannot add.", node);
            return;
        }
        physicsBodies.put(node.getObjectId(), node);

        //Workaround
        //It seems that adding a Kinematic RigidBody to the dynamicWorld prevents it from being non-kinematic again afterward.
        //so we add it non kinematic, then set it kinematic again.
        boolean kinematic = false;
        if (node.isKinematic()) {
            kinematic = true;
            node.setKinematic(false);
        }
        addRigidBody(physicsSpaceId, node.getObjectId());
        if (kinematic) {
            node.setKinematic(true);
        }

        logger.log(Level.FINE, "Adding RigidBody {0} to physics space.", node.getObjectId());
        if (node instanceof PhysicsVehicle) {
            PhysicsVehicle vehicle = (PhysicsVehicle) node;
            vehicle.createVehicle(this);
            long vehicleId = vehicle.getVehicleId();
            assert vehicleId != 0L;
            logger.log(Level.FINE,
                    "Adding vehicle constraint {0} to physics space.",
                    Long.toHexString(vehicleId));
            physicsVehicles.put(vehicleId, vehicle);
            addVehicle(physicsSpaceId, vehicleId);
        }
    }

    private void removeRigidBody(PhysicsRigidBody node) {
        if (!physicsBodies.containsKey(node.getObjectId())) {
            logger.log(Level.WARNING, "RigidBody {0} does not exist in PhysicsSpace, cannot remove.", node);
            return;
        }
        if (node instanceof PhysicsVehicle) {
            logger.log(Level.FINE, "Removing vehicle constraint {0} from physics space.", Long.toHexString(((PhysicsVehicle) node).getVehicleId()));
            physicsVehicles.remove(((PhysicsVehicle) node).getVehicleId());
            removeVehicle(physicsSpaceId, ((PhysicsVehicle) node).getVehicleId());
        }
        logger.log(Level.FINE, "Removing RigidBody {0} from physics space.", Long.toHexString(node.getObjectId()));
        physicsBodies.remove(node.getObjectId());
        removeRigidBody(physicsSpaceId, node.getObjectId());
    }

    private void addJoint(PhysicsJoint joint) {
        if (physicsJoints.containsKey(joint.getObjectId())) {
            logger.log(Level.WARNING, "Joint {0} already exists in PhysicsSpace, cannot add.", joint);
            return;
        }
        logger.log(Level.FINE, "Adding Joint {0} to physics space.", Long.toHexString(joint.getObjectId()));
        physicsJoints.put(joint.getObjectId(), joint);
        addConstraintC(physicsSpaceId, joint.getObjectId(), !joint.isCollisionBetweenLinkedBodys());
//        dynamicsWorld.addConstraint(joint.getObjectId(), !joint.isCollisionBetweenLinkedBodys());
    }

    private void removeJoint(PhysicsJoint joint) {
        if (!physicsJoints.containsKey(joint.getObjectId())) {
            logger.log(Level.WARNING, "Joint {0} does not exist in PhysicsSpace, cannot remove.", joint);
            return;
        }
        logger.log(Level.FINE, "Removing Joint {0} from physics space.", Long.toHexString(joint.getObjectId()));
        physicsJoints.remove(joint.getObjectId());
        removeConstraint(physicsSpaceId, joint.getObjectId());
//        dynamicsWorld.removeConstraint(joint.getObjectId());
    }

    /**
     * Copy the list of rigid bodies that have been added to this space and not
     * yet removed.
     *
     * @return a new list (not null)
     */
    public Collection<PhysicsRigidBody> getRigidBodyList() {
        return new LinkedList<PhysicsRigidBody>(physicsBodies.values());
    }

    /**
     * Copy the list of ghost objects that have been added to this space and not
     * yet removed.
     *
     * @return a new list (not null)
     */
    public Collection<PhysicsGhostObject> getGhostObjectList() {
        return new LinkedList<PhysicsGhostObject>(physicsGhostObjects.values());
    }

    /**
     * Copy the list of physics characters that have been added to this space
     * and not yet removed.
     *
     * @return a new list (not null)
     */
    public Collection<PhysicsCharacter> getCharacterList() {
        return new LinkedList<PhysicsCharacter>(physicsCharacters.values());
    }

    /**
     * Copy the list of physics joints that have been added to this space and
     * not yet removed.
     *
     * @return a new list (not null)
     */
    public Collection<PhysicsJoint> getJointList() {
        return new LinkedList<PhysicsJoint>(physicsJoints.values());
    }

    /**
     * Copy the list of physics vehicles that have been added to this space and
     * not yet removed.
     *
     * @return a new list (not null)
     */
    public Collection<PhysicsVehicle> getVehicleList() {
        return new LinkedList<PhysicsVehicle>(physicsVehicles.values());
    }

    /**
     * Alter the gravitational acceleration acting on newly-added bodies.
     * <p>
     * Whenever a rigid body is added to a space, the body's gravity gets set to
     * that of the space. Thus it makes sense to set the space's vector before
     * adding any bodies to the space.
     *
     * @param gravity the desired acceleration vector (not null, unaffected)
     */
    public void setGravity(Vector3f gravity) {
        this.gravity.set(gravity);
        setGravity(physicsSpaceId, gravity);
    }

    private native void setGravity(long spaceId, Vector3f gravity);

    /**
     * copy of gravity-acceleration vector (default is 9.81 in the -Y direction,
     * corresponding to Earth-normal in MKS units)
     */
    private final Vector3f gravity = new Vector3f(0,-9.81f,0);
    /**
     * Copy the gravitational acceleration acting on newly-added bodies.
     *
     * @param gravity storage for the result (not null, modified)
     * @return acceleration (in the vector provided)
     */
    public Vector3f getGravity(Vector3f gravity) {
        return gravity.set(this.gravity);
    }

//    /**
//     * applies gravity value to all objects
//     */
//    public void applyGravity() {
////        dynamicsWorld.applyGravity();
//    }
//
//    /**
//     * clears forces of all objects
//     */
//    public void clearForces() {
////        dynamicsWorld.clearForces();
//    }
//
    /**
     * Register the specified tick listener with this space.
     * <p>
     * Tick listeners are notified before and after each physics step. A physics
     * step is not necessarily the same as a frame; it is more influenced by the
     * accuracy of the physics space.
     *
     * @see #setAccuracy(float)
     *
     * @param listener the listener to register (not null)
     */
    public void addTickListener(PhysicsTickListener listener) {
        tickListeners.add(listener);
    }

    /**
     * De-register the specified tick listener.
     *
     * @see #addTickListener(com.jme3.bullet.PhysicsTickListener)
     * @param listener the listener to de-register (not null)
     */
    public void removeTickListener(PhysicsTickListener listener) {
        tickListeners.remove(listener);
    }

    /**
     * Register the specified collision listener with this space.
     * <p>
     * Collision listeners are notified when collisions occur in the space.
     *
     * @param listener the listener to register (not null, alias created)
     */
    public void addCollisionListener(PhysicsCollisionListener listener) {
        collisionListeners.add(listener);
    }

    /**
     * De-register the specified collision listener.
     *
     * @see
     * #addCollisionListener(com.jme3.bullet.collision.PhysicsCollisionListener)
     * @param listener the listener to de-register (not null)
     */
    public void removeCollisionListener(PhysicsCollisionListener listener) {
        collisionListeners.remove(listener);
    }

    /**
     * Register the specified collision-group listener with the specified
     * collision group of this space.
     * <p>
     * Such a listener can disable collisions when they occur. There can be only
     * one listener per collision group per space.
     *
     * @param listener the listener to register (not null)
     * @param collisionGroup which group it should listen for (bit mask with
     * exactly one bit set)
     */
    public void addCollisionGroupListener(PhysicsCollisionGroupListener listener, int collisionGroup) {
        collisionGroupListeners.put(collisionGroup, listener);
    }

    /**
     * De-register the specified collision-group listener.
     *
     * @see
     * #addCollisionGroupListener(com.jme3.bullet.collision.PhysicsCollisionGroupListener,
     * int)
     * @param collisionGroup the group of the listener to de-register (bit mask
     * with exactly one bit set)
     */
    public void removeCollisionGroupListener(int collisionGroup) {
        collisionGroupListeners.remove(collisionGroup);
    }
    
    /**
     * Perform a ray-collision test and return the results as a list of
     * PhysicsRayTestResults sorted by ascending hitFraction.
     *
     * @param from the starting location (physics-space coordinates, not null,
     * unaffected)
     * @param to the ending location (in physics-space coordinates, not null,
     * unaffected)
     * @return a new list of results (not null)
     */
    public List<PhysicsRayTestResult> rayTest(Vector3f from, Vector3f to) {
        List<PhysicsRayTestResult> results = new ArrayList<PhysicsRayTestResult>();
        rayTest(from, to, results);
        
        return results;
    }
    
    /**
     * Perform a ray-collision test and return the results as a list of
     * PhysicsRayTestResults in arbitrary order.
     *
     * @param from the starting location (in physics-space coordinates, not
     * null, unaffected)
     * @param to the ending location (in physics-space coordinates, not null,
     * unaffected)
     * @return a new list of results (not null)
     */
    public List<PhysicsRayTestResult> rayTestRaw(Vector3f from, Vector3f to) {
        List<PhysicsRayTestResult> results = new ArrayList<PhysicsRayTestResult>();
        rayTestRaw(from, to, results);
        
        return results;
    }

    /**
     * Alters the m_flags used in ray tests. see
     * https://code.google.com/p/bullet/source/browse/trunk/src/BulletCollision/NarrowPhaseCollision/btRaycastCallback.h
     * for possible options. Defaults to using the faster, approximate raytest.
     *
     * @param flags the desired flags, ORed together (default=0x4)
     */
    public void SetRayTestFlags(int flags) {
        rayTestFlags = flags;
    }

    /**
     * Reads m_flags used in ray tests. see
     * https://code.google.com/p/bullet/source/browse/trunk/src/BulletCollision/NarrowPhaseCollision/btRaycastCallback.h
     * for possible options.
     *
     * @return which flags are used
     */
    public int GetRayTestFlags() {
        return rayTestFlags;
    }

    private static Comparator<PhysicsRayTestResult> hitFractionComparator = new Comparator<PhysicsRayTestResult>() {
        @Override
        public int compare(PhysicsRayTestResult r1, PhysicsRayTestResult r2) {
            float comp = r1.getHitFraction() - r2.getHitFraction();
            return comp > 0 ? 1 : -1;
        }
    };
    
    /**
     * Perform a ray-collision test and return the results as a list of
     * PhysicsRayTestResults sorted by ascending hitFraction.
     *
     * @param from coordinates of the starting location (in physics space, not
     * null, unaffected)
     * @param to coordinates of the ending location (in physics space, not null,
     * unaffected)
     * @param results the list to hold results (not null, modified)
     * @return results
     */
    public List<PhysicsRayTestResult> rayTest(Vector3f from, Vector3f to, List<PhysicsRayTestResult> results) {
        results.clear();
        rayTest_native(from, to, physicsSpaceId, results, rayTestFlags);
        
        Collections.sort(results, hitFractionComparator);
        return results;
    }
    
    /**
     * Perform a ray-collision test and return the results as a list of
     * PhysicsRayTestResults in arbitrary order.
     *
     * @param from coordinates of the starting location (in physics space, not
     * null, unaffected)
     * @param to coordinates of the ending location (in physics space, not null,
     * unaffected)
     * @param results the list to hold results (not null, modified)
     * @return results
     */
    public List<PhysicsRayTestResult> rayTestRaw(Vector3f from, Vector3f to, List<PhysicsRayTestResult> results) {
        results.clear();
        rayTest_native(from, to, physicsSpaceId, results, rayTestFlags);
        return results;
    }

    public native void rayTest_native(Vector3f from, Vector3f to, long physicsSpaceId, List<PhysicsRayTestResult> results, int flags);

//    private class InternalRayListener extends CollisionWorld.RayResultCallback {
//
//        private List<PhysicsRayTestResult> results;
//
//        public InternalRayListener(List<PhysicsRayTestResult> results) {
//            this.results = results;
//        }
//
//        @Override
//        public float addSingleResult(LocalRayResult lrr, boolean bln) {
//            PhysicsCollisionObject obj = (PhysicsCollisionObject) lrr.collisionObject.getUserPointer();
//            results.add(new PhysicsRayTestResult(obj, Converter.convert(lrr.hitNormalLocal), lrr.hitFraction, bln));
//            return lrr.hitFraction;
//        }
//    }
//
//


    /**
     * Perform a sweep-collision test and return the results as a new list.
     * <p>
     * The starting and ending locations must be at least 0.4f physics-space
     * units apart.
     * <p>
     * A sweep test will miss a collision if it starts inside an object and
     * sweeps away from the object's center.
     *
     * @param shape the shape to sweep (not null)
     * @param start the starting physics-space transform (not null)
     * @param end the ending physics-space transform (not null)
     * @return a new list of results
     */
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape, Transform start, Transform end) {
        List results = new LinkedList();
        sweepTest(shape, start, end , results);
        return results;
    }

    /**
     * Perform a sweep-collision test and store the results in an existing list.
     * <p>
     * The starting and ending locations must be at least 0.4f physics-space
     * units apart.
     * <p>
     * A sweep test will miss a collision if it starts inside an object and
     * sweeps away from the object's center.
     *
     * @param shape the shape to sweep (not null)
     * @param start the starting physics-space transform (not null)
     * @param end the ending physics-space transform (not null)
     * @param results the list to hold results (not null, modified)
     * @return results
     */    
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape, Transform start, Transform end, List<PhysicsSweepTestResult> results) {
        return sweepTest(shape, start, end, results, 0.0f);
    }

    public native void sweepTest_native(long shape, Transform from, Transform to, long physicsSpaceId, List<PhysicsSweepTestResult> results, float allowedCcdPenetration);
    /**
     * Perform a sweep-collision test and store the results in an existing list.
     * <p>
     * The starting and ending locations must be at least 0.4f physics-space
     * units apart.
     * <p>
     * A sweep test will miss a collision if it starts inside an object and
     * sweeps away from the object's center.
     *
     * @param shape the shape to sweep (not null)
     * @param start the starting physics-space transform (not null)
     * @param end the ending physics-space transform (not null)
     * @param results the list to hold results (not null, modified)
     * @param allowedCcdPenetration true&rarr;allow, false&rarr;disallow
     * @return results
     */
    public List<PhysicsSweepTestResult> sweepTest(CollisionShape shape, Transform start, Transform end, List<PhysicsSweepTestResult> results, float allowedCcdPenetration ) {
        results.clear();
        sweepTest_native(shape.getObjectId(), start, end, physicsSpaceId, results, allowedCcdPenetration);
        return results;
    }

/*    private class InternalSweepListener extends CollisionWorld.ConvexResultCallback {

        private List<PhysicsSweepTestResult> results;

        public InternalSweepListener(List<PhysicsSweepTestResult> results) {
            this.results = results;
        }

        @Override
        public float addSingleResult(LocalConvexResult lcr, boolean bln) {
            PhysicsCollisionObject obj = (PhysicsCollisionObject) lcr.hitCollisionObject.getUserPointer();
            results.add(new PhysicsSweepTestResult(obj, Converter.convert(lcr.hitNormalLocal), lcr.hitFraction, bln));
            return lcr.hitFraction;
        }
    }

    */
    
    /**
     * Destroy this space so that a new one can be instantiated.
     */
    public void destroy() {
        physicsBodies.clear();
        physicsJoints.clear();

//        dynamicsWorld.destroy();
//        dynamicsWorld = null;
    }

    /**
     * // * used internally //
     *
     * @return the dynamicsWorld //
     */
    public long getSpaceId() {
        return physicsSpaceId;
    }

    /**
     * Read the type of acceleration structure used.
     *
     * @return an enum value (not null)
     */
    public BroadphaseType getBroadphaseType() {
        return broadphaseType;
    }

    /**
     * Alter the type of acceleration structure used.
     * 
     * @param broadphaseType the desired algorithm (not null)
     */
    public void setBroadphaseType(BroadphaseType broadphaseType) {
        this.broadphaseType = broadphaseType;
    }

    /**
     * Alter the maximum number of physics steps per frame.
     * <p>
     * Extra physics steps help maintain determinism when the render fps drops
     * below 1/accuracy. For example a value of 2 can compensate for frame rates
     * as low as 30fps, assuming the physics has an accuracy of 1/60 sec.
     * <p>
     * Setting this value too high can depress the frame rate.
     *
     * @param steps the desired maximum number of steps per frame (&ge;1,
     * default=4)
     */
    public void setMaxSubSteps(int steps) {
        maxSubSteps = steps;
    }

    /**
     * Read the accuracy (time step) of the physics simulation.
     *
     * @return the timestep (in seconds, &gt;0)
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * Alter the accuracy (time step) of the physics simulation.
     * <p>
     * In general, the smaller the time step, the more accurate (and
     * compute-intensive) the simulation will be. Bullet works best with a
     * time step of no more than 1/60 second.
     *
     * @param accuracy the desired time step (in seconds, &gt;0, default=1/60)
     */
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }

    /**
     * Access the minimum coordinate values for this space.
     *
     * @return the pre-existing vector
     */
    public Vector3f getWorldMin() {
        return worldMin;
    }

    /**
     * Alter the minimum coordinate values for this space. (only affects
     * AXIS_SWEEP broadphase algorithms)
     *
     * @param worldMin the desired minimum coordinate values (not null,
     * unaffected)
     */
    public void setWorldMin(Vector3f worldMin) {
        this.worldMin.set(worldMin);
    }

    /**
     * Access the maximum coordinate values for this space.
     *
     * @return the pre-existing vector (not null)
     */
    public Vector3f getWorldMax() {
        return worldMax;
    }

    /**
     * only applies for AXIS_SWEEP broadphase
     *
     * @param worldMax
     */
    public void setWorldMax(Vector3f worldMax) {
        this.worldMax.set(worldMax);
    }

    /**
     * Alter the number of iterations used by the contact-and-constraint solver.
     * <p>
     * Use 4 for low quality, 20 for high quality.
     *
     * @param numIterations the desired number of iterations (&ge;1, default=10)
     */
    public void setSolverNumIterations(int numIterations) {
        this.solverNumIterations = numIterations;
        setSolverNumIterations(physicsSpaceId, numIterations);
    }
    
    /**
     * Read the number of iterations used by the contact-and-constraint solver.
     *
     * @return the number of iterations used
     */
    public int getSolverNumIterations() {
        return solverNumIterations;
    }
    
    private native void setSolverNumIterations(long physicsSpaceId, int numIterations);
    
    public static native void initNativePhysics();

    /**
     * Enumerate the available acceleration structures for broadphase collision
     * detection.
     */
    public enum BroadphaseType {

        /**
         * btSimpleBroadphase: a brute-force reference implementation for
         * debugging purposes
         */
        SIMPLE,
        /**
         * btAxisSweep3: uses incremental 3-D sweep and prune, requires world
         * bounds, limited to 16_384 objects
         */
        AXIS_SWEEP_3,
        /**
         * bt32BitAxisSweep3: uses incremental 3-D sweep and prune, requires
         * world bounds, limited to 65_536 objects
         */
        AXIS_SWEEP_3_32,
        /**
         * btDbvtBroadphase: uses a fast, dynamic bounding-volume hierarchy
         * based on AABB tree to allow quicker addition/removal of physics
         * objects
         */
        DBVT;
    }

    /**
     * Finalize this physics space just before it is destroyed. Should be
     * invoked only by a subclass or by the garbage collector.
     *
     * @throws Throwable ignored by the garbage collector
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Finalizing PhysicsSpace {0}", Long.toHexString(physicsSpaceId));
        finalizeNative(physicsSpaceId);
    }

    private native void finalizeNative(long objectId);
}
