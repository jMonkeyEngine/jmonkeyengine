package com.jme3.scene.plugins.ogre;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.scene.Spatial;

/**
 * Caches loaded Ogre scene mesh data to avoid re-parsing.
 * 
 * @deprecated The Ogre format is outdated and no longer maintained.
 *             Use glTF (.gltf/.glb) or J3O format instead.
 *             See the <a href="https://jmonkeyengine.org/wiki/">jMonkeyEngine Wiki</a>
 *             for migration guidance.
 */
@Deprecated
public class SceneMeshLoader extends MeshLoader{
    private Map<AssetKey,Spatial> cache=new HashMap<>();
    @Override
    public Object load(AssetInfo info) throws IOException {
        AssetKey key=info.getKey();
        Spatial output=cache.get(key);
        if(output==null){
            output=(Spatial)super.load(info);
            cache.put(key,output);
        }
        return output.clone(false);
    }
    public void reset(){
        cache.clear();
    }
}
