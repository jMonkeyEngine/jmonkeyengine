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
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * The LWJGL implementation of {@link AL}.
 */
public final class LwjglAL implements AL {

    public LwjglAL() {
    }

    @Override
    public String alGetString(final int parameter) {
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
    public void alDeleteSources(final int numSources, final IntBuffer sources) {
        if (sources.position() != 0) throw new AssertionError();
        if (sources.limit() != numSources) throw new AssertionError();
        AL10.alDeleteSources(sources);
    }

    @Override
    public void alGenBuffers(final int numBuffers, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alGenBuffers(buffers);
    }

    @Override
    public void alDeleteBuffers(final int numBuffers, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alDeleteBuffers(buffers);
    }

    @Override
    public void alSourceStop(final int source) {
        AL10.alSourceStop(source);
    }

    @Override
    public void alSourcei(final int source, final int param, final int value) {
        AL10.alSourcei(source, param, value);
    }

    @Override
    public void alBufferData(final int buffer, final int format, final ByteBuffer data, final int size, final int frequency) {
        if (data.position() != 0) throw new AssertionError();
        if (data.limit() != size) throw new AssertionError();
        AL10.alBufferData(buffer, format, data, frequency);
    }

    @Override
    public void alSourcePlay(final int source) {
        AL10.alSourcePlay(source);
    }

    @Override
    public void alSourcePause(final int source) {
        AL10.alSourcePause(source);
    }

    @Override
    public void alSourcef(final int source, final int param, final float value) {
        AL10.alSourcef(source, param, value);
    }

    @Override
    public void alSource3f(final int source, final int param, final float value1, final float value2, final float value3) {
        AL10.alSource3f(source, param, value1, value2, value3);
    }

    @Override
    public int alGetSourcei(final int source, final int param) {
        return AL10.alGetSourcei(source, param);
    }

    @Override
    public void alSourceUnqueueBuffers(final int source, final int numBuffers, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alSourceUnqueueBuffers(source, buffers);
    }

    @Override
    public void alSourceQueueBuffers(final int source, final int numBuffers, final IntBuffer buffers) {
        if (buffers.position() != 0) throw new AssertionError();
        if (buffers.limit() != numBuffers) throw new AssertionError();
        AL10.alSourceQueueBuffers(source, buffers);
    }

    @Override
    public void alListener(final int param, final FloatBuffer data) {
        AL10.alListenerfv(param, data);
    }

    @Override
    public void alListenerf(final int param, final float value) {
        AL10.alListenerf(param, value);
    }

    @Override
    public void alListener3f(final int param, final float value1, final float value2, final float value3) {
        AL10.alListener3f(param, value1, value2, value3);
    }

    @Override
    public void alSource3i(final int source, final int param, final int value1, final int value2, final int value3) {
        AL11.alSource3i(source, param, value1, value2, value3);
    }
}
