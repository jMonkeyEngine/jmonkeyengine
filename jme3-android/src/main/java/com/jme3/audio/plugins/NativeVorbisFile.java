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
        nativeInit();
    }
    
    public NativeVorbisFile(int fd, long off, long len) throws IOException {
        open(fd, off, len);
    }
    
    private native void open(int fd, long off, long len) throws IOException;
    
    public native void seekTime(double time) throws IOException;
    
    public native int read(byte[] buf, int off, int len) throws IOException;
    
    public native void readFully(ByteBuffer out) throws IOException;
    
    public native void close();
    
    public static native void nativeInit();
}
