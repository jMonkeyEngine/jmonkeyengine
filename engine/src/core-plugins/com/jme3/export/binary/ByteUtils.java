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

package com.jme3.export.binary;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <code>ByteUtils</code> is a helper class for converting numeric primitives
 * to and from byte representations.
 * 
 * @author Joshua Slack
 */
public class ByteUtils {

    /**
     * Takes an InputStream and returns the complete byte content of it
     * 
     * @param inputStream
     *            The input stream to read from
     * @return The byte array containing the data from the input stream
     * @throws java.io.IOException
     *             thrown if there is a problem reading from the input stream
     *             provided
     */
    public static byte[] getByteContent(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(
                16 * 1024);
        byte[] buffer = new byte[1024];
        int byteCount = -1;
        byte[] data = null;

        // Read the byte content into the output stream first
        while ((byteCount = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, byteCount);
        }

        // Set data with byte content from stream
        data = outputStream.toByteArray();

        // Release resources
        outputStream.close();

        return data;
    }

    
    // **********  byte <> short  METHODS  **********

    /**
     * Writes a short out to an OutputStream.
     * 
     * @param outputStream
     *            The OutputStream the short will be written to
     * @param value
     *            The short to write
     * @throws IOException
     *             Thrown if there is a problem writing to the OutputStream
     */
    public static void writeShort(OutputStream outputStream, short value)
            throws IOException {
        byte[] byteArray = convertToBytes(value);

        outputStream.write(byteArray);

        return;
    }

    public static byte[] convertToBytes(short value) {
        byte[] byteArray = new byte[2];

        byteArray[0] = (byte) (value >> 8);
        byteArray[1] = (byte) value;
        return byteArray;
    }

    /**
     * Read in a short from an InputStream
     * 
     * @param inputStream
     *            The InputStream used to read the short
     * @return A short, which is the next 2 bytes converted from the InputStream
     * @throws IOException
     *             Thrown if there is a problem reading from the InputStream
     */
    public static short readShort(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[2];

        // Read in the next 2 bytes
        inputStream.read(byteArray);

        short number = convertShortFromBytes(byteArray);

        return number;
    }

    public static short convertShortFromBytes(byte[] byteArray) {
        return convertShortFromBytes(byteArray, 0);
    }

    public static short convertShortFromBytes(byte[] byteArray, int offset) {
        // Convert it to a short
        short number = (short) ((byteArray[offset+1] & 0xFF) + ((byteArray[offset+0] & 0xFF) << 8));
        return number;
    }

    
    // **********  byte <> int  METHODS  **********

    /**
     * Writes an integer out to an OutputStream.
     * 
     * @param outputStream
     *            The OutputStream the integer will be written to
     * @param integer
     *            The integer to write
     * @throws IOException
     *             Thrown if there is a problem writing to the OutputStream
     */
    public static void writeInt(OutputStream outputStream, int integer)
            throws IOException {
        byte[] byteArray = convertToBytes(integer);

        outputStream.write(byteArray);

        return;
    }

    public static byte[] convertToBytes(int integer) {
        byte[] byteArray = new byte[4];

        byteArray[0] = (byte) (integer >> 24);
        byteArray[1] = (byte) (integer >> 16);
        byteArray[2] = (byte) (integer >> 8);
        byteArray[3] = (byte) integer;
        return byteArray;
    }

    /**
     * Read in an integer from an InputStream
     * 
     * @param inputStream
     *            The InputStream used to read the integer
     * @return An int, which is the next 4 bytes converted from the InputStream
     * @throws IOException
     *             Thrown if there is a problem reading from the InputStream
     */
    public static int readInt(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[4];

        // Read in the next 4 bytes
        inputStream.read(byteArray);

        int number = convertIntFromBytes(byteArray);

        return number;
    }

    public static int convertIntFromBytes(byte[] byteArray) {
        return convertIntFromBytes(byteArray, 0);
    }
    
    public static int convertIntFromBytes(byte[] byteArray, int offset) {
        // Convert it to an int
        int number = ((byteArray[offset] & 0xFF) << 24)
                + ((byteArray[offset+1] & 0xFF) << 16) + ((byteArray[offset+2] & 0xFF) << 8)
                + (byteArray[offset+3] & 0xFF);
        return number;
    }

    
    // **********  byte <> long  METHODS  **********
    
    /**
     * Writes a long out to an OutputStream.
     * 
     * @param outputStream
     *            The OutputStream the long will be written to
     * @param value
     *            The long to write
     * @throws IOException
     *             Thrown if there is a problem writing to the OutputStream
     */
    public static void writeLong(OutputStream outputStream, long value)
            throws IOException {
        byte[] byteArray = convertToBytes(value);

        outputStream.write(byteArray);

        return;
    }

    public static byte[] convertToBytes(long n) {
        byte[] bytes = new byte[8];

        bytes[7] = (byte) (n);
        n >>>= 8;
        bytes[6] = (byte) (n);
        n >>>= 8;
        bytes[5] = (byte) (n);
        n >>>= 8;
        bytes[4] = (byte) (n);
        n >>>= 8;
        bytes[3] = (byte) (n);
        n >>>= 8;
        bytes[2] = (byte) (n);
        n >>>= 8;
        bytes[1] = (byte) (n);
        n >>>= 8;
        bytes[0] = (byte) (n);

        return bytes;
    }
    
    /**
     * Read in a long from an InputStream
     * 
     * @param inputStream
     *            The InputStream used to read the long
     * @return A long, which is the next 8 bytes converted from the InputStream
     * @throws IOException
     *             Thrown if there is a problem reading from the InputStream
     */
    public static long readLong(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[8];

        // Read in the next 8 bytes
        inputStream.read(byteArray);

        long number = convertLongFromBytes(byteArray);

        return number;
    }

    public static long convertLongFromBytes(byte[] bytes) {
        return convertLongFromBytes(bytes, 0);
    }

    public static long convertLongFromBytes(byte[] bytes, int offset) {
        // Convert it to an long
        return    ((((long) bytes[offset+7]) & 0xFF) 
                + ((((long) bytes[offset+6]) & 0xFF) << 8)
                + ((((long) bytes[offset+5]) & 0xFF) << 16)
                + ((((long) bytes[offset+4]) & 0xFF) << 24)
                + ((((long) bytes[offset+3]) & 0xFF) << 32)
                + ((((long) bytes[offset+2]) & 0xFF) << 40)
                + ((((long) bytes[offset+1]) & 0xFF) << 48) 
                + ((((long) bytes[offset+0]) & 0xFF) << 56));
    }

    
    // **********  byte <> double  METHODS  **********
    
    /**
     * Writes a double out to an OutputStream.
     * 
     * @param outputStream
     *            The OutputStream the double will be written to
     * @param value
     *            The double to write
     * @throws IOException
     *             Thrown if there is a problem writing to the OutputStream
     */
    public static void writeDouble(OutputStream outputStream, double value)
            throws IOException {
        byte[] byteArray = convertToBytes(value);

        outputStream.write(byteArray);

        return;
    }

    public static byte[] convertToBytes(double n) {
        long bits = Double.doubleToLongBits(n);
        return convertToBytes(bits);
    }
    
    /**
     * Read in a double from an InputStream
     * 
     * @param inputStream
     *            The InputStream used to read the double
     * @return A double, which is the next 8 bytes converted from the InputStream
     * @throws IOException
     *             Thrown if there is a problem reading from the InputStream
     */
    public static double readDouble(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[8];

        // Read in the next 8 bytes
        inputStream.read(byteArray);

        double number = convertDoubleFromBytes(byteArray);

        return number;
    }

    public static double convertDoubleFromBytes(byte[] bytes) {
        return convertDoubleFromBytes(bytes, 0);
    }

    public static double convertDoubleFromBytes(byte[] bytes, int offset) {
        // Convert it to a double
        long bits = convertLongFromBytes(bytes, offset);
        return Double.longBitsToDouble(bits);
    }
    
    //  **********  byte <> float  METHODS  **********

    /**
     * Writes an float out to an OutputStream.
     * 
     * @param outputStream
     *            The OutputStream the float will be written to
     * @param fVal
     *            The float to write
     * @throws IOException
     *             Thrown if there is a problem writing to the OutputStream
     */
    public static void writeFloat(OutputStream outputStream, float fVal)
            throws IOException {
        byte[] byteArray = convertToBytes(fVal);

        outputStream.write(byteArray);

        return;
    }

    public static byte[] convertToBytes(float f) {
        int temp = Float.floatToIntBits(f);
        return convertToBytes(temp);
    }

    /**
     * Read in a float from an InputStream
     * 
     * @param inputStream
     *            The InputStream used to read the float
     * @return A float, which is the next 4 bytes converted from the InputStream
     * @throws IOException
     *             Thrown if there is a problem reading from the InputStream
     */
    public static float readFloat(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[4];

        // Read in the next 4 bytes
        inputStream.read(byteArray);

        float number = convertFloatFromBytes(byteArray);

        return number;
    }

    public static float convertFloatFromBytes(byte[] byteArray) {
        return convertFloatFromBytes(byteArray, 0); 
    }
    public static float convertFloatFromBytes(byte[] byteArray, int offset) {
        // Convert it to an int
        int number = convertIntFromBytes(byteArray, offset);
        return Float.intBitsToFloat(number);
    }

    
    
    //  **********  byte <> boolean  METHODS  **********

    /**
     * Writes a boolean out to an OutputStream.
     * 
     * @param outputStream
     *            The OutputStream the boolean will be written to
     * @param bVal
     *            The boolean to write
     * @throws IOException
     *             Thrown if there is a problem writing to the OutputStream
     */
    public static void writeBoolean(OutputStream outputStream, boolean bVal)
            throws IOException {
        byte[] byteArray = convertToBytes(bVal);

        outputStream.write(byteArray);

        return;
    }

    public static byte[] convertToBytes(boolean b) {
        byte[] rVal = new byte[1];
        rVal[0] = b ? (byte)1 : (byte)0;
        return rVal;
    }

    /**
     * Read in a boolean from an InputStream
     * 
     * @param inputStream
     *            The InputStream used to read the boolean
     * @return A boolean, which is the next byte converted from the InputStream (iow, byte != 0)
     * @throws IOException
     *             Thrown if there is a problem reading from the InputStream
     */
    public static boolean readBoolean(InputStream inputStream) throws IOException {
        byte[] byteArray = new byte[1];

        // Read in the next byte
        inputStream.read(byteArray);

        return convertBooleanFromBytes(byteArray);
    }

    public static boolean convertBooleanFromBytes(byte[] byteArray) {
        return convertBooleanFromBytes(byteArray, 0); 
    }
    public static boolean convertBooleanFromBytes(byte[] byteArray, int offset) {
        return byteArray[offset] != 0;
    }

    
    /**
     * Properly reads in data from the given stream until the specified number
     * of bytes have been read.
     * 
     * @param store
     *            the byte array to store in. Should have a length > bytes
     * @param bytes
     *            the number of bytes to read.
     * @param is
     *            the stream to read from
     * @return the store array for chaining purposes
     * @throws IOException
     *             if an error occurs while reading from the stream
     * @throws ArrayIndexOutOfBoundsException
     *             if bytes greater than the length of the store.
     */
    public static byte[] readData(byte[] store, int bytes, InputStream is) throws IOException {
        for (int i = 0; i < bytes; i++) {
            store[i] = (byte)is.read();
        }
        return store;
    }

    public static byte[] rightAlignBytes(byte[] bytes, int width) {
        if (bytes.length != width) {
            byte[] rVal = new byte[width];
            for (int x = width - bytes.length; x < width; x++) {
                rVal[x] = bytes[x - (width - bytes.length)];
            }
            return rVal;
        }
            
        return bytes;
    }

}
