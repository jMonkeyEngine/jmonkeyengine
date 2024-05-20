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
    private int textureBinds = 0;

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
        if (i < 0) {
            throw new NullPointerException(ticket+" does not point to any resource (negative index).");
        }
        if (i < resources.size()) {
            RenderResource<T> res = resources.get(i);
            if (res != null) {
                return res;
            }
            throw new NullPointerException(ticket+" points to null resource.");
        }
        throw new IndexOutOfBoundsException(ticket+" is out of bounds for size "+resources.size());
    }
    protected boolean validate(ResourceTicket ticket) {
        return ticket != null && ticket.getWorldIndex() >= 0;
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
    
    /**
     * Declares a new resource.
     * 
     * @param <T>
     * @param producer
     * @param def
     * @param store
     * @return 
     */
    public <T> ResourceTicket<T> declare(ResourceProducer producer, ResourceDef<T> def, ResourceTicket<T> store) {
        return create(producer, def).getTicket().copyIndexTo(store);
    }
    
    /**
     * If the ticket contains a valid object ID, that object will be reserved
     * at the index.
     * <p>
     * Reserved objects cannot be allocated to another resource before the indexed
     * pass occurs, unless that object is also reserved by another resource.
     * 
     * @param passIndex
     * @param ticket 
     */
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
    
    /**
     * References the resource associated with the ticket.
     * <p>
     * The pass index indicates when the resource will be acquired by the entity
     * which is referencing the resource, which is important for determining resource
     * lifetime.
     * 
     * @param passIndex
     * @param ticket 
     */
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
    
    /**
     * Gets the definition of the resource associated with the ticket.
     * 
     * @param <T>
     * @param <R>
     * @param type
     * @param ticket
     * @return 
     */
    public <T, R extends ResourceDef<T>> R getDefinition(Class<R> type, ResourceTicket<T> ticket) {
        ResourceDef<T> def = locate(ticket).getDefinition();
        if (type.isAssignableFrom(def.getClass())) {
            return (R)def;
        }
        return null;
    }
    
    /**
     * Marks the resource associated with the ticket as undefined.
     * <p>
     * Undefined resources cannot hold objects. If an undefined resource is acquired acquired (unless with
     * {@link #acquireOrElse(com.jme3.renderer.framegraph.ResourceTicket, java.lang.Object) acquireOrElse}),
     * an exception will occur.
     * 
     * @param ticket 
     */
    public void setUndefined(ResourceTicket ticket) {
        locate(ticket).setUndefined();
    }
    
    /**
     * Marks the existing object held be the resource associated with the ticket as constant.
     * <p>
     * Constant objects cannot be reallocated until the end of the frame.
     * 
     * @param ticket 
     */
    public void setConstant(ResourceTicket ticket) {
        RenderObject obj = locate(ticket).getObject();
        if (obj != null) {
            obj.setConstant(true);
        }
    }
    public void setConstantOptional(ResourceTicket ticket) {
        if (validate(ticket)) {
            setConstant(ticket);
        }
    }
    
    /**
     * Sets the resource at the ticket so that it cannot be culled
     * by number of references.
     * 
     * @param ticket 
     */
    public void setSurvivesReferenceCull(ResourceTicket ticket) {
        locate(ticket).setSurvivesRefCull(true);
    }
    
    public boolean isVirtual(ResourceTicket ticket, boolean optional) {
        if (!optional || validate(ticket)) {
            return locate(ticket).isVirtual();
        }
        return true;
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
    /**
     * Acquires and returns the value associated with the resource at the ticket.
     * <p>
     * If the resource is virtual (not holding a object), then either an existing
     * object will be reallocated to the resource or a new object will be created.
     * 
     * @param <T>
     * @param ticket
     * @return 
     */
    public <T> T acquire(ResourceTicket<T> ticket) {
        RenderResource<T> resource = locate(ticket);
        if (resource.isUndefined()) {
            throw new NullPointerException("Cannot acquire undefined resource.");
        }
        return acquire(resource, ticket);
    }
    public <T> T acquireOrElse(ResourceTicket<T> ticket, T value) {
        if (validate(ticket)) {
            RenderResource<T> resource = locate(ticket);
            if (!resource.isUndefined()) {
                return acquire(resource, ticket);
            }
        }
        return value;
    }
    /**
     * Acquires and assigns textures as color targets to the framebuffer.
     * <p>
     * If a texture is already assigned to the framebuffer at the same color target index,
     * then nothing will be changed at that index.
     * <p>
     * Existing texture targets beyond the number of tickets passed will be removed.
     * 
     * @param fbo
     * @param tickets 
     */
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
                textureBinds++;
            }
        }
        for (; i < tickets.length; i++) {
            fbo.addColorTarget(FrameBuffer.FrameBufferTarget.newTarget(acquire(tickets[i])));
            textureBinds++;
        }
    }
    /**
     * Acquires and assigns a texture as the depth target to the framebuffer.
     * <p>
     * If the texture is already assigned to the framebuffer as the depth target,
     * the nothing changes.
     * 
     * @param <T>
     * @param fbo
     * @param ticket 
     * @return  
     */
    public <T extends Texture> T acquireDepthTarget(FrameBuffer fbo, ResourceTicket<T> ticket) {
        T acquired = acquire(ticket);
        FrameBuffer.RenderBuffer target = fbo.getDepthTarget();
        boolean nullTarget = target == null;
        boolean unequalTargets = target != null && acquired != target.getTexture();
        System.out.println("null? "+nullTarget+"   unequal? "+unequalTargets);
        if (nullTarget || unequalTargets) {
            fbo.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(acquired));
            textureBinds++;
        }
        return acquired;
    }
    
    protected <T> T extract(RenderResource<T> resource, ResourceTicket<T> ticket) {
        if (!resource.isUsed()) {
            throw new IllegalStateException(resource+" was unexpectedly extracted.");
        }
        resource.getTicket().copyObjectTo(ticket);
        return map.extract(resource);
    }
    /**
     * Permanently extracts the object from the object manager.
     * <p>
     * Extracted objects are no longer tracked by the object manager,
     * and can therefore not be reallocated for any task.
     * 
     * @param <T>
     * @param ticket
     * @return 
     */
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
    
    /**
     * Releases the resource from use.
     * 
     * @param ticket 
     */
    public void release(ResourceTicket ticket) {
        RenderResource res = locate(ticket);
        res.release();
        if (!res.isUsed()) {
            remove(ticket.getWorldIndex());
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
    
    /**
     * Prepares this for rendering.
     */
    public void beginRenderingSession() {
        textureBinds = 0;
    }
    
    /**
     * Culls all resources and resource producers found to be unused.
     * <p>
     * This should only be called after producers have fully counted their
     * references, and prior to execution.
     */
    public void cullUnreferenced() {
        LinkedList<RenderResource> cull = new LinkedList<>();
        for (RenderResource r : resources) {
            if (r != null && !r.isReferenced() && !r.isSurvivesRefCull()) {
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
                    remove(t.getLocalIndex());
                }
            }
        }
    }
    
    /**
     * Clears the resource list.
     */
    public void clear() {
        // TODO: throw exceptions for unreleased resources.
        int size = resources.size();
        resources = new ArrayList<>(size);
        nextSlot = 0;
    }
    
    public int getTextureBinds() {
        return textureBinds;
    }
    
}
