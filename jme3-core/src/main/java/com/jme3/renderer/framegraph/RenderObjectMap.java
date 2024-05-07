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
        RenderObject obj = new RenderObject(def.createResource(), timeout, def.getDisposalMethod());
        objectMap.put(obj.getId(), obj);
        return obj;
    }
    protected <T> boolean applyToResource(RenderResource<T> resource, RenderObject object) {
        T r = resource.getDefinition().applyResource(object.getObject());
        if (r != null) {
            object.acquire();
            resource.setObject(object);
            return true;
        }
        return false;
    }
    
    public <T> void allocate(RenderResource<T> resource) {
        ResourceDef<T> def = resource.getDefinition();
        int id = resource.getTicket().getObjectId();
        if (id >= 0) {
            RenderObject reserved = objectMap.get(id);
            if (reserved != null && !reserved.isAcquired() && applyToResource(resource, reserved)) {
                return;
            }
        }
        for (RenderObject obj : objectMap.values()) {
            if (!obj.isAcquired() && !obj.isReservedWithin(resource.getLifeTime())
                    && applyToResource(resource, obj)) {
                return;
            }
        }
        RenderObject<T> obj = create(def);
        obj.acquire();
        resource.setObject(obj);
    }
    public boolean reserve(int objectId, int index) {
        RenderObject obj = objectMap.get(objectId);
        if (obj != null) {
            obj.reserve(index);
            return true;
        }
        return false;
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
            }
        }
    }
    public void clearMap() {
        for (RenderObject obj : objectMap.values()) {
            obj.dispose();
        }
        objectMap.clear();
    }
    
}
