/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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

    public long getTime() {
        //return System.currentTimeMillis() - startTime;
        return System.nanoTime() - startTime;
    }

    public long getResolution() {
        return TIMER_RESOLUTION;
    }

    public float getFrameRate() {
        return fps;
    }

    public float getTimePerFrame() {
        return tpf;
    }

    public void update() {
        tpf = (getTime() - previousTime) * (1.0f / TIMER_RESOLUTION);
        if (tpf >= 0.2) {
            //the frame lasted more than 200ms we erase its time to 16ms.
            tpf = 0.016666f;
        } else {
            fps = 1.0f / tpf;
        }
        previousTime = getTime();
    }

    public void reset() {
        //startTime = System.currentTimeMillis();
        startTime = System.nanoTime();
        previousTime = getTime();
    }


}
