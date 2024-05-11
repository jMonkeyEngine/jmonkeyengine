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
    
    private final HashMap<Integer, RenderObject> objectMap = new HashMap<>();
    private final int timeout = 1;
    
    protected <T> RenderObject<T> create(ResourceDef<T> def) {
        return create(def, def.createResource());
    }
    protected <T> RenderObject<T> create(ResourceDef<T> def, T value) {
        RenderObject obj = new RenderObject(def, value, timeout);
        objectMap.put(obj.getId(), obj);
        return obj;
    }
    protected <T> boolean applyToResource(RenderResource<T> resource, RenderObject object) {
        if (!object.isAcquired() && !object.isConstant()) {
            T r = resource.getDefinition().applyResource(object.getObject());
            if (r != null) {
                object.acquire();
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
        int id = resource.getTicket().getObjectId();
        if (id >= 0) {
            RenderObject obj = objectMap.get(id);
            if (obj != null && applyToResource(resource, obj)) {
                return;
            }
        }
        for (RenderObject obj : objectMap.values()) {
            if (!obj.isReserved(resource.getLifeTime()) && applyToResource(resource, obj)) {
                return;
            }
        }
        RenderObject<T> obj = create(def);
        obj.acquire();
        resource.setObject(obj, obj.getObject());
    }
    public boolean reserve(int objectId, int index) {
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
        int id = resource.getTicket().getObjectId();
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
