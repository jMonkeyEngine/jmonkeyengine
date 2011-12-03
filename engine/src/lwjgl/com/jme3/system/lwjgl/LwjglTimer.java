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

package com.jme3.system.lwjgl;

import com.jme3.system.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.Sys;

/**
 * <code>Timer</code> handles the system's time related functionality. This
 * allows the calculation of the framerate. To keep the framerate calculation
 * accurate, a call to update each frame is required. <code>Timer</code> is a
 * singleton object and must be created via the <code>getTimer</code> method.
 *
 * @author Mark Powell
 * @version $Id: LWJGLTimer.java,v 1.21 2007/09/22 16:46:35 irrisor Exp $
 */
public class LwjglTimer extends Timer {
    private static final Logger logger = Logger.getLogger(LwjglTimer.class
            .getName());

    //frame rate parameters.
    private long oldTime;
    private long startTime;

    private float lastTPF, lastFPS;

    private final static long LWJGL_TIMER_RES = Sys.getTimerResolution();
    private final static float INV_LWJGL_TIMER_RES = ( 1f / LWJGL_TIMER_RES );

    public final static long LWJGL_TIME_TO_NANOS = (1000000000 / LWJGL_TIMER_RES);

    /**
     * Constructor builds a <code>Timer</code> object. All values will be
     * initialized to it's default values.
     */
    public LwjglTimer() {
        reset();
        logger.log(Level.INFO, "Timer resolution: {0} ticks per second", LWJGL_TIMER_RES);
    }

    public void reset() {
        startTime = Sys.getTime();
        oldTime = getTime();
    }

    @Override
    public float getTimeInSeconds() {
        return getTime() * INV_LWJGL_TIMER_RES;
    }

    /**
     * @see com.jme.util.Timer#getTime()
     */
    public long getTime() {
        return Sys.getTime() - startTime;
    }

    /**
     * @see com.jme.util.Timer#getResolution()
     */
    public long getResolution() {
        return LWJGL_TIMER_RES;
    }

    /**
     * <code>getFrameRate</code> returns the current frame rate since the last
     * call to <code>update</code>.
     *
     * @return the current frame rate.
     */
    public float getFrameRate() {
        return lastFPS;
    }

    public float getTimePerFrame() {
        return lastTPF;
    }

    /**
     * <code>update</code> recalulates the frame rate based on the previous
     * call to update. It is assumed that update is called each frame.
     */
    public void update() {
        long curTime = getTime();
        lastTPF = (curTime - oldTime) * (1.0f / LWJGL_TIMER_RES);
        lastFPS = 1.0f / lastTPF;
        oldTime = curTime;
    }

    /**
     * <code>toString</code> returns the string representation of this timer
     * in the format: <br>
     * <br>
     * jme.utility.Timer@1db699b <br>
     * Time: {LONG} <br>
     * FPS: {LONG} <br>
     *
     * @return the string representation of this object.
     */
    @Override
    public String toString() {
        String string = super.toString();
        string += "\nTime: " + oldTime;
        string += "\nFPS: " + getFrameRate();
        return string;
    }
}