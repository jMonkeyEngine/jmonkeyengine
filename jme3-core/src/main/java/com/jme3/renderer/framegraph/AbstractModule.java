/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.parameters.RenderParameter;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public abstract class AbstractModule implements FGModule {

    private final LinkedList<RenderParameter> parameters = new LinkedList<>();
    
    @Override
    public Collection<RenderParameter> getRenderParameters() {
        return parameters;
    }
    
    @Override
    public void preFrame(RenderContext context) {}
    
    @Override
    public void postQueue(RenderContext context) {}
    
    protected final <T extends RenderParameter> T addParameter(T p) {
        parameters.add(p);
        return p;
    }
    
}
