/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;

public class FbxReader {

    /**
     * magic string at start:
     * "Kaydara FBX Binary\x20\x20\x00\x1a\x00"
     */
    public static final byte[] HEAD_MAGIC = new byte[]{0x4b, 0x61, 0x79, 0x64, 0x61, 0x72, 0x61, 0x20, 0x46, 0x42, 0x58, 0x20, 0x42, 0x69, 0x6e, 0x61, 0x72, 0x79, 0x20, 0x20, 0x00, 0x1a, 0x00};

    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private FbxReader() {
    }

    public static FbxFile readFBX(InputStream stream) throws IOException {
        FbxFile fbxFile = new FbxFile();
        // Read the file to a ByteBuffer, so we can determine positions in the file.
        ByteBuffer byteBuffer = readToByteBuffer(stream);
        try {
            stream.close();
        } catch(IOException e) {
        }
        // Check magic header
        byte[] magic = getBytes(byteBuffer, HEAD_MAGIC.length);
        if(!Arrays.equals(HEAD_MAGIC, magic))
            throw new IOException("Either ASCII FBX or corrupt file. "
                                            + "Only binary FBX files are supported");

        // Read version
        fbxFile.version = getUInt(byteBuffer);
        // Read root elements
        while(true) {
            FbxElement e = readFBXElement(byteBuffer, fbxFile);
            if(e == null)
                break;
            fbxFile.rootElements.add(e);
        }
        return fbxFile;
    }

    private static FbxElement readFBXElement(ByteBuffer byteBuffer, FbxFile file)
            throws IOException {
        long endOffset = getUInt(byteBuffer);
        if (file.hasExtendedOffsets()) {
            long upper = getUInt(byteBuffer);
            if (upper != 0L) {
                throw new IOException(
                        "Non-zero upper bytes: 0x" + Long.toHexString(upper));
            }
        }
        if(endOffset == 0)
            return null;

        long propCount = getUInt(byteBuffer);
        if (file.hasExtendedOffsets()) {
            long upper = getUInt(byteBuffer);
            if (upper != 0L) {
                throw new IOException(
                        "Non-zero upper bytes: 0x" + Long.toHexString(upper));
            }
        }

        getUInt(byteBuffer); // Properties length unused
        if (file.hasExtendedOffsets()) {
            long upper = getUInt(byteBuffer);
            if (upper != 0L) {
                throw new IOException(
                        "Non-zero upper bytes: 0x" + Long.toHexString(upper));
            }
        }

        FbxElement element = new FbxElement((int) propCount);
        element.id = new String(getBytes(byteBuffer, getUByte(byteBuffer)));
        
        for(int i = 0; i < propCount; ++i) {
            char dataType = readDataType(byteBuffer);
            element.properties.add(readData(byteBuffer, dataType));
            element.propertiesTypes[i] = dataType;
        }
        if(byteBuffer.position() < endOffset) {
            int blockSentinelLength = file.numSentinelBytes();
            while (byteBuffer.position() < (endOffset - blockSentinelLength)) {
                FbxElement child = readFBXElement(byteBuffer, file);
                if (child != null) {
                    element.children.add(child);
                }
            }

            if (!allZero(getBytes(byteBuffer, blockSentinelLength))) {
                throw new IOException("Block sentinel is corrupt: expected all zeros.");
            }
        }
        if(byteBuffer.position() != endOffset)
            throw new IOException("Data length not equal to expected");
        return element;
    }

    /**
     * Tests whether all bytes in the specified array are zero.
     *
     * @param array the array to test (not null, unaffected)
     * @return true if all zeroes, otherwise false
     */
    private static boolean allZero(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    private static Object readData(ByteBuffer byteBuffer, char dataType) throws IOException {
        switch(dataType) {
        case 'Y':
            return byteBuffer.getShort();
        case 'C':
            return byteBuffer.get() == 1;
        case 'I':
            return byteBuffer.getInt();
        case 'F':
            return byteBuffer.getFloat();
        case 'D':
            return byteBuffer.getDouble();
        case 'L':
            return byteBuffer.getLong();
        case 'R':
            return getBytes(byteBuffer, (int) getUInt(byteBuffer));
        case 'S':
            return new String(getBytes(byteBuffer, (int) getUInt(byteBuffer)));
        case 'f':
            return readArray(byteBuffer, 'f', 4);
        case 'i':
            return readArray(byteBuffer, 'i', 4);
        case 'd':
            return readArray(byteBuffer, 'd', 8);
        case 'l':
            return readArray(byteBuffer, 'l', 8);
        case 'b':
            return readArray(byteBuffer, 'b', 1);
        case 'c':
            return readArray(byteBuffer, 'c', 1);
        }
        throw new IOException("Unknown data type: " + dataType);
    }

    private static Object readArray(ByteBuffer byteBuffer, char type, int bytes) throws IOException {
        int count = (int) getUInt(byteBuffer);
        int encoding = (int) getUInt(byteBuffer);
        int length = (int) getUInt(byteBuffer);

        byte[] data = getBytes(byteBuffer, length);
        if(encoding == 1)
            data = inflate(data);
        if(data.length != count * bytes)
            throw new IOException("Wrong data length. Expected: " + count * bytes + ", got: " + data.length);
        ByteBuffer dis = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        switch(type) {
        case 'f':
            float[] arr = new float[count];
            for(int i = 0; i < count; ++i)
                arr[i] = dis.getFloat();
            return arr;
        case 'i':
            int[] arr2 = new int[count];
            for(int i = 0; i < count; ++i)
                arr2[i] = dis.getInt();
            return arr2;
        case 'd':
            double[] arr3 = new double[count];
            for(int i = 0; i < count; ++i)
                arr3[i] = dis.getDouble();
            return arr3;
        case 'l':
            long[] arr4 = new long[count];
            for(int i = 0; i < count; ++i)
                arr4[i] = dis.getLong();
            return arr4;
        case 'b':
            boolean[] arr5 = new boolean[count];
            for(int i = 0; i < count; ++i)
                arr5[i] = dis.get() == 1;
            return arr5;
        case 'c':
            int[] arr6 = new int[count];
            for(int i = 0; i < count; ++i)
                arr6[i] = dis.get() & 0xFF;
            return arr6;
        }
        throw new IOException("Unknown array data type: " + type);
    }

    private static byte[] inflate(byte[] input) throws IOException {
        InflaterInputStream gzis = new InflaterInputStream(new ByteArrayInputStream(input));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while(gzis.available() > 0) {
            int l = gzis.read(buffer);
            if(l > 0)
                out.write(buffer, 0, l);
        }
        return out.toByteArray();
    }

    private static char readDataType(ByteBuffer byteBuffer) {
        return (char) byteBuffer.get();
    }

    private static long getUInt(ByteBuffer byteBuffer) {
        return byteBuffer.getInt() & 0x00000000ffffffffL;
    }
    
    private static int getUByte(ByteBuffer byteBuffer) {
        return byteBuffer.get() & 0xFF;
    }

    private static byte[] getBytes(ByteBuffer byteBuffer, int size) {
        byte[] b = new byte[size];
        byteBuffer.get(b);
        return b;
    }

    private static ByteBuffer readToByteBuffer(InputStream input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        byte[] tmp = new byte[2048];
        while(true) {
            int r = input.read(tmp);
            if(r == -1)
                break;
            out.write(tmp, 0, r);
        }
        return ByteBuffer.wrap(out.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
    }
}
