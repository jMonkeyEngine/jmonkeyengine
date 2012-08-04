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

package com.jme3.input.controls;

import com.jme3.input.SensorInput;

/**
 * A <code>SensorTrigger</code> is used as a mapping to receive events
 * from a sensor.
 *
 * @author Kirill Vainer
 */
public class SensorTrigger implements Trigger {

//    private final SensorInput.Type sensorType;
    private final int sensorType;

    /**
     * Create a new <code>SensorTrigger</code> to receive sensor events.
     *
     * @param Sensor Type. See {@link SensorInput}.
     */
//    public SensorTrigger(SensorInput.Type sensorType) {
    public SensorTrigger(int sensorType) {
//        if  (sensorType == null)
        if  (sensorType < 0 || sensorType > 255)
            throw new IllegalArgumentException("Invalide Sensor Type");

        this.sensorType = sensorType;
    }

//    public SensorInput.Type getSensorType() {
    public int getSensorType() {
        return sensorType;
    }

    public String getName() {
        return sensorType + " Sensor";
    }

//    public static int sensorHash(SensorInput.Type sensorType){
    public static int sensorHash(int sensorType){
//        assert sensorType != null;
//        return 256 | (sensorType.ordinal() & 0xff);
        assert sensorType >= 0 && sensorType <= 255;
        return 1024 | (sensorType & 0xff);
    }

    public int triggerHashCode() {
        return sensorHash(sensorType);
    }

}
