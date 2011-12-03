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
package com.jme3.animation;

import com.jme3.export.*;
import com.jme3.math.Quaternion;
import java.io.IOException;

/**
 * Serialize and compress {@link Quaternion}[] by indexing same values
 * It is converted to float[]
 * @author Lim, YongHoon
 */
public class CompactQuaternionArray extends CompactArray<Quaternion> implements Savable {

    /**
     * creates a compact Quaternion array
     */
    public CompactQuaternionArray() {
    }

    /**
     * creates a compact Quaternion array
     * @param dataArray the data array
     * @param index  the indices array
     */
    public CompactQuaternionArray(float[] dataArray, int[] index) {
        super(dataArray, index);
    }

    @Override
    protected final int getTupleSize() {
        return 4;
    }

    @Override
    protected final Class<Quaternion> getElementClass() {
        return Quaternion.class;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        serialize();
        OutputCapsule out = ex.getCapsule(this);
        out.write(array, "array", null);
        out.write(index, "index", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        array = in.readFloatArray("array", null);
        index = in.readIntArray("index", null);
    }

    @Override
    protected void serialize(int i, Quaternion store) {
        int j = i * getTupleSize();
        array[j] = store.getX();
        array[j + 1] = store.getY();
        array[j + 2] = store.getZ();
        array[j + 3] = store.getW();
    }

    @Override
    protected Quaternion deserialize(int i, Quaternion store) {
        int j = i * getTupleSize();
        store.set(array[j], array[j + 1], array[j + 2], array[j + 3]);
        return store;
    }
}
