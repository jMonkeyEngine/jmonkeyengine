package com.jme3.util.natives;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class BasicNativeManager implements NativeManager {

    private static NativeManager globalInstance = new BasicNativeManager();

    public static void setGlobalInstance(NativeManager instance) {
        globalInstance = Objects.requireNonNull(instance, "NativeManager global instance cannot be null.");
    }

    public static NativeManager getGlobalInstance() {
        return globalInstance;
    }

    private final ReferenceQueue<Native> unreachable = new ReferenceQueue<>();
    private final Map<Long, NativeRef> refMap = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(0L);

    @Override
    public NativeRef register(Native object) {
        NativeRef ref = new NativeRef(nextId.getAndIncrement(), object, unreachable);
        refMap.put(ref.getId(), ref);
        return ref;
    }

    @Override
    public int flush() {
        int flushed = 0;
        for (NativeRef ref; (ref = (NativeRef)unreachable.poll()) != null;) {
            ref.destroy();
            refMap.remove(ref.id);
            flushed++;
        }
        if (flushed > 0) {
            refMap.values().removeIf(NativeRef::isDestroyed);
        }
        return flushed;
    }

    @Override
    public int clear() {
        int size = refMap.size();
        for (NativeRef ref : refMap.values()) {
            ref.destroy();
        }
        refMap.clear();
        return size;
    }

    public class NativeRef extends WeakReference<Native> implements NativeReference {

        private final long id;
        private final WeakReference<Native> weakRef;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private final Collection<NativeReference> dependents = new ArrayList<>();
        private Runnable destroyer;

        private NativeRef(long id, Native referent, ReferenceQueue<Native> q) {
            super(referent, q);
            this.id = id;
            destroyer = referent.createNativeDestroyer();
            weakRef = new WeakReference<>(referent);
        }

        @Override
        public void destroy() {
            if (active.getAndSet(false)) {
                for (NativeReference ref : dependents) {
                    ref.destroy();
                }
                dependents.clear();
                destroyer.run();
                Native referent = weakRef.get();
                if (referent != null) {
                    referent.prematureNativeDestruction();
                }
            }
        }

        @Override
        public void addDependent(NativeReference reference) {
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
            Native obj = weakRef.get();
            if (obj != null) {
                destroyer = obj.createNativeDestroyer();
            }
        }

        private long getId() {
            return id;
        }

    }

}
