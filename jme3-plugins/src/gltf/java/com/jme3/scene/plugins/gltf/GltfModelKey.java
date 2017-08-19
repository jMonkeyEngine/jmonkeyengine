package com.jme3.scene.plugins.gltf;

import com.jme3.asset.ModelKey;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nehon on 08/08/2017.
 */
public class GltfModelKey extends ModelKey {

    private Map<String, MaterialAdapter> materialAdapters = new HashMap<>();
    private boolean keepSkeletonPose = false;

    public GltfModelKey(String name) {
        super(name);
    }

    public GltfModelKey() {
    }

    public void registerMaterialAdapter(String gltfMaterialName, MaterialAdapter adapter) {
        materialAdapters.put(gltfMaterialName, adapter);
    }

    public MaterialAdapter getAdapterForMaterial(String gltfMaterialName) {
        return materialAdapters.get(gltfMaterialName);
    }

    public boolean isKeepSkeletonPose() {
        return keepSkeletonPose;
    }

    public void setKeepSkeletonPose(boolean keepSkeletonPose) {
        this.keepSkeletonPose = keepSkeletonPose;
    }
}
