/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 *
 * @author codex
 */
public class PassIndex {
    
    private int threadIndex, queueIndex;
    
    public PassIndex(int queueIndex) {
        this(FrameGraph.RENDER_THREAD, queueIndex);
    }
    public PassIndex(int threadIndex, int queueIndex) {
        this.threadIndex = threadIndex;
        this.queueIndex = queueIndex;
    }
    
    public PassIndex set(PassIndex index) {
        threadIndex = index.threadIndex;
        queueIndex = index.queueIndex;
        return this;
    }
    
    /**
     * Shifts the thread index up or down one if the thread index is greater
     * than the given index.
     * 
     * @param i
     * @param pos
     * @return new thread index
     */
    public int shiftThread(int i, boolean pos) {
        if (threadIndex > i) {
            threadIndex += pos ? 1 : -1;
        }
        return threadIndex;
    }
    /**
     * Shifts the queue index up or down one if the thread index is greater than
     * the given index.
     * 
     * @param i
     * @param pos
     * @return new queue index
     */
    public int shiftQueue(int i, boolean pos) {
        if (queueIndex > i) {
            queueIndex += pos ? 1 : -1;
        }
        return queueIndex;
    }
    
    public int getThreadIndex() {
        return threadIndex;
    }
    public int getQueueIndex() {
        return queueIndex;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + this.threadIndex;
        hash = 19 * hash + this.queueIndex;
        return hash;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PassIndex other = (PassIndex) obj;
        if (this.threadIndex != other.threadIndex) {
            return false;
        }
        return this.queueIndex == other.queueIndex;
    }
    @Override
    public String toString() {
        return PassIndex.class.getSimpleName()+"[thread="+threadIndex+", queue="+queueIndex+']';
    }
    
}
