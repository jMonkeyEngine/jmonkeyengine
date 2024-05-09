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
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class ResourceList {

    private static final int INITIAL_SIZE = 20;
    
    private final RenderObjectMap map;
    private ArrayList<RenderResource> resources = new ArrayList<>(INITIAL_SIZE);
    private int nextSlot = 0;

    public ResourceList(RenderObjectMap map) {
        this.map = map;
    }
    
    protected <T> RenderResource<T> create(ResourceProducer producer, ResourceDef<T> def) {
        RenderResource res = new RenderResource<>(producer, def, new ResourceTicket<>());
        res.getTicket().setLocalIndex(add(res));
        return res;
    }
    protected <T> RenderResource<T> locate(ResourceTicket<T> ticket) {
        if (ticket == null) {
            throw new NullPointerException("Ticket cannot be null.");
        }
        final int i = ticket.getWorldIndex();
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
    
    public <T> ResourceTicket<T> declare(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> store) {
        return create(producer, def).getTicket().copyIndexTo(store);
    }
    
    public void reserve(int passIndex, ResourceTicket ticket) {
        if (ticket.getObjectId() >= 0) {
            map.reserve(ticket.getObjectId(), passIndex);
            ticket.copyObjectTo(locate(ticket).getTicket());
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
        if (ticket != null && ticket.getWorldIndex() >= 0) {
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
    public void markUndefined(ResourceTicket ticket) {
        locate(ticket).setUndefined();
    }
    
    protected <T> T acquire(RenderResource<T> resource, ResourceTicket<T> ticket) {
        if (!resource.isUsed()) {
            throw new IllegalStateException(resource+" was unexpectedly acquired.");
        }
        if (resource.isVirtual()) {
            map.allocate(resource);
        }
        resource.getTicket().copyObjectTo(ticket);
        return resource.getResource();
    }
    public <T> T acquire(ResourceTicket<T> ticket) {
        RenderResource<T> resource = locate(ticket);
        if (resource.isUndefined()) {
            throw new NullPointerException("Resource is undefined.");
        }
        return acquire(resource, ticket);
    }
    public <T> T acquireOrElse(ResourceTicket<T> ticket, T value) {
        if (ticket != null && ticket.getWorldIndex() >= 0) {
            RenderResource<T> resource = locate(ticket);
            if (!resource.isUndefined()) {
                return acquire(resource, ticket);
            }
        }
        return value;
    }
    public void acquireColorTargets(FrameBuffer fbo, ResourceTicket<? extends Texture>... tickets) {
        if (tickets.length == 0) {
            fbo.clearColorTargets();
            return;
        }
        if (tickets.length < fbo.getNumColorTargets()) {
            fbo.trimColorTargetsTo(tickets.length-1);
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
    
    protected <T> T extract(RenderResource<T> resource, ResourceTicket<T> ticket) {
        if (!resource.isUsed()) {
            throw new IllegalStateException(resource+" was unexpectedly extracted.");
        }
        resource.getTicket().copyObjectTo(ticket);
        return map.extract(resource);
    }
    public <T> T extract(ResourceTicket<T> ticket) {
        RenderResource<T> resource = locate(ticket);
        T object = extract(resource, ticket);
        if (object == null) {
            throw new NullPointerException("Failed to extract resource.");
        }
        return object;
    }
    public <T> T extractOrElse(ResourceTicket<T> ticket, T value) {
        if (ticket != null && ticket.getWorldIndex() >= 0) {
            T object = extract(locate(ticket), ticket);
            if (object != null) return object;
        }
        return value;
    }
    
    public void release(ResourceTicket ticket) {
        RenderResource res = locate(ticket);
        res.release();
        if (!res.isUsed()) {
            remove(ticket.getWorldIndex());
            res.getObject().release();
            res.setObject(null);
            if (res.getDefinition().isDisposeOnRelease()) {
                map.dispose(res);
            }
        }
    }
    public boolean releaseOptional(ResourceTicket ticket) {
        if (ticket != null && ticket.getWorldIndex() >= 0) {
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
            if (producer == null) {
                remove(resource.getIndex());
                continue;
            }
            if (!producer.dereference()) {
                // if producer is not referenced, dereference all input resources
                for (ResourceTicket t : producer.getInputTickets()) {
                    if (t.getWorldIndex() < 0) {
                        continue;
                    }
                    RenderResource r = locate(t);
                    r.release();
                    if (!r.isReferenced()) {
                        cull.addLast(r);
                    }
                }
                // remove all output resources
                for (ResourceTicket t : producer.getOutputTickets()) {
                    remove(t.getWorldIndex());
                }
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
