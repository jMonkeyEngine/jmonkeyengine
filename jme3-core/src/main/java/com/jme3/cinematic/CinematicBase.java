package com.jme3.cinematic;

import com.jme3.animation.LoopMode;

/**
 * The base interface for cinematic. 
 */
public interface CinematicBase {

    /**
     * Starts the animation
     */
    public void play();

    /**
     * Stops the animation
     */
    public void stop();
    
    /**
     * this method can be implemented if the event needs different handling when 
     * stopped naturally (when the event reach its end)
     * or when it was forced stopped during playback
     * otherwise it just calls regular stop()
     */
    public void forceStop();

    /**
     * Pauses the animation
     */
    public void pause();

    /**
     * Returns the actual duration of the animation
     * @return the duration
     */
    public float getDuration();

    /**
     * Sets the speed of the animation (1 is normal speed, 2 is twice faster)
     *
     * @param speed the desired speed (default=1)
     */
    public void setSpeed(float speed);

    /**
     * returns the speed of the animation
     * @return the speed
     */
    public float getSpeed();

    /**
     * returns the PlayState of the animation
     * @return the plat state
     */
    public PlayState getPlayState();

    /**
     * @param loop Set the loop mode for the channel. The loop mode
     * determines what will happen to the animation once it finishes
     * playing.
     *
     * For more information, see the LoopMode enum class.
     * @see LoopMode
     */
    public void setLoopMode(LoopMode loop);

    /**
     * @return The loop mode currently set for the animation. The loop mode
     * determines what will happen to the animation once it finishes
     * playing.
     *
     * For more information, see the LoopMode enum class.
     * @see LoopMode
     */
    public LoopMode getLoopMode();

    /**
     * returns the initial duration of the animation at speed = 1 in seconds.
     * @return the initial duration
     */
    public float getInitialDuration();

    /**
     * Sets the duration of the animation at speed = 1, in seconds.
     *
     * @param initialDuration the desired duration (in de-scaled seconds)
     */
    public void setInitialDuration(float initialDuration);

    /**
     * Fast-forwards to the given time, where time=0 is the start of the event.
     *
     * @param time the time to fast-forward to
     */
    public void setTime(float time);    
   
    /**
     * returns the current time of the cinematic event
     * @return the time
     */
    public float getTime();

}
