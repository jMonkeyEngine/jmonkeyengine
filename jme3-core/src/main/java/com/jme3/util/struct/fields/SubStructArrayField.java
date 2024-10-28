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
package com.jme3.util.struct.fields;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;

public class SubStructArrayField<T extends Struct> extends StructField<T[]> {

    @SuppressWarnings("unchecked")
    public SubStructArrayField(int position, String name, T[] value) {
        super(position, name, value);
        initializeToZero((Class<? extends T>) value[0].getClass());
    }

    @SuppressWarnings("unchecked")
    public SubStructArrayField(int position, String name, int length, Class<? extends T> structClass) {
        super(position, name, (T[]) Array.newInstance(structClass, length));
        initializeToZero(structClass);
    }

    private void initializeToZero(Class<? extends T> structClass) {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) try {
                Constructor<? extends T> constructor = structClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                value[i] = constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Can't create new instance of " + structClass + " default constructor is missing? ",e);
            }
        }
    }

}
