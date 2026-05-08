package com.jme3.vulkan.shader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ShaderAsset implements AssetLoader {

    private final String assetName, code;

    public ShaderAsset() {
        this(null, null);
    }

    public ShaderAsset(String assetName, String code) {
        this.assetName = assetName;
        this.code = code;
    }

    @Override
    public ShaderAsset load(AssetInfo assetInfo) throws IOException {
        return new ShaderAsset(assetInfo.getKey().getName(), new BufferedReader(
                new InputStreamReader(assetInfo.openStream())).lines().collect(Collectors.joining("\n")));
    }

    public String getAssetName() {
        return assetName;
    }

    public String getCode() {
        return code;
    }

}
