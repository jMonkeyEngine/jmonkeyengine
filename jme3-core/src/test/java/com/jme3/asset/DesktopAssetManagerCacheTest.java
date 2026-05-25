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
 * Tests {@link DesktopAssetManager} caching and {@link AssetEventListener} behavior.
 */
public class DesktopAssetManagerCacheTest {

    private static final String TEXTURE_PATH = "Textures/Terrain/Pond/Pond.jpg";

    private DesktopAssetManager assetManager;
    private TextureKey textureKey;

    private static final class CountingAssetListener implements AssetEventListener {

        private final List<AssetKey<?>> requested = new ArrayList<>();
        private final List<AssetKey<?>> loaded = new ArrayList<>();

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

        public int requestCount() {
            return requested.size();
        }

        public int loadedCount() {
            return loaded.size();
        }
    }

    @Before
    public void setUp() {
        assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("/", ClasspathLocator.class);
        textureKey = new TextureKey(TEXTURE_PATH, false);
    }

    /**
     * A cache hit should still notify assetRequested but must not invoke assetLoaded again.
     */
    @Test
    public void cachedLoadFiresRequestedButNotSecondLoadedEvent() {
        CountingAssetListener listener = new CountingAssetListener();
        assetManager.addAssetEventListener(listener);

        Texture first = assetManager.loadTexture(textureKey);
        Texture second = assetManager.loadTexture(textureKey);

        Assert.assertNotNull(first);
        Assert.assertNotNull(second);
        Assert.assertEquals(2, listener.requestCount());
        Assert.assertEquals(1, listener.loadedCount());
        Assert.assertNotNull(assetManager.getFromCache(textureKey));
        Assert.assertNotSame(first, second);
    }

    /**
     * Clearing the cache forces a full reload on the next request.
     */
    @Test
    public void clearCacheCausesAssetToReload() {
        CountingAssetListener listener = new CountingAssetListener();
        assetManager.addAssetEventListener(listener);

        assetManager.loadTexture(textureKey);
        assetManager.clearCache();
        assetManager.loadTexture(textureKey);

        Assert.assertEquals(2, listener.requestCount());
        Assert.assertEquals(2, listener.loadedCount());
    }

    /**
     * loadAsset must reject a null key before locating or caching.
     */
    @Test(expected = IllegalArgumentException.class)
    public void loadAssetRejectsNullKey() {
        assetManager.loadAsset((AssetKey<?>) null);
    }
}
