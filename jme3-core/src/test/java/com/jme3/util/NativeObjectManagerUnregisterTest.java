/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.PhantomReference;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exercises explicit native-object unregistration without a renderer or GL context. */
public class NativeObjectManagerUnregisterTest {

    @Test
    public void explicitDeleteAndReRegistrationLeavesOnlyTheCurrentIdRegistered() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(11, manager, deletedIds);
        manager.registerObject(image);

        // This models a successful explicit GL delete followed by renderer reset.
        image.explicitDelete();
        image.setId(22);
        manager.registerObject(image);

        manager.deleteAllObjects(new Object());

        assertEquals(Arrays.asList(11, 22), deletedIds,
                "cleanup must not revisit the explicitly deleted id");
        assertEquals(NativeObject.INVALID_ID, image.getId());
    }

    @Test
    public void managerDeletionCallbackCanUnregisterAgainAndImageCanBeReRegistered() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(31, manager, deletedIds);
        manager.registerObject(image);

        // deleteAllObjects removes the map entry before invoking the clone callback.
        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(31), deletedIds);
        assertEquals(NativeObject.INVALID_ID, image.getId());

        image.setId(32);
        manager.registerObject(image);
        manager.deleteAllObjects(new Object());

        assertEquals(Arrays.asList(31, 32), deletedIds);
        assertEquals(NativeObject.INVALID_ID, image.getId());
    }

    @Test
    public void unregisteringAnOlderObjectDoesNotRemoveAnotherObjectsSameKey() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject older = new FakeNativeObject(41, manager, deletedIds);
        FakeNativeObject current = new FakeNativeObject(41, manager, deletedIds);
        manager.registerObject(older);
        manager.registerObject(current);

        manager.unregisterObject(older);
        manager.deleteAllObjects(new Object());

        assertEquals(Arrays.asList(41), deletedIds);
        assertEquals(NativeObject.INVALID_ID, current.getId());
        assertEquals(41, older.getId(), "unregistering does not reset another object or the caller");
    }

    @Test
    public void repeatedUnregistrationIsANoopAndDoesNotDeleteTheObject() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(51, manager, deletedIds);
        manager.registerObject(image);

        manager.unregisterObject(image);
        manager.unregisterObject(image);

        assertEquals(51, image.getId());
        assertTrue(deletedIds.isEmpty());
        manager.deleteAllObjects(new Object());
        assertTrue(deletedIds.isEmpty());
    }

    @Test
    public void queuedDisposeDoesNotFollowTheSameObjectIntoAnotherRegistration() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(62, manager, deletedIds);
        manager.registerObject(image);
        image.dispose();
        // Models a renderer deletion/reset followed by reuse before queue drain.
        // The old registration remains tracked until explicit unregistration.
        image.resetObject();
        image.setId(62);
        manager.registerObject(image);
        manager.deleteUnused(new Object());
        assertTrue(deletedIds.isEmpty());
        assertEquals(62, image.getId());
        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(62), deletedIds);
    }

    @Test
    public void deferredDisposeDoesNotDeleteAReplacementRegistration() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(61, manager, deletedIds);
        manager.registerObject(image);

        // Queue cleanup for ID 61, then model an explicit delete and a new GL ID
        // being assigned to the same Java object before the queue is drained.
        image.dispose();
        image.explicitDelete();
        image.setId(62);
        manager.registerObject(image);

        manager.deleteUnused(new Object());

        assertEquals(Arrays.asList(61), deletedIds,
                "a deferred request for the old registration must not delete ID 62");
        assertEquals(62, image.getId(), "the replacement registration remains live");
        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(61, 62), deletedIds,
                "the replacement registration is still available for normal cleanup");
    }

    @Test
    public void deferredDisposeFromBeforeResetDoesNotDeleteNewContextObject() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(71, manager, deletedIds);
        manager.registerObject(image);

        image.dispose();
        manager.resetObjects();
        image.setId(72);
        manager.registerObject(image);

        manager.deleteUnused(new Object());

        assertTrue(deletedIds.isEmpty(), "reset discards deferred requests for the old context");
        assertEquals(72, image.getId(), "the new context registration remains live");
        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(72), deletedIds);
    }

    @Test
    public void duplicateDisposeForOneRegistrationDeletesItOnlyOnce() {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject image = new FakeNativeObject(81, manager, deletedIds);
        manager.registerObject(image);

        image.dispose();
        image.dispose();
        manager.deleteUnused(new Object());

        assertEquals(Arrays.asList(81), deletedIds,
                "duplicate requests for one registration must result in one deletion");
        assertEquals(NativeObject.INVALID_ID, image.getId());
        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(81), deletedIds,
                "the deleted registration must not be visited again");
    }

    @Test
    public void staleDisposeRequestsStillCountTowardPerFrameCleanupLimit()
            throws ReflectiveOperationException {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        // Distinct objects retain distinct stale requests. Repeated disposal
        // of one object is intentionally deduplicated by the upstream manager.
        for (int request = 0; request < 101; request++) {
            FakeNativeObject stale = new FakeNativeObject(101, manager, deletedIds);
            manager.registerObject(stale);
            stale.dispose();
        }
        FakeNativeObject image = new FakeNativeObject(101, manager, deletedIds);
        manager.registerObject(image);

        assertEquals(101, getUserDeletionQueueSize(manager), "all old requests are queued");
        manager.deleteUnused(new Object());

        assertEquals(1, getUserDeletionQueueSize(manager),
                "the frame limit processes only 100 requests, including stale ones");
        assertTrue(deletedIds.isEmpty(),
                "draining stale requests must not delete the replacement registration");
        assertEquals(101, image.getId(), "the replacement remains live after the first drain");

        manager.deleteUnused(new Object());

        assertEquals(0, getUserDeletionQueueSize(manager), "the next frame drains the last stale request");
        assertTrue(deletedIds.isEmpty(),
                "the remaining stale request must not delete the replacement registration");
        assertEquals(101, image.getId(), "the replacement remains live after all stale requests drain");

        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(101), deletedIds,
                "normal cleanup still deletes the replacement registration exactly once");
    }

    @Test
    public void queuedPhantomForReplacedRegistrationDoesNotRemoveCurrentRegistration()
            throws ReflectiveOperationException {
        NativeObjectManager manager = new NativeObjectManager();
        List<Integer> deletedIds = new ArrayList<>();
        FakeNativeObject older = new FakeNativeObject(91, manager, deletedIds);
        FakeNativeObject current = new FakeNativeObject(91, manager, deletedIds);
        manager.registerObject(older);
        PhantomReference<?> replacedReference = getRegisteredReference(manager, older.getUniqueId());

        // Both objects intentionally have the same unique key. A delayed queue
        // entry for the replaced reference must not act on the current mapping.
        manager.registerObject(current);
        assertTrue(replacedReference.enqueue(), "the old phantom reference should be queued");
        manager.deleteUnused(new Object());

        assertTrue(deletedIds.isEmpty(), "a stale phantom reference must not delete either object");
        assertEquals(91, current.getId(), "the current registration remains live");
        manager.deleteAllObjects(new Object());
        assertEquals(Arrays.asList(91), deletedIds,
                "normal cleanup still deletes the current registration exactly once");
        assertEquals(NativeObject.INVALID_ID, current.getId());
    }

    @Test
    public void resetRetainsReplacementBuffersButReleasesCurrentDisposedBuffers() {
        boolean previousUnsafe = NativeObjectManager.UNSAFE;
        NativeObjectManager.UNSAFE = true;
        try {
            NativeObjectManager manager = new NativeObjectManager();
            List<Integer> deletedIds = new ArrayList<>();
            FakeNativeObject replaced = new FakeNativeObject(111, manager, deletedIds);
            manager.registerObject(replaced);
            replaced.dispose();
            replaced.resetObject();
            replaced.setId(111);
            manager.registerObject(replaced);
            FakeNativeObject disposed = new FakeNativeObject(112, manager, deletedIds);
            manager.registerObject(disposed);
            disposed.dispose();
            FakeNativeObject older = new FakeNativeObject(113, manager, deletedIds);
            manager.registerObject(older);
            older.dispose();
            FakeNativeObject current = new FakeNativeObject(113, manager, deletedIds);
            manager.registerObject(current);

            manager.resetObjects();

            assertEquals(0, replaced.nativeBufferDeletes,
                    "context loss must retain CPU data belonging to the replacement");
            assertEquals(1, disposed.nativeBufferDeletes,
                    "a current disposal still releases its CPU data during reset");
            assertEquals(1, older.nativeBufferDeletes,
                    "a distinct older wrapper keeps its own CPU-disposal request");
            assertEquals(0, current.nativeBufferDeletes,
                    "the newer distinct wrapper retains its data");
            assertTrue(deletedIds.isEmpty(), "reset does not delete old-context native names");
            manager.deleteUnused(new Object());
            assertEquals(1, disposed.nativeBufferDeletes, "reset drains the request once");
        } finally {
            NativeObjectManager.UNSAFE = previousUnsafe;
        }
    }

    @Test
    public void terminalDrainRetainsReplacementBuffersBeyondThePerFrameLimit() {
        boolean previousUnsafe = NativeObjectManager.UNSAFE;
        NativeObjectManager.UNSAFE = true;
        try {
            NativeObjectManager manager = new NativeObjectManager();
            List<Integer> deletedIds = new ArrayList<>();
            FakeNativeObject replaced = new FakeNativeObject(121, manager, deletedIds);
            manager.registerObject(replaced);
            replaced.dispose();
            List<FakeNativeObject> disposed = new ArrayList<>();
            // Requests are LIFO: the stale request sits behind a full frame's work.
            for (int i = 0; i < 100; i++) {
                FakeNativeObject object = new FakeNativeObject(200 + i, manager, deletedIds);
                manager.registerObject(object);
                object.dispose();
                disposed.add(object);
            }
            replaced.resetObject();
            replaced.setId(121);
            manager.registerObject(replaced);

            manager.deleteAllObjects(new Object());

            assertEquals(0, replaced.nativeBufferDeletes,
                    "a stale tail request must not free replacement CPU data");
            assertEquals(101, deletedIds.size(), "terminal cleanup deletes all live native names");
            for (FakeNativeObject object : disposed) {
                assertEquals(1, object.nativeBufferDeletes,
                        "each current explicit disposal releases CPU data exactly once");
            }
            manager.deleteAllObjects(new Object());
            assertEquals(101, deletedIds.size(), "terminal cleanup drains every request");
        } finally {
            NativeObjectManager.UNSAFE = previousUnsafe;
        }
    }

    @Test
    public void replacementCanRequestItsOwnUnsafeDisposal() {
        boolean previousUnsafe = NativeObjectManager.UNSAFE;
        NativeObjectManager.UNSAFE = true;
        try {
            NativeObjectManager manager = new NativeObjectManager();
            FakeNativeObject object = new FakeNativeObject(131, manager, new ArrayList<>());
            manager.registerObject(object);
            object.dispose();
            object.resetObject();
            object.setId(131);
            manager.registerObject(object);
            object.dispose();

            manager.resetObjects();

            assertEquals(1, object.nativeBufferDeletes,
                    "re-registration only cancels earlier requests, not a later disposal");
        } finally {
            NativeObjectManager.UNSAFE = previousUnsafe;
        }
    }

    private static PhantomReference<?> getRegisteredReference(NativeObjectManager manager, long id)
            throws ReflectiveOperationException {
        Field refMapField = NativeObjectManager.class.getDeclaredField("refMap");
        refMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<Long, ?> refMap = (HashMap<Long, ?>) refMapField.get(manager);
        return (PhantomReference<?>) refMap.get(id);
    }

    private static int getUserDeletionQueueSize(NativeObjectManager manager)
            throws ReflectiveOperationException {
        Field queueField = NativeObjectManager.class.getDeclaredField("userDeletionQueue");
        queueField.setAccessible(true);
        return ((ArrayDeque<?>) queueField.get(manager)).size();
    }

    private static final class FakeNativeObject extends NativeObject {

        private final NativeObjectManager manager;
        private final List<Integer> deletedIds;
        private final FakeNativeObject realObject;
        private int nativeBufferDeletes;

        FakeNativeObject(int id, NativeObjectManager manager, List<Integer> deletedIds) {
            super();
            this.id = id;
            this.manager = manager;
            this.deletedIds = deletedIds;
            this.realObject = this;
        }

        private FakeNativeObject(int id, NativeObjectManager manager, List<Integer> deletedIds,
                                 FakeNativeObject realObject) {
            super(id);
            this.manager = manager;
            this.deletedIds = deletedIds;
            this.realObject = realObject;
        }

        @Override
        protected void deleteNativeBuffers() {
            nativeBufferDeletes++;
        }

        @Override
        public void resetObject() {
            id = INVALID_ID;
            setUpdateNeeded();
        }

        @Override
        public void deleteObject(Object rendererObject) {
            manager.unregisterObject(realObject);
            deletedIds.add(id);
            resetObject();
        }

        void explicitDelete() {
            deletedIds.add(id);
            manager.unregisterObject(this);
            resetObject();
        }

        @Override
        public NativeObject createDestructableClone() {
            return new FakeNativeObject(id, manager, deletedIds, realObject);
        }

        @Override
        public long getUniqueId() {
            return ((long) OBJTYPE_TEXTURE << 32) | (0xffffffffL & (long) id);
        }
    }
}
