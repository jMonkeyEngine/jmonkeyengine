/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.util.NativeObject;
import java.util.BitSet;
import java.util.function.Consumer;

/**
 *
 * @author codex
 * @param <T>
 */
public class RenderObject <T> {
    
    private static final Consumer DEFAULT = object -> {};
    private static final Consumer<NativeObject> NATIVE = object -> object.dispose();
    
    private static int nextId = 0;
    
    private final int id;
    private final T object;
    private final int timeoutDuration;
    private int timeout = 0;
    private boolean acquired = false;
    private final BitSet reservations = new BitSet();
    private final Consumer disposer;

    public RenderObject(T object, int timeout, Consumer<T> disposer) {
        this.id = nextId++;
        this.object = object;
        this.timeoutDuration = timeout;
        if (disposer != null) {
            this.disposer = disposer;
        } else if (object instanceof NativeObject) {
            this.disposer = NATIVE;
        } else {
            this.disposer = DEFAULT;
        }
    }

    public boolean acquire(TimeFrame time, boolean reserved) {
        if (acquired || (!reserved && isReservedWithin(time))) {
            return false;
        }
        acquire();
        return true;
    }
    public void acquire() {
        timeout = timeoutDuration;
        acquired = true;
    }
    public void release() {
        if (!acquired) {
            throw new IllegalStateException("Already released.");
        }
        acquired = false;
    }
    public void reserve(int index) {
        reservations.set(index);
    }
    public void dispose() {
        disposer.accept(object);
    }
    
    public void clearReservations() {
        reservations.clear();
    }
    public boolean tickTimeout() {
        return timeout-- > 0;
    }
    
    public int getId() {
        return id;
    }
    public T getObject() {
        return object;
    }
    public boolean isAcquired() {
        return acquired;
    }
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
    
    public static int getNextId() {
        return nextId;
    }
    
}
