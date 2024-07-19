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
 * Represents a period of time starting at the start of the indexed pass, and
 * lasting for the duration of a number of following passes.
 * <p>
 * Used primarily to track the lifetime of ResourceViews, which is then used
 * to determine if a ResourceView violates any reservations.
 * <p>
 * This would rather overestimate that underestimate, so asynchronous resources
 * are tracked as surviving from first inception to frame end.
 * 
 * @author codex
 */
public class TimeFrame {
    
    private int thread;
    private int queue, length;
    private boolean async = false;
    
    /**
     * 
     */
    private TimeFrame() {}
    /**
     * 
     * @param index
     * @param length 
     */
    public TimeFrame(PassIndex index, int length) {
        this.thread = index.getThreadIndex();
        this.queue = index.getQueueIndex();
        this.length = length;
        if (this.queue < 0) {
            throw new IllegalArgumentException("Pass index cannot be negative.");
        }
        if (this.length < 0) {
            throw new IllegalArgumentException("Length cannot be negative.");
        }
    }
    
    /**
     * Extends the length so that this time frame includes the given index.
     * 
     * @param passIndex 
     */
    public void extendTo(PassIndex passIndex) {
        if (passIndex.getThreadIndex() != thread) {
            async = true;
        } else {
            length = Math.max(length, passIndex.getQueueIndex()-this.queue);
        }
    }
    /**
     * Copies this to the target time frame.
     * 
     * @param target
     * @return 
     */
    public TimeFrame copyTo(TimeFrame target) {
        if (target == null) {
            target = new TimeFrame();
        }
        target.thread = thread;
        target.queue = queue;
        target.length = length;
        target.async = async;
        return target;
    }
    
    /**
     * Gets the index of the thread this timeframe is based from.
     * 
     * @return 
     */
    public int getThreadIndex() {
        return thread;
    }
    /**
     * Gets index of the first pass this time frame includes.
     * 
     * @return 
     */
    public int getStartQueueIndex() {
        return queue;
    }
    /**
     * Gets the length.
     * 
     * @return 
     */
    public int getLength() {
        return length;
    }
    /**
     * Gets index of the last pass this time frame includes.
     * 
     * @return 
     */
    public int getEndQueueIndex() {
        return queue+length;
    }
    /**
     * Returns true if this timeframe is asynchronous.
     * <p>
     * An asynchronous timeframe's end index is unreliable.
     * 
     * @return 
     */
    public boolean isAsync() {
        return async;
    }
    
    /**
     * Returns true if this time frame overlaps the given time frame.
     * 
     * @param time
     * @return 
     */
    public boolean overlaps(TimeFrame time) {
        return queue <= time.queue+time.length && queue+length >= time.queue;
    }
    /**
     * Returns true if this time frame includes the given index.
     * 
     * @param index
     * @return 
     */
    public boolean includes(int index) {
        return queue <= index && queue+length >= index;
    }
    
}
