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
public class ResourceAllocator {
    
    private final LinkedList<RenderResource> resources = new LinkedList<>();
    
    public void add(RenderResource res) {
        if (res.isVirtual()) {
            throw new IllegalArgumentException("Resource cannot be virtual.");
        }
        resources.add(res);
    }
    
    /**
     * Reallocates a resource from the pool to the given resource, if the
     * resource definition accepts.
     * 
     * @param <T>
     * @param resource
     * @return true if a resource was reallocated to the given resource
     */
    public <T> boolean reallocateTo(RenderResource<T> resource) {
        for (Iterator<RenderResource> it = resources.iterator(); it.hasNext();) {
            RenderResource res = it.next();
            if (resource.getDefinition().isOfResourceType(res.getResource().getClass())) {
                T r = (T)res.getResource();
                if (resource.getDefinition().acceptReallocationOf(r)) {
                    resource.setResource(r);
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }
    
}
