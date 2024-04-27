/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public class ResourceRegistry {
    
    private final ArrayList<RenderResource> resources = new ArrayList<>();
    private final ResourcePool pool;

    public ResourceRegistry(ResourcePool pool) {
        this.pool = pool;
    }
    
    protected <T> RenderResource<T> locateResource(ResourceTicket<T> ticket) {
        if (ticket.isLocateByName()) {
            String name = ticket.getName();
            int i = 0;
            for (RenderResource r : resources) {
                if (r.getTicket().getName().equals(name)) {
                    // make the next location operation with this ticket faster
                    ticket.setIndex(i);
                    return r;
                }
                i++;
            }
            throw new NullPointerException("Unable to locate resource by "+ticket);
        } else {
            int i = ticket.getIndex();
            if (i < 0 || i >= resources.size()) {
                throw new IndexOutOfBoundsException(ticket+" for size "+resources.size());
            }
            RenderResource<T> res = resources.get(i);
            if (!res.getTicket().getName().equals(ticket.getName())) {
                throw new NullPointerException("Unable to locate resource by "+ticket);
            }
            return res;
        }
    }
    
    public <T> ResourceTicket<T> registerFutureResource(RenderPass producer, String name, ResourceDef<T> def) {
        ResourceTicket<T> ticket = new ResourceTicket(name, resources.size());
        RenderResource<T> res = new RenderResource<>(producer, def, ticket);
        resources.add(res);
        return ticket;
    }
    
    public void referenceResource(ResourceTicket ticket) {
        locateResource(ticket).reference();
    }
    
    public <T> T acquireResource(ResourceTicket<T> ticket) {
        RenderResource<T> res = locateResource(ticket);
        if (res.isVirtual() && !pool.acquireExisting(res)) {
            res.create();
        }
        res.acquire();
        return res.getResource();
    }
    
    public void releaseResource(ResourceTicket ticket) {
        RenderResource res = locateResource(ticket);
        if (!res.release()) {
            pool.add(res);
        }
    }
    
    public void getUnreferencedResources(List<RenderResource> resList) {
        for (RenderResource r : resources) {
            if (!r.isReferenced()) {
                resList.add(r);
            }
        }
    }
    
}
