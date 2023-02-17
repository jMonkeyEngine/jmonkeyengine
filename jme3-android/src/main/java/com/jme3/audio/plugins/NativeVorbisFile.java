package com.jme3.audio.plugins;

import java.io.IOException;
import java.nio.ByteBuffer;

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
     * Initializes an ogg vorbis native file from a file descriptor [fd].
     * 
     * @param fd an integer representing the file descriptor 
     * @param offset an integer representing the start of the 
     * @param length an integer representing the length of the 
     * @throws IOException
     */
    public NativeVorbisFile(int fd, long offset, long length) throws IOException {
        init(fd, offset, length);
    }
    
    private native void init(int fd, long offset, long length) throws IOException;
    
    public native void seekTime(double time) throws IOException;
    
    public native int read(byte[] buf, int off, int len) throws IOException;
    
    public native void readFully(ByteBuffer out) throws IOException;
    
    /**
     * Clears the native resources by calling free() destroying the structure defining this buffer.
     */
    public native void clearResources();
    
    public static native void preInit();
}
