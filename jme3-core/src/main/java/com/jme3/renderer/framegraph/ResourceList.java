/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author codex
 */
public class ResourceList {
    
    private static final int INITIAL_SIZE = 20;
    
    private RenderObjectMap map;
    private ArrayList<RenderResource> resources = new ArrayList<>(INITIAL_SIZE);
    private int nextSlot = 0;

    public ResourceList(RenderObjectMap recycler) {
        this.map = recycler;
    }
    
    protected <T> RenderResource<T> create(ResourceProducer producer, ResourceDef<T> def) {
        RenderResource res = new RenderResource<>(producer, def, new ResourceTicket<>());
        res.getTicket().setIndex(add(res));
        return res;
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
    
    public <T> ResourceTicket<T> register(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> store) {
        return create(producer, def).getTicket().copyIndexTo(store);
    }
    
    public void reserve(int passIndex, ResourceTicket ticket) {
        if (ticket.getObjectId() >= 0) {
            map.reserve(ticket.getObjectId(), passIndex);
            locate(ticket).getTicket().setObjectId(ticket.getObjectId());
        }
    }
    public void reserve(int passIndex, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reserve(passIndex, t);
        }
    }
    
    public void reference(int passIndex, ResourceTicket ticket) {
        locate(ticket).reference(passIndex);
    }
    public boolean referenceOptional(int passIndex, ResourceTicket ticket) {
        if (ticket != null) {
            reference(passIndex, ticket);
            return true;
        }
        return false;
    }
    public void reference(int passIndex, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reference(passIndex, t);
        }
    }
    public void referenceOptional(int passIndex, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            referenceOptional(passIndex, t);
        }
    }
    
    public <T, R extends ResourceDef<T>> R getDefinition(Class<R> type, ResourceTicket<T> ticket) {
        ResourceDef<T> def = locate(ticket).getDefinition();
        if (type.isAssignableFrom(def.getClass())) {
            return (R)def;
        }
        return null;
    }
    
    public <T> T acquire(ResourceTicket<T> ticket) {
        RenderResource<T> resource = locate(ticket);
        if (!resource.isUsed()) {
            throw new IllegalStateException(resource+" was unexpectedly acquired.");
        }
        if (resource.isVirtual()) {
            map.allocate(resource);
        }
        ticket.setObjectId(resource.getObject().getId());
        return resource.getResource();
    }
    public <T> T acquireOrElse(ResourceTicket<T> ticket, T value) {
        if (ticket != null) {
            return acquire(ticket);
        }
        return value;
    }
    
    public void acquireColorTargets(FrameBuffer fbo, ResourceTicket<? extends Texture>... tickets) {
        if (tickets.length == 0) {
            fbo.clearColorTargets();
            return;
        }
        if (tickets.length < fbo.getNumColorTargets()) {
            fbo.removeColorTargetsAbove(tickets.length-1);
        }
        int i = 0;
        for (int n = Math.min(fbo.getNumColorTargets(), tickets.length); i < n; i++) {
            Texture existing = fbo.getColorTarget(i).getTexture();
            Texture acquired = acquire((ResourceTicket<Texture>)tickets[i]);
            if (acquired != existing) {
                fbo.setColorTarget(i, FrameBuffer.FrameBufferTarget.newTarget(acquired));
            }
        }
        for (; i < tickets.length; i++) {
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(acquire(tickets[i])));
        }
    }
    public void acquireDepthTarget(FrameBuffer fbo, ResourceTicket<? extends Texture> ticket) {
        Texture acquired = acquire((ResourceTicket<Texture>)ticket);
        if (fbo.getDepthTarget() != null && acquired == fbo.getDepthTarget().getTexture()) {
            return;
        }
        fbo.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(acquired));
    }
    
    public void release(ResourceTicket ticket) {
        RenderResource res = locate(ticket);
        res.release();
        if (!res.isUsed()) {
            remove(ticket.getIndex());
            res.getObject().release();
            res.setObject(null);
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
            ResourceProducer producer = resource.getProducer();
            if (producer != null) {
                if (!producer.dereference()) {
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
            } else {
                remove(resource.getIndex());
            }
        }
    }
    public void clear() {
        // TODO: throw exceptions for unreleased resources.
        int size = resources.size();
        resources = new ArrayList<>(size);
        nextSlot = 0;
    }
    
}
