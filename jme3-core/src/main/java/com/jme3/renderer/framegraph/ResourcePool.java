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
public class ResourcePool {
    
    private final LinkedList<RenderResource> resources = new LinkedList<>();
    
    public void add(RenderResource res) {
        if (res.isVirtual()) {
            throw new IllegalArgumentException("Resource cannot be virtual.");
        }
        resources.add(res);
    }
    
    public <T> boolean acquireExisting(RenderResource<T> resource) {
        for (Iterator<RenderResource> it = resources.iterator(); it.hasNext();) {
            RenderResource<Object> r = it.next();
            T result = resource.getDefinition().repurpose(r.getDefinition(), r.getResource());
            if (result != null) {
                resource.setResource(result);
                it.remove();
                return true;
            }
        }
        return false;
    }
    
}
