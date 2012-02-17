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
 */
package com.jme3.cinematic.events;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.PlayState;
import com.jme3.export.Savable;

/**
 *
 * @author Nehon
 */
public interface CinematicEvent extends Savable {

    /**
     * Starts the animation
     */
    public void play();

    /**
     * Stops the animation
     */
    public void stop();

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
     * @param speed
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
     * Sets the duration of the antionamtion at speed = 1 in seconds
     * @param initialDuration
     */
    public void setInitialDuration(float initialDuration);

    /**
     * called internally in the update method, place here anything you want to run in the update loop
     * @param tpf time per frame
     */
    public void internalUpdate(float tpf);

    /**
     * initialize this event
     * @param app the application
     * @param cinematic the cinematic
     */
    public void initEvent(Application app, Cinematic cinematic);
    
    /**
     * When this method is invoked, the event should fast forward to the given time according tim 0 is the start of the event.
     * @param time the time to fast forward to
     */
    public void setTime(float time);    
   
    /**
     * returns the current time of the cinematic event
     * @return the time
     */
    public float getTime();
        
    
}
