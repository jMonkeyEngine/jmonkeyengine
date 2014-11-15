/*
 * Copyright (c) 2009-2014 jMonkeyEngine
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

package com.jme3.audio.joal;

import com.jme3.util.BufferUtils;
import com.jogamp.openal.AL;
import com.jogamp.openal.ALFactory;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Exposes OpenAL functions via JOAL.
 * 
 * @author Kirill Vainer
 */
public final class JoalAL implements com.jme3.audio.openal.AL {

    private final AL joalAl;
    
    public JoalAL() {
        this.joalAl = ALFactory.getAL();
    }

    public String alGetString(int parameter) {
        return joalAl.alGetString(parameter);
    }

    public int alGenSources() {
        IntBuffer ib = BufferUtils.createIntBuffer(1);
        joalAl.alGenSources(1, ib);
        return ib.get(0);
    }

    public int alGetError() {
        return joalAl.alGetError();
    }

    public void alDeleteSources(int numSources, IntBuffer sources) {
        joalAl.alDeleteSources(numSources, sources);
    }

    public void alGenBuffers(int numBuffers, IntBuffer buffers) {
        joalAl.alGenBuffers(numBuffers, buffers);
    }

    public void alDeleteBuffers(int numBuffers, IntBuffer buffers) {
        joalAl.alDeleteBuffers(numBuffers, buffers);
    }

    public void alSourceStop(int source) {
        joalAl.alSourceStop(source);
    }

    public void alSourcei(int source, int param, int value) {
        joalAl.alSourcei(source, param, value);
    }

    public void alBufferData(int buffer, int format, ByteBuffer data, int size, int frequency) {
        joalAl.alBufferData(buffer, format, data, size, frequency);
    }

    public void alSourcePlay(int source) {
        joalAl.alSourcePlay(source);
    }

    public void alSourcePause(int source) {
        joalAl.alSourcePause(source);
    }

    public void alSourcef(int source, int param, float value) {
        joalAl.alSourcef(source, param, value);
    }

    public void alSource3f(int source, int param, float value1, float value2, float value3) {
        joalAl.alSource3f(source, param, value1, value2, value3);
    }

    public int alGetSourcei(int source, int param) {
        IntBuffer ib = BufferUtils.createIntBuffer(1);
        joalAl.alGetSourcei(source, param, ib);
        return ib.get(0);
    }

    public void alSourceUnqueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        joalAl.alSourceUnqueueBuffers(source, numBuffers, buffers);
    }

    public void alSourceQueueBuffers(int source, int numBuffers, IntBuffer buffers) {
        joalAl.alSourceQueueBuffers(source, numBuffers, buffers);
    }

    public void alListener(int param, FloatBuffer data) {
        joalAl.alListenerfv(param, data);
    }

    public void alListenerf(int param, float value) {
        joalAl.alListenerf(param, value);
    }

    public void alListener3f(int param, float value1, float value2, float value3) {
        joalAl.alListener3f(param, value1, value2, value3);
    }

    public void alSource3i(int source, int param, int value1, int value2, int value3) {
        joalAl.alSource3i(source, param, value1, value2, value3);
    }
}
