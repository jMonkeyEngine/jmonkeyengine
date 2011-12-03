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

import com.jme3.math.FastMath;
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
public class LwjglSmoothingTimer extends Timer {
    private static final Logger logger = Logger.getLogger(LwjglSmoothingTimer.class
            .getName());

    private long lastFrameDiff;

    //frame rate parameters.
    private long oldTime;

    private float lastTPF, lastFPS;

    public static int TIMER_SMOOTHNESS = 32;

    private long[] tpf;

    private int smoothIndex;

    private final static long LWJGL_TIMER_RES = Sys.getTimerResolution();
    private final static float INV_LWJGL_TIMER_RES = ( 1f / LWJGL_TIMER_RES );
    private static float invTimerRezSmooth;

    public final static long LWJGL_TIME_TO_NANOS = (1000000000 / LWJGL_TIMER_RES);

    private long startTime;

    private boolean allSmooth = false;

    /**
     * Constructor builds a <code>Timer</code> object. All values will be
     * initialized to it's default values.
     */
    public LwjglSmoothingTimer() {
        reset();

        //print timer resolution info
        logger.log(Level.INFO, "Timer resolution: {0} ticks per second", LWJGL_TIMER_RES);
    }

    public void reset() {
        lastFrameDiff = 0;
        lastFPS = 0;
        lastTPF = 0;

        // init to -1 to indicate this is a new timer.
        oldTime = -1;
        //reset time
        startTime = Sys.getTime();

        tpf = new long[TIMER_SMOOTHNESS];
        smoothIndex = TIMER_SMOOTHNESS - 1;
        invTimerRezSmooth = ( 1f / (LWJGL_TIMER_RES * TIMER_SMOOTHNESS));

        // set tpf... -1 values will not be used for calculating the average in update()
        for ( int i = tpf.length; --i >= 0; ) {
            tpf[i] = -1;
        }
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
        long newTime = Sys.getTime();
        long oldTime = this.oldTime;
        this.oldTime = newTime;
        if ( oldTime == -1 ) {
            // For the first frame use 60 fps. This value will not be counted in further averages.
            // This is done so initialization code between creating the timer and the first
            // frame is not counted as a single frame on it's own.
            lastTPF = 1 / 60f;
            lastFPS = 1f / lastTPF;
            return;
        }

        long frameDiff = newTime - oldTime;
        long lastFrameDiff = this.lastFrameDiff;
        if ( lastFrameDiff > 0 && frameDiff > lastFrameDiff *100 ) {
            frameDiff = lastFrameDiff *100;
        }
        this.lastFrameDiff = frameDiff;
        tpf[smoothIndex] = frameDiff;
        smoothIndex--;
        if ( smoothIndex < 0 ) {
            smoothIndex = tpf.length - 1;
        }

        lastTPF = 0.0f;
        if (!allSmooth) {
            int smoothCount = 0;
            for ( int i = tpf.length; --i >= 0; ) {
                if ( tpf[i] != -1 ) {
                    lastTPF += tpf[i];
                    smoothCount++;
                }
            }
            if (smoothCount == tpf.length)
                allSmooth  = true;
            lastTPF *= ( INV_LWJGL_TIMER_RES / smoothCount );
        } else {
            for ( int i = tpf.length; --i >= 0; ) {
                if ( tpf[i] != -1 ) {
                    lastTPF += tpf[i];
                }
            }
            lastTPF *= invTimerRezSmooth;
        }
        if ( lastTPF < FastMath.FLT_EPSILON ) {
            lastTPF = FastMath.FLT_EPSILON;
        }

        lastFPS = 1f / lastTPF;
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