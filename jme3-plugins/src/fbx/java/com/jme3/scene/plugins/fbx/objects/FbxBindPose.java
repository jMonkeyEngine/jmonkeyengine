package com.jme3.scene.plugins.fbx.objects;

import java.util.HashMap;
import java.util.Map;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxBindPose extends FbxObject {

    public Map<Long, Matrix4f> nodeTransforms = new HashMap<>();

    public FbxBindPose(SceneLoader scene, FbxElement element) {
        super(scene, element);
        if(type.equals("BindPose")) {
            for(FbxElement e : element.children) {
                if(e.id.equals("PoseNode")) {
                    long nodeId = 0;
                    double[] transform = null;
                    for(FbxElement e2 : e.children) {
                        switch(e2.id) {
                        case "Node":
                            nodeId = (Long) e2.properties.get(0);
                            break;
                        case "Matrix":
                            transform = (double[]) e2.properties.get(0);
                            break;
                        }
                    }
                    Matrix4f t = buildTransform(transform);
                    t.scale(new Vector3f(scene.unitSize, scene.unitSize, scene.unitSize));
                    nodeTransforms.put(nodeId, t);
                }
            }
        }
    }

    public void fillBindTransforms() {
        for(long nodeId : nodeTransforms.keySet()) {
            FbxNode node = scene.modelMap.get(nodeId);
            node.bindTransform = nodeTransforms.get(nodeId).clone();
        }
    }

    private static Matrix4f buildTransform(double[] transform) {
        float[] m = new float[transform.length];
        for(int i = 0; i < transform.length; ++i)
            m[i] = (float) transform[i];
        Matrix4f matrix = new Matrix4f();
        matrix.set(m, false);
        return matrix;
    }
}
