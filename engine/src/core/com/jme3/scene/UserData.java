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
package com.jme3.scene;

import com.jme3.export.*;
import java.io.IOException;

/**
 * <code>UserData</code> is used to contain user data objects
 * set on spatials (primarily primitives) that do not implement
 * the {@link Savable} interface. Note that attempting
 * to export any models which have non-savable objects
 * attached to them will fail.
 */
public final class UserData implements Savable {

    /**
     * Boolean type on Geometries to indicate that physics collision
     * shape generation should ignore them.
     */
    public static final String JME_PHYSICSIGNORE = "JmePhysicsIgnore";
    
    /**
     * For geometries using shared mesh, this will specify the shared
     * mesh reference.
     */
    public static final String JME_SHAREDMESH = "JmeSharedMesh";
    
    protected byte type;
    protected Object value;

    public UserData() {
    }

    /**
     * Creates a new <code>UserData</code> with the given 
     * type and value.
     * 
     * @param type Type of data, should be between 0 and 4.
     * @param value Value of the data
     */
    public UserData(byte type, Object value) {
        assert type >= 0 && type <= 4;
        this.type = type;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    public static byte getObjectType(Object type) {
        if (type instanceof Integer) {
            return 0;
        } else if (type instanceof Float) {
            return 1;
        } else if (type instanceof Boolean) {
            return 2;
        } else if (type instanceof String) {
            return 3;
        } else if (type instanceof Long) {
            return 4;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getClass().getName());
        }
    }

    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(type, "type", (byte)0);

        switch (type) {
            case 0:
                int i = (Integer) value;
                oc.write(i, "intVal", 0);
                break;
            case 1:
                float f = (Float) value;
                oc.write(f, "floatVal", 0f);
                break;
            case 2:
                boolean b = (Boolean) value;
                oc.write(b, "boolVal", false);
                break;
            case 3:
                String s = (String) value;
                oc.write(s, "strVal", null);
                break;
            case 4:
                Long l = (Long) value;
                oc.write(l, "longVal", 0l);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        type = ic.readByte("type", (byte) 0);

        switch (type) {
            case 0:
                value = ic.readInt("intVal", 0);
                break;
            case 1:
                value = ic.readFloat("floatVal", 0f);
                break;
            case 2:
                value = ic.readBoolean("boolVal", false);
                break;
            case 3:
                value = ic.readString("strVal", null);
                break;
            case 4:
                value = ic.readLong("longVal", 0l);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
