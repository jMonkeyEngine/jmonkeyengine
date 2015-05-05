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
         * {@link AudioSource#play() } is called.
         */
        Playing,
        
        /**
         * The audio source is currently paused.
         */
        Paused,
        
        /**
         * The audio source is currently stopped.
         * This will be set if {@link AudioSource#stop() } is called 
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
     * @see AudioSource#setDryFilter(com.jme3.audio.Filter) 
     */
    public Filter getDryFilter();

    /**
     * @return The {@link AudioData} set previously with 
     * {@link AudioSource#setAudioData(com.jme3.audio.AudioData, com.jme3.audio.AudioKey) }
     * or any of the constructors that initialize the audio data.
     */
    public AudioData getAudioData();

    /**
     * Do not use.
     */
    public void setStatus(Status status);
    
    /**
     * @return The {@link Status} of the audio source. 
     * The status will be changed when either the {@link AudioSource#play() }
     * or {@link AudioSource#stop() } methods are called.
     */
    public Status getStatus();

    /**
     * @return True if the audio will keep looping after it is done playing,
     * otherwise, false.
     * @see AudioSource#setLooping(boolean)
     */
    public boolean isLooping();

    /**
     * @return The pitch of the audio, also the speed of playback.
     * 
     * @see AudioSource#setPitch(float) 
     */
    public float getPitch();

    /**
     * @return The volume of this audio source.
     * 
     * @see AudioSource#setVolume(float)
     */
    public float getVolume();

    /**
     * @return the time offset in the sound sample when to start playing.
     */
    public float getTimeOffset();
    
    /**
     * @return the current playback position of the source in seconds.
     */
    public float getPlaybackTime();

    /**
     * @return The velocity of the audio source.
     * 
     * @see AudioSource#setVelocity(com.jme3.math.Vector3f)
     */
    public Vector3f getPosition();
    
    /**
     * @return The velocity of the audio source.
     * 
     * @see AudioSource#setVelocity(com.jme3.math.Vector3f)
     */
    public Vector3f getVelocity();

    /**
     * @return True if reverb is enabled, otherwise false.
     * 
     * @see AudioSource#setReverbEnabled(boolean)
     */
    public boolean isReverbEnabled();

    /**
     * @return Filter for the reverberations of this audio source.
     * 
     * @see AudioSource#setReverbFilter(com.jme3.audio.Filter) 
     */
    public Filter getReverbFilter();

    /**
     * @return Max distance for this audio source.
     * 
     * @see AudioSource#setMaxDistance(float)
     */
    public float getMaxDistance();

    /**
     * @return The reference playing distance for the audio source.
     * 
     * @see AudioSource#setRefDistance(float) 
     */
    public float getRefDistance();

    /**
     * @return True if the audio source is directional
     * 
     * @see AudioSource#setDirectional(boolean) 
     */
    public boolean isDirectional();

    /**
     * @return The direction of this audio source.
     * 
     * @see AudioSource#setDirection(com.jme3.math.Vector3f)
     */
    public Vector3f getDirection();

    /**
     * @return The directional audio source, cone inner angle.
     * 
     * @see AudioSource#setInnerAngle(float) 
     */
    public float getInnerAngle();

    /**
     * @return The directional audio source, cone outer angle.
     * 
     * @see AudioSource#setOuterAngle(float) 
     */
    public float getOuterAngle();

    /**
     * @return True if the audio source is positional.
     * 
     * @see AudioSource#setPositional(boolean) 
     */
    public boolean isPositional();

}
