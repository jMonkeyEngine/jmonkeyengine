/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

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
    
    protected final <T extends RenderParameter> T addParameter(T p) {
        parameters.add(p);
        return p;
    }
    
}
