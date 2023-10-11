/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *FGBindable can be any resource that needs binding (such as state machine, FBO, Texture, Paramater...)
 * @author JohnKkk
 */
public abstract class FGBindable {
    private final static String _S_DEFAULT_BINDABLE_UID = "";
    
    public void bind(FGRenderContext renderContext){}
    
    public String getUID(){
        assert false;
        return _S_DEFAULT_BINDABLE_UID;
    }
}
