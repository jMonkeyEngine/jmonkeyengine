/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

/**
 * Holds indices to a render pass within a framegraph.
 * <p>
 * Negative indices denote using defaults.
 * 
 * @author codex
 */
public final class PassIndex {
    
    /**
     * Index that conforms to defaults only.
     */
    public static final PassIndex PASSIVE = new PassIndex(-1, -1);
    
    /**
     * Index of the thread the pass is executed on.
     */
    public int threadIndex;
    /**
     * Index in the thread the pass is executed at.
     */
    public int queueIndex;
    
    /**
     * Creates a pass index with all defaults (negative indices).
     */
    public PassIndex() {
        this(-1, -1);
    }
    /**
     * 
     * @param queueIndex 
     */
    public PassIndex(int queueIndex) {
        this(FrameGraph.RENDER_THREAD, queueIndex);
    }
    /**
     * 
     * @param threadIndex
     * @param queueIndex 
     */
    public PassIndex(int threadIndex, int queueIndex) {
        this.threadIndex = threadIndex;
        this.queueIndex = queueIndex;
    }
    /**
     * 
     * @param index 
     */
    public PassIndex(PassIndex index) {
        this(index.threadIndex, index.queueIndex);
    }
    
    /**
     * 
     * @param index index to set to (not null)
     * @return this
     */
    public PassIndex set(PassIndex index) {
        threadIndex = index.threadIndex;
        queueIndex = index.queueIndex;
        return this;
    }
    /**
     * 
     * @param threadIndex
     * @param queueIndex
     * @return this
     */
    public PassIndex set(int threadIndex, int queueIndex) {
        this.threadIndex = threadIndex;
        this.queueIndex = queueIndex;
        return this;
    }
    /**
     * 
     * @param threadIndex
     * @return this
     */
    public PassIndex setThreadIndex(int threadIndex) {
        this.threadIndex = threadIndex;
        return this;
    }
    /**
     * 
     * @param queueIndex
     * @return this
     */
    public PassIndex setQueueIndex(int queueIndex) {
        this.queueIndex = queueIndex;
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
    
    /**
     * Gets the index of the thread the pass is executed on.
     * 
     * @return 
     */
    public int getThreadIndex() {
        return threadIndex;
    }
    /**
     * Gets the index of the pass in a queue.
     * 
     * @return 
     */
    public int getQueueIndex() {
        return queueIndex;
    }
    
    /**
     * Returns true if the default thread index is used (thread index is negative).
     * 
     * @return 
     */
    public boolean useDefaultThread() {
        return threadIndex < 0;
    }
    /**
     * Returns true if the default queue index is used (queue index is negative).
     * 
     * @return 
     */
    public boolean useDefaultQueueIndex() {
        return queueIndex < 0;
    }
    
    /**
     * Throws an exception if either index is less than zero.
     */
    public void requirePositive() {
        if (threadIndex < 0) {
            throw new IndexOutOfBoundsException("Thread index cannot be negative in this context.");
        }
        if (queueIndex < 0) {
            throw new IndexOutOfBoundsException("Queue index cannot be negative in this context.");
        }
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
    @Override
    public PassIndex clone() {
        return new PassIndex(threadIndex, queueIndex);
    }
    
}
