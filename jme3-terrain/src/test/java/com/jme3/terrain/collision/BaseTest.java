package com.jme3.terrain.collision;

import com.jme3.asset.AssetManager;
import com.jme3.system.TestUtil;

/**
 * This class provides some utility functions to properly test the jMonkeyEngine.<br>
 * Thus it contains simple methods to get and create a headless assetManager amongst other things.<br>
 * If you need support for image/texture formats (png, tga, jpg, ...) see {@link BaseAWTTest}
 *
 * @author MeFisto94
 */
public abstract class BaseTest {
    private AssetManager assetManager;

    public AssetManager getAssetManager() {
        if (assetManager == null) {
            assetManager = createAssetManager();
        }

        return assetManager;
    }

    private AssetManager createAssetManager() {
        return TestUtil.createAssetManager();
    }

}
