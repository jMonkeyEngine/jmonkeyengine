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
package com.jme3.asset;

import com.jme3.asset.plugins.ClasspathLocator;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link DesktopAssetManager#reloadAsset(com.jme3.asset.AssetKey)}.
 */
public class DesktopAssetManagerReloadTest {

    private static final String TEXTURE_PATH = "Textures/Terrain/Pond/Pond.jpg";

    private DesktopAssetManager assetManager;
    private TextureKey textureKey;

    private static final class TrackingListener implements AssetEventListener {

        private final List<AssetKey<?>> requested = new ArrayList<>();
        private final List<AssetKey<?>> loaded = new ArrayList<>();
        private final List<AssetKey<?>> reloaded = new ArrayList<>();

        @Override
        public void assetLoaded(AssetKey key) {
            loaded.add(key);
        }

        @Override
        public void assetRequested(AssetKey key) {
            requested.add(key);
        }

        @Override
        public void assetDependencyNotFound(AssetKey parentKey, AssetKey dependentAssetKey) {
        }

        @Override
        public void assetReloaded(AssetKey<?> key) {
            reloaded.add(key);
        }

        public int requestCount() {
            return requested.size();
        }

        public int loadedCount() {
            return loaded.size();
        }

        public int reloadedCount() {
            return reloaded.size();
        }
    }

    @Before
    public void setUp() {
        assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("/", ClasspathLocator.class);
        textureKey = new TextureKey(TEXTURE_PATH, false);
    }

    /**
     * reloadAsset should invalidate the cache entry, parse again, and notify listeners.
     */
    @Test
    public void reloadAssetReparsesAndNotifiesListeners() {
        TrackingListener listener = new TrackingListener();
        assetManager.addAssetEventListener(listener);

        Texture initial = assetManager.loadTexture(textureKey);
        Assert.assertNotNull(initial);
        Assert.assertEquals(1, listener.loadedCount());

        Texture reloaded = assetManager.reloadAsset(textureKey);

        Assert.assertNotNull(reloaded);
        Assert.assertEquals(2, listener.requestCount());
        Assert.assertEquals(2, listener.loadedCount());
        Assert.assertEquals(1, listener.reloadedCount());
        Assert.assertNotSame(initial, reloaded);
    }

    /**
     * reloadAsset must reject a null key.
     */
    @Test(expected = IllegalArgumentException.class)
    public void reloadAssetRejectsNullKey() {
        assetManager.reloadAsset(null);
    }
}
