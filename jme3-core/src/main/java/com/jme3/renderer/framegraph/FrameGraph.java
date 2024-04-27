/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class FrameGraph {
    
    private final LinkedList<RenderPass> passes = new LinkedList<>();
    private final ResourceRegistry resources;

    public FrameGraph(ResourcePool pool) {
        this.resources = new ResourceRegistry(pool);
    }
    
    public void cull() {
        // count number of outputs for each pass
        for (RenderPass p : passes) {
            p.countReferences();
        }
        // fetch unreferences resources
        LinkedList<RenderResource> cull = new LinkedList<>();
        resources.getUnreferencedResources(cull);
        RenderResource res;
        while ((res = cull.pollFirst()) != null) {
            // dereference producer of resource
            if (!res.getProducer().dereference()) {
                // if producer is not referenced, dereference all input resources
                res.getProducer().dereferenceInputs(resources, cull);
            }
        }
    }
    
    public void execute() {
        
    }
    
}
