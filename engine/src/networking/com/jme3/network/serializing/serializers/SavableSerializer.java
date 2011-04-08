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

import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.network.serializing.Serializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SavableSerializer extends Serializer {

    private BinaryExporter exporter = new BinaryExporter();
    private BinaryImporter importer = new BinaryImporter();

    private static class BufferOutputStream extends OutputStream {

        ByteBuffer output;

        public BufferOutputStream(ByteBuffer output){
            this.output = output;
        }

        @Override
        public void write(int b) throws IOException {
            output.put( (byte) b );
        }

        @Override
        public void write(byte[] b){
            output.put(b);
        }

        @Override
        public void write(byte[] b, int off, int len){
            output.put(b, off, len);
        }
    }

    private static class BufferInputStream extends InputStream {

        ByteBuffer input;

        public BufferInputStream(ByteBuffer input){
            this.input = input;
        }

        @Override
        public int read() throws IOException {
            if (input.remaining() == 0)
                return -1;
            else
                return input.get() & 0xff;
        }

        @Override
        public int read(byte[] b){
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte[] b, int off, int len){
            int toRead = len > input.remaining() ? input.remaining() : len;
            input.get(b, off, len);
            return toRead;
        }

    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T readObject(ByteBuffer data, Class<T> c) throws IOException {
        BufferInputStream in = new BufferInputStream(data);
        Savable s = importer.load(in);
        in.close();
        return (T) s;
    }

    @Override
    public void writeObject(ByteBuffer buffer, Object object) throws IOException {
        Savable s = (Savable) object;
        BufferOutputStream out = new BufferOutputStream(buffer);
        exporter.save(s, out);
        out.close();
    }

}
