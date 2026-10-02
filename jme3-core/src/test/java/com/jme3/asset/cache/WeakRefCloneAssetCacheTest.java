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
package com.jme3.asset.cache;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.asset.AssetKey;
import com.jme3.asset.ModelKey;
import com.jme3.scene.Node;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WeakRefCloneAssetCacheTest {

    @Test
    public void collectedKeyRemovesCachedAsset() throws ReflectiveOperationException {
        WeakRefCloneAssetCache cache = new WeakRefCloneAssetCache();
        ModelKey key = new ModelKey("collected.j3o");
        Node original = new Node("original");
        cache.addToCache(key, original);
        Node clone = (Node) original.clone();
        cache.registerAssetClone(key, clone);
        assertSame(key, clone.getKey());
        assertNull(original.getKey());
        assertSame(original, cache.getFromCache(new ModelKey("collected.j3o")));
        cache.notifyNoAssetClone();

        // Simulate collection without depending on when the JVM runs its collector.
        Reference<?> reference = cachedReference(cache, key);
        reference.clear();
        assertTrue(reference.enqueue());
        addAsset(cache, new ModelKey("another.j3o"));

        assertNull(cache.getFromCache(key));
        assertFalse(cache.deleteFromCache(key), "The collected entry should already have been removed");
    }

    @Test
    public void delayedCollectionDoesNotRemoveReplacement() throws ReflectiveOperationException {
        WeakRefCloneAssetCache cache = new WeakRefCloneAssetCache();
        ModelKey key = new ModelKey("replaced.j3o");
        addAsset(cache, key);
        Reference<?> oldReference = cachedReference(cache, key);
        ModelKey replacementKey = new ModelKey("replaced.j3o");
        Node replacement = addAsset(cache, replacementKey);

        oldReference.clear();
        assertTrue(oldReference.enqueue());
        addAsset(cache, new ModelKey("another.j3o"));

        assertSame(replacement, cache.getFromCache(key));
        cache.notifyNoAssetClone();
        assertSame(replacementKey, ((Reference<?>) cachedEntry(cache, key)).get());
    }

    @Test
    public void delayedCollectionAfterDeleteDoesNotRemoveReloadedAsset() throws ReflectiveOperationException {
        WeakRefCloneAssetCache cache = new WeakRefCloneAssetCache();
        ModelKey key = new ModelKey("deleted.j3o");
        addAsset(cache, key);
        Reference<?> oldReference = cachedReference(cache, key);
        assertTrue(cache.deleteFromCache(key));
        ModelKey replacementKey = new ModelKey("deleted.j3o");
        Node replacement = addAsset(cache, replacementKey);

        oldReference.clear();
        assertTrue(oldReference.enqueue());
        addAsset(cache, new ModelKey("another.j3o"));

        assertSame(replacement, cache.getFromCache(key));
        cache.notifyNoAssetClone();
        assertSame(replacementKey, ((Reference<?>) cachedEntry(cache, key)).get());
    }

    @Test
    public void delayedCollectionAfterClearDoesNotRemoveReloadedAsset() throws ReflectiveOperationException {
        WeakRefCloneAssetCache cache = new WeakRefCloneAssetCache();
        ModelKey key = new ModelKey("cleared.j3o");
        addAsset(cache, key);
        Reference<?> oldReference = cachedReference(cache, key);
        cache.clearCache();
        ModelKey replacementKey = new ModelKey("cleared.j3o");
        Node replacement = addAsset(cache, replacementKey);

        oldReference.clear();
        assertTrue(oldReference.enqueue());
        addAsset(cache, new ModelKey("another.j3o"));

        assertSame(replacement, cache.getFromCache(key));
        cache.notifyNoAssetClone();
        assertSame(replacementKey, ((Reference<?>) cachedEntry(cache, key)).get());
    }

    private static Node addAsset(WeakRefCloneAssetCache cache, ModelKey key) {
        Node original = new Node(key.getName());
        cache.addToCache(key, original);
        cache.notifyNoAssetClone();
        return original;
    }

    private static Reference<?> cachedReference(WeakRefCloneAssetCache cache, AssetKey<?> key)
            throws ReflectiveOperationException {
        // Reflection intentionally exposes the tracked phantom reference so tests
        // can enqueue it deterministically, without depending on JVM GC timing.
        Object entry = cachedEntry(cache, key);
        Field field = entry.getClass().getDeclaredField("cleanupRef");
        field.setAccessible(true);
        return (Reference<?>) field.get(entry);
    }

    // Keep inspection test-only instead of exposing a public cache getter.
    private static Object cachedEntry(WeakRefCloneAssetCache cache, AssetKey<?> key)
            throws ReflectiveOperationException {
        Field field = WeakRefCloneAssetCache.class.getDeclaredField("smartCache");
        field.setAccessible(true);
        Map<?, ?> entries = (Map<?, ?>) field.get(cache);
        return entries.get(key);
    }
}
