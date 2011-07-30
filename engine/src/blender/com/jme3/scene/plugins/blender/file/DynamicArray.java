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
package com.jme3.scene.plugins.blender.file;

import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;

/**
 * An array that can be dynamically modified/
 * @author Marcin Roguski
 * @param <T>
 *        the type of stored data in the array
 */
public class DynamicArray<T> implements Cloneable {

    /** An array object that holds the required data. */
    private T[] array;
    /**
     * This table holds the sizes of dimetions of the dynamic table. It's length specifies the table dimension or a
     * pointer level. For example: if tableSizes.length == 3 then it either specifies a dynamic table of fixed lengths:
     * dynTable[a][b][c], where a,b,c are stored in the tableSizes table.
     */
    private int[] tableSizes;

    /**
     * Constructor. Builds an empty array of the specified sizes.
     * @param tableSizes
     *        the sizes of the table
     * @throws BlenderFileException
     *         an exception is thrown if one of the sizes is not a positive number
     */
    @SuppressWarnings("unchecked")
    public DynamicArray(int[] tableSizes) throws BlenderFileException {
        this.tableSizes = tableSizes;
        int totalSize = 1;
        for (int size : tableSizes) {
            if (size <= 0) {
                throw new BlenderFileException("The size of the table must be positive!");
            }
            totalSize *= size;
        }
        this.array = (T[]) new Object[totalSize];
    }

    /**
     * Constructor. Builds an empty array of the specified sizes.
     * @param tableSizes
     *        the sizes of the table
     * @throws BlenderFileException
     *         an exception is thrown if one of the sizes is not a positive number
     */
    public DynamicArray(int[] tableSizes, T[] data) throws BlenderFileException {
        this.tableSizes = tableSizes;
        int totalSize = 1;
        for (int size : tableSizes) {
            if (size <= 0) {
                throw new BlenderFileException("The size of the table must be positive!");
            }
            totalSize *= size;
        }
        if (totalSize != data.length) {
            throw new IllegalArgumentException("The size of the table does not match the size of the given data!");
        }
        this.array = data;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * This method returns a value on the specified position. The dimension of the table is not taken into
     * consideration.
     * @param position
     *        the position of the data
     * @return required data
     */
    public T get(int position) {
        return array[position];
    }

    /**
     * This method returns a value on the specified position in multidimensional array. Be careful not to exceed the
     * table boundaries. Check the table's dimension first.
     * @param position
     *        the position of the data indices of data position
     * @return required data required data
     */
    public T get(int... position) {
        if (position.length != tableSizes.length) {
            throw new ArrayIndexOutOfBoundsException("The table accepts " + tableSizes.length + " indexing number(s)!");
        }
        int index = 0;
        for (int i = 0; i < position.length - 1; ++i) {
            index += position[i] * tableSizes[i + 1];
        }
        index += position[position.length - 1];
        return array[index];
    }

    /**
     * This method returns the total amount of data stored in the array.
     * @return the total amount of data stored in the array
     */
    public int getTotalSize() {
        return array.length;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (array instanceof Character[]) {//in case of character array we convert it to String
            for (int i = 0; i < array.length && (Character) array[i] != '\0'; ++i) {//strings are terminater with '0'
                result.append(array[i]);
            }
        } else {
            result.append('[');
            for (int i = 0; i < array.length; ++i) {
                result.append(array[i].toString());
                if (i + 1 < array.length) {
                    result.append(',');
                }
            }
            result.append(']');
        }
        return result.toString();
    }
}