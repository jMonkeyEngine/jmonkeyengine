package com.jme3.material;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetProcessor;

public class MaterialProcessor implements AssetProcessor {

    public Object postProcess(AssetKey key, Object obj) {
        return null;
    }

    public Object createClone(Object obj) {
        return ((Material) obj).clone();
    }
}
