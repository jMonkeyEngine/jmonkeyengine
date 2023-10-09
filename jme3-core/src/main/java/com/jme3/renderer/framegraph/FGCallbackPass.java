/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author JohnKkk
 */
public class FGCallbackPass extends FGBindingPass{
    public static interface IFGCallbackInterface{
        void callbackPass();
    }
    private IFGCallbackInterface iFGCallbackInterface;
    
    private FGCallbackPass(String name, IFGCallbackInterface ifgci) {
        super(name);
        iFGCallbackInterface = ifgci;
    }

    @Override
    public void execute(FGRenderContext renderContext) {
        bindAll(renderContext);
        if(iFGCallbackInterface != null){
            iFGCallbackInterface.callbackPass();
        }
    }
    
    public final static FGCallbackPass makePass(String passName, IFGCallbackInterface iFGCallbackInterface){
        return new FGCallbackPass(passName, iFGCallbackInterface);
    }
    
}
