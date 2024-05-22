/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Manages render resource declarations, references, and releases for a framegraph.
 * 
 * @author codex
 */
public class ResourceList {

    private static final int INITIAL_SIZE = 20;
    
    private RenderObjectMap map;
    private ArrayList<RenderResource> resources = new ArrayList<>(INITIAL_SIZE);
    private int nextSlot = 0;
    private int textureBinds = 0;

    public ResourceList() {}
    public ResourceList(RenderObjectMap map) {
        this.map = map;
    }
    
    /**
     * Creates and adds a new render resource.
     * 
     * @param <T>
     * @param producer
     * @param def
     * @return new render resource
     */
    protected <T> RenderResource<T> create(ResourceProducer producer, ResourceDef<T> def) {
        RenderResource res = new RenderResource<>(producer, def, new ResourceTicket<>());
        res.getTicket().setLocalIndex(add(res));
        return res;
    }
    /**
     * Locates the resource associated with the ticket.
     * 
     * @param <T>
     * @param ticket ticket to locate with (not null)
     * @return located resource
     * @throws NullPointerException if ticket is null
     * @throws NullPointerException if ticket's world index is negative
     * @throws NullPointerException if ticket points to a null resource
     * @throws IndexOutOfBoundsException if ticket's world index is &gt;= size
     */
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
    /**
     * Returns true if the ticket can be used to locate a resource.
     * 
     * @param ticket
     * @return 
     */
    protected boolean validate(ResourceTicket ticket) {
        return ticket != null && ticket.getWorldIndex() >= 0;
    }
    /**
     * Adds the resource to the first available slot.
     * 
     * @param res
     * @return 
     */
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
    /**
     * Removes the resource at the index.
     * 
     * @param index
     * @return 
     */
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
     * If the ticket contains a valid object ID, that render object will be reserved
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
    /**
     * Makes reservations for each given ticket.
     * 
     * @param passIndex
     * @param tickets 
     */
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
     * @param passIndex render pass index
     * @param ticket 
     */
    public void reference(int passIndex, ResourceTicket ticket) {
        locate(ticket).reference(passIndex);
    }
    /**
     * References the resource associated with the ticket if the ticket
     * is not null and does not have a negative world index.
     * 
     * @param passIndex render pass index
     * @param ticket
     * @return 
     */
    public boolean referenceOptional(int passIndex, ResourceTicket ticket) {
        if (validate(ticket)) {
            reference(passIndex, ticket);
            return true;
        }
        return false;
    }
    /**
     * References resources associated with the tickets.
     * 
     * @param passIndex render pass index
     * @param tickets 
     */
    public void reference(int passIndex, ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            reference(passIndex, t);
        }
    }
    /**
     * Optionally references resources associated with the tickets.
     * 
     * @param passIndex render pass index
     * @param tickets 
     */
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
    /**
     * Marks the resource associated with the ticket if the ticket is not
     * null and does not have a negative world index.
     * 
     * @param ticket 
     */
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
    
    /**
     * Returns true if the resource associated with the ticket is virtual.
     * <p>
     * A resource is virtual if it does not contain a concrete object and is
     * not marked as undefined.
     * 
     * @param ticket
     * @param optional
     * @return 
     */
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
    /**
     * If the ticket is not null and has a positive or zero world index, an object
     * will be acquired for the resource and returned.
     * <p>
     * Otherwise, the given default value will be returned.
     * 
     * @param <T>
     * @param ticket
     * @param value default value
     * @return 
     */
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
    /**
     * If the ticket is not null and has a positive or zero world index, an object
     * will be extracted by the resource and returned.
     * <p>
     * Otherwise, the given default value will be returned.
     * 
     * @param <T>
     * @param ticket
     * @param value
     * @return 
     */
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
    /**
     * Releases the ticket if the ticket is not null and contains a non-negative
     * world index.
     * 
     * @param ticket
     * @return 
     */
    public boolean releaseOptional(ResourceTicket ticket) {
        if (ticket != null && ticket.getWorldIndex() >= 0) {
            release(ticket);
            return true;
        }
        return false;
    }
    /**
     * Releases the resources obtained by the tickets from use.
     * 
     * @param tickets 
     */
    public void release(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            release(t);
        }
    }
    /**
     * Optionally releases the resources obtained by the tickets from use.
     * 
     * @param tickets 
     */
    public void releaseOptional(ResourceTicket... tickets) {
        for (ResourceTicket t : tickets) {
            releaseOptional(t);
        }
    }
    
    /**
     * Prepares this for rendering.
     * <p>
     * This should only be called once per frame.
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
    
    /**
     * Gets the number of known texture binds that occured during
     * the last render frame.
     * 
     * @return 
     */
    public int getTextureBinds() {
        return textureBinds;
    }
    
    /**
     * Sets the render object map.
     * 
     * @param map 
     */
    public void setObjectMap(RenderObjectMap map) {
        this.map = map;
    }
    
}
