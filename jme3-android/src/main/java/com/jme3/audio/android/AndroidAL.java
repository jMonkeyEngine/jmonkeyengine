package com.jme3.audio.android;

import com.jme3.audio.openal.AL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class AndroidAL implements AL {

    public AndroidAL() {
    }

    public native String alGetString(int parameter);

    public native int alGenSources();

    public native int alGetError();

    public native void alDeleteSources(int numSources, IntBuffer sources);

    public native void alGenBuffers(int numBuffers, IntBuffer buffers);

    public native void alDeleteBuffers(int numBuffers, IntBuffer buffers);

    public native void alSourceStop(int source);

    public native void alSourcei(int source, int param, int value);

    public native void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency);

    public native void alSourcePlay(int source);

    public native void alSourcePause(int source);

    public native void alSourcef(int source, int param, float value);

    public native void alSource3f(int source, int param, float value1, float value2, float value3);

    public native int alGetSourcei(int source, int param);

    public native void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers);

    public native void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers);

    public native void alListener(int param, FloatBuffer data);

    public native void alListenerf(int param, float value);

    public native void alListener3f(int param, float value1, float value2, float value3);

    public native void alSource3i(int source, int param, int value1, int value2, int value3);

}
