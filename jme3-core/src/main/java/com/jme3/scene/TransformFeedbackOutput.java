/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.scene;

import java.util.ArrayList;

/**
 * This class defines a list of buffer objects which
 * will be bound to ouput of transform feedback.
 * 
 * @author Juraj Papp
 */
public class TransformFeedbackOutput {
    
    protected Mesh.Mode mode;
    protected boolean currentlyInUse = false;
    protected ArrayList<OutputBuffer> buffers = new ArrayList<>();
    
    public class OutputBuffer {
        public BufferObject buf;
        public int slot;
        public boolean range;
        public long offset;
        public long size;

        public OutputBuffer(BufferObject b, int slot) {
            this.buf = b;
            this.slot = slot;
            this.range = false;
        }
        public OutputBuffer(BufferObject b, int slot, long offset, long size) {
            this.buf = b;
            this.slot = slot;
            this.range = true;
            this.offset = offset;
            this.size = size;
        }
    }

    /**
     * Create a new transform feedback output objects which
     * stores output buffers and mode.
     * 
     * Allowed modes are: Points, Lines and Triangles.
     * eg. if TriangleFan is specified the mode is converted to Triangles, etc...
     * 
     * @param mode POINTS, LINES or TRIANGLES
     */
    public TransformFeedbackOutput(Mesh.Mode mode) {
        setMode(mode);
    }
    
    /**
     * Add output buffer.
     * 
     * @param b buffer object to capture into
     */
    public void add(BufferObject b) {
        add(b, buffers.size());
    }
    
    /**
     * Add output buffer to the selected binding slot.
     * If a buffer with the given slot index exists, an IllegalArgumentException is thrown.
     * 
     * @param b buffer object to capture into
     * @param slot binding index
     */
    public void add(BufferObject b, int slot) {
        if(isInUse()) throw new IllegalArgumentException("The output buffer is currently in use!");
        for(int i = 0; i < buffers.size(); i++) {
            OutputBuffer buf = buffers.get(i);
            if(buf.slot == slot) 
                throw new IllegalArgumentException("Buffer slot already specified.");
        }
        buffers.add(new OutputBuffer(b, slot));
    }
    /**
     * Add output buffer to the selected binding slot and range.
     * If a buffer with the given slot index exists, and their ranges overlap
     * an IllegalArgumentException is thrown.
     * 
     * @param b buffer object to capture into
     * @param offset 4 byte aligned offset, or 8 byte aligned if capturing doubles
     * @param size amount of data that can be used from offset
     */
    public void add(BufferObject b, long offset, long size) {
        add(b, buffers.size(), offset, size);
    }
    /**
     * Add output buffer to the selected binding slot and range.
     * If a buffer with the given slot index exists, and their ranges overlap
     * an IllegalArgumentException is thrown.
     * 
     * @param b buffer object to capture into
     * @param slot binding index
     * @param offset 4 byte aligned offset, or 8 byte aligned if capturing doubles
     * @param size amount of data that can be used from offset
     */
    public void add(BufferObject b, int slot, long offset, long size) {
        if(isInUse()) throw new IllegalArgumentException("The output buffer is currently in use!");
        if((offset&3) != 0) throw new IllegalArgumentException("The offset must by 4 byte aligned.");
        for(int i = 0; i < buffers.size(); i++) {
            OutputBuffer buf = buffers.get(i);
            if(buf.slot == slot && 
                (!buf.range ||
                    Math.max(buf.offset, offset) <= Math.min(buf.offset+buf.size, offset+size)
                    )
                ) 
                throw new IllegalArgumentException("Buffer slots overlap!");
        }
        buffers.add(new OutputBuffer(b, slot, offset, size));
    }
    
    /**
     * Used internally by renderer. Do not use.
     * @param inUse
     */
    public void setInUse(boolean inUse) { currentlyInUse = inUse; }
    
    /**
     * Returns true if this output buffer is currently in use.
     * @return true if in use
     */
    public boolean isInUse() { return currentlyInUse; }

    /**
     * Returns the binding at specified index.
     * @param i
     * @return binding at position i 
     */
    public OutputBuffer getOutputBinding(int i) {
        return buffers.get(i);
    }
    
    /**
     * Removed the binding at specified index.
     * @param i
     * @return the binding removed
     */
    public OutputBuffer removeBinding(int i) {
        if(isInUse()) throw new IllegalArgumentException("The output buffer is currently in use!");
        return buffers.remove(i);
    }
    
    /**
     * Clears all bindings.
     */
    public void clear() {
        if(isInUse()) throw new IllegalArgumentException("The output buffer is currently in use!");
        buffers.clear();
    }
    
    /**
     * 
     * @return the number of buffer bindings
     */
    public int getNumberOfBufferBindings() {
        return buffers.size();
    }

    /**
     * Returns the generated primitives which are produced by transform feedback.
     * @return mode POINTS, LINES or TRIANGLES
     */
    public Mesh.Mode getMode() {
        return mode;
    }

    /**
     * Set the type of primitives which are produced by transform feedback.
     * Allowed modes are: Points, Lines and Triangles.
     * 
     * eg. if TriangleFan is specified the mode is converted to Triangles, etc...
     * 
     * @param mode POINTS, LINES or TRIANGLES
     */
    public void setMode(Mesh.Mode mode) {
        if(isInUse()) throw new IllegalArgumentException("The output buffer is currently in use!");
        if(mode == null) throw new IllegalArgumentException("Mode cannot be null");
        this.mode = mode;
    }
    
    
    protected Mesh.Mode convertTFMode(Mesh.Mode mode) {
        switch (mode) {
            case Points:
                return Mesh.Mode.Points;
            case Lines:
            case LineLoop:
            case LineStrip:
                return Mesh.Mode.Lines;
            case Triangles:
            case TriangleFan:
            case TriangleStrip:
                return Mesh.Mode.Triangles;
            default:
                throw new UnsupportedOperationException("Invalid transform feedback mode: " + mode);
        }
    }
}
