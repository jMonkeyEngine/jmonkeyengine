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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages creation, reallocation, and disposal of {@link RenderObject}s.
 * 
 * @author codex
 */
public class RenderObjectMap {
    
    private final FGPipelineContext context;
    private final Map<Long, RenderObject> objectMap;
    private int staticTimeout = 1;
    
    // statistics
    private int totalAllocations = 0;
    private int officialReservations = 0;
    private int completedReservations = 0;
    private int failedReservations = 0;
    private int objectsCreated = 0;
    private int objectsReallocated = 0;
    private int totalObjects = 0;
    private int flushedObjects = 0;
    
    /**
     * 
     * @param context
     * @param async 
     */
    public RenderObjectMap(FGPipelineContext context, boolean async) {
        this.context = context;
        objectMap = new ConcurrentHashMap<>();
    }
    
    private <T> RenderObject<T> create(ResourceDef<T> def) {
        return create(def, def.createResource());
    }
    private <T> RenderObject<T> create(ResourceDef<T> def, T value) {
        RenderObject obj = new RenderObject(def, value, staticTimeout);
        objectMap.put(obj.getId(), obj);
        return obj;
    }
    private boolean isAvailable(RenderObject object) {
        return !object.isAcquired() && !object.isConstant();
    }
    
    /**
     * Allocates a render object to the ResourceView.
     * <p>
     * First, if this resource holds an object id, then corresponding render object,
     * if it still exists, will be tried for reallocation. If that fails, each render object
     * will be tried for reallocation. Finally, if that fails, a new render object
     * will be created and allocated to the resource.
     * 
     * @param <T>
     * @param resource 
     * @param async true to execute asynchronous methods, otherwise synchronous methods will
     * be used in the interest of efficiency
     */
    public <T> void allocate(ResourceView<T> resource, boolean async) {
        if (async) {
            allocateAsync(resource);
        } else {
            allocateSync(resource);
        }
    }
    
    private <T> void allocateSync(ResourceView<T> resource) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        GraphEventCapture cap = context.getEventCapture();
        totalAllocations++;
        ResourceDef<T> def = resource.getDefinition();
        if (def.isUseExisting()) {
            // first try allocating a specific object, which is much faster
            if (allocateSpecificSync(resource)) {
                return;
            }
            // find object to allocate
            T indirectRes = null;
            RenderObject indirectObj = null;
            for (RenderObject obj : objectMap.values()) {
                if (isAvailable(obj) && !obj.isReservedWithin(resource.getLifeTime())) {
                    // try applying a direct resource
                    T r = def.applyDirectResource(obj.getObject());
                    if (r != null) {
                        resource.setObject(obj, r);
                        if (cap != null) cap.reallocateObject(obj.getId(), resource.getIndex(),
                                resource.getResource().getClass().getSimpleName());
                        objectsReallocated++;
                        return;
                    }
                    // then try applying an indirect resource, which is not as desirable
                    if (indirectObj == null) {
                        indirectRes = def.applyIndirectResource(obj.getObject());
                        if (indirectRes != null) {
                            indirectObj = obj;
                        }
                    }
                }
            }
            // allocate indirect object
            if (indirectObj != null) {
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
    private <T> boolean allocateSpecificSync(ResourceView<T> resource) {
        GraphEventCapture cap = context.getEventCapture();
        ResourceDef<T> def = resource.getDefinition();
        long id = resource.getTicket().getObjectId();
        if (id < 0) return false;
        // allocate reserved object
        RenderObject obj = objectMap.get(id);        
        if (obj != null) {
            if (cap != null) cap.attemptReallocation(id, resource.getIndex());
            if (isAvailable(obj) && (obj.claimReservation(resource.getProducer().getIndex())
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
        }
        failedReservations++;
        return false;
    }
    private <T> void allocateAsync(ResourceView<T> resource) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        GraphEventCapture cap = context.getEventCapture();
        totalAllocations++;
        ResourceDef<T> def = resource.getDefinition();
        if (def.isUseExisting()) {
            // first try allocating a specific object, which is much faster
            if (allocateSpecificAsync(resource)) {
                return;
            }
            // find object to allocate
            T indirectRes = null;
            RenderObject indirectObj = null;
            LinkedList<RenderObject> skipped = new LinkedList<>();
            Iterator<RenderObject> it = objectMap.values().iterator();
            boolean next;
            while ((next = it.hasNext()) || !skipped.isEmpty()) {
                RenderObject obj;
                if (next) obj = it.next();
                else obj = skipped.removeFirst();
                if (isAvailable(obj)) {
                    if ((next || !skipped.isEmpty()) && obj.isInspect()) {
                        // Inspect this object later, because something else is inspecting it.
                        // This makes this thread try other objects first, instead of waiting
                        // for a synchronized block to be available.
                        skipped.addLast(obj);
                        continue;
                    }
                    // If multiple threads do happen to be here at the same time, ensure only one
                    // will inspect at a time.
                    synchronized (obj) {
                        // The thread we were waiting on may have claimed to object, so check again
                        // if it is available.
                        if (!isAvailable(obj)) {
                            continue;
                        }
                        obj.startInspect();
                        if (!obj.isReservedWithin(resource.getLifeTime())) {
                            // try applying a direct resource
                            T r = def.applyDirectResource(obj.getObject());
                            if (r != null) {
                                resource.setObject(obj, r);
                                if (cap != null) cap.reallocateObject(obj.getId(), resource.getIndex(),
                                        resource.getResource().getClass().getSimpleName());
                                objectsReallocated++;
                                obj.endInspect();
                                return;
                            }
                            // then try applying an indirect resource, which is not as desirable
                            if (!obj.isPrioritized() && indirectObj == null) {
                                indirectRes = def.applyIndirectResource(obj.getObject());
                                if (indirectRes != null) {
                                    indirectObj = obj;
                                    // make sure no other thread attempts to apply this indirectly at the same time
                                    obj.setPrioritized(true);
                                    obj.endInspect();
                                    continue;
                                }
                            }
                        }
                        obj.endInspect();
                    }
                }
            }
            // allocate indirect object
            if (indirectObj != null) synchronized (indirectObj) {
                // disable priority flag
                indirectObj.setPrioritized(false);
                // check again if object is available
                if (isAvailable(indirectObj)) {
                    indirectObj.startInspect();
                    resource.setObject(indirectObj, indirectRes);
                    if (cap != null) cap.reallocateObject(indirectObj.getId(), resource.getIndex(),
                            resource.getResource().getClass().getSimpleName());
                    objectsReallocated++;
                    indirectObj.endInspect();
                } else {
                    // In the unlikely event that another thread "steals" this object
                    // from this thread, try allocating again.
                    allocateAsync(resource);
                }
                return;
            }
        }
        // create new object
        resource.setObject(create(def));
        if (cap != null) cap.createObject(resource.getObject().getId(),
                resource.getIndex(), resource.getResource().getClass().getSimpleName());
        objectsCreated++;
    }
    private <T> boolean allocateSpecificAsync(ResourceView<T> resource) {
        GraphEventCapture cap = context.getEventCapture();
        ResourceDef<T> def = resource.getDefinition();
        long id = resource.getTicket().getObjectId();
        if (id < 0) return false;
        // allocate reserved object
        RenderObject obj = objectMap.get(id);        
        if (obj != null) {
            if (cap != null) cap.attemptReallocation(id, resource.getIndex());
            if (isAvailable(obj)) synchronized (obj) {
                obj.startInspect();
                if (obj.claimReservation(resource.getProducer().getIndex())
                        || !obj.isReservedWithin(resource.getLifeTime())) {
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
                        obj.endInspect();
                        return true;
                    }
                }
                obj.endInspect();
            }
        }
        failedReservations++;
        return false;
    }
    
    /**
     * Makes a reservation of render object holding the specified id at the render
     * pass index.
     * <p>
     * A reservation blocks other reallocation requests for the remainder of the frame.
     * It is not strictly guaranteed to block all other requests, so it is not considered
     * good practice to rely on a reservation blocking all such requests.
     * 
     * @param objectId id of the object to reserve
     * @param index index to reserve the object at
     * @return true if the referenced object exists
     */
    public boolean reserve(long objectId, PassIndex index) {
        RenderObject obj = objectMap.get(objectId);
        if (obj != null) {
            obj.reserve(index);
            officialReservations++;
            if (context.getEventCapture() != null) {
                context.getEventCapture().reserveObject(objectId, index);
            }
            return true;
        }
        return false;
    }
    /**
     * Disposes the render object pointed to by the ResourceView's internal ticket.
     * 
     * @param resource 
     */
    public void dispose(ResourceView resource) {
        long id = resource.getTicket().getObjectId();
        if (id >= 0) {
            RenderObject obj = objectMap.remove(id);
            if (obj != null) {
                obj.dispose();
                if (context.getEventCapture() != null) {
                    context.getEventCapture().disposeObject(id);
                }
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
        GraphEventCapture cap = context.getEventCapture();
        if (cap != null) cap.flushObjects(totalObjects);
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
        GraphEventCapture cap = context.getEventCapture();
        for (RenderObject obj : objectMap.values()) {
            if (cap != null) cap.disposeObject(obj.getId());
            obj.dispose();
        }
        objectMap.clear();
    }
    
    /**
     * Sets the default number of frame boundaries an object can experience without
     * being used before being disposed.
     * <p>
     * default=1 (can survive one frame boundary)
     * 
     * @param staticTimeout 
     */
    public void setStaticTimeout(int staticTimeout) {
        this.staticTimeout = staticTimeout;
    }
    
    /**
     * Gets the default number of frame boundaries an object can experience without
     * being used before being disposed.
     * 
     * @return 
     */
    public int getStaticTimeout() {
        return staticTimeout;
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
