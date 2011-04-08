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
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * String serializer.
 *
 * @author Lars Wesselius
 */
@SuppressWarnings("unchecked")
public class StringSerializer extends Serializer {

    public String readObject(ByteBuffer data, Class c) throws IOException {

        int length = -1;
        byte type = data.get();
        if (type == (byte)0) {
            return null;
        } else if (type == (byte)1) {
            // Byte
            length = data.get();
        } else if (type == (byte)2) {
            // Short
            length = data.getShort();
        } else if (type == (byte)3) {
            // Int
            length = data.getInt();
        }
        if (length == -1) throw new IOException("Could not read String: Invalid length identifier.");

        byte[] buffer = new byte[length];
        data.get(buffer);
        return new String(buffer, "UTF-8");
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        String string = (String)object;

        if (string == null) {
            // Write that it's 0.
            buffer.put((byte)0);
            return;
        }
        byte[] stringBytes = string.getBytes("UTF-8");
        int bufferLength = stringBytes.length;

        try {
            if (bufferLength <= Byte.MAX_VALUE) {
                buffer.put((byte)1);
                buffer.put((byte)bufferLength);
            } else if (bufferLength <= Short.MAX_VALUE) {
                buffer.put((byte)2);
                buffer.putShort((short)bufferLength);
            } else {
                buffer.put((byte)3);
                buffer.putInt(bufferLength);
            }
            buffer.put(stringBytes);
        }
        catch (BufferOverflowException e) {
            e.printStackTrace();
        }
    }
}
