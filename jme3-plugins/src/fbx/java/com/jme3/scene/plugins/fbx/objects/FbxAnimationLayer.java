package com.jme3.scene.plugins.fbx.objects;

import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxAnimationLayer extends FbxObject {
	
	public FbxAnimationLayer(SceneLoader scene, FbxElement element) {
		super(scene, element);
	}

	@Override
	public void link(FbxObject child) {
		if(child instanceof FbxAnimNode) {
			((FbxAnimNode) child).layerId = id;
		}
	}
}
