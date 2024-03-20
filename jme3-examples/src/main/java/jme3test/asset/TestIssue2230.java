/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.asset;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.FileLocator;

/**
 *
 * @author codex
 */
public class TestIssue2230 extends SimpleApplication {

    public static void main(String[] args) {
        new TestIssue2230().start();
    }
    
    @Override
    public void simpleInitApp() {
        
        assetManager.registerLocator("NonExistantFolder", FileLocator.class);
        assetManager.loadModel("NonExistantModel.j3o");
        
        stop();
        
    }
    
}
