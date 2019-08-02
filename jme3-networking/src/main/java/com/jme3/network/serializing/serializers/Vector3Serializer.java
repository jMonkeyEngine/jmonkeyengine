/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import com.jme3.math.Vector3f;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author Kirill Vainer
 */
@SuppressWarnings("unchecked")
public class Vector3Serializer extends Serializer {

    public Vector3f readObject(ByteBuffer data, Class c) throws IOException {
        // Read the null/non-null marker
        if (data.get() == 0x0) {
            return null;
        }
        
        Vector3f vec3 = new Vector3f();
        vec3.x = data.getFloat();
        vec3.y = data.getFloat();
        vec3.z = data.getFloat();
        return vec3;
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        // Add the null/non-null marker
        buffer.put((byte) (object != null ? 0x1 : 0x0));
        if (object == null) {
            // Nothing left to do
            return;
        }
        
        Vector3f vec3 = (Vector3f) object;
        buffer.putFloat(vec3.x);
        buffer.putFloat(vec3.y);
        buffer.putFloat(vec3.z);
    }
}
