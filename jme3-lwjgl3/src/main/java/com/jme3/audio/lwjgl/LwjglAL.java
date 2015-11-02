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
 */package com.jme3.audio.lwjgl;

import com.jme3.audio.openal.AL;
import com.jme3.system.NativeLibraryLoader;
import com.jme3.system.Platform;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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
        AL10.alListenerfv(param, data);
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
