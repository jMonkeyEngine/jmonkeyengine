/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public abstract class RenderPass {
    
    private final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    private final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    private int refs = 0;
    
    public abstract void createResources(RenderContext context, ResourceList resources);
    public abstract void execute(RenderContext context);
    
    public void countReferences() {
        refs = outputs.size();
    }
    public boolean dereference() {
        refs--;
        return isReferenced();
    }
    public boolean isReferenced() {
        return refs > 0;
    }
    
    public void dereferenceInputs(ResourceList resources, List<RenderResource> resList) {
        resources.dereferenceListed(inputs, resList);
    }
    
}
