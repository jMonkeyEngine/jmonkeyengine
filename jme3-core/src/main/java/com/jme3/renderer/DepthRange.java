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
package com.jme3.renderer;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * Defines a range within 0 and 1 that render depth values are clamped to.
 * 
 * @author codex
 */
public class DepthRange implements Savable {
    
    /**
     * Range between 0 and 1.
     */
    public static final DepthRange IDENTITY = new DepthRange();
    /**
     * Range that clamps to zero.
     */
    public static final DepthRange FRONT = new DepthRange(0, 0);
    /**
     * Range that clamps to one.
     */
    public static final DepthRange REAR = new DepthRange(1, 1);
    
    private float start, end;

    /**
     * Creates a new range between 0 and 1.
     */
    public DepthRange() {
        set(0, 1);
    }
    /**
     * 
     * @param start
     * @param end 
     */
    public DepthRange(float start, float end) {
        set(start, end);
    }
    /**
     * 
     * @param range 
     */
    public DepthRange(DepthRange range) {
        set(range);
    }
    
    /**
     * Sets the range.
     * 
     * @param start lower bound
     * @param end upper bound
     * @return this instance
     */
    public final DepthRange set(float start, float end) {
        validateRange(start, end);
        this.start = start;
        this.end = end;
        return this;
    }
    
    /**
     * Sets the range.
     * 
     * @param range
     * @return this instance
     */
    public final DepthRange set(DepthRange range) {
        // no need to validate range here
        start = range.start;
        end = range.end;
        return this;
    }
    
    /**
     * Sets the start (lower) bound.
     * 
     * @param start
     * @return this instance
     */
    public final DepthRange setStart(float start) {
        validateRange(start, end);
        this.start = start;
        return this;
    }
    
    /**
     * Sets the end (upper) bound.
     * 
     * @param end
     * @return 
     */
    public final DepthRange setEnd(float end) {
        validateRange(start, end);
        this.end = end;
        return this;
    }
    
    /**
     * Gets the start (lower) bound.
     * 
     * @return 
     */
    public float getStart() {
        return start;
    }
    
    /**
     * Gets the end (upper) bound.
     * 
     * @return 
     */
    public float getEnd() {
        return end;
    }
    
    private void validateRange(float start, float end) {
        if (start > end) {
            throw new IllegalStateException("Depth start cannot be beyond depth end.");
        }
        if (start > 1 || start < 0 || end > 1 || end < 0) {
            throw new IllegalStateException("Depth parameters must be between 0 and 1 (inclusive).");
        }
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof DepthRange)) {
            return false;
        }
        DepthRange obj = (DepthRange)object;
        return start == obj.start && end == obj.end;
    }
    
    public boolean equals(DepthRange range) {
        return range != null && start == range.start && end == range.end;
    }
    
    public boolean equals(float start, float end) {
        return this.start != start && this.end != end;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + Float.floatToIntBits(this.start);
        hash = 79 * hash + Float.floatToIntBits(this.end);
        return hash;
    }
    @Override
    public String toString() {
        return "DepthRange["+start+" -> "+end+"]";
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(start, "start", 0);
        out.write(end, "end", 1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        start = in.readFloat("start", 0);
        end = in.readFloat("end", 1);
    }
    
}
