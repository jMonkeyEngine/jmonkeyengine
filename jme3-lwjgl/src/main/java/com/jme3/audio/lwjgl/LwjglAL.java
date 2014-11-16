package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.AL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

public final class LwjglAL implements AL {
    
    public LwjglAL() {
    }

    public String alGetString(int parameter) {
        return AL10.alGetString(parameter);
    }

    public int alGenSources() {
        return AL10.alGenSources();
    }

    public int alGetError() {
        return AL10.alGetError();
    }

    public void alDeleteSources(int numSources, IntBuffer sources) {
        if (sources.position() != 0) throw new AssertionError();
        if (sources.limit() != numSources) throw new AssertionError();
        AL10.alDeleteSources(sources);
    }

    public void alGenBuffers(int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alGenBuffers(buffers);
    }

    public void alDeleteBuffers(int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alDeleteBuffers(buffers);
    }

    public void alSourceStop(int source) {
        AL10.alSourceStop(source);
    }

    public void alSourcei(int source, int param, int value) {
        AL10.alSourcei(source, param, value);
    }

    public void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency) {
        if (data.position() != 0) throw new AssertionError();
        if (data.limit() != size) throw new AssertionError();
        AL10.alBufferData(buffer, format, data, frequency);
    }

    public void alSourcePlay(int source) {
        AL10.alSourcePlay(source);
    }

    public void alSourcePause(int source) {
        AL10.alSourcePause(source);
    }

    public void alSourcef(int source, int param, float value) {
        AL10.alSourcef(source, param, value);
    }

    public void alSource3f(int source, int param, float value1, float value2, float value3) {
        AL10.alSource3f(source, param, value1, value2, value3);
    }

    public int alGetSourcei(int source, int param) {
        return AL10.alGetSourcei(source, param);
    }

    public void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alSourceUnqueueBuffers(source, buffers);
    }

    public void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alSourceQueueBuffers(source, buffers);
    }

    public void alListener(int param, FloatBuffer data) {
        AL10.alListener(param, data);
    }

    public void alListenerf(int param, float value) {
        AL10.alListenerf(param, value);
    }

    public void alListener3f(int param, float value1, float value2, float value3) {
        AL10.alListener3f(param, value1, value2, value3);
    }

    public void alSource3i(int source, int param, int value1, int value2, int value3) {
        AL11.alSource3i(source, param, value1, value2, value3);
    }

}
