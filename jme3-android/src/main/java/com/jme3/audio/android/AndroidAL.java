package com.jme3.audio.android;

import com.jme3.audio.openal.AL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class AndroidAL implements AL {

    public AndroidAL() {
    }

    @Override
    public native String alGetString(int parameter);

    @Override
    public native int alGenSources();

    @Override
    public native int alGetError();

    @Override
    public native void alDeleteSources(int numSources, IntBuffer sources);

    @Override
    public native void alGenBuffers(int numBuffers, IntBuffer buffers);

    @Override
    public native void alDeleteBuffers(int numBuffers, IntBuffer buffers);

    @Override
    public native void alSourceStop(int source);

    @Override
    public native void alSourcei(int source, int param, int value);

    @Override
    public native void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency);

    @Override
    public native void alSourcePlay(int source);

    @Override
    public native void alSourcePause(int source);

    @Override
    public native void alSourcef(int source, int param, float value);

    @Override
    public native void alSource3f(int source, int param, float value1, float value2, float value3);

    @Override
    public native int alGetSourcei(int source, int param);

    @Override
    public native void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers);

    @Override
    public native void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers);

    @Override
    public native void alListener(int param, FloatBuffer data);

    @Override
    public native void alListenerf(int param, float value);

    @Override
    public native void alListener3f(int param, float value1, float value2, float value3);

    @Override
    public native void alSource3i(int source, int param, int value1, int value2, int value3);

}
