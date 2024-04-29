/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.RenderManager;
import java.util.LinkedList;

/**
 * Manages render passes, dependencies, and resources in a node-based parameter system.
 * 
 * @author codex
 */
public class FrameGraph {
    
    private final LinkedList<RenderPass> passes = new LinkedList<>();
    private final ResourceList resources;
    private final RenderContext context;

    public FrameGraph(RenderManager renderManager) {
        this.resources = new ResourceList(renderManager.getResourcePool(), 30);
        this.context = new RenderContext(renderManager);
    }
    
    public void cull() {
        // count number of outputs for each pass
        for (RenderPass p : passes) {
            p.countReferences();
        }
        // fetch unreferences resources
        LinkedList<RenderResource> cull = new LinkedList<>();
        resources.getUnreferenced(cull);
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
        // create resources...
        // execute
        for (RenderPass p : passes) {
            if (p.isReferenced()) {
                p.execute(context);
            }
        }
    }
    
}
