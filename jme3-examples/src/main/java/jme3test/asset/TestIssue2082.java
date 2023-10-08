/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.asset;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.gltf.GltfLoader;

/**
 * Test application of issue 2082.
 * 
 * {@link GltfLoader} is incorrectly loading blend modes.
 * 
 * @author codex
 */
public class TestIssue2082 extends SimpleApplication {
    
    public static void main(String[] args) {
        new TestIssue2082().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        Spatial model = assetManager.loadModel("Models/gltf/BlendModeTest/AlphaBlendModeTest.gltf");
        rootNode.attachChild(model);
        
        rootNode.addLight(new DirectionalLight(new Vector3f(0f, 0f, -1f)));
        
    }
    
}
