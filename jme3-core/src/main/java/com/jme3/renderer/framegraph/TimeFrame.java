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
 * 
 * @author codex
 */
public class TimeFrame {
    
    private int index, length;
    
    /**
     * 
     */
    private TimeFrame() {}
    /**
     * 
     * @param passIndex
     * @param length 
     */
    public TimeFrame(int passIndex, int length) {
        this.index = passIndex;
        this.length = length;
        if (this.index < 0) {
            throw new IllegalArgumentException("Pass index cannot be negative.");
        }
        if (this.length < 0) {
            throw new IllegalArgumentException("Length cannot be negative.");
        }
    }
    
    /**
     * Extends, but does not retract, the length so that this time frame
     * includes the given index.
     * 
     * @param passIndex 
     */
    public void extendTo(int passIndex) {
        length = Math.max(length, passIndex-this.index);
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
        target.index = index;
        target.length = length;
        return target;
    }
    
    /**
     * Gets index of the first pass this time frame includes.
     * 
     * @return 
     */
    public int getStartIndex() {
        return index;
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
    public int getEndIndex() {
        return index+length;
    }
    
    /**
     * Returns true if this time frame overlaps the given time frame.
     * 
     * @param time
     * @return 
     */
    public boolean overlaps(TimeFrame time) {
        return index <= time.index+time.length && index+length >= time.index;
    }
    /**
     * Returns true if this time frame includes the given index.
     * 
     * @param index
     * @return 
     */
    public boolean includes(int index) {
        return index <= index && index+length >= index;
    }
    
}
