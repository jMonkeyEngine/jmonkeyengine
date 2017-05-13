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
	
	public boolean hasValue(long time) {
		for(int i = 0; i < keyTimes.length; ++i) {
			if(keyTimes[i] == time) { // hit the keyframe
				return true;
			} else if(keyTimes[i] > time) {
				break;
			}
		}
		return false;
	}
	
	public float getValue(long time, float def) {
		// Search animation interval
		for(int i = 0; i < keyTimes.length; ++i) {
			if(keyTimes[i] == time) { // hit the keyframe
				return keyValues[i];
			} else if(keyTimes[i] > time) {
				if(i == 0) { // left from the whole range
					return keyValues[0];
				} else {
					// Interpolate between two keyframes
					float dt = (float) (keyTimes[i] - keyTimes[i - 1]);
					float dtInt = (float) (time - keyTimes[i - 1]);
					float dv = keyValues[i] - keyValues[i - 1];
					float result = keyValues[i - 1] + dv * (dtInt / dt);
					// For proper interpolation we need to use Bezier curves from KeyAttrDataFloat
					// Just bake your animations...
					return result;
				}
			}
		}
		// right from the whole range
		return keyValues[keyValues.length - 1];
	}
}