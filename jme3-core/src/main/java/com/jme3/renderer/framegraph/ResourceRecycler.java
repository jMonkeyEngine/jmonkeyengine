/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.Iterator;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class ResourceRecycler {
    
    private final LinkedList<RenderResource> resources = new LinkedList<>();
    private int timeout = 1;
    
    public void add(RenderResource res) {
        if (res.isVirtual()) {
            throw new IllegalArgumentException("Resource cannot be virtual.");
        }
        if (res.getDefinition().getRecycleTimeout() >= 0) {
            res.setTimeout(res.getDefinition().getRecycleTimeout());
        } else {
            res.setTimeout(timeout);
        }
        //res.getDefinition().destroy(res.getResource());
        resources.add(res);
    }
    
    public <T> boolean recycle(RenderResource<T> resource) {
        ResourceDef<T> def = resource.getDefinition();
        if (!def.isAcceptsRecycled()) {
            return false;
        }
        System.out.println("      try "+resources.size()+" available resources to recycle");
        for (Iterator<RenderResource> it = resources.iterator(); it.hasNext();) {
            RenderResource res = it.next();
            if (def.isOfResourceType(res.getResource().getClass())) {
                T r = (T)res.getResource();
                if (def.applyRecycled(r)) {
                    System.out.println("      recycle for "+resource);
                    resource.setResource(r);
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }
    
    public void flush() {
        // Note: flushing resources on the same frame they were created causes the
        // screen to render black. Guess: destroying output texture early causes
        // the input to become null?
        int n = 0;
        for (Iterator<RenderResource> it = resources.iterator(); it.hasNext();) {
            RenderResource r = it.next();
            if (!r.tickTimeout()) {
                n++;
                r.getDefinition().destroy(r.getResource());
                it.remove();
            }
        }
        System.out.println("resources flushed: "+n);
    }
    
}
