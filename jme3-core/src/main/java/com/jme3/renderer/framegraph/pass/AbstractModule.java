/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.renderer.framegraph.parameters.ParamSocket;
import com.jme3.renderer.framegraph.parameters.Referenceable;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public abstract class AbstractModule implements FGModule {

    private final LinkedList<ParamSocket> inSockets = new LinkedList<>();
    private final LinkedList<ParamSocket> outSockets = new LinkedList<>();
    private int refs = 0;
    
    @Override
    public Collection<ParamSocket> getInputSockets() {
        return inSockets;
    }
    
    @Override
    public Collection<ParamSocket> getOutputSockets() {
        return outSockets;
    }
    
    @Override
    public void preFrame(RenderContext context) {}
    
    @Override
    public void postQueue(RenderContext context) {}

    @Override
    public int compileNumReferences() {
        return (refs = outSockets.size());
    }

    @Override
    public int removeReference() {
        return --refs;
    }

    @Override
    public void dereferenceUpstream(Collection<Referenceable> derefs) {
        derefs.addAll(inSockets);
    }
    
    @Override
    public int getNumReferences() {
        return refs;
    }
    
}
