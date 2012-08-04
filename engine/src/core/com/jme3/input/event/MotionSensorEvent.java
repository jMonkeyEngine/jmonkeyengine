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

package com.jme3.input.event;

/**
 * Motion Sensor event.
 *
 * @author iwgeric
 */
public class MotionSensorEvent extends InputEvent {

    private int sensorType;
    private float x, y, z, dX, dY, dZ;

    public MotionSensorEvent(int sensorType, float x, float y, float z, float dX, float dY, float dZ) {
        this.sensorType = sensorType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dX = dX;
        this.dY = dY;
        this.dZ = dZ;
    }

    /**
     * Sensor Type
     * @return Sensor Type
     */
    public int getSensorType() {
        return sensorType;
    }

    /**
     * Current X coordinate
     * @return Current X coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * Current Y coordinate
     * @return Current Y coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * Current Z coordinate
     * @return Current Z coordinate
     */
    public float getZ() {
        return z;
    }

    /**
     * The change in X coordinate
     * @return change in X coordinate
     */
    public float getDX() {
        return dX;
    }

    /**
     * The change in Y coordinate
     *
     * @return change in Y coordinate
     */
    public float getDY() {
        return dY;
    }

    /**
     * The change in Z coordinate
     *
     * @return change in Z coordinate
     */
    public float getDZ() {
        return dZ;
    }

    @Override
    public String toString(){
        return "MotionSensor(Type="+sensorType+", X="+x+", Y="+y+", Z="+z+", DX="+dX+", DY="+dY+", DZ="+dZ+")";
    }

}
