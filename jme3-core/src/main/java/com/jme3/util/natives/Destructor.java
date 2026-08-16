package com.jme3.util.natives;

import java.lang.ref.PhantomReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Phantomly references an object to execute destruction logic when
 * that object is reclaimed by the garbage collector.
 */
public abstract class Destructor extends PhantomReference<Object> {

    private static final Set<Destructor> DESTRUCTORS = new HashSet<>();

    public static final Destructor NULL = new Destructor() {
        @Override
        public void destroy() {}
        @Override
        protected void runDestroy() {}
        @Override
        public Destructor addDependent(Destructor destructor) {
            return destructor;
        }
    };

    public static void destroyAll() {
        Collection<Destructor> toDestroy = new ArrayList<>(DESTRUCTORS);
        for (Destructor h : toDestroy) {
            h.destroy();
        }
    }

    private final AtomicBoolean alive = new AtomicBoolean(true);
    private final Collection<Destructor> dependents = new ArrayList<>(0);

    public Destructor(Object referent) {
        super(referent, null);
        DESTRUCTORS.add(this);
    }

    private Destructor() {
        super(DESTRUCTORS, null);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean enqueue() {
        boolean result = super.enqueue();
        destroy();
        return result;
    }

    public void destroy() {
        if (alive.getAndSet(false)) {
            for (Destructor d : dependents) {
                d.destroy();
            }
            runDestroy();
            DESTRUCTORS.remove(this);
        }
    }

    protected abstract void runDestroy();

    public Destructor addDependent(Destructor destructor) {
        dependents.add(destructor);
        return this;
    }

    public boolean isAlive() {
        return alive.get();
    }

    public static Destructor run(Object referent, Runnable destroy) {
        return new RunDestructorImpl(referent, destroy);
    }

    private static class RunDestructorImpl extends Destructor {

        private final Runnable destroy;

        public RunDestructorImpl(Object referent, Runnable destroy) {
            super(referent);
            this.destroy = destroy;
        }

        @Override
        protected void runDestroy() {
            destroy.run();
        }

    }

}
