package com.jme3.scene.plugins.fbx.objects;

import java.util.ArrayList;
import java.util.List;

import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxAnimationStack extends FbxObject {
	
	public List<Long> animationLayers = new ArrayList<Long>();
	public long start;
	public long stop;

	public FbxAnimationStack(SceneLoader scene, FbxElement element) {
		super(scene, element);
	}
	
	@Override
	public void link(FbxObject child) {
		if(child instanceof FbxAnimationLayer) {
			animationLayers.add(child.id);
		}
	}
}
