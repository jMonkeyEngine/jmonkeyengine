package com.jme3.material.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;

import java.io.IOException;

/**
 * J3md loader which throws an exception on load. Useful for ensuring
 * not j3md usages are in an incompatible application and potentially
 * messing things up.
 */
public class J3mdDeprecationLoader implements AssetLoader {

    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        throw new UnsupportedOperationException("j3md material definition format is not supported by the application. Use the j4md format instead.");
    }

}
