/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.math.Vector3f;

/**
 *
 * @author normenhansen
 */
public interface AudioSource {
        /**
     * <code>Status</code> indicates the current status of the audio source.
     */
    public enum Status {
        /**
         * The audio source is currently playing. This will be set if
         * {@link AudioNode#play()} is called.
         */
        Playing,
        
        /**
         * The audio source is currently paused.
         */
        Paused,
        
        /**
         * The audio source is currently stopped.
         * This will be set if {@link AudioNode#stop()} is called 
         * or the audio has reached the end of the file.
         */
        Stopped,
    }

    
    /**
     * Do not use.
     *
     * @param channel the desired channel index, or -1 if stopped
     */
    public void setChannel(int channel);

    /**
     * Do not use.
     *
     * @return the channel index, or -1 if stopped
     */
    public int getChannel();

    /**
     * @return The {#link Filter dry filter} that is set.
     * @see AudioNode#setDryFilter(com.jme3.audio.Filter) 
     */
    public Filter getDryFilter();

    /**
     * @return The {@link AudioData} set previously with 
     * {@link AudioNode#setAudioData(com.jme3.audio.AudioData, com.jme3.audio.AudioKey)}
     * or any of the constructors that initialize the audio data.
     */
    public AudioData getAudioData();

    /**
     * Do not use.
     * 
     * @param status the desired Status
     */
    public void setStatus(Status status);
    
    /**
     * @return The {@link Status} of the audio source. 
     * The status will be changed when either the {@link AudioNode#play()}
     * or {@link AudioNode#stop()} methods are called.
     */
    public Status getStatus();

    /**
     * @return True if the audio will keep looping after it is done playing,
     * otherwise, false.
     * @see AudioNode#setLooping(boolean)
     */
    public boolean isLooping();

    /**
     * @return The pitch of the audio, also the speed of playback.
     * 
     * @see AudioNode#setPitch(float) 
     */
    public float getPitch();

    /**
     * @return The volume of this audio source.
     * 
     * @see AudioNode#setVolume(float)
     */
    public float getVolume();

    /**
     * @return the time offset in the sound sample to start playing
     */
    public float getTimeOffset();
    
    /**
     * @return the current playback position of the source in seconds.
     */
    public float getPlaybackTime();

    /**
     * @return The position of the audio source.
     */
    public Vector3f getPosition();
    
    /**
     * @return The velocity of the audio source.
     * 
     * @see AudioNode#setVelocity(com.jme3.math.Vector3f)
     */
    public Vector3f getVelocity();

    /**
     * @return True if reverb is enabled, otherwise false.
     * 
     * @see AudioNode#setReverbEnabled(boolean)
     */
    public boolean isReverbEnabled();

    /**
     * @return Filter for the reverberations of this audio source.
     * 
     * @see AudioNode#setReverbFilter(com.jme3.audio.Filter) 
     */
    public Filter getReverbFilter();

    /**
     * @return Maximum distance for this audio source.
     * 
     * @see AudioNode#setMaxDistance(float)
     */
    public float getMaxDistance();

    /**
     * @return The reference playing distance for the audio source.
     * 
     * @see AudioNode#setRefDistance(float) 
     */
    public float getRefDistance();

    /**
     * @return True if the audio source is directional
     * 
     * @see AudioNode#setDirectional(boolean) 
     */
    public boolean isDirectional();

    /**
     * @return The direction of this audio source.
     * 
     * @see AudioNode#setDirection(com.jme3.math.Vector3f)
     */
    public Vector3f getDirection();

    /**
     * @return The directional audio source, cone inner angle.
     * 
     * @see AudioNode#setInnerAngle(float) 
     */
    public float getInnerAngle();

    /**
     * @return The directional audio source, cone outer angle.
     * 
     * @see AudioNode#setOuterAngle(float) 
     */
    public float getOuterAngle();

    /**
     * @return True if the audio source is positional.
     * 
     * @see AudioNode#setPositional(boolean) 
     */
    public boolean isPositional();

}
