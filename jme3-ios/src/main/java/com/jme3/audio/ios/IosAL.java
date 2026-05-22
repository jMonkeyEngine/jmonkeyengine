package com.jme3.audio.ios;

import com.jme3.audio.openal.AL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class IosAL implements AL {
    public IosAL() {
    }

    @Override
    public String alGetString(int parameter) {
        return org.ngengine.libjglios.openal.ios.IosAL.alGetString(parameter);
    }

    @Override
    public int alGenSources() {
        return org.ngengine.libjglios.openal.ios.IosAL.alGenSources();
    }

    @Override
    public int alGetError() {
        return org.ngengine.libjglios.openal.ios.IosAL.alGetError();
    }

    @Override
    public void alDeleteSources(int numSources, IntBuffer sources) {
        org.ngengine.libjglios.openal.ios.IosAL.alDeleteSources(numSources, sources);
    }

    @Override
    public void alGenBuffers(int numBuffers, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosAL.alGenBuffers(numBuffers, buffers);
    }

    @Override
    public void alDeleteBuffers(int numBuffers, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosAL.alDeleteBuffers(numBuffers, buffers);
    }

    @Override
    public void alSourceStop(int source) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourceStop(source);
    }

    @Override
    public void alSourcei(int source, int param, int value) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourcei(source, param, value);
    }

    @Override
    public void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency) {
        org.ngengine.libjglios.openal.ios.IosAL.alBufferData(buffer, format, data, size, frequency);
    }

    @Override
    public void alSourcePlay(int source) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourcePlay(source);
    }

    @Override
    public void alSourcePause(int source) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourcePause(source);
    }

    @Override
    public void alSourcef(int source, int param, float value) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourcef(source, param, value);
    }

    @Override
    public void alSource3f(int source, int param, float value1, float value2, float value3) {
        org.ngengine.libjglios.openal.ios.IosAL.alSource3f(source, param, value1, value2, value3);
    }

    @Override
    public int alGetSourcei(int source, int param) {
        return org.ngengine.libjglios.openal.ios.IosAL.alGetSourcei(source, param);
    }

    @Override
    public void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourceUnqueueBuffers(source, numBuffers, buffers);
    }

    @Override
    public void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        org.ngengine.libjglios.openal.ios.IosAL.alSourceQueueBuffers(source, numBuffers, buffers);
    }

    @Override
    public void alListener(int param, FloatBuffer data) {
        org.ngengine.libjglios.openal.ios.IosAL.alListener(param, data);
    }

    @Override
    public void alListenerf(int param, float value) {
        org.ngengine.libjglios.openal.ios.IosAL.alListenerf(param, value);
    }

    @Override
    public void alListener3f(int param, float value1, float value2, float value3) {
        org.ngengine.libjglios.openal.ios.IosAL.alListener3f(param, value1, value2, value3);
    }

    @Override
    public void alSource3i(int source, int param, int value1, int value2, int value3) {
        org.ngengine.libjglios.openal.ios.IosAL.alSource3i(source, param, value1, value2, value3);
    }
}
