package com.jme3.util.natives;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BasicDisposableManager implements DisposableManager {

    private static DisposableManager globalInstance = new BasicDisposableManager();

    public static void setGlobalInstance(DisposableManager instance) {
        globalInstance = Objects.requireNonNull(instance, "NativeManager global instance cannot be null.");
    }

    public static DisposableManager getGlobalInstance() {
        return globalInstance;
    }

    private final ReferenceQueue<Disposable> unreachable = new ReferenceQueue<>();
    private final Map<Long, DisposableReference> refMap = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0L);

    @Override
    public DisposableReference register(Disposable object) {
        RefImpl ref = new RefImpl(nextId.getAndIncrement(), object, unreachable);
        refMap.put(ref.id, ref);
        return ref;
    }

    @Override
    public void flush() {
        for (DisposableReference ref; (ref = (DisposableReference)unreachable.poll()) != null;) {
            ref.destroy();
        }
    }

    @Override
    public void clear() {
        Collection<DisposableReference> toDestroy = new ArrayList<>(refMap.values());
        refMap.clear();
        for (DisposableReference ref : toDestroy) {
            ref.destroy();
        }
    }

    public class RefImpl extends WeakReference<Disposable> implements DisposableReference {

        private final long id;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final Collection<DisposableReference> dependents = new ArrayList<>();
        private Runnable destroyer;

        private RefImpl(long id, Disposable referent, ReferenceQueue<Disposable> q) {
            super(referent, q);
            this.id = id;
            destroyer = referent.createDestroyer();
        }

        @Override
        public void destroy() {
            if (active.getAndSet(false)) {
                for (DisposableReference ref : dependents) {
                    ref.destroy();
                }
                dependents.clear();
                destroyer.run();
                Disposable referent = get();
                if (referent != null) {
                    referent.prematureDestruction();
                }
                refMap.remove(id, this);
            }
        }

        @Override
        public void addDependent(DisposableReference reference) {
            if (isDestroyed()) {
                throw new IllegalStateException("Cannot add dependent to destroyed resource.");
            }
            dependents.add(reference);
        }

        @Override
        public boolean isDestroyed() {
            return !active.get();
        }

        @Override
        public void refresh() {
            Disposable obj = get();
            if (obj != null) {
                destroyer = obj.createDestroyer();
            }
        }

    }

}
