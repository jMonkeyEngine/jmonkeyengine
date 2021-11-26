package com.jme3.scene.plugins.fbx.objects;

import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxObject {

    protected final SceneLoader scene;
    public final FbxElement element;
    public final long id;
    public final String name;
    public final String type;

    public FbxObject(SceneLoader scene, FbxElement element) {
        this.scene = scene;
        this.element = element;
        this.id = (Long) element.properties.get(0);
        String name = (String) element.properties.get(1);
        this.name = name.substring(0, name.indexOf(0));
        this.type = (String) element.properties.get(2);
    }

    public void link(FbxObject child) {
    }

    public void link(FbxObject child, String propertyName) {
    }

    // Parent is 0 id
    public void linkToZero() {
    }

    protected static void readVectorFromProp(Vector3f store, FbxElement propElement) {
        float x = ((Double) propElement.properties.get(4)).floatValue();
        float y = ((Double) propElement.properties.get(5)).floatValue();
        float z = ((Double) propElement.properties.get(6)).floatValue();
        store.set(x, y, z);
    }
}
