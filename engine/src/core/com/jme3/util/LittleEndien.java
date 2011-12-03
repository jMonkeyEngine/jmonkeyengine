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
package com.jme3.util;

import java.io.*;

/**
 * <code>LittleEndien</code> is a class to read littleendien stored data
 * via a InputStream.  All functions work as defined in DataInput, but
 * assume they come from a LittleEndien input stream.  Currently used to read .ms3d and .3ds files.
 * @author Jack Lindamood
 */
public class LittleEndien extends InputStream implements DataInput {

    private BufferedInputStream in;
    private BufferedReader inRead;

    /**
     * Creates a new LittleEndien reader from the given input stream.  The
     * stream is wrapped in a BufferedReader automatically.
     * @param in The input stream to read from.
     */
    public LittleEndien(InputStream in) {
        this.in = new BufferedInputStream(in);
        inRead = new BufferedReader(new InputStreamReader(in));
    }

    public int read() throws IOException {
        return in.read();
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return in.read(buf);
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return in.read(buf, off, len);
    }

    public int readUnsignedShort() throws IOException {
        return (in.read() & 0xff) | ((in.read() & 0xff) << 8);
    }

    /**
     * read an unsigned int as a long
     */
    public long readUInt() throws IOException {
        return ((in.read() & 0xff)
                | ((in.read() & 0xff) << 8)
                | ((in.read() & 0xff) << 16)
                | (((long) (in.read() & 0xff)) << 24));
    }

    public boolean readBoolean() throws IOException {
        return (in.read() != 0);
    }

    public byte readByte() throws IOException {
        return (byte) in.read();
    }

    public int readUnsignedByte() throws IOException {
        return in.read();
    }

    public short readShort() throws IOException {
        return (short) this.readUnsignedShort();
    }

    public char readChar() throws IOException {
        return (char) this.readUnsignedShort();
    }

    public int readInt() throws IOException {
        return ((in.read() & 0xff)
                | ((in.read() & 0xff) << 8)
                | ((in.read() & 0xff) << 16)
                | ((in.read() & 0xff) << 24));
    }

    public long readLong() throws IOException {
        return ((in.read() & 0xff)
                | ((long) (in.read() & 0xff) << 8)
                | ((long) (in.read() & 0xff) << 16)
                | ((long) (in.read() & 0xff) << 24)
                | ((long) (in.read() & 0xff) << 32)
                | ((long) (in.read() & 0xff) << 40)
                | ((long) (in.read() & 0xff) << 48)
                | ((long) (in.read() & 0xff) << 56));
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public void readFully(byte b[]) throws IOException {
        in.read(b, 0, b.length);
    }

    public void readFully(byte b[], int off, int len) throws IOException {
        in.read(b, off, len);
    }

    public int skipBytes(int n) throws IOException {
        return (int) in.skip(n);
    }

    public String readLine() throws IOException {
        return inRead.readLine();
    }

    public String readUTF() throws IOException {
        throw new IOException("Unsupported operation");
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }
}
