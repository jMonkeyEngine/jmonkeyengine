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
package jme3test.model.anim;

import com.jme3.system.Timer;

/**
 * @author Nehon
 */
public class EraseTimer extends Timer {


    //private static final long TIMER_RESOLUTION = 1000L;
    //private static final float INVERSE_TIMER_RESOLUTION = 1f/1000L;
    private static final long TIMER_RESOLUTION = 1000000000L;
    private static final float INVERSE_TIMER_RESOLUTION = 1f / 1000000000L;

    private long startTime;
    private long previousTime;
    private float tpf;
    private float fps;

    public EraseTimer() {
        //startTime = System.currentTimeMillis();
        startTime = System.nanoTime();
    }

    /**
     * Returns the time in seconds. The timer starts
     * at 0.0 seconds.
     *
     * @return the current time in seconds
     */
    @Override
    public float getTimeInSeconds() {
        return getTime() * INVERSE_TIMER_RESOLUTION;
    }

    @Override
    public long getTime() {
        //return System.currentTimeMillis() - startTime;
        return System.nanoTime() - startTime;
    }

    @Override
    public long getResolution() {
        return TIMER_RESOLUTION;
    }

    @Override
    public float getFrameRate() {
        return fps;
    }

    @Override
    public float getTimePerFrame() {
        return tpf;
    }

    @Override
    public void update() {
        tpf = (getTime() - previousTime) * (1.0f / TIMER_RESOLUTION);
        if (tpf >= 0.2) {
            // The frame lasted more than 200ms, so we erase its time to 16ms.
            tpf = 0.016666f;
        } else {
            fps = 1.0f / tpf;
        }
        previousTime = getTime();
    }

    @Override
    public void reset() {
        //startTime = System.currentTimeMillis();
        startTime = System.nanoTime();
        previousTime = getTime();
    }


}
