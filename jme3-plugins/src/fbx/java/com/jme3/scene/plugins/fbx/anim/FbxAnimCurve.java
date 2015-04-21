/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.anim;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.obj.FbxObject;

public class FbxAnimCurve extends FbxObject {

    private long[] keyTimes;
    private float[] keyValues;
    
    public FbxAnimCurve(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        
        for (FbxElement e : element.children) {
            if (e.id.equals("KeyTime")) {
                keyTimes = (long[]) e.properties.get(0);
            } else if (e.id.equals("KeyValueFloat")) {
                keyValues = (float[]) e.properties.get(0);
            }
        }
        
        long time = -1;
        for (int i = 0; i < keyTimes.length; i++) {
            if (time >= keyTimes[i]) {
                throw new UnsupportedOperationException("Keyframe times must be sequential, but they are not.");
            }
            time = keyTimes[i];
        }
    }

    /**
     * Get the times for the keyframes.
     * @return Keyframe times. 
     */
    public long[] getKeyTimes() {
        return keyTimes;
    }
    
    /**
     * Retrieve the curve value at the given time.
     * If the curve has no data, 0 is returned.
     * If the time is outside the curve, then the closest value is returned.
     * If the time isn't on an exact keyframe, linear interpolation is used
     * to determine the value between the keyframes at the given time.
     * @param time The time to get the curve value at (in FBX time units).
     * @return The value at the given time.
     */
    public float getValueAtTime(long time) {
        if (keyTimes.length == 0) {
            return 0;
        }
        
        // If the time is outside the range, 
        // we just return the closest value. (No extrapolation)
        if (time <= keyTimes[0]) {
            return keyValues[0];
        } else if (time >= keyTimes[keyTimes.length - 1]) {
            return keyValues[keyValues.length - 1];
        }
        
        

        int startFrame = 0;
        int endFrame = 1;
        int lastFrame = keyTimes.length - 1;
        
        for (int i = 0; i < lastFrame && keyTimes[i] < time; ++i) {
            startFrame = i;
            endFrame = i + 1;
        }
        
        long keyTime1    = keyTimes[startFrame];
        float keyValue1  = keyValues[startFrame];
        long keyTime2    = keyTimes[endFrame];
        float keyValue2  = keyValues[endFrame];
        
        if (keyTime2 == time) {
            return keyValue2;
        }
        
        long prevToNextDelta    = keyTime2 - keyTime1;
        long prevToCurrentDelta = time     - keyTime1;
        float lerpAmount = (float)prevToCurrentDelta / prevToNextDelta;
        
        return FastMath.interpolateLinear(lerpAmount, keyValue1, keyValue2);
    }

    @Override
    protected Object toJmeObject() {
        // An AnimCurve has no jME3 representation.
        // The parent AnimCurveNode is responsible to create the jME3 
        // representation.
        throw new UnsupportedOperationException("No jME3 object conversion available");
    }

    @Override
    public void connectObject(FbxObject object) {
        unsupportedConnectObject(object);
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        unsupportedConnectObjectProperty(object, property);
    }
    
}
