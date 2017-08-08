package com.jme3.scene.plugins.gltf;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;

import java.io.IOException;

/**
 * Created by Nehon on 08/08/2017.
 */
public class BinLoader implements AssetLoader {
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        return assetInfo.openStream();
    }
}
