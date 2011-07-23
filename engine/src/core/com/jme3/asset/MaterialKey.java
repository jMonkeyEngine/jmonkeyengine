package com.jme3.asset;

import com.jme3.material.Material;

/**
 * Used for loading {@link Material materials} only (not material definitions).
 * 
 * @author Kirill Vainer
 */
public class MaterialKey extends AssetKey {
    public MaterialKey(String name){
        super(name);
    }

    public MaterialKey(){
        super();
    }

    @Override
    public boolean useSmartCache(){
        return true;
    }
    
    @Override
    public Object createClonedInstance(Object asset){
        Material mat = (Material) asset;
        return mat.clone();
    }
}
