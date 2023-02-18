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
package com.jme3.audio.plugins;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Represents the android implementation for the native <a href="https://xiph.org"/> vorbis file decoder.
 * This decoder initializes an OggVorbis_File from an already opened file designated by the {@link NativeVorbisFile#fd}.
 * <br/>
 * 
 * Code by Kirill Vainer.
 * <br/>
 * 
 * Modified by pavl_g.
 */
public class NativeVorbisFile {
    
    public int fd;
    public ByteBuffer ovf;
    public boolean seekable;
    public int channels;
    public int sampleRate;
    public int bitRate;
    public int totalBytes;
    public float duration;
    
    static {
        System.loadLibrary("decodejme");
        preInit();
    }
    
    /**
     * Initializes an ogg vorbis native file from a file descriptor [fd] of an already opened file.
     * 
     * @param fd an integer representing the file descriptor 
     * @param offset an integer indicating the start of the buffer
     * @param length an integer indicating the end of the buffer
     * @throws IOException in cases of a failure to initialize the vorbis file 
     */
    public NativeVorbisFile(int fd, long offset, long length) throws IOException {
        init(fd, offset, length);
    }
    
    /**
     * Seeks to a playback time relative to the decompressed pcm (Pulse-code modulation) stream.
     * 
     * @param time the playback seek time
     * @throws IOException if the seek is not successful
     */
    public native void seekTime(double time) throws IOException;
    
    /**
     * Reads the vorbis file into a primitive byte buffer [buf].
     * 
     * @param buffer a primitive byte buffer to read the data into it
     * @param offset an integer representing the offset or the start of the data read
     * @param length an integer representing the end of the data read
     * @return the number of the read bytes, (-1) if the reading has failed indicating an EOF, 
     *         returns (0) if the reading has failed or the primitive [buffer] passed is null
     * @throws IOException if the library has failed to read the file into the [out] buffer
     *                     or if the java primitive byte array [buffer] is inaccessible
     */
    public native int readIntoArray(byte[] buffer, int offset, int length) throws IOException;
    
    /**
     * Reads the vorbis file into a direct {@link java.nio.ByteBuffer}, starting from offset [0] till the buffer capacity.
     * 
     * @param out a reference to the output direct buffer 
     * @throws IOException if a premature EOF is encountered before reaching the end of the buffer
     *                     or if the library has failed to read the file into the [out] buffer
     */
    public native void readIntoBuffer(ByteBuffer out) throws IOException;
    
    /**
     * Clears the native resources and destroys the buffer {@link NativeVorbisFile#ovf} reference.
     */
    public native void clearResources();
    
    /**
     * Prepares the java fields for the native environment.
     */
    private static native void preInit();
    
    /**
     * Initializes an ogg vorbis native file from a file descriptor [fd] of an already opened file.
     * 
     * @param fd an integer representing the file descriptor 
     * @param offset an integer representing the start of the buffer
     * @param length an integer representing the length of the buffer
     * @throws IOException in cases of a failure to initialize the vorbis file 
     */
    private native void init(int fd, long offset, long length) throws IOException;
}
