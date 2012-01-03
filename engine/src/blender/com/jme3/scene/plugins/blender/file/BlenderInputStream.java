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
package com.jme3.scene.plugins.blender.file;

import com.jme3.asset.AssetManager;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * An input stream with random access to data.
 * @author Marcin Roguski
 */
public class BlenderInputStream extends InputStream {

    private static final Logger LOGGER = Logger.getLogger(BlenderInputStream.class.getName());
    /** The default size of the blender buffer. */
    private static final int DEFAULT_BUFFER_SIZE = 1048576;												//1MB
    /** The application's asset manager. */
    private AssetManager assetManager;
    /**
     * Size of a pointer; all pointers in the file are stored in this format. '_' means 4 bytes and '-' means 8 bytes.
     */
    private int pointerSize;
    /**
     * Type of byte ordering used; 'v' means little endian and 'V' means big endian.
     */
    private char endianess;
    /** Version of Blender the file was created in; '248' means version 2.48. */
    private String versionNumber;
    /** The buffer we store the read data to. */
    protected byte[] cachedBuffer;
    /** The total size of the stored data. */
    protected int size;
    /** The current position of the read cursor. */
    protected int position;
	/** The input stream we read the data from. */
	protected InputStream		inputStream;

    /**
     * Constructor. The input stream is stored and used to read data.
     * @param inputStream
     *        the stream we read data from
     * @param assetManager
     *        the application's asset manager
     * @throws BlenderFileException
     *         this exception is thrown if the file header has some invalid data
     */
    public BlenderInputStream(InputStream inputStream, AssetManager assetManager) throws BlenderFileException {
        this.assetManager = assetManager;
        this.inputStream = inputStream;
        //the size value will canche while reading the file; the available() method cannot be counted on
        try {
            size = inputStream.available();
        } catch (IOException e) {
            size = 0;
        }
        if (size <= 0) {
            size = BlenderInputStream.DEFAULT_BUFFER_SIZE;
        }

        //buffered input stream is used here for much faster file reading
        BufferedInputStream bufferedInputStream;
        if (inputStream instanceof BufferedInputStream) {
            bufferedInputStream = (BufferedInputStream) inputStream;
        } else {
            bufferedInputStream = new BufferedInputStream(inputStream);
        }

        try {
            this.readStreamToCache(bufferedInputStream);
        } catch (IOException e) {
            throw new BlenderFileException("Problems occured while caching the file!", e);
        }

        try {
            this.readFileHeader();
        } catch (BlenderFileException e) {//the file might be packed, don't panic, try one more time ;)
            this.decompressFile();
            this.position = 0;
            this.readFileHeader();
        }
    }

    /**
     * This method reads the whole stream into a buffer.
     * @param inputStream
     *        the stream to read the file data from
     * @throws IOException 
     * 		   an exception is thrown when data read from the stream is invalid or there are problems with i/o
     *         operations
     */
    private void readStreamToCache(InputStream inputStream) throws IOException {
        int data = inputStream.read();
        cachedBuffer = new byte[size];
        size = 0;//this will count the actual size
        while (data != -1) {
            cachedBuffer[size++] = (byte) data;
            if (size >= cachedBuffer.length) {//widen the cached array
                byte[] newBuffer = new byte[cachedBuffer.length + (cachedBuffer.length >> 1)];
                System.arraycopy(cachedBuffer, 0, newBuffer, 0, cachedBuffer.length);
                cachedBuffer = newBuffer;
            }
            data = inputStream.read();
        }
    }

    /**
     * This method is used when the blender file is gzipped. It decompresses the data and stores it back into the
     * cachedBuffer field.
     */
    private void decompressFile() {
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream(new ByteArrayInputStream(cachedBuffer));
            this.readStreamToCache(gis);
        } catch (IOException e) {
            throw new IllegalStateException("IO errors occured where they should NOT! "
                    + "The data is already buffered at this point!", e);
        } finally {
            try {
                if (gis != null) {
                    gis.close();
                }
            } catch (IOException e) {
                LOGGER.warning(e.getMessage());
            }
        }
    }

    /**
     * This method loads the header from the given stream during instance creation.
     * @param inputStream
     *        the stream we read the header from
     * @throws BlenderFileException
     *         this exception is thrown if the file header has some invalid data
     */
    private void readFileHeader() throws BlenderFileException {
        byte[] identifier = new byte[7];
        int bytesRead = this.readBytes(identifier);
        if (bytesRead != 7) {
            throw new BlenderFileException("Error reading header identifier. Only " + bytesRead + " bytes read and there should be 7!");
        }
        String strIdentifier = new String(identifier);
        if (!"BLENDER".equals(strIdentifier)) {
            throw new BlenderFileException("Wrong file identifier: " + strIdentifier + "! Should be 'BLENDER'!");
        }
        char pointerSizeSign = (char) this.readByte();
        if (pointerSizeSign == '-') {
            pointerSize = 8;
        } else if (pointerSizeSign == '_') {
            pointerSize = 4;
        } else {
            throw new BlenderFileException("Invalid pointer size character! Should be '_' or '-' and there is: " + pointerSizeSign);
        }
        endianess = (char) this.readByte();
        if (endianess != 'v' && endianess != 'V') {
            throw new BlenderFileException("Unknown endianess value! 'v' or 'V' expected and found: " + endianess);
        }
        byte[] versionNumber = new byte[3];
        bytesRead = this.readBytes(versionNumber);
        if (bytesRead != 3) {
            throw new BlenderFileException("Error reading version numberr. Only " + bytesRead + " bytes read and there should be 3!");
        }
        this.versionNumber = new String(versionNumber);
    }

    @Override
    public int read() throws IOException {
        return this.readByte();
    }

    /**
     * This method reads 1 byte from the stream.
     * It works just in the way the read method does.
     * It just not throw an exception because at this moment the whole file
     * is loaded into buffer, so no need for IOException to be thrown.
     * @return a byte from the stream (1 bytes read)
     */
    public int readByte() {
        return cachedBuffer[position++] & 0xFF;
    }

    /**
     * This method reads a bytes number big enough to fill the table. 
     * It does not throw exceptions so it is for internal use only.
     * @param bytes
     *            an array to be filled with data
     * @return number of read bytes (a length of array actually)
     */
    private int readBytes(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) this.readByte();
        }
        return bytes.length;
    }

    /**
     * This method reads 2-byte number from the stream.
     * @return a number from the stream (2 bytes read)
     */
    public int readShort() {
        int part1 = this.readByte();
        int part2 = this.readByte();
        if (endianess == 'v') {
            return (part2 << 8) + part1;
        } else {
            return (part1 << 8) + part2;
        }
    }

    /**
     * This method reads 4-byte number from the stream.
     * @return a number from the stream (4 bytes read)
     */
    public int readInt() {
        int part1 = this.readByte();
        int part2 = this.readByte();
        int part3 = this.readByte();
        int part4 = this.readByte();
        if (endianess == 'v') {
            return (part4 << 24) + (part3 << 16) + (part2 << 8) + part1;
        } else {
            return (part1 << 24) + (part2 << 16) + (part3 << 8) + part4;
        }
    }

    /**
     * This method reads 4-byte floating point number (float) from the stream.
     * @return a number from the stream (4 bytes read)
     */
    public float readFloat() {
        int intValue = this.readInt();
        return Float.intBitsToFloat(intValue);
    }

    /**
     * This method reads 8-byte number from the stream.
     * @return a number from the stream (8 bytes read)
     */
    public long readLong() {
        long part1 = this.readInt();
        long part2 = this.readInt();
        long result = -1;
        if (endianess == 'v') {
            result = part2 << 32 | part1;
        } else {
            result = part1 << 32 | part2;
        }
        return result;
    }

    /**
     * This method reads 8-byte floating point number (double) from the stream.
     * @return a number from the stream (8 bytes read)
     */
    public double readDouble() {
        long longValue = this.readLong();
        return Double.longBitsToDouble(longValue);
    }

    /**
     * This method reads the pointer value. Depending on the pointer size defined in the header, the stream reads either
     * 4 or 8 bytes of data.
     * @return the pointer value
     */
    public long readPointer() {
        if (pointerSize == 4) {
            return this.readInt();
        }
        return this.readLong();
    }

    /**
     * This method reads the string. It assumes the string is terminated with zero in the stream.
     * @return the string read from the stream
     */
    public String readString() {
        StringBuilder stringBuilder = new StringBuilder();
        int data = this.readByte();
        while (data != 0) {
            stringBuilder.append((char) data);
            data = this.readByte();
        }
        return stringBuilder.toString();
    }

    /**
     * This method sets the current position of the read cursor.
     * @param position
     *        the position of the read cursor
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * This method returns the position of the read cursor.
     * @return the position of the read cursor
     */
    public int getPosition() {
        return position;
    }

    /**
     * This method returns the blender version number where the file was created.
     * @return blender version number
     */
    public String getVersionNumber() {
        return versionNumber;
    }

    /**
     * This method returns the size of the pointer.
     * @return the size of the pointer
     */
    public int getPointerSize() {
        return pointerSize;
    }

    /**
     * This method returns the application's asset manager.
     * @return the application's asset manager
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * This method aligns cursor position forward to a given amount of bytes.
     * @param bytesAmount
     *        the byte amount to which we aligh the cursor
     */
    public void alignPosition(int bytesAmount) {
        if (bytesAmount <= 0) {
            throw new IllegalArgumentException("Alignment byte number shoulf be positivbe!");
        }
        long move = position % bytesAmount;
        if (move > 0) {
            position += bytesAmount - move;
        }
    }

    @Override
    public void close() throws IOException {
		inputStream.close();
//		cachedBuffer = null;
//		size = position = 0;
    }
}
