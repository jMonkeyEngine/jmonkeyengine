package com.jme3.scene.plugins.fbx.objects;

import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxCluster extends FbxObject {

    public int[] indexes;
    public double[] weights;
    public double[] transform;
    public double[] transformLink;
    public FbxSkin skin;

    public FbxCluster(SceneLoader scene, FbxElement element) {
        super(scene, element);
        for(FbxElement e : element.children) {
            switch(e.id) {
            case "Indexes":
                indexes = (int[]) e.properties.get(0);
                break;
            case "Weights":
                weights = (double[]) e.properties.get(0);
                break;
            case "Transform":
                transform = (double[]) e.properties.get(0);
                break;
            case "TransformLink":
                transformLink = (double[]) e.properties.get(0);
                break;
            }
        }
    }

    @Override
    public void link(FbxObject child) {
        if(child instanceof FbxNode) {
            FbxNode limb = (FbxNode) child;
            limb.skinToCluster.put(skin.id, this);
            skin.bones.add(limb);
        }
    }
}
