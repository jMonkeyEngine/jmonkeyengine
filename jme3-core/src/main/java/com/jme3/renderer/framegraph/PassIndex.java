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

/**
 * Holds indices pointing to a render pass held within a framegraph.
 * <p>
 * The FrameGraph system uses indices to schedule resources, particularly
 * with reservations.
 * <p>
 * Negative indices denote using defaults assigned by the FrameGraph.
 * 
 * @author codex
 */
public final class PassIndex {
    
    /**
     * Index of the main render thread.
     */
    public static final int MAIN_THREAD = 0;
    
    /**
     * Index that conforms to defaults only.
     */
    public static final PassIndex DEFAULT = new PassIndex();
    
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
        this(MAIN_THREAD, -1);
    }
    /**
     * 
     * @param queueIndex 
     */
    public PassIndex(int queueIndex) {
        this(MAIN_THREAD, queueIndex);
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
     * Returns true if this thread index matches the main thread index
     * 
     * @return 
     */
    public boolean isMainThread() {
        return threadIndex == MAIN_THREAD;
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
