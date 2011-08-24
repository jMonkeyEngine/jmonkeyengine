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
import com.jme3.network.serializing.SerializerException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Enum serializer.
 *
 * @author Lars Wesselius
 */
public class EnumSerializer extends Serializer {
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        try {
            int ordinal = data.getInt();

            if (ordinal == -1) return null;
            T[] enumConstants = c.getEnumConstants();
            if (enumConstants == null)
                throw new SerializerException( "Class has no enum constants:" + c );
            return enumConstants[ordinal];
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        if (object == null) {
            buffer.putInt(-1);
        } else {
            buffer.putInt(((Enum)object).ordinal());
        }
    }
}
