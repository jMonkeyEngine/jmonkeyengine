/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public class ResourceList {
    
    private static final int INITIAL_SIZE = 20;
    
    private ResourceRecycler recycler;
    private ArrayList<RenderResource> resources = new ArrayList<>(INITIAL_SIZE);
    private int nextSlot = 0;

    public ResourceList(ResourceRecycler recycler) {
        this.recycler = recycler;
    }
    
    protected <T> RenderResource<T> locate(ResourceTicket<T> ticket) {
        if (ticket == null) {
            throw new NullPointerException("Ticket cannot be null.");
        }
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
        RenderResource prev = resources.set(index, null);
        if (prev != null && prev.isReferenced()) {
            throw new IllegalStateException("Cannot remove "+prev+" because it is referenced.");
        }
        nextSlot = Math.min(nextSlot, index);
        return prev;
    }
    
    public <T> ResourceTicket<T> register(ResourceProducer producer, ResourceDef<T> def) {
        return register(producer, def, null);
    }
    public <T> ResourceTicket<T> register(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> store) {
        ResourceTicket<T> t = new ResourceTicket<>();
        t.setIndex(add(new RenderResource<>(producer, def, t)));
        store = t.copyIndexTo(store);
        return store;
    }
    
    public void reference(ResourceTicket ticket) {
        locate(ticket).reference();
    }
    public boolean referenceOptional(ResourceTicket ticket) {
        if (ticket != null) {
            reference(ticket);
            return true;
        }
        return false;
    }
    public void reference(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reference(t);
        }
    }
    public void referenceOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            referenceOptional(t);
        }
    }
    
    public <T, R extends ResourceDef<T>> R getDef(Class<R> type, ResourceTicket<T> ticket) {
        ResourceDef<T> def = locate(ticket).getDefinition();
        if (type.isAssignableFrom(def.getClass())) {
            return (R)def;
        }
        return null;
    }
    public <T> void setDirect(ResourceTicket<T> ticket, T resource) {
        locate(ticket).setResource(resource);
    }
    
    public <T> T acquire(ResourceTicket<T> ticket) {
        RenderResource<T> res = locate(ticket);
        if (!res.isUsed()) {
            throw new IllegalStateException(res+" was unexpectedly acquired.");
        }
        if (res.isVirtual() && !recycler.recycle(res)) {
            res.create();
        }
        return res.getResource();
    }
    public <T> T acquire(ResourceTicket<T> ticket, T value) {
        if (ticket != null) {
            T r = acquire(ticket);
            if (r != null) {
                return r;
            }
        }
        return value;
    }
    public void acquireColorTargets(FrameBuffer fbo, ResourceTicket<? extends Texture>... tickets) {
        for (ResourceTicket<? extends Texture> t : tickets) {
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(acquire(t)));
        }
    }
    
    public void release(ResourceTicket ticket) {
        RenderResource res = locate(ticket);
        res.release();
        if (!res.isUsed()) {
            remove(ticket.getIndex());
            if (res.getDefinition().isRecycleable()) {
                recycler.add(res);
            } else if (res.getResource() != null) {
                res.getDefinition().destroy(res.getResource());
            }
        }
    }
    public void releaseNoRecycle(ResourceTicket ticket) {
        RenderResource res = locate(ticket);
        res.release();
        if (!res.isUsed()) {
            remove(ticket.getIndex());
        } else if (res.getResource() != null) {
            res.getDefinition().destroy(res.getResource());
        }
    }
    public boolean releaseOptional(ResourceTicket ticket) {
        if (ticket != null) {
            release(ticket);
            return true;
        }
        return false;
    }
    public void release(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            release(t);
        }
    }
    public void releaseNoRecycle(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            releaseNoRecycle(t);
        }
    }
    public void releaseOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            releaseOptional(t);
        }
    }
    
    public void watch(ResourceTicket ticket) {
        locate(ticket).setWatched(true);
    }
    public boolean watchOptional(ResourceTicket ticket) {
        if (ticket != null) {
            watch(ticket);
            return true;
        }
        return false;
    }
    public void watch(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            watch(t);
        }
    }
    public void watchOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            watchOptional(t);
        }
    }
    
    public void dereferenceListed(List<ResourceTicket> tickets, List<RenderResource> target) {
        for (ResourceTicket t : tickets) {
            RenderResource r = locate(t);
            r.release();
            if (!r.isReferenced()) {
                target.add(r);
            }
        }
    }
    public void cullUnreferenced() {
        LinkedList<RenderResource> cull = new LinkedList<>();
        for (RenderResource r : resources) {
            if (r != null && !r.isReferenced()) {
                cull.add(r);
            }
        }
        RenderResource resource;
        while ((resource = cull.pollFirst()) != null) {
            // dereference producer of resource
            if (!resource.getProducer().dereference()) {
                // if producer is not referenced, dereference all input resources
                for (ResourceTicket t : resource.getProducer().getInputTickets()) {
                    RenderResource r = locate(t);
                    r.release();
                    if (!r.isReferenced()) {
                        cull.addLast(r);
                    }
                }
                // remove all output resources
                for (ResourceTicket t : resource.getProducer().getOutputTickets()) {
                    remove(t.getIndex());
                }
            }
        }
    }
    
    public void clear() {
        int size = resources.size();
        resources = new ArrayList<>(size);
        nextSlot = 0;
    }
    
}
