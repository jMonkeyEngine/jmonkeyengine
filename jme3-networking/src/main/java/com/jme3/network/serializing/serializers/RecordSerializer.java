/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.network.serializing.serializers;

import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serializer for Java {@code record} types. Uses cached reflection 
 * for performance supporting immutable records across modular Java runtimes.
 * 
 * @author thkluge
 */
public class RecordSerializer extends Serializer {
    
    private static final Map<Class<?>, RecordCache> CACHE = new ConcurrentHashMap<>();
    
    private static class RecordCache {
        Constructor<?> constructor;
        Method[] accessors;
    }
    
    /**
     * Initializes and caches the record's constructor and component accessor methods.
     * 
     * @param clazz The record class being registered.
     */
    @Override
    public void initialize(Class clazz) {
        try {
            RecordCache rc = new RecordCache();
            Method getRecordComponents = Class.class.getMethod("getRecordComponents");
            Object[] components = (Object[]) getRecordComponents.invoke(clazz);
            
            Class<?>[] types = new Class<?>[components.length];
            rc.accessors = new Method[components.length];
            
            if (components.length > 0) {
                Method getType = components[0].getClass().getMethod("getType");
                Method getAccessor = components[0].getClass().getMethod("getAccessor");
                
                for (int i = 0; i < components.length; i++) {
                    types[i] = (Class<?>) getType.invoke(components[i]);
                    rc.accessors[i] = (Method) getAccessor.invoke(components[i]);
                    rc.accessors[i].setAccessible(true);
                }
            }
            
            rc.constructor = clazz.getDeclaredConstructor(types);
            rc.constructor.setAccessible(true);
            CACHE.put(clazz, rc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize RecordCache for: " + clazz.getName(), e);
        }
    }

    /**
     * Reads record fields from the buffer and creates a new record instance using its constructor.
     * 
     * @param data  The buffer to read from.
     * @param clazz The class of the record.
     * @param <T>   The record type.
     * @return The deserialized record instance.
     * @throws IOException If deserialization fails.
     */
    @Override
    public <T> T readObject(ByteBuffer data, Class<T> cls) throws IOException {
        try {
            RecordCache rc = CACHE.get(cls);
            Object[] args = new Object[rc.accessors.length];
            
            for (int i = 0; i < rc.accessors.length; i++) {
                args[i] = Serializer.readClassAndObject(data);
            }
            
            return (T) rc.constructor.newInstance(args);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize record: " + cls.getName(), e);
        }
    }

    /**
     * Writes the record's field values to the buffer using cached accessors.
     * 
     * @param buffer The buffer to write to.
     * @param object The record object to serialize.
     * @throws IOException If serialization fails.
     */
    @Override
    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        try {
            RecordCache rc = CACHE.get(object.getClass());
            
            for (Method accessor : rc.accessors) {
                Object value = accessor.invoke(object);
                Serializer.writeClassAndObject(buffer, value);
            }
        } catch (Exception e) {
            throw new IOException("Failed to serialize record: " + object.getClass().getName(), e);
        }
    }
}