/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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
package com.jme3.shader.bufferobject.layout;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.shader.bufferobject.BufferRegion;
import com.jme3.util.functional.Function;
import com.jme3.util.struct.Struct;

/**
 * Layout serializer for buffers
 * 
 * @author Riccardo Balbo
 */
public abstract class  BufferLayout {

    public static abstract class ObjectSerializer<T> {
        private Function<Boolean, Object> filter;

        public ObjectSerializer(Class<T> cls) {
            this(obj -> {
                Class<?> objc = obj instanceof Class ? (Class<?>) obj : obj.getClass();
                return cls.isAssignableFrom(objc);
            });

        }

        public ObjectSerializer(Function<Boolean, Object> filter) {
            this.filter = filter;
        }

        public final boolean canSerialize(Object obj) {
            return filter.eval(obj);
        }

        public abstract int length(BufferLayout layout, T obj);

        public abstract int basicAlignment(BufferLayout layout, T obj);

        public abstract void write(BufferLayout layout, ByteBuffer bbf, T obj);
    }

    protected List<ObjectSerializer<?>> serializers = new ArrayList<ObjectSerializer<?>>();

    protected ObjectSerializer<?> getSerializer(Object obj) {
        for (int i = serializers.size() - 1; i >= 0; i--) {
            ObjectSerializer<?> sr = serializers.get(i);
            if (sr.canSerialize(obj)) return sr;
            
        }
        throw new RuntimeException("Serializer not found for " + obj + " of type " + obj.getClass());
    }

    /**
     * Register a serializer
     * @param serializer An object of type {@link ObjectSerializer}
     */
    protected void registerSerializer(ObjectSerializer<?> serializer) {
        serializers.add(serializer);
    }
    
    /**
     * Estimate size of Object when serialized accordingly with std140
     * 
     * @param o
     *            the object to serialize
     * @return the size
     */
    @SuppressWarnings("unchecked")
    public int estimateSize(Object o) {
        ObjectSerializer s = getSerializer(o);
        return s.length(this, o);
    }

    /**
     * Get basic alignment of Object when serialized accordingly with std140
     * 
     * @param o
     *            the object to serialize
     * @return the basic alignment
     */
    @SuppressWarnings("unchecked")
    public int getBasicAlignment(Object o) {
        ObjectSerializer s = getSerializer(o);
        return s.basicAlignment(this, o);
    }

    /**
     * Align a position to the given basicAlignment
     * 
     * @param pos
     *            the position to align
     * @param basicAlignment
     *            the basic alignment
     * @return the aligned position
     */
    public int align(int pos, int basicAlignment) {
        return pos == 0 ? pos : FastMath.toMultipleOf(pos, basicAlignment);
    }

    /**
     * Serialize an object accordingly with the std140 layout and write the
     * result to a BufferObject
     * 
     * @param out
     *            the output BufferObject where the object will be serialized
     *            (starting from the current position)
     * @param o
     *            the Object to serialize
     */
    @SuppressWarnings("unchecked")
    public void write(ByteBuffer out, Object o) {
        ObjectSerializer s = getSerializer(o);        
        s.write(this, out, o);
    }

    public abstract String getId();

    public abstract List<BufferRegion> generateFieldRegions(Struct struct);

    public abstract BufferSlice getNextFieldRegion(int position, StructField field, StructField next);

}
