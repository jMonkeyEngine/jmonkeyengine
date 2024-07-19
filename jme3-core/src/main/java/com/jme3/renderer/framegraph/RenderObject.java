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
import com.jme3.util.NativeObject;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Handles a raw object used for rendering processes within a FrameGraph.
 * 
 * @author codex
 * @param <T>
 */
public class RenderObject <T> {
    
    private static final Consumer<Object> DEFAULT = object -> {};
    private static final Consumer<NativeObject> NATIVE = object -> object.dispose();
    
    private static long nextId = 0;
    
    private final long id;
    private final T object;
    private final LinkedList<Reservation> reservations = new LinkedList<>();
    private int timeoutDuration;
    private int timeout = 0;
    private boolean acquired = false;
    private boolean constant = false;
    private boolean inspect = false;
    private boolean prioritized = false;
    private Consumer disposer;
    
    /**
     * 
     * @param def
     * @param object
     * @param timeout 
     */
    public RenderObject(ResourceDef<T> def, T object, int timeout) {
        this.id = nextId++;
        if (object == null) {
            throw new NullPointerException("Object cannot be null.");
        }
        this.object = object;
        this.timeoutDuration = def.getStaticTimeout();
        if (this.timeoutDuration < 0) {
            this.timeoutDuration = timeout;
        }
        disposer = def.getDisposalMethod();
        if (disposer != null);
        else if (object instanceof NativeObject) {
            this.disposer = NATIVE;
        } else {
            this.disposer = DEFAULT;
        }
    }
    
    /**
     * Starts an inspection of this object.
     * <p>
     * This blocks other threads from inspecting this object at the same time.
     * Often, other threads will save this object for later, and continue inspecting
     * other objects in the meantime.
     * <p>
     * Note that this is not a threadsafe operation, meaning some threads may "escape"
     * this check. A synchronized block should be used to catch these exceptions.
     * 
     * @see #endInspect()
     */
    public void startInspect() {
        inspect = true;
    }
    /**
     * Ends an inspection of this object.
     * 
     * @see #startInspect()
     */
    public void endInspect() {
        inspect = false;
    }
    /**
     * Returns true if this object is currently being inspected.
     * 
     * @return 
     */
    public boolean isInspect() {
        return inspect;
    }
    
    /**
     * Marks this RenderObject as priorized, but not officially claimed, by a thread.
     * <p>
     * This will block threads from attempting to reallocate this as an indirect
     * resource.
     * 
     * @param prioritized 
     */
    public void setPrioritized(boolean prioritized) {
        this.prioritized = prioritized;
    }
    /**
     * Returns true if this RenderObject is prioritized.
     * 
     * @return 
     */
    public boolean isPrioritized() {
        return prioritized;
    }
    
    /**
     * Acquires this render object for use.
     */
    public void acquire() {
        if (acquired) {
            throw new IllegalStateException("Already acquired.");
        }
        timeout = timeoutDuration;
        acquired = true;
    }
    /**
     * Releases this render object from use.
     */
    public void release() {
        if (!acquired) {
            throw new IllegalStateException("Already released.");
        }
        acquired = false;
    }
    /**
     * Reserves this render object for use at the specified render pass index.
     * 
     * @param index 
     */
    public void reserve(PassIndex index) {
        reservations.add(new Reservation(index));
    }
    /**
     * Disposes the internal object.
     */
    public void dispose() {
        // ensure this cannot be acquired
        acquired = true;
        disposer.accept(object);
    }
    
    /**
     * Claims the reservation pertaining to the index.
     * 
     * @param index
     * @return true if a reservation was claimed.
     */
    public boolean claimReservation(PassIndex index) {
        for (Reservation r : reservations) {
            if (r.claim(index)) return true;
        }
        return false;
    }
    /**
     * Determine if reallocating this object with the context would
     * result in a violation of a reservation.
     * 
     * @param frame
     * @return true if this object is reserved within the timeframe
     */
    public boolean isReservedWithin(TimeFrame frame) {
        for (Reservation r : reservations) {
            if (r.violates(frame)) return true;
        }
        return false;
    }
    /**
     * Clears all reservations.
     */
    public void clearReservations() {
        reservations.clear();
    }
    
    /**
     * Decrements the integer tracking frames until the object is deemed
     * abandoned.
     * <p>
     * Abandoned objects are removed and disposed.
     * 
     * @return true if the object is not considered abandoned
     */
    public boolean tickTimeout() {
        return timeout-- > 0;
    }
    
    /**
     * Sets this as constant, which blocks reallocations until the rendering ends.
     * 
     * @param constant 
     */
    public void setConstant(boolean constant) {
        this.constant = constant;
    }
    
    /**
     * Gets the id of this render object.
     * 
     * @return 
     */
    public long getId() {
        return id;
    }
    /**
     * Gets the internal object.
     * 
     * @return 
     */
    public T getObject() {
        return object;
    }
    /**
     * Returns true if this render object is acquired (and not yet released).
     * 
     * @return 
     */
    public boolean isAcquired() {
        return acquired;
    }
    /**
     * Returns true if this render object is constant.
     * 
     * @return 
     */
    public boolean isConstant() {
        return constant;
    }
    
    /**
     * Gets the next unique id of RenderObjects.
     * 
     * @return 
     */
    public static long getNextId() {
        return nextId;
    }
    
    private static class Reservation {
        
        private final PassIndex index;
        private boolean claimed = false;
        
        public Reservation(PassIndex index) {
            this.index = index;
        }
        
        public boolean claim(PassIndex index) {
            if (this.index.equals(index)) {
                claimed = true;
                return true;
            }
            return false;
        }
        public boolean violates(TimeFrame frame) {
            return !claimed && (frame.isAsync() || frame.getThreadIndex() != index.getThreadIndex());
        }
        
    }
    
}
