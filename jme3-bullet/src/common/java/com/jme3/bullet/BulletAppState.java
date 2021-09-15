/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An app state to manage a single Bullet physics space.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class BulletAppState
        extends AbstractAppState
        implements PhysicsTickListener {

    /**
     * true if-and-only-if the physics simulation is running (started but not
     * yet stopped)
     */
    protected volatile boolean isRunning = false;
    protected Application app;
    /**
     * manager that manages this state, set during attach
     */
    protected AppStateManager stateManager;
    /**
     * executor service for physics tasks, or null if parallel simulation is not
     * running
     */
    protected ScheduledThreadPoolExecutor executor;
    /**
     * physics space managed by this state, or null if no simulation running
     */
    protected PhysicsSpace pSpace;
    /**
     * threading mode to use (not null)
     */
    protected ThreadingType threadingType = ThreadingType.SEQUENTIAL;
    /**
     * broadphase collision-detection algorithm for the physics space to use
     * (not null)
     */
    protected BroadphaseType broadphaseType = BroadphaseType.DBVT;
    /**
     * minimum coordinate values for the physics space when using AXIS_SWEEP
     * broadphase algorithms (not null)
     */
    protected Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    /**
     * maximum coordinate values for the physics space when using AXIS_SWEEP
     * broadphase algorithms (not null)
     */
    protected Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    /**
     * simulation speed multiplier (default=1, paused=0)
     */
    protected float speed = 1;
    /**
     * true if-and-only-if this state is enabled
     */
    protected boolean active = true;
    /**
     * true if-and-only-if debug visualization is enabled
     */
    protected boolean debugEnabled = false;
    /**
     * app state to manage the debug visualization, or null if none
     */
    protected BulletDebugAppState debugAppState;
    /**
     * time interval between frames (in seconds) from the most recent update
     */
    protected float tpf;
    /**
     * current physics task, or null if none
     */
    protected Future physicsFuture;

    /**
     * Instantiate an app state to manage a new PhysicsSpace with DBVT collision
     * detection.
     * <p>
     * Use getStateManager().addState(bulletAppState) to start physics.
     */
    public BulletAppState() {
    }

    /**
     * Instantiate an app state to manage a new PhysicsSpace.
     * <p>
     * Use getStateManager().addState(bulletAppState) to start physics.
     *
     * @param broadphaseType which broadphase collision-detection algorithm to
     * use (not null)
     */
    public BulletAppState(BroadphaseType broadphaseType) {
        this(new Vector3f(-10000f, -10000f, -10000f), new Vector3f(10000f, 10000f, 10000f), broadphaseType);
    }

    /**
     * Instantiate an app state to manage a new PhysicsSpace with AXIS_SWEEP_3
     * collision detection.
     * <p>
     * Use getStateManager().addState(bulletAppState) to start physics.
     *
     * @param worldMin the desired minimum coordinate values (not null,
     * unaffected, default=-10k,-10k,-10k)
     * @param worldMax the desired maximum coordinate values (not null,
     * unaffected, default=10k,10k,10k)
     */
    public BulletAppState(Vector3f worldMin, Vector3f worldMax) {
        this(worldMin, worldMax, BroadphaseType.AXIS_SWEEP_3);
    }

    /**
     * Instantiate an app state to manage a new PhysicsSpace.
     * <p>
     * Use getStateManager().addState(bulletAppState) to enable physics.
     *
     * @param worldMin the desired minimum coordinate values (not null,
     * unaffected, default=-10k,-10k,-10k)
     * @param worldMax the desired maximum coordinate values (not null,
     * unaffected, default=10k,10k,10k)
     * @param broadphaseType which broadphase collision-detection algorithm to
     * use (not null)
     */
    public BulletAppState(Vector3f worldMin, Vector3f worldMax, BroadphaseType broadphaseType) {
        this.worldMin.set(worldMin);
        this.worldMax.set(worldMax);
        this.broadphaseType = broadphaseType;
    }

    /**
     * Allocate the physics space and start physics for ThreadingType.PARALLEL.
     *
     * @return true if successful, otherwise false
     */
    private boolean startPhysicsOnExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
        executor = new ScheduledThreadPoolExecutor(1);
        final BulletAppState app = this;
        Callable<Boolean> call = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                detachedPhysicsLastUpdate = System.currentTimeMillis();
                pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
                pSpace.addTickListener(app);
                return true;
            }
        };
        try {
            return executor.submit(call).get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(BulletAppState.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    private Callable<Boolean> parallelPhysicsUpdate = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            pSpace.update(tpf * getSpeed());
            return true;
        }
    };
    long detachedPhysicsLastUpdate = 0;
    private Callable<Boolean> detachedPhysicsUpdate = new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
            pSpace.update(getPhysicsSpace().getAccuracy() * getSpeed());
            pSpace.distributeEvents();
            long update = System.currentTimeMillis() - detachedPhysicsLastUpdate;
            detachedPhysicsLastUpdate = System.currentTimeMillis();
            executor.schedule(detachedPhysicsUpdate, Math.round(getPhysicsSpace().getAccuracy() * 1000000.0f) - (update * 1000), TimeUnit.MICROSECONDS);
            return true;
        }
    };

    /**
     * Access the PhysicsSpace managed by this state. Normally there is none
     * until the state is attached.
     *
     * @return the pre-existing instance, or null if no simulation running
     */
    public PhysicsSpace getPhysicsSpace() {
        return pSpace;
    }

    /**
     * Allocate a physics space and start physics.
     * <p>
     * Physics starts automatically after the state is attached. To start it
     * sooner, invoke this method.
     */
    public void startPhysics() {
        if (isRunning) {
            return;
        }

        switch (threadingType) {
            case PARALLEL:
                boolean success = startPhysicsOnExecutor();
                assert success;
                assert pSpace != null;
                break;

            case SEQUENTIAL:
                pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
                pSpace.addTickListener(this);
                break;

            default:
                throw new IllegalStateException(threadingType.toString());
        }

        isRunning = true;
    }

    /**
     * Stop physics after this state is detached.
     */
    public void stopPhysics() {
        if (!isRunning) {
            return;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        pSpace.removeTickListener(this);
        pSpace.destroy();
        isRunning = false;
    }

    /**
     * Initialize this state prior to its 1st update. Should be invoked only by
     * a subclass or by the AppStateManager.
     *
     * @param stateManager the manager for this state (not null)
     * @param app the application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;
        this.stateManager = stateManager;
        startPhysics();
    }

    /**
     * Alter whether debug visualization is enabled.
     *
     * @param debugEnabled true &rarr; enable, false &rarr; disable
     */
    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }


    /**
     * Test whether debug visualization is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Transition this state from detached to initializing. Should be invoked
     * only by a subclass or by the AppStateManager.
     *
     * @param stateManager (not null)
     */
    @Override
    public void stateAttached(AppStateManager stateManager) {
        super.stateAttached(stateManager);
        if (!isRunning) {
            startPhysics();
        }
        if (threadingType == ThreadingType.PARALLEL) {
            PhysicsSpace.setLocalThreadPhysicsSpace(pSpace);
        }
        if (debugEnabled) {
            debugAppState = new BulletDebugAppState(pSpace);
            stateManager.attach(debugAppState);
        }
    }

    /**
     * Update this state prior to rendering. Should be invoked only by a
     * subclass or by the AppStateManager. Invoked once per frame, provided the
     * state is attached and enabled.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        if (debugEnabled && debugAppState == null && pSpace != null) {
            debugAppState = new BulletDebugAppState(pSpace);
            stateManager.attach(debugAppState);
        } else if (!debugEnabled && debugAppState != null) {
            stateManager.detach(debugAppState);
            debugAppState = null;
        }
        if (!active) {
            return;
        }
        pSpace.distributeEvents();
        this.tpf = tpf;
    }

    /**
     * Render this state. Should be invoked only by a subclass or by the
     * AppStateManager. Invoked once per frame, provided the state is attached
     * and enabled.
     *
     * @param rm the render manager (not null)
     */
    @Override
    public void render(RenderManager rm) {
        super.render(rm);
        if (!active) {
            return;
        }
        if (threadingType == ThreadingType.PARALLEL) {
            physicsFuture = executor.submit(parallelPhysicsUpdate);
        } else if (threadingType == ThreadingType.SEQUENTIAL) {
            pSpace.update(active ? tpf * speed : 0);
        } else {
        }
    }

    /**
     * Update this state after all rendering commands are flushed. Should be
     * invoked only by a subclass or by the AppStateManager. Invoked once per
     * frame, provided the state is attached and enabled.
     */
    @Override
    public void postRender() {
        super.postRender();
        if (physicsFuture != null) {
            try {
                physicsFuture.get();
                physicsFuture = null;
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(BulletAppState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Transition this state from terminating to detached. Should be invoked
     * only by a subclass or by the AppStateManager. Invoked once for each time
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     * is invoked.
     */
    @Override
    public void cleanup() {
        if (debugAppState != null) {
            stateManager.detach(debugAppState);
            debugAppState = null;
        }
        stopPhysics();
        super.cleanup();
    }

    /**
     * Read which type of threading this app state uses.
     *
     * @return the threadingType (not null)
     */
    public ThreadingType getThreadingType() {
        return threadingType;
    }

    /**
     * Alter which type of threading this app state uses. Not allowed after
     * attaching the app state.
     *
     * @param threadingType the desired type (not null, default=SEQUENTIAL)
     */
    public void setThreadingType(ThreadingType threadingType) {
        this.threadingType = threadingType;
    }

    /**
     * Alter the broadphase type the physics space will use. Not allowed after
     * attaching the app state.
     *
     * @param broadphaseType an enum value (not null, default=DBVT)
     */
    public void setBroadphaseType(BroadphaseType broadphaseType) {
        this.broadphaseType = broadphaseType;
    }

    /**
     * Alter the coordinate range. Not allowed after attaching the app state.
     *
     * @param worldMin the desired minimum coordinate values when using
     * AXIS_SWEEP broadphase algorithms (not null, alias created,
     * default=-10k,-10k,-10k)
     */
    public void setWorldMin(Vector3f worldMin) {
        this.worldMin = worldMin;
    }

    /**
     * Alter the coordinate range. Not allowed after attaching the app state.
     *
     * @param worldMax the desired maximum coordinate values when using
     * AXIS_SWEEP broadphase algorithms (not null, alias created,
     * default=10k,10k,10k)
     */
    public void setWorldMax(Vector3f worldMax) {
        this.worldMax = worldMax;
    }

    /**
     * Read the simulation speed.
     *
     * @return speed (&ge;0, default=1)
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Alter the simulation speed.
     *
     * @param speed the desired speed (&ge;0, default=1)
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }
    /**
     * Callback from Bullet, invoked just before the physics is stepped. A good
     * time to clear/apply forces.
     *
     * @param space the space that is about to be stepped (not null)
     * @param f the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void prePhysicsTick(PhysicsSpace space, float f) {
    }

    /**
     * Callback from Bullet, invoked just after the physics is stepped. A good
     * time to clear/apply forces.
     *
     * @param space the space that is about to be stepped (not null)
     * @param f the time per physics step (in seconds, &ge;0)
     */
    @Override
    public void physicsTick(PhysicsSpace space, float f) {
    }

    /**
     * Enumerate threading modes.
     */
    public enum ThreadingType {

        /**
         * Default mode: user update, physics update, and rendering happen
         * sequentially. (single threaded)
         */
        SEQUENTIAL,
        /**
         * Parallel threaded mode: physics update and rendering are executed in
         * parallel, update order is maintained.
         */
        PARALLEL,
    }
}
