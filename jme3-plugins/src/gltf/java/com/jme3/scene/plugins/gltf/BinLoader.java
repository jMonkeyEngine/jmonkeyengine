package com.jme3.scene.plugins.gltf;

import com.jme3.asset.*;

import java.io.IOException;

/**
 * Created by Nehon on 08/08/2017.
 */
public class BinLoader implements AssetLoader {
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {

        if (!(assetInfo.getKey() instanceof BinDataKey)) {
            throw new AssetLoadException(".bin files cannot be loaded directly, load the associated .gltf file");
        }

        return assetInfo.openStream();
    }
}
