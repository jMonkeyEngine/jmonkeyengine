package com.jme3.scene.plugins.fbx.objects;

import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxTexture extends FbxObject {

    String bindType;
    String filename;

    public Texture texture;

    public FbxTexture(SceneLoader scene, FbxElement element) {
        super(scene, element);
        for(FbxElement e : element.children) {
            switch(e.id) {
            case "Type":
                bindType = (String) e.properties.get(0);
                break;
            case "FileName":
                filename = (String) e.properties.get(0);
                break;
            }
        }
        texture = new Texture2D();
        texture.setName(name);
        texture.setWrap(WrapMode.Repeat); // Default FBX wrapping. TODO: Investigate where this is stored (probably, in material)
    }

    @Override
    public void link(FbxObject otherObject) {
        if(otherObject instanceof FbxImage) {
            FbxImage img = (FbxImage) otherObject;
            if(img.image == null)
                return;
            texture.setImage(img.image);
        }
    }
}
