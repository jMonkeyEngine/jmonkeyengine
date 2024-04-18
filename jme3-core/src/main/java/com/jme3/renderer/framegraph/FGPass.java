/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;

/**
 * Defines a rendering pass made by the framegraph.
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
    
    public void setSinkLinkage(String registeredName, String targetPass, String targetResource);
    
    public void registerSink(AbstractFGSink sink);
    
    public void registerSource(FGSource source);
    
    public void resetPass();
    
    public void finalizePass();
    
}
