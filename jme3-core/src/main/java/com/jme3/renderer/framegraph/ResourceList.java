/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public class ResourceList {
    
    private static final int INITIAL_SIZE = 20;
    
    private final ResourceAllocator allocator;
    private ArrayList<RenderResource> resources = new ArrayList<>(INITIAL_SIZE);
    private int nextSlot = 0;

    public ResourceList(ResourceAllocator allocator) {
        this.allocator = allocator;
    }
    
    protected <T> RenderResource<T> locate(ResourceTicket<T> ticket) {
        final int i = ticket.getIndex();
        if (i >= 0 && i < resources.size()) {
            RenderResource<T> res = resources.get(i);
            if (res != null) {
                return res;
            }
            throw new NullPointerException(ticket+" points to null resource.");
        }
        throw new IndexOutOfBoundsException(ticket+" is out of bounds for size "+resources.size());
    }
    protected int add(RenderResource res) {
        assert res != null;
        if (nextSlot >= resources.size()) {
            // add resource to end of list
            resources.add(res);
            nextSlot++;
            return resources.size()-1;
        } else {
            // insert resource into available slot
            int i = nextSlot;
            resources.set(i, res);
            // find next available slot
            while (++nextSlot < resources.size()) {
                RenderResource r = resources.get(nextSlot);
                if (r == null) {
                    break;
                }
            }
            return i;
        }
    }
    protected RenderResource remove(int index) {
        nextSlot = Math.min(nextSlot, index);
        return resources.set(index, null);
    }
    
    public <T> ResourceTicket<T> register(RenderPass producer, ResourceDef<T> def) {
        return new ResourceTicket<>(add(new RenderResource<>(producer, def)));
    }
    public void reference(ResourceTicket ticket) {
        locate(ticket).reference();
    }
    public <T> T acquire(ResourceTicket<T> ticket) {
        RenderResource<T> res = locate(ticket);
        if (res.isVirtual() && (!res.getDefinition().isAcceptsReallocated() || allocator.reallocateTo(res))) {
            res.create();
        }
        return res.getResource();
    }
    public void release(ResourceTicket ticket) {
        RenderResource res = locate(ticket);
        res.release();
        if (!res.isUsed()) {
            remove(ticket.getIndex());
        }
        if (res.getDefinition().isReallocatable()) {
            allocator.add(res);
        }
    }
    
    public void getUnreferenced(List<RenderResource> resList) {
        for (int i = 0; i < resources.size(); i++) {
            RenderResource r = resources.get(i);
            if (r != null && !r.isReferenced()) {
                resList.add(r);
            }
        }
    }
    public void dereferenceListed(List<ResourceTicket> tickets, List<RenderResource> resList) {
        for (ResourceTicket t : tickets) {
            RenderResource r = locate(t);
            r.release();
            if (!r.isReferenced()) {
                resList.add(r);
            }
        }
    }
    public void discardUnreferenced() {
        for (int i = 0; i < resources.size(); i++) {
            RenderResource r = resources.get(i);
            if (!r.isReferenced()) {
                remove(i);
            }
        }
    }
    public void clear() {
        int size = resources.size();
        resources = new ArrayList<>(size);
        nextSlot = 0;
    }
    
}
