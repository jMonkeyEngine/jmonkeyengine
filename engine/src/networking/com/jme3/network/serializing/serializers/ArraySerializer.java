/*
 * Copyright (c) 2009-2010 jMonkeyEngine, Java Game Networking
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
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;

/**
 * Array serializer
 *
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class ArraySerializer extends Serializer {
    private int[] getDimensions (Object array) {
        int depth = 0;
        Class nextClass = array.getClass().getComponentType();
        while (nextClass != null) {
            depth++;
            nextClass = nextClass.getComponentType();
        }
        int[] dimensions = new int[depth];
        dimensions[0] = Array.getLength(array);
        if (depth > 1) collectDimensions(array, 1, dimensions);
        return dimensions;
    }

    private void collectDimensions (Object array, int dimension, int[] dimensions) {
        boolean elementsAreArrays = dimension < dimensions.length - 1;
        for (int i = 0, s = Array.getLength(array); i < s; i++) {
            Object element = Array.get(array, i);
            if (element == null) continue;
            dimensions[dimension] = Math.max(dimensions[dimension], Array.getLength(element));
            if (elementsAreArrays) collectDimensions(element, dimension + 1, dimensions);
        }
    }

    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        byte dimensionCount = data.get();
        if (dimensionCount == 0)
            return null;

        int[] dimensions = new int[dimensionCount];
        for (int i = 0; i < dimensionCount; i++)
                dimensions[i] = data.getInt();

        Serializer elementSerializer = null;

        Class elementClass = c;
        while (elementClass.getComponentType() != null)
            elementClass = elementClass.getComponentType();

        if (Modifier.isFinal(elementClass.getModifiers())) elementSerializer = Serializer.getSerializer(elementClass);
        // Create array and read in the data.
        T array = (T)Array.newInstance(elementClass, dimensions);
        readArray(elementSerializer, elementClass, data, array, 0, dimensions);
        return array;
    }

    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        if (object == null){
            buffer.put((byte)0);
            return;
        }

        int[] dimensions = getDimensions(object);
        buffer.put((byte)dimensions.length);
        for (int dimension : dimensions) buffer.putInt(dimension);
        Serializer elementSerializer = null;

        Class elementClass = object.getClass();
        while (elementClass.getComponentType() != null) {
            elementClass = elementClass.getComponentType();
        }

        if (Modifier.isFinal(elementClass.getModifiers())) elementSerializer = Serializer.getSerializer(elementClass);
        writeArray(elementSerializer, buffer, object, 0, dimensions.length);
    }

    private void writeArray(Serializer elementSerializer, ByteBuffer buffer, Object array, int dimension, int dimensionCount) throws IOException {
        int length = Array.getLength(array);
        if (dimension > 0) {
            buffer.putInt(length);
        }
        // Write array data.
        boolean elementsAreArrays = dimension < dimensionCount - 1;
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (elementsAreArrays) {
                if (element != null) writeArray(elementSerializer, buffer, element, dimension + 1, dimensionCount);
            } else if (elementSerializer != null) {
                elementSerializer.writeObject(buffer, element);
            } else {
                // Each element could be a different type. Store the class with the object.
                Serializer.writeClassAndObject(buffer, element);
            }
        }
    }

    private void readArray (Serializer elementSerializer, Class elementClass, ByteBuffer buffer, Object array, int dimension, int[] dimensions) throws IOException {
        boolean elementsAreArrays = dimension < dimensions.length - 1;
        int length;
        if (dimension == 0) {
            length = dimensions[0];
        } else {
            length = buffer.getInt();
        }
        for (int i = 0; i < length; i++) {
            if (elementsAreArrays) {
                // Nested array.
                Object element = Array.get(array, i);
                if (element != null) readArray(elementSerializer, elementClass, buffer, element, dimension + 1, dimensions);
            } else if (elementSerializer != null) {
                // Use same converter (and class) for all elements.
                Array.set(array, i, elementSerializer.readObject(buffer, elementClass));
            } else {
                // Each element could be a different type. Look up the class with the object.
                Array.set(array, i, Serializer.readClassAndObject(buffer));
            }
        }
    }
}
