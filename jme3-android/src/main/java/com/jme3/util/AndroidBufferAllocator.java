/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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

package com.jme3.util;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jesus Oliver
 * @deprecated implemented {@link AndroidNativeBufferAllocator} instead.
 */
@Deprecated
public class AndroidBufferAllocator implements BufferAllocator {

    // We make use of the ReflectionAllocator to remove the inner buffer
    private static final ReflectionAllocator reflectionAllocator = new ReflectionAllocator();

    private static final String[] wrapperClassNames = {
            "java.nio.ByteBufferAsFloatBuffer",
            "java.nio.ByteBufferAsIntBuffer",
            "java.nio.ByteBufferAsDoubleBuffer",
            "java.nio.ByteBufferAsShortBuffer",
            "java.nio.ByteBufferAsLongBuffer",
            "java.nio.ByteBufferAsCharBuffer",
    };
    private static final String[] possibleBufferFieldNames = {"bb", "byteBuffer"};

    // Keep track of ByteBuffer field by the wrapper class
    private static final Map<Class, Field> fieldIndex = new HashMap<>();

    static {
        for (String className : wrapperClassNames) {
            try {
                Class clazz = Class.forName(className);

                // loop for all possible field names in android
                for (String fieldName : possibleBufferFieldNames) {
                    try {
                        Field field = clazz.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        fieldIndex.put(clazz, field);
                        break;
                    } catch (NoSuchFieldException e) {
                    }
                }
            } catch (ClassNotFoundException ex) {
            }
        }
    }

    /**
     * Searches the inner direct buffer of the Android-specific wrapped buffer classes
     * and destroys it using the reflection allocator method.
     *
     * @param toBeDestroyed The direct buffer that will be "cleaned".
     *
     */
    @Override
    public void destroyDirectBuffer(Buffer toBeDestroyed) {
        // If it is a wrapped buffer, get it's inner direct buffer field and destroy it
        Field field = fieldIndex.get(toBeDestroyed.getClass());
        if (field != null) {
            try {
                ByteBuffer innerBuffer = (ByteBuffer) field.get(toBeDestroyed);
                if (innerBuffer != null) {
                    // Destroy it using the reflection method
                    reflectionAllocator.destroyDirectBuffer(innerBuffer);
                }
            } catch (IllegalAccessException ex) {
            }

        } else {
            // It is not a wrapped buffer, use default reflection allocator to remove it instead.
            reflectionAllocator.destroyDirectBuffer(toBeDestroyed);
        }
    }

    @Override
    public ByteBuffer allocate(int size) {
        return ByteBuffer.allocateDirect(size);
    }
}

