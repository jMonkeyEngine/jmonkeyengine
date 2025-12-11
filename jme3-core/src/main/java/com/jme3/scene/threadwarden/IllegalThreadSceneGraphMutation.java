package com.jme3.scene.threadwarden;

public class IllegalThreadSceneGraphMutation extends IllegalStateException{
    public IllegalThreadSceneGraphMutation(String message){
        super(message);
    }
}
