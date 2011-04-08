/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import com.jme3.network.serializing.SerializerRegistration;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Serializes collections.
 *
 * @author Lars Wesselius
 */
public class CollectionSerializer extends Serializer {

    @SuppressWarnings("unchecked")
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        int length = data.getInt();

        Collection collection;
        try {
            collection = (Collection)c.newInstance();
        } catch (Exception e) {
            log.log(Level.FINE, "[Serializer][???] Could not determine collection type. Using ArrayList.");
            collection = new ArrayList(length);
        }

        if (length == 0) return (T)collection;

        if (data.get() == (byte)1) {
            SerializerRegistration reg = Serializer.readClass(data);
            Class clazz = reg.getType();
            Serializer serializer = reg.getSerializer();

            for (int i = 0; i != length; ++i) {
                collection.add(serializer.readObject(data, clazz));
            }
        } else {
            for (int i = 0; i != length; ++i) {
                collection.add(Serializer.readClassAndObject(data));
            }
        }
        return (T)collection;
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        Collection collection = (Collection)object;
        int length = collection.size();

        buffer.putInt(length);
        if (length == 0) return;

        Iterator it = collection.iterator();
        Class elementClass = it.next().getClass();
        while (it.hasNext()) {
            Object obj = it.next();

            if (obj.getClass() != elementClass) {
                elementClass = null;
                break;
            }
        }

        if (elementClass != null) {
            buffer.put((byte)1);
            Serializer.writeClass(buffer, elementClass);
            Serializer serializer = Serializer.getSerializer(elementClass);

            for (Object elem : collection) {
                serializer.writeObject(buffer, elem);
            }
        } else {
            buffer.put((byte)0);
            for (Object elem : collection) {
                Serializer.writeClassAndObject(buffer, elem);
            }
        }
    }
}
