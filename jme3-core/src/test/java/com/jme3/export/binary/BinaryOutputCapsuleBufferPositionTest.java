/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.export.binary;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinaryOutputCapsuleBufferPositionTest {

    @Test
    public void savePreservesNioBufferPositions() throws IOException {
        BufferPositionSavable savable = new BufferPositionSavable();
        savable.byteBuffer = byteBuffer(1, 2, 3, 4);
        savable.floatBuffer = floatBuffer(1f, 2f, 3f, 4f);
        savable.intBuffer = intBuffer(1, 2, 3, 4);
        savable.shortBuffer = shortBuffer((short) 1, (short) 2, (short) 3, (short) 4);

        savable.byteBuffer.position(2);
        savable.floatBuffer.position(2);
        savable.intBuffer.position(2);
        savable.shortBuffer.position(2);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BinaryExporter.getInstance().save(savable, output);

        assertEquals(2, savable.byteBuffer.position());
        assertEquals(2, savable.floatBuffer.position());
        assertEquals(2, savable.intBuffer.position());
        assertEquals(2, savable.shortBuffer.position());

        BufferPositionSavable copy = (BufferPositionSavable) BinaryImporter.getInstance()
                .load(new ByteArrayInputStream(output.toByteArray()));

        assertEquals(4, copy.byteBuffer.limit());
        assertEquals(1, copy.byteBuffer.get(0));
        assertEquals(4, copy.byteBuffer.get(3));
        assertEquals(4, copy.floatBuffer.limit());
        assertEquals(1f, copy.floatBuffer.get(0));
        assertEquals(4f, copy.floatBuffer.get(3));
        assertEquals(4, copy.intBuffer.limit());
        assertEquals(1, copy.intBuffer.get(0));
        assertEquals(4, copy.intBuffer.get(3));
        assertEquals(4, copy.shortBuffer.limit());
        assertEquals((short) 1, copy.shortBuffer.get(0));
        assertEquals((short) 4, copy.shortBuffer.get(3));
    }

    private static ByteBuffer byteBuffer(int... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length);
        for (int value : values) {
            buffer.put((byte) value);
        }
        buffer.rewind();
        return buffer;
    }

    private static FloatBuffer floatBuffer(float... values) {
        FloatBuffer buffer = FloatBuffer.allocate(values.length);
        buffer.put(values);
        buffer.rewind();
        return buffer;
    }

    private static IntBuffer intBuffer(int... values) {
        IntBuffer buffer = IntBuffer.allocate(values.length);
        buffer.put(values);
        buffer.rewind();
        return buffer;
    }

    private static ShortBuffer shortBuffer(short... values) {
        ShortBuffer buffer = ShortBuffer.allocate(values.length);
        buffer.put(values);
        buffer.rewind();
        return buffer;
    }

    public static class BufferPositionSavable implements Savable {

        ByteBuffer byteBuffer;
        FloatBuffer floatBuffer;
        IntBuffer intBuffer;
        ShortBuffer shortBuffer;

        @Override
        public void write(JmeExporter exporter) throws IOException {
            OutputCapsule capsule = exporter.getCapsule(this);
            capsule.write(byteBuffer, "byteBuffer", null);
            capsule.write(floatBuffer, "floatBuffer", null);
            capsule.write(intBuffer, "intBuffer", null);
            capsule.write(shortBuffer, "shortBuffer", null);
        }

        @Override
        public void read(JmeImporter importer) throws IOException {
            InputCapsule capsule = importer.getCapsule(this);
            byteBuffer = capsule.readByteBuffer("byteBuffer", null);
            floatBuffer = capsule.readFloatBuffer("floatBuffer", null);
            intBuffer = capsule.readIntBuffer("intBuffer", null);
            shortBuffer = capsule.readShortBuffer("shortBuffer", null);
        }
    }
}
