package com.jme3.scene.plugins.fbx.objects;

import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxAnimCurve extends FbxObject {

    public long[] keyTimes;
    public float[] keyValues;
    public float defaultValue = 0.0f;

    public FbxAnimCurve(SceneLoader scene, FbxElement element) {
        super(scene, element);
        for(FbxElement e : element.children) {
            switch(e.id) {
            case "KeyTime":
                keyTimes = (long[]) e.properties.get(0);
                break;
            case "KeyValueFloat":
                keyValues = (float[]) e.properties.get(0);
                break;
            case "Default":
                defaultValue = ((Number) e.properties.get(0)).floatValue();
                break;
            }
        }
    }

    public float getValue(long time) {
        // Search animation interval
        for(int i = 0; i < keyTimes.length; ++i) {
            if(keyTimes[i] == time) { // hit the keyframe
                return keyValues[i];
            } else if(keyTimes[i] > time) {
                if(i == 0) { // left from the whole range
                    return defaultValue;//keyValues[0];
                } else {
                    // Interpolate between two keyframes
                    float dt = keyTimes[i] - keyTimes[i - 1];
                    float dtInt = time - keyTimes[i - 1];
                    float dv = keyValues[i] - keyValues[i - 1];
                    return keyValues[i - 1] + dv * (dtInt / dt);
                }
            }
        }
        // right from the whole range
        return defaultValue;//keyValues[keyValues.length - 1];
    }
}