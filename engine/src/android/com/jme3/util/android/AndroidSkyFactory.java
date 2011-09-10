package com.jme3.util.android;


import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;

/**
 * <code>AndroidSkyFactory</code> creates a sky box spatial
 * @author larynx, derived from SkyFactory and adapted for android
 * @deprecated Use {@link SkyFactory} instead
 */
@Deprecated
public class AndroidSkyFactory {

    public static Spatial createSky(AssetManager assetManager, Texture texture, Vector3f normalScale, boolean sphereMap) {
        return SkyFactory.createSky(assetManager, texture, normalScale, sphereMap);
    }

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south,
            Texture up, Texture down, Vector3f normalScale) {
        return SkyFactory.createSky(assetManager, west, east, north, south, up, down, normalScale);
    }

    public static Spatial createSky(AssetManager assetManager, Texture west, Texture east, Texture north, Texture south,
            Texture up, Texture down) {
        return SkyFactory.createSky(assetManager, west, east, north, south, up, down, Vector3f.UNIT_XYZ);
    }

    public static Spatial createSky(AssetManager assetManager, Texture texture, boolean sphereMap) {
        return SkyFactory.createSky(assetManager, texture, Vector3f.UNIT_XYZ, sphereMap);
    }

    public static Spatial createSky(AssetManager assetManager, String textureName, boolean sphereMap) {
        return SkyFactory.createSky(assetManager, textureName, sphereMap);
    }
}