package com.jme3.terrain.collision;

import com.jme3.asset.AssetManager;
import com.jme3.system.JmeDesktopSystem;
import com.jme3.system.JmeSystem;

/**
 * This class provides some utility functions to properly test the jMonkeyEngine.<br>
 * Thus it contains simple methods to get and create a headless assetManager amongst other things.<br>
 * In comparison to {@link BaseTest} it provides a DesktopAssetManager capable of loading image formats using AWT, which
 * however makes those tests unsuitable for headless ci testing. This requires jme3-desktop to be a testRuntime dependency.
 *
 * @author MeFisto94
 */
public abstract class BaseAWTTest {
    private AssetManager assetManager;

    static {
        //JmeSystem.setSystemDelegate(new JmeDesktopSystem());
    }

    public AssetManager getAssetManager() {
        if (assetManager == null) {
            assetManager = createAssetManager();
        }

        return assetManager;
    }

    private AssetManager createAssetManager() {
        /* Desktop.cfg supports the following additional file formats at the time of writing:
            LOADER com.jme3.texture.plugins.AWTLoader : jpg, bmp, gif, png, jpeg
            LOADER com.jme3.audio.plugins.OGGLoader : ogg
         */
        return JmeSystem.newAssetManager(BaseTest.class.getResource("/com/jme3/asset/Desktop.cfg"));
    }

}
