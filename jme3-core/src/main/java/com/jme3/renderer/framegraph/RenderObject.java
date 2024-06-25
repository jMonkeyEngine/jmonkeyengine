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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * Stores an object used for rendering.
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
    private final LinkedList<PassIndex> reservations = new LinkedList<>();
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
    
    public void startInspect() {
        inspect = true;
    }
    public void endInspect() {
        inspect = false;
    }
    public boolean isInspect() {
        return inspect;
    }
    
    public void setPrioritized(boolean prioritized) {
        this.prioritized = prioritized;
    }
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
        reservations.add(index);
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
     * Returns true if
     * 
     * @param index
     * @return 
     */
    public boolean claimReservation(PassIndex index) {
        for (Iterator<PassIndex> it = reservations.iterator(); it.hasNext();) {
            if (it.next().equals(index)) {
                it.remove();
                return true;
            }
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
     * Ticks down the timer tracking frames since last use.
     * 
     * @return true if the timer has not expired
     */
    public boolean tickTimeout() {
        return timeout-- > 0;
    }
    
    /**
     * Sets this render object as constant, so that this cannot be reallocated.
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
     * Returns true if this render object is reserved at the given
     * render pass index.
     * 
     * @param index
     * @return 
     */
    public boolean isReservedAt(PassIndex index) {
        return reservations.contains(index);
    }
    /**
     * Returns true if this render object is reserved within the time frame.
     * 
     * @param frame
     * @return 
     */
    public boolean isReservedWithin(TimeFrame frame) {
        if (frame.getStartQueueIndex() >= reservations.size()) {
            return false;
        }
        int n = Math.min(reservations.size()-1, frame.getEndQueueIndex());
        for (int i = frame.getStartQueueIndex(); i <= n; i++) {
            if (reservations.get(i)) {
                return true;
            }
        }
        return false;
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
     * Gets the next id.
     * 
     * @return 
     */
    public static long getNextId() {
        return nextId;
    }
    
}
