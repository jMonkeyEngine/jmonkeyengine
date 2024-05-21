/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.renderer.framegraph.definitions.ResourceDef;
import com.jme3.util.NativeObject;
import java.util.BitSet;
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
    private final BitSet reservations = new BitSet();
    private int timeoutDuration;
    private int timeout = 0;
    private boolean acquired = false;
    private boolean constant = true;
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
    public void reserve(int index) {
        reservations.set(index);
    }
    /**
     * Disposes the internal object.
     */
    public void dispose() {
        disposer.accept(object);
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
    public boolean isReservedAt(int index) {
        return reservations.get(index);
    }
    /**
     * Returns true if this render object is reserved within the time frame.
     * 
     * @param frame
     * @return 
     */
    public boolean isReservedWithin(TimeFrame frame) {
        if (frame.getStartIndex() >= reservations.size()) {
            return false;
        }
        int n = Math.min(reservations.size()-1, frame.getEndIndex());
        for (int i = frame.getStartIndex(); i <= n; i++) {
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
