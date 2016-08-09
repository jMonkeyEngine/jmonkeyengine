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

import com.jme3.animation.AnimationUtils;
import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.PlayState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This call contains the basic behavior of a cinematic event.
 * Every cinematic event must extend this class.
 * 
 * A cinematic event must be given an initial duration in seconds 
 * (duration of the event at speed = 1). Default is 10 sec.
 * @author Nehon
 */
public abstract class AbstractCinematicEvent implements CinematicEvent {

    protected PlayState playState = PlayState.Stopped;
    protected LoopMode loopMode = LoopMode.DontLoop;
    protected float initialDuration = 10;
    protected float speed = 1;
    protected float time = 0;
    protected boolean resuming = false;
    
    /**
     * The list of listeners.
     */
    protected List<CinematicEventListener> listeners;

    /**
     * Construct a cinematic event (empty constructor).
     */
    public AbstractCinematicEvent() {
    }

    /**
     * Construct a cinematic event with the given initial duration.
     * @param initialDuration 
     */
    public AbstractCinematicEvent(float initialDuration) {
        this.initialDuration = initialDuration;
    }

    /**
     * Construct a cinematic event with the given loopMode.
     * @param loopMode 
     */
    public AbstractCinematicEvent(LoopMode loopMode) {
        this.loopMode = loopMode;
    }

    /**
     * Construct a cinematic event with the given loopMode and the given initialDuration.
     * @param initialDuration the duration of the event at speed = 1.
     * @param loopMode the loop mode of the event.
     */
    public AbstractCinematicEvent(float initialDuration, LoopMode loopMode) {
        this.initialDuration = initialDuration;
        this.loopMode = loopMode;
    }
    
    /**
     * Implement this method if the event needs different handling when 
     * stopped naturally (when the event reach its end),
     * or when it was force-stopped during playback.
     * By default, this method just calls regular stop().
     */
    public void forceStop(){
        stop();
    }

    /**
     * Play this event.
     */
    public void play() {
        onPlay();        
        playState = PlayState.Playing;
        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                CinematicEventListener cel = listeners.get(i);
                cel.onPlay(this);
            }
        }
    }

    /**
     * Implement this method with code that you want to execute when the event is started.
     */
    protected abstract void onPlay();

    /**
     * Used internally only.
     * @param tpf time per frame.
     */
    public void internalUpdate(float tpf) {
        if (playState == PlayState.Playing) {
            time = time + (tpf * speed);         
            onUpdate(tpf);
            if (time >= initialDuration && loopMode == LoopMode.DontLoop) {
                stop();
            } else if(time >= initialDuration && loopMode == LoopMode.Loop){
                setTime(0);
            }else{
                time = AnimationUtils.clampWrapTime(time, initialDuration, loopMode);
                if(time<0){
                    speed = - speed;
                    time = - time;
                }
            }
        }

    }

    /**
     * Implement this method with the code that you want to execute on update 
     * (only called when the event is playing).
     * @param tpf time per frame
     */
    protected abstract void onUpdate(float tpf);

    /**
     * Stops the animation. 
     * Next time when play() is called, the animation starts from the beginning.
     */
    public void stop() {
        onStop();
        time = 0;
        playState = PlayState.Stopped;
        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                CinematicEventListener cel = listeners.get(i);
                cel.onStop(this);
            }
        }
    }

    /**
     * Implement this method with code that you want to execute when the event is stopped.
     */
    protected abstract void onStop();

    /**
     * Pause this event.
     * Next time when play() is called, the animation restarts from here.
     */
    public void pause() {
        onPause();
        playState = PlayState.Paused;
        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                CinematicEventListener cel = listeners.get(i);
                cel.onPause(this);
            }
        }
    }

    /**
     * Implement this method with code that you want to execute when the event is paused.
     */
    public abstract void onPause();

    /**
     * Returns the actual duration of the animation (initialDuration/speed)
     * @return
     */
    public float getDuration() {
        return initialDuration / speed;
    }

    /**
     * Sets the speed of the animation.
     * At speed = 1, the animation will last initialDuration seconds,
     * At speed = 2, the animation will last initialDuration/2...
     * @param speed
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * Returns the speed of the animation.
     * @return
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Returns the current playstate of the animation (playing or paused or stopped).
     * @return
     */
    public PlayState getPlayState() {
        return playState;
    }

    /**
     * Returns the initial duration of the animation at speed = 1 in seconds.
     * @return
     */
    public float getInitialDuration() {
        return initialDuration;
    }

    /**
     * Sets the duration of the animation at speed = 1 in seconds.
     * @param initialDuration
     */
    public void setInitialDuration(float initialDuration) {
        this.initialDuration = initialDuration;
    }

    /**
     * Returns the loopMode of the animation.
     * @see LoopMode
     * @return
     */
    public LoopMode getLoopMode() {
        return loopMode;
    }

    /**
     * Sets the loopMode of the animation.
     * @see LoopMode
     * @param loopMode
     */
    public void setLoopMode(LoopMode loopMode) {
        this.loopMode = loopMode;
    }

    /**
     * Used for serialization only.
     * @param ex exporter
     * @throws IOException 
     */
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(playState, "playState", PlayState.Stopped);
        oc.write(speed, "speed", 1);
        oc.write(initialDuration, "initalDuration", 10);
        oc.write(loopMode, "loopMode", LoopMode.DontLoop);
    }

    /**
     * Used for serialization only.
     * @param im importer
     * @throws IOException 
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        playState = ic.readEnum("playState", PlayState.class, PlayState.Stopped);
        speed = ic.readFloat("speed", 1);
        initialDuration = ic.readFloat("initalDuration", 10);
        loopMode = ic.readEnum("loopMode", LoopMode.class, LoopMode.DontLoop);
    }

    /**
     * Initialize this event (called internally only).
     * @param app
     * @param cinematic 
     */
    public void initEvent(Application app, Cinematic cinematic) {
    }

    /**
     * Returns the list of CinematicEventListeners added to this event.
     * @return 
     */
    private List<CinematicEventListener> getListeners() {
        if (listeners == null) {
            listeners = new ArrayList<CinematicEventListener>();
        }
        return listeners;
    }

    /**
     * Add a CinematicEventListener to this event.
     * @param listener CinematicEventListener
     */
    public void addListener(CinematicEventListener listener) {
        getListeners().add(listener);
    }

    /**
     * Remove a CinematicEventListener from this event.
     * @param listener CinematicEventListener
     */
    public void removeListener(CinematicEventListener listener) {
        getListeners().remove(listener);
    }

    /**
     * Fast-forward the event to the given timestamp. Time=0 is the start of the event.
     * @param time the time to fast forward to.
     */
    public void setTime(float time) {
        this.time = time ;
    }

    /**
     * Return the current timestamp of the event. Time=0 is the start of the event.
     */
    public float getTime() {
        return time;
    }

    public void dispose() {    
    }
    
    
}
