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

    @Override
    public String alGetString(int parameter) {
        return AL10.alGetString(parameter);
    }

    @Override
    public int alGenSources() {
        return AL10.alGenSources();
    }

    @Override
    public int alGetError() {
        return AL10.alGetError();
    }

    @Override
    public void alDeleteSources(int numSources, IntBuffer sources) {
        if (sources.position() != 0) throw new AssertionError();
        if (sources.limit() != numSources) throw new AssertionError();
        AL10.alDeleteSources(sources);
    }

    @Override
    public void alGenBuffers(int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alGenBuffers(buffers);
    }

    @Override
    public void alDeleteBuffers(int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alDeleteBuffers(buffers);
    }

    @Override
    public void alSourceStop(int source) {
        AL10.alSourceStop(source);
    }

    @Override
    public void alSourcei(int source, int param, int value) {
        AL10.alSourcei(source, param, value);
    }

    @Override
    public void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency) {
        if (data.position() != 0) throw new AssertionError();
        if (data.limit() != size) throw new AssertionError();
        AL10.alBufferData(buffer, format, data, frequency);
    }

    @Override
    public void alSourcePlay(int source) {
        AL10.alSourcePlay(source);
    }

    @Override
    public void alSourcePause(int source) {
        AL10.alSourcePause(source);
    }

    @Override
    public void alSourcef(int source, int param, float value) {
        AL10.alSourcef(source, param, value);
    }

    @Override
    public void alSource3f(int source, int param, float value1, float value2, float value3) {
        AL10.alSource3f(source, param, value1, value2, value3);
    }

    @Override
    public int alGetSourcei(int source, int param) {
        return AL10.alGetSourcei(source, param);
    }

    @Override
    public void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alSourceUnqueueBuffers(source, buffers);
    }

    @Override
    public void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alSourceQueueBuffers(source, buffers);
    }

    @Override
    public void alListener(int param, FloatBuffer data) {
        AL10.alListener(param, data);
    }

    @Override
    public void alListenerf(int param, float value) {
        AL10.alListenerf(param, value);
    }

    @Override
    public void alListener3f(int param, float value1, float value2, float value3) {
        AL10.alListener3f(param, value1, value2, value3);
    }

    @Override
    public void alSource3i(int source, int param, int value1, int value2, int value3) {
        AL11.alSource3i(source, param, value1, value2, value3);
    }

}
