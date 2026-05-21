package com.jme3.vulkan.slang;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;

import java.io.IOException;

public class SlangModuleLoader implements AssetLoader {

    private static final GlobalSession global = new GlobalSession();
    private final Session session = global.createSession();

    @Override
    public Module load(AssetInfo assetInfo) throws IOException {

    }

}
