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

import com.jme3.audio.AudioData.DataType;
import com.jme3.util.NativeObject;
import java.nio.ByteBuffer;

/**
 * An <code>AudioBuffer</code> is an implementation of AudioData
 * where the audio is buffered (stored in memory). All parts of it
 * are accessible at any time. <br/>
 * AudioBuffers are useful for short sounds, like effects, etc.
 *
 * @author Kirill Vainer
 */
public class AudioBuffer extends AudioData {

    /**
     * The audio data buffer. Should be direct and native ordered.
     */
    protected ByteBuffer audioData;

    public AudioBuffer(){
        super();
    }
    
    protected AudioBuffer(int id){
        super(id);
    }

    public DataType getDataType() {
        return DataType.Buffer;
    }

    /**
     * @return The duration of the audio in seconds. It is expected
     * that audio is uncompressed.
     */
    public float getDuration(){
        int bytesPerSec = (bitsPerSample / 8) * channels * sampleRate;
        if (audioData != null)
            return (float) audioData.capacity() / bytesPerSec;
        else
            return Float.NaN; // unknown
    }

    @Override
    public String toString(){
        return getClass().getSimpleName() +
               "[id="+id+", ch="+channels+", bits="+bitsPerSample +
               ", rate="+sampleRate+", duration="+getDuration()+"]";
    }

    /**
     * Update the data in the buffer with new data.
     * @param data
     */
    public void updateData(ByteBuffer data){
        this.audioData = data;
        updateNeeded = true;
    }

    /**
     * @return The buffered audio data.
     */
    public ByteBuffer getData(){
        return audioData;
    }

    public void resetObject() {
        id = -1;
        setUpdateNeeded();
    }

    public void deleteObject(AudioRenderer ar) {
        
    }

    @Override
    public void deleteObject(Object rendererObject) {
        ((AudioRenderer)rendererObject).deleteAudioData(this);
    }

    @Override
    public NativeObject createDestructableClone() {
        return new AudioBuffer(id);
    }

}
