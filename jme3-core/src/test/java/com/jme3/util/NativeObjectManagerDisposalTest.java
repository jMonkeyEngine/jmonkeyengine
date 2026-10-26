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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Explicit CPU-buffer disposal must survive removal of obsolete GL requests. */
public class NativeObjectManagerDisposalTest {
    private final Object renderer = new Object();
    private boolean originalUnsafe;

    @BeforeEach
    void saveUnsafe() {
        originalUnsafe = NativeObjectManager.UNSAFE;
    }

    @AfterEach
    void restoreUnsafe() {
        NativeObjectManager.UNSAFE = originalUnsafe;
    }

    private static final class Counts {
        int nativeDeletes;
        int bufferDeletes;
    }

    private static class Resource extends NativeObject {
        final Counts counts;

        Resource() {
            counts = new Counts();
        }

        Resource(int id, Counts counts) {
            super(id);
            this.counts = counts;
        }

        @Override
        public void resetObject() {
            id = INVALID_ID;
            setUpdateNeeded();
        }

        @Override
        public void deleteObject(Object rendererObject) {
            counts.nativeDeletes++;
            resetObject();
        }

        @Override
        protected void deleteNativeBuffers() {
            // No raw memory is allocated/freed by this fixture.
            counts.bufferDeletes++;
        }

        @Override
        public NativeObject createDestructableClone() {
            return new Resource(id, counts);
        }

        @Override
        public long getUniqueId() {
            return ((long) OBJTYPE_VERTEXBUFFER << 32) | (id & 0xffffffffL);
        }
    }

    private static Resource register(NativeObjectManager manager, int id) {
        Resource resource = new Resource();
        resource.setId(id);
        manager.registerObject(resource);
        return resource;
    }

    @Test
    void unsafeResetReleasesOnlyExplicitlyQueuedBuffersExactlyOnce() {
        NativeObjectManager.UNSAFE = true;
        NativeObjectManager manager = new NativeObjectManager();
        Resource disposed = register(manager, 1);
        Resource retained = register(manager, 2);
        disposed.dispose();
        disposed.dispose();
        manager.resetObjects();
        assertEquals(-1, disposed.getId());
        assertEquals(-1, retained.getId());
        assertEquals(1, disposed.counts.bufferDeletes);
        assertEquals(0, retained.counts.bufferDeletes);
        assertEquals(0, disposed.counts.nativeDeletes);
        assertEquals(0, retained.counts.nativeDeletes);
        manager.deleteUnused(renderer);
        manager.resetObjects();
        assertEquals(1, disposed.counts.bufferDeletes);
        assertEquals(0, disposed.counts.nativeDeletes);
    }

    @Test
    void safeResetDiscardsOldRequestsBeforeReinitialization() {
        NativeObjectManager.UNSAFE = false;
        NativeObjectManager manager = new NativeObjectManager();
        Resource resource = register(manager, 1);
        resource.dispose();
        manager.resetObjects();
        resource.setId(2);
        manager.registerObject(resource);
        manager.deleteUnused(renderer);
        assertEquals(2, resource.getId());
        assertEquals(0, resource.counts.nativeDeletes);
        assertEquals(0, resource.counts.bufferDeletes);
        resource.dispose();
        manager.deleteUnused(renderer);
        assertEquals(1, resource.counts.nativeDeletes);
        assertEquals(0, resource.counts.bufferDeletes);
    }

    @Test
    void fullCleanupReleasesAllQueuedBuffersBeyondFrameLimit() {
        NativeObjectManager.UNSAFE = true;
        NativeObjectManager manager = new NativeObjectManager();
        Resource[] queued = new Resource[150];
        for (int i = 0; i < queued.length; i++) {
            queued[i] = register(manager, i + 1);
            queued[i].dispose();
            queued[i].dispose();
        }
        Resource retained = register(manager, 1000);
        manager.deleteAllObjects(renderer);
        for (Resource resource : queued) {
            assertEquals(-1, resource.getId());
            assertEquals(1, resource.counts.nativeDeletes);
            assertEquals(1, resource.counts.bufferDeletes);
        }
        assertEquals(1, retained.counts.nativeDeletes);
        assertEquals(0, retained.counts.bufferDeletes);
        manager.deleteUnused(renderer);
        manager.deleteAllObjects(renderer);
        assertEquals(1, queued[0].counts.bufferDeletes);
    }

    @Test
    void safeFullCleanupDoesNotFreeCpuBuffersOrLeaveDeletionRequests() {
        NativeObjectManager.UNSAFE = false;
        NativeObjectManager manager = new NativeObjectManager();
        Resource[] resources = new Resource[150];
        for (int i = 0; i < resources.length; i++) {
            resources[i] = register(manager, i + 1);
            resources[i].dispose();
        }
        manager.deleteAllObjects(renderer);
        for (Resource resource : resources) {
            assertEquals(1, resource.counts.nativeDeletes);
            assertEquals(0, resource.counts.bufferDeletes);
        }
        resources[0].setId(1);
        manager.registerObject(resources[0]);
        manager.deleteUnused(renderer);
        assertEquals(1, resources[0].getId());
        assertEquals(1, resources[0].counts.nativeDeletes);
    }

    @Test
    void unsafeDirectUnregistrationHonorsQueuedCpuDisposalOnce() {
        NativeObjectManager.UNSAFE = true;
        NativeObjectManager manager = new NativeObjectManager();
        Resource resource = register(manager, 1);
        resource.dispose();
        resource.dispose();
        manager.unregisterObject(resource);
        resource.resetObject();
        manager.deleteUnused(renderer);
        manager.resetObjects();
        assertEquals(1, resource.counts.bufferDeletes);
        assertEquals(0, resource.counts.nativeDeletes);
    }

    @Test
    void safeDirectUnregistrationDoesNotDeleteReusedNativeName() {
        NativeObjectManager.UNSAFE = false;
        NativeObjectManager manager = new NativeObjectManager();
        Resource resource = register(manager, 1);
        resource.dispose();
        manager.unregisterObject(resource);
        resource.resetObject();
        resource.setId(1);
        manager.registerObject(resource);
        manager.deleteUnused(renderer);
        assertEquals(1, resource.getId());
        assertEquals(0, resource.counts.nativeDeletes);
        assertEquals(0, resource.counts.bufferDeletes);
    }

    @Test
    void managedDisposalDoesNotRunCpuCleanupTwiceViaUnregister() {
        NativeObjectManager.UNSAFE = true;
        NativeObjectManager manager = new NativeObjectManager();
        Resource resource = new Resource() {
            @Override
            public void deleteObject(Object rendererObject) {
                manager.unregisterObject(this);
                super.deleteObject(rendererObject);
            }
        };
        resource.setId(1);
        manager.registerObject(resource);
        resource.dispose();
        resource.dispose();
        manager.deleteUnused(renderer);
        assertEquals(1, resource.counts.nativeDeletes);
        assertEquals(1, resource.counts.bufferDeletes);
    }

    @Test
    void ordinaryCleanupRetainsItsFrameLimitAndDeduplicatesRequests() {
        NativeObjectManager.UNSAFE = true;
        NativeObjectManager manager = new NativeObjectManager();
        Resource[] resources = new Resource[150];
        for (int i = 0; i < resources.length; i++) {
            resources[i] = register(manager, i + 1);
            resources[i].dispose();
            resources[i].dispose();
        }
        manager.deleteUnused(renderer);
        int deleted = 0;
        for (Resource resource : resources) {
            deleted += resource.counts.nativeDeletes;
            assertEquals(resource.counts.nativeDeletes, resource.counts.bufferDeletes);
        }
        assertEquals(100, deleted);
        manager.deleteUnused(renderer);
        for (Resource resource : resources) {
            assertEquals(1, resource.counts.nativeDeletes);
            assertEquals(1, resource.counts.bufferDeletes);
        }
    }

    @Test
    void unqueuedDirectUnregistrationNeverFreesCpuBuffers() {
        NativeObjectManager.UNSAFE = true;
        NativeObjectManager manager = new NativeObjectManager();
        Resource resource = register(manager, 1);
        manager.unregisterObject(resource);
        resource.resetObject();
        manager.deleteUnused(renderer);
        assertEquals(0, resource.counts.nativeDeletes);
        assertEquals(0, resource.counts.bufferDeletes);
    }
}
