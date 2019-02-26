/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
     */
    public void setChannel(int channel);

    /**
     * Do not use.
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
     * @return Max distance for this audio source.
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
