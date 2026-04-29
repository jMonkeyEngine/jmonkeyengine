package com.jme3.scene.plugins.ogre;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.scene.Spatial;

/**
 * Caching mesh loader for Ogre scene files.
 * 
 * @deprecated as of jMonkeyEngine 3.6, for removal in a future version.
 * The Ogre model format is deprecated. Consider migrating to glTF (.glb/.gltf) 
 * or other modern model formats for better performance, broader tool support, and future compatibility.
 * See <a href="https://jmonkeyengine.org/docs/3.6/manual/upgrading_to_glTF/">glTF migration guide</a>.
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
