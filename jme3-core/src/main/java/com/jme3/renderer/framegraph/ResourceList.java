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
        System.out.println("    "+producer+": register "+t);
        return store;
    }
    
    public void reference(ResourceTicket ticket) {
        locate(ticket).reference();
        System.out.println("    "+locate(ticket).getProducer()+": reference "+ticket);
    }
    public boolean referenceOptional(ResourceTicket ticket) {
        if (ticket != null) {
            reference(ticket);
            return true;
        }
        System.out.println("    optional reference failed");
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
    
    public <T> T acquire(ResourceTicket<T> ticket) {
        RenderResource<T> res = locate(ticket);
        if (!res.isUsed()) {
            throw new IllegalStateException(res+" was unexpectedly acquired.");
        }
        System.out.println("    acquire resource from "+ticket);
        if (!res.isVirtual()) {
            System.out.println("      resource already created");
        }
        if (res.isVirtual() && !recycler.recycle(res)) {
            System.out.println("      create from scratch for "+res);
            res.create();
        }
        return res.getResource();
    }
    public <T> T acquire(ResourceTicket<T> ticket, T value) {
        if (ticket != null) {
            return acquire(ticket);
        } else {
            return value;
        }
    }
    public void acquireColorTargets(FrameBuffer fbo, ResourceTicket<? extends Texture>... tickets) {
        for (ResourceTicket<? extends Texture> t : tickets) {
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(acquire(t)));
        }
    }
    
    public void release(ResourceTicket ticket) {
        if (ticket != null) {
            RenderResource res = locate(ticket);
            res.release();
            if (!res.isUsed()) {
                System.out.println("    resource is not used: "+res);
                remove(ticket.getIndex());
                if (res.getDefinition().isRecycleable()) {
                    recycler.add(res);
                }
            }
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
        int n = 0;
        int size = resources.size();
        for (RenderResource r : resources) {
            if (r != null) {
                n++;
                if (r.isUsed()) {
                    System.out.println("Warning: "+r+" still retains "+(r.getNumReferences()+1)+" references");
                }
            }
        }
        System.out.println("peak concurrent resources: "+size);
        System.out.println("unculled resources: "+n);
        resources = new ArrayList<>(size);
        nextSlot = 0;
    }
    
}
