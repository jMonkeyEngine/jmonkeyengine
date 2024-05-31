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

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.debug.GraphEventCapture;
import com.jme3.renderer.framegraph.definitions.ResourceDef;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Manages creation, reallocation, and disposal of {@link RenderObject}s.
 * 
 * @author codex
 */
public class RenderObjectMap {
    
    private final RenderManager renderManager;
    private final HashMap<Long, RenderObject> objectMap = new HashMap<>();
    private final int timeout = 2;
    private int totalAllocations = 0;
    private int officialReservations = 0;
    private int completedReservations = 0;
    private int failedReservations = 0;
    private int objectsCreated = 0;
    private int objectsReallocated = 0;
    private int totalObjects = 0;
    private int flushedObjects = 0;
    
    public RenderObjectMap(RenderManager renderManager) {
        this.renderManager = renderManager;
    }
    
    /**
     * Creates a new render object with a new internal object.
     * 
     * @param <T>
     * @param def
     * @return 
     */
    protected <T> RenderObject<T> create(ResourceDef<T> def) {
        return create(def, def.createResource());
    }
    /**
     * Creates a new render object with the given internal object.
     * 
     * @param <T>
     * @param def
     * @param value internal object
     * @return 
     */
    protected <T> RenderObject<T> create(ResourceDef<T> def, T value) {
        RenderObject obj = new RenderObject(def, value, timeout);
        objectMap.put(obj.getId(), obj);
        return obj;
    }
    /**
     * Returns true if the render object is available for reallocation.
     * 
     * @param object
     * @return 
     */
    protected boolean isAvailable(RenderObject object) {
        return !object.isAcquired() && !object.isConstant();
    }
    
    /**
     * Allocates a render object to the resource.
     * <p>
     * First, if this resource holds an object id, the corresponding render object,
     * if it still exists, will be tried for reallocation. Then, each render object
     * will be tried for reallocation. Finally, if all else fails, a new render object
     * will be created and allocated to the resource.
     * 
     * @param <T>
     * @param resource 
     */
    public <T> void allocate(RenderResource<T> resource) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        GraphEventCapture cap = renderManager.getGraphCapture();
        totalAllocations++;
        ResourceDef<T> def = resource.getDefinition();
        if (def.isUseExisting()) {
            if (allocateSpecific(resource)) {
                return;
            }
            // find object to allocate
            T indirectRes = null;
            RenderObject indirectObj = null;
            for (RenderObject obj : objectMap.values()) {
                if (isAvailable(obj) && !obj.isReservedWithin(resource.getLifeTime())) {
                    T r = def.applyDirectResource(obj.getObject());
                    if (r != null) {
                        resource.setObject(obj, r);
                        if (cap != null) cap.reallocateObject(obj.getId(), resource.getIndex(),
                                resource.getResource().getClass().getSimpleName());
                        objectsReallocated++;
                        return;
                    }
                    if (indirectObj == null) {
                        indirectRes = def.applyIndirectResource(obj.getObject());
                        if (indirectRes != null) {
                            indirectObj = obj;
                        }
                    }
                }
            }
            if (indirectObj != null) {
                // allocate indirect object
                resource.setObject(indirectObj, indirectRes);
                if (cap != null) cap.reallocateObject(indirectObj.getId(), resource.getIndex(),
                        resource.getResource().getClass().getSimpleName());
                objectsReallocated++;
                return;
            }
        }
        // create new object
        resource.setObject(create(def));
        if (cap != null) cap.createObject(resource.getObject().getId(),
                resource.getIndex(), resource.getResource().getClass().getSimpleName());
        objectsCreated++;
    }
    /**
     * Allocates the specific render object directly referenced by the resource,
     * if one is referenced.
     * 
     * @param <T>
     * @param resource 
     * @return true if the specific render object was allocated
     */
    public <T> boolean allocateSpecific(RenderResource<T> resource) {
        GraphEventCapture cap = renderManager.getGraphCapture();
        ResourceDef<T> def = resource.getDefinition();
        long id = resource.getTicket().getObjectId();
        if (id < 0) return false;
        // allocate reserved object
        RenderObject obj = objectMap.get(id);
        if (cap != null) cap.attemptReallocation(id, resource.getIndex());
        if (obj != null && isAvailable(obj)
                && (obj.isReservedAt(resource.getLifeTime().getStartIndex())
                || !obj.isReservedWithin(resource.getLifeTime()))) {
            // reserved object is only applied if it is accepted by the definition
            T r = def.applyDirectResource(obj.getObject());
            if (r == null) {
                r = def.applyIndirectResource(obj.getObject());
            }
            if (r != null) {
                resource.setObject(obj, r);
                if (cap != null) cap.reallocateObject(id, resource.getIndex(),
                        resource.getResource().getClass().getSimpleName());
                completedReservations++;
                objectsReallocated++;
                return true;
            }
        }
        failedReservations++;
        if (cap != null) cap.allocateSpecificFailed(obj, resource);
        return false;
    }
    /**
     * Directly creates a new render object containing the value for the render
     * resource.
     * <p>
     * The object is still subject to the resource's definition, although the definition
     * may not have created the internal value.
     * 
     * @param <T>
     * @param resource
     * @param value 
     */
    public <T> void setDirect(RenderResource<T> resource, T value) {
        RenderObject<T> object = create(resource.getDefinition(), value);
        resource.setObject(object, value);
        if (renderManager.getGraphCapture() != null) {
            renderManager.getGraphCapture().setObjectDirect(
                    object.getId(), resource.getIndex(), value.getClass().getSimpleName());
        }
    }
    /**
     * Makes a reservation of render object holding the specified id at the render
     * pass index so that no other resource may (without a reservation) use that 
     * render object at that time.
     * 
     * @param objectId
     * @param index
     * @return 
     */
    public boolean reserve(long objectId, int index) {
        RenderObject obj = objectMap.get(objectId);
        if (obj != null) {
            obj.reserve(index);
            officialReservations++;
            if (renderManager.getGraphCapture() != null) {
                renderManager.getGraphCapture().reserveObject(objectId, index);
            }
            return true;
        }
        return false;
    }
    /**
     * Untracks the render object held by the resource.
     * <p>
     * If the resource is virtual, a new resource will be allocated then
     * immediately untracked.
     * 
     * @param <T>
     * @param resource
     * @return 
     */
    public <T> T extract(RenderResource<T> resource) {
        if (resource.isUndefined()) {
            return null;
        }
        if (resource.isVirtual()) {
            allocate(resource);
        }
        RenderObject<T> obj = objectMap.remove(resource.getTicket().getObjectId());
        return (obj != null ? obj.getObject() : null);
    }
    /**
     * Disposes the render object pointed to by the resource.
     * 
     * @param resource 
     */
    public void dispose(RenderResource resource) {
        long id = resource.getTicket().getObjectId();
        if (id >= 0) {
            RenderObject obj = objectMap.remove(id);
            if (obj != null) {
                if (renderManager.getGraphCapture() != null) {
                    renderManager.getGraphCapture().disposeObject(id);
                }
                obj.dispose();
            }
        }
    }
    
    /**
     * Should be called only when a new rendering frame begins (before rendering).
     */
    public void newFrame() {
        totalAllocations = 0;
        officialReservations = 0;
        completedReservations = 0;
        failedReservations = 0;
        objectsCreated = 0;
        objectsReallocated = 0;
        flushedObjects = 0;
    }
    /**
     * Clears reservations of all tracked render objects.
     */
    public void clearReservations() {
        for (RenderObject obj : objectMap.values()) {
            obj.clearReservations();
        }
    }
    /**
     * Flushes the map.
     * <p>
     * Any render objects that have not been used for a number of frames are disposed.
     */
    public void flushMap() {
        totalObjects = objectMap.size();
        if (renderManager.getGraphCapture() != null) {
            renderManager.getGraphCapture().flushObjects(totalObjects);
        }
        GraphEventCapture cap = renderManager.getGraphCapture();
        for (Iterator<RenderObject> it = objectMap.values().iterator(); it.hasNext();) {
            RenderObject obj = it.next();
            if (!obj.tickTimeout()) {
                if (cap != null) cap.disposeObject(obj.getId());
                obj.dispose();
                it.remove();
                flushedObjects++;
                continue;
            }
            obj.setConstant(false);
        }
        if (cap != null) {
            cap.value("totalAllocations", totalAllocations);
            cap.value("officialReservations", officialReservations);
            cap.value("completedReservations", completedReservations);
            cap.value("failedReservations", failedReservations);
            cap.value("objectsCreated", objectsCreated);
            cap.value("objectsReallocated", objectsReallocated);
            cap.value("flushedObjects", flushedObjects);
        }
    }
    /**
     * Clears the map.
     * <p>
     * All tracked render objects are disposed.
     */
    public void clearMap() {
        GraphEventCapture cap = renderManager.getGraphCapture();
        for (RenderObject obj : objectMap.values()) {
            if (cap != null) cap.disposeObject(obj.getId());
            obj.dispose();
        }
        objectMap.clear();
    }

    /**
     * Get the total number of allocations that occured during the last render frame.
     * 
     * @return 
     */
    public int getTotalAllocations() {
        return totalAllocations;
    }
    /**
     * Gets the number of official reservations that occured during the last
     * render frame.
     * <p>
     * An official reservation is one made using {@link #reserve(long, int)}.
     * 
     * @return 
     */
    public int getOfficialReservations() {
        return officialReservations;
    }
    /**
     * Gets the number of completed reservations that occured during the
     * last render frame.
     * <p>
     * A completed reservation is declared and allocated.
     * 
     * @return 
     */
    public int getCompletedReservations() {
        return completedReservations;
    }
    /**
     * Gets the number of incomplete or failed reservations that occured
     * during the last render frame.
     * 
     * @return 
     */
    public int getFailedReservations() {
        return failedReservations;
    }
    /**
     * Gets the number of render objects created during the last render frame.
     * 
     * @return 
     */
    public int getObjectsCreated() {
        return objectsCreated;
    }
    /**
     * Gets the number of reallocations that occured during the last render frame.
     * 
     * @return 
     */
    public int getObjectsReallocated() {
        return objectsReallocated;
    }
    /**
     * Gets the number of render objects present before flushing.
     * 
     * @return 
     */
    public int getTotalObjects() {
        return totalObjects;
    }
    /**
     * Gets the number of render objects disposed during flushing.
     * 
     * @return 
     */
    public int getFlushedObjects() {
        return flushedObjects;
    }
    
}
