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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

public class MapSerializer extends Serializer {

    /*

    Structure:

    struct Map {
        INT length
        BYTE flags = { 0x01 = all keys have the same type,
                       0x02 = all values have the same type }
        if (flags has 0x01 set)
            SHORT keyType
        if (flags has 0x02 set)
            SHORT valType

        struct MapEntry[length] entries {
            if (flags does not have 0x01 set)
                SHORT keyType
            OBJECT key

            if (flags does not have 0x02 set)
                SHORT valType
            OBJECT value
        }
    }

     */

    @SuppressWarnings("unchecked")
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        int length = data.getInt();

        Map map;
        try {
            map = (Map)c.newInstance();
        } catch (Exception e) {
            log.log(Level.WARNING, "[Serializer][???] Could not determine map type. Using HashMap.");
            map = new HashMap();
        }

        if (length == 0) return (T)map;

        int flags = data.get() & 0xff;
        boolean uniqueKeys = (flags & 0x01) == 0;
        boolean uniqueVals = (flags & 0x02) == 0;

        Class keyClazz = null;
        Class valClazz = null;
        Serializer keySerial = null;
        Serializer valSerial = null;
        if (!uniqueKeys){
            SerializerRegistration reg = Serializer.readClass(data);
            keyClazz = reg.getType();
            keySerial = reg.getSerializer();
        }
        if (!uniqueVals){
            SerializerRegistration reg = Serializer.readClass(data);
            valClazz = reg.getType();
            valSerial = reg.getSerializer();
        }

        for (int i = 0; i < length; i++){
            Object key;
            Object value;
            if (uniqueKeys){
                key = Serializer.readClassAndObject(data);
            }else{
                key = keySerial.readObject(data, keyClazz);
            }
            if (uniqueVals){
                value = Serializer.readClassAndObject(data);
            }else{
                value = valSerial.readObject(data, valClazz);
            }

            map.put(key, value);
        }

        return (T)map;
    }

    @SuppressWarnings("unchecked")
    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        Map map = (Map)object;
        int length = map.size();

        buffer.putInt(length);
        if (length == 0) return;


        Set<Entry> entries = map.entrySet();

        Iterator<Entry> it = entries.iterator();

        Entry entry = it.next();
        Class keyClass = entry.getKey().getClass();
        Class valClass = entry.getValue().getClass();
        while (it.hasNext()) {
            entry = it.next();

            if (entry.getKey().getClass() != keyClass){
                keyClass = null;
                if (valClass == null)
                    break;
            }
            if (entry.getValue().getClass() != valClass){
                valClass = null;
                if (keyClass == null)
                    break;
            }
        }

        boolean uniqueKeys = keyClass == null;
        boolean uniqueVals = valClass == null;
        int flags = 0;
        if (!uniqueKeys) flags |= 0x01;
        if (!uniqueVals) flags |= 0x02;
        buffer.put( (byte) flags );

        Serializer keySerial = null, valSerial = null;
        if (!uniqueKeys){
            Serializer.writeClass(buffer, keyClass);
            keySerial = Serializer.getSerializer(keyClass);
        }
        if (!uniqueVals){
            Serializer.writeClass(buffer, valClass);
            valSerial = Serializer.getSerializer(valClass);
        }

        it = entries.iterator();
        while (it.hasNext()) {
            entry = it.next();
            if (uniqueKeys){
                Serializer.writeClassAndObject(buffer, entry.getKey());
            }else{
                keySerial.writeObject(buffer, entry.getKey());
            }
            if (uniqueVals){
                Serializer.writeClassAndObject(buffer, entry.getValue());
            }else{
                valSerial.writeObject(buffer, entry.getValue());
            }
        }
    }
}
