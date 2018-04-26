/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.PhysicsSpace.BroadphaseType;
import com.jme3.bullet.debug.BulletDebugAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>BulletAppState</code> allows using bullet physics in an Application.
 *
 * @author normenhansen
 */
public class BulletAppState implements AppState, PhysicsTickListener {

    protected boolean initialized = false;
    protected Application app;
    protected AppStateManager stateManager;
    protected ScheduledThreadPoolExecutor executor;
    protected PhysicsSpace pSpace;
    protected ThreadingType threadingType = ThreadingType.SEQUENTIAL;
    protected BroadphaseType broadphaseType = BroadphaseType.DBVT;
    protected Vector3f worldMin = new Vector3f(-10000f, -10000f, -10000f);
    protected Vector3f worldMax = new Vector3f(10000f, 10000f, 10000f);
    protected float speed = 1;
    protected boolean active = true;
    protected boolean debugEnabled = false;
    protected BulletDebugAppState debugAppState;
    protected float tpf;
    protected Future physicsFuture;

    /**
     * Creates a new BulletAppState running a PhysicsSpace for physics
     * simulation, use getStateManager().attach(bulletAppState) to enable
     * physics for an Application.
     */
    public BulletAppState() {
    }

    /**
     * Creates a new BulletAppState running a PhysicsSpace for physics
     * simulation, use getStateManager().attach(bulletAppState) to enable
     * physics for an Application.
     *
     * @param broadphaseType The type of broadphase collision detection,
     * BroadphaseType.DVBT is the default
     */
    public BulletAppState(BroadphaseType broadphaseType) {
        this(new Vector3f(-10000f, -10000f, -10000f), new Vector3f(10000f, 10000f, 10000f), broadphaseType);
    }

    /**
     * Creates a new BulletAppState running a PhysicsSpace for physics
     * simulation, use getStateManager().attach(bulletAppState) to enable
     * physics for an Application. An AxisSweep broadphase is used.
     *
     * @param worldMin The minimum world extent
     * @param worldMax The maximum world extent
     */
    public BulletAppState(Vector3f worldMin, Vector3f worldMax) {
        this(worldMin, worldMax, BroadphaseType.AXIS_SWEEP_3);
    }

    public BulletAppState(Vector3f worldMin, Vector3f worldMax, BroadphaseType broadphaseType) {
        this.worldMin.set(worldMin);
        this.worldMax.set(worldMax);
        this.broadphaseType = broadphaseType;
    }

    private boolean startPhysicsOnExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
        executor = new ScheduledThreadPoolExecutor(1);
        final BulletAppState app = this;
        Callable<Boolean> call = new Callable<Boolean>() {
            public Boolean call() throws Exception {
                detachedPhysicsLastUpdate = System.currentTimeMillis();
                pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
                pSpace.addTickListener(app);
                return true;
            }
        };
        try {
            return executor.submit(call).get();
        } catch (InterruptedException ex) {
            Logger.getLogger(BulletAppState.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (ExecutionException ex) {
            Logger.getLogger(BulletAppState.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    private Callable<Boolean> parallelPhysicsUpdate = new Callable<Boolean>() {
        public Boolean call() throws Exception {
            pSpace.update(tpf * getSpeed());
            return true;
        }
    };
    long detachedPhysicsLastUpdate = 0;
    private Callable<Boolean> detachedPhysicsUpdate = new Callable<Boolean>() {
        public Boolean call() throws Exception {
            pSpace.update(getPhysicsSpace().getAccuracy() * getSpeed());
            pSpace.distributeEvents();
            long update = System.currentTimeMillis() - detachedPhysicsLastUpdate;
            detachedPhysicsLastUpdate = System.currentTimeMillis();
            executor.schedule(detachedPhysicsUpdate, Math.round(getPhysicsSpace().getAccuracy() * 1000000.0f) - (update * 1000), TimeUnit.MICROSECONDS);
            return true;
        }
    };

    public PhysicsSpace getPhysicsSpace() {
        return pSpace;
    }

    /**
     * The physics system is started automatically on attaching, if you want to
     * start it before for some reason, you can use this method.
     */
    public void startPhysics() {
        if (initialized) {
            return;
        }
        //start physics thread(pool)
        if (threadingType == ThreadingType.PARALLEL) {
            startPhysicsOnExecutor();
        } else {
            pSpace = new PhysicsSpace(worldMin, worldMax, broadphaseType);
        }
        pSpace.addTickListener(this);
        initialized = true;
    }

    public void stopPhysics() {
        if(!initialized){
            return;
        }
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
        pSpace.removeTickListener(this);
        pSpace.destroy();
        initialized = false;
    }

    public void initialize(AppStateManager stateManager, Application app) {
        this.app = app;
        this.stateManager = stateManager;
        startPhysics();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setEnabled(boolean enabled) {
        this.active = enabled;
    }

    public boolean isEnabled() {
        return active;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void stateAttached(AppStateManager stateManager) {
        if (!initialized) {
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

    public void stateDetached(AppStateManager stateManager) {
    }

    public void update(float tpf) {
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

    public void render(RenderManager rm) {
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

    public void postRender() {
        if (physicsFuture != null) {
            try {
                physicsFuture.get();
                physicsFuture = null;
            } catch (InterruptedException ex) {
                Logger.getLogger(BulletAppState.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(BulletAppState.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void cleanup() {
        if (debugAppState != null) {
            stateManager.detach(debugAppState);
            debugAppState = null;
        }
        stopPhysics();
    }

    /**
     * @return the threadingType
     */
    public ThreadingType getThreadingType() {
        return threadingType;
    }

    /**
     * Use before attaching state
     *
     * @param threadingType the threadingType to set
     */
    public void setThreadingType(ThreadingType threadingType) {
        this.threadingType = threadingType;
    }

    /**
     * Use before attaching state
     */
    public void setBroadphaseType(BroadphaseType broadphaseType) {
        this.broadphaseType = broadphaseType;
    }

    /**
     * Use before attaching state
     */
    public void setWorldMin(Vector3f worldMin) {
        this.worldMin = worldMin;
    }

    /**
     * Use before attaching state
     */
    public void setWorldMax(Vector3f worldMax) {
        this.worldMax = worldMax;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void prePhysicsTick(PhysicsSpace space, float f) {
    }

    public void physicsTick(PhysicsSpace space, float f) {
    }

    public enum ThreadingType {

        /**
         * Default mode; user update, physics update and rendering happen
         * sequentially (single threaded)
         */
        SEQUENTIAL,
        /**
         * Parallel threaded mode; physics update and rendering are executed in
         * parallel, update order is kept.<br/> Multiple BulletAppStates will
         * execute in parallel in this mode.
         */
        PARALLEL,
    }
}
