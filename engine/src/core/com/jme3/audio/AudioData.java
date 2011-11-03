/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.audio;

import com.jme3.util.NativeObject;

/**
 * <code>AudioData</code> is an abstract representation
 * of audio data. There are two ways to handle audio data, short audio files
 * are to be stored entirely in memory, while long audio files (music) are
 * streamed from the hard drive as they are played.
 *
 * @author Kirill Vainer
 */
public abstract class AudioData extends NativeObject {

    protected int sampleRate;
    protected int channels;
    protected int bitsPerSample;

    public enum DataType {
        Buffer,
        Stream
    }
    
    public AudioData(){
        super(AudioData.class);
    }

    protected AudioData(int id){
        super(AudioData.class, id);
    }
    
    /**
     * @return The data type, either <code>Buffer</code> or <code>Stream</code>.
     */
    public abstract DataType getDataType();

    /**
     * @return the duration in seconds of the audio clip.
     */
    public abstract float getDuration();

    /**
     * @return Bits per single sample from a channel.
     */
    public int getBitsPerSample() {
        return bitsPerSample;
    }

    /**
     * @return Number of channels. 1 for mono, 2 for stereo, etc.
     */
    public int getChannels() {
        return channels;
    }

    /**
     * @return The sample rate, or how many samples per second.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Setup the format of the audio data.
     * @param channels # of channels, 1 = mono, 2 = stereo
     * @param bitsPerSample Bits per sample, e.g 8 bits, 16 bits.
     * @param sampleRate Sample rate, 44100, 22050, etc.
     */
    public void setupFormat(int channels, int bitsPerSample, int sampleRate){
        if (id != -1)
            throw new IllegalStateException("Already set up");

        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
    }

}
