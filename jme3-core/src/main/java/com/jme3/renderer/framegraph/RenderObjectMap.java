/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author codex
 */
public class RenderObjectMap {
    
    private final HashMap<Long, RenderObject> objectMap = new HashMap<>();
    private final int timeout = 1;
    
    protected <T> RenderObject<T> create(ResourceDef<T> def) {
        return create(def, def.createResource());
    }
    protected <T> RenderObject<T> create(ResourceDef<T> def, T value) {
        RenderObject obj = new RenderObject(def, value, timeout);
        objectMap.put(obj.getId(), obj);
        return obj;
    }
    protected boolean isAvailable(RenderObject object) {
        return !object.isAcquired() && !object.isConstant();
    }
    protected <T> boolean applyDirectResource(RenderResource<T> resource, RenderObject object) {
        if (!object.isAcquired() && !object.isConstant()) {
            T r = resource.getDefinition().applyDirectResource(object.getObject());
            if (r != null) {
                resource.setObject(object, r);
                return true;
            }
        }
        return false;
    }
    
    public <T> void allocate(RenderResource<T> resource) {
        if (resource.isUndefined()) {
            throw new IllegalArgumentException("Cannot allocate object to an undefined resource.");
        }
        ResourceDef<T> def = resource.getDefinition();
        long id = resource.getTicket().getObjectId();
        // allocate reserved object
        if (id >= 0) {
            RenderObject obj = objectMap.get(id);
            if (obj != null && isAvailable(obj) && 
                    (obj.isReservedAt(resource.getLifeTime().getStartIndex())
                    || !obj.isReservedWithin(resource.getLifeTime()))) {
                // reserved object is only applied if it is accepted by the definition
                T r = def.applyDirectResource(obj.getObject());
                if (r == null) {
                    r = def.applyIndirectResource(obj.getObject());
                }
                if (r != null) {
                    resource.setObject(obj, r);
                    return;
                }
            }
        }
        // find object to allocate
        T indirectRes = null;
        RenderObject indirectObj = null;
        for (RenderObject obj : objectMap.values()) {
            if (isAvailable(obj) && !obj.isReservedWithin(resource.getLifeTime())) {
                T r = def.applyDirectResource(obj.getObject());
                if (r != null) {
                    resource.setObject(obj, r);
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
        } else {
            // create new object
            resource.setObject(create(def));
        }
    }
    public boolean reserve(long objectId, int index) {
        RenderObject obj = objectMap.get(objectId);
        if (obj != null) {
            obj.reserve(index);
            return true;
        }
        return false;
    }
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
    public void dispose(RenderResource resource) {
        long id = resource.getTicket().getObjectId();
        if (id >= 0) {
            RenderObject obj = objectMap.remove(id);
            if (obj != null) {
                obj.dispose();
            }
        }
    }
    
    public void clearReservations() {
        for (RenderObject obj : objectMap.values()) {
            obj.clearReservations();
        }
    }
    public void flushMap() {
        for (Iterator<RenderObject> it = objectMap.values().iterator(); it.hasNext();) {
            RenderObject obj = it.next();
            if (!obj.tickTimeout()) {
                obj.dispose();
                it.remove();
                continue;
            }
            obj.setConstant(false);
        }
    }
    public void clearMap() {
        for (RenderObject obj : objectMap.values()) {
            obj.dispose();
        }
        objectMap.clear();
    }
    
}
