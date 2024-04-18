/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;

/**
 *
 * @author codex
 */
public interface FGPass {

    public String getName();

    public ArrayList<FGSink> getSinks();

    public ArrayList<FGSource> getSources();
    
    public boolean isResetResources();
    
    public void prepare(FGRenderContext renderContext);
    
    public void execute(FGRenderContext renderContext);
    
    public FGSource getSource(String name);
    
    public FGSink getSink(String registeredName);
    
    public void setSinkLinkage(String registeredName, String target);
    
    public void registerSink(AbstractFGSink sink);
    
    public void registerSource(FGSource source);
    
    public void reset();
    
    // WARNING:
    // This method's name conflicts with Object.finalize. I am not sure if
    // that is on purpose, so I will not attempt to change it.
    public void finalize();
    
}
