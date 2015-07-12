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
package com.jme3.scene.plugins.fbx.misc;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxGlobalSettings {
    
    private static final Logger logger = Logger.getLogger(FbxGlobalSettings.class.getName());
    
    private static final Map<Integer, Float> timeModeToFps = new HashMap<Integer, Float>();
    
    static {
        timeModeToFps.put(1,  120f);
        timeModeToFps.put(2,  100f);
        timeModeToFps.put(3,   60f);
        timeModeToFps.put(4,   50f);
        timeModeToFps.put(5,   48f);
        timeModeToFps.put(6,   30f);
        timeModeToFps.put(9,   30f / 1.001f);
        timeModeToFps.put(10,  25f);
        timeModeToFps.put(11,  24f);
        timeModeToFps.put(13,  24f / 1.001f);
        timeModeToFps.put(14, -1f);
        timeModeToFps.put(15,  96f);
        timeModeToFps.put(16,  72f);
        timeModeToFps.put(17,  60f / 1.001f);
    }
    
    public float unitScaleFactor  = 1.0f;
    public ColorRGBA ambientColor = ColorRGBA.Black;
    public float frameRate  = 25.0f;
    
    /**
     * @return A {@link Transform} that converts from the FBX file coordinate
     * system to jME3 coordinate system. 
     * jME3's coordinate system is:
     * <ul>
     * <li>Units are specified in meters.</li>
     * <li>Orientation is right-handed with Y-up.</li>
     * </ul>
     */
    public Transform getGlobalTransform() {
        // Default unit scale factor is 1 (centimeters),
        // convert to meters.
        float scale = unitScaleFactor / 100.0f;
        
        // TODO: handle rotation
        
        return new Transform(Vector3f.ZERO, Quaternion.IDENTITY, new Vector3f(scale, scale, scale));
    }
    
    public void fromElement(FbxElement element) {
        // jME3 uses a +Y up, -Z forward coordinate system (same as OpenGL)
        // Luckily enough, this is also the default for FBX models.
        
        int timeMode = -1;
        float customFrameRate = 30.0f;
        
        for (FbxElement e2 : element.getFbxProperties()) {
            String propName = (String) e2.properties.get(0);
            if (propName.equals("UnitScaleFactor")) {
                unitScaleFactor = ((Double) e2.properties.get(4)).floatValue();
                if (unitScaleFactor != 100.0f) {
                    logger.log(Level.WARNING, "FBX model isn't using meters for world units. Scale could be incorrect.");
                }
            } else if (propName.equals("TimeMode")) {
                timeMode = (Integer) e2.properties.get(4);
            } else if (propName.equals("CustomFrameRate")) {
                float framerate = ((Double) e2.properties.get(4)).floatValue();
                if (framerate != -1) {
                    customFrameRate = framerate;
                }
            } else if (propName.equals("UpAxis")) {
                Integer upAxis = (Integer) e2.properties.get(4);
                if (upAxis != 1) {
                    logger.log(Level.WARNING, "FBX model isn't using Y as up axis. Orientation could be incorrect");
                }
            } else if (propName.equals("UpAxisSign")) {
                Integer upAxisSign = (Integer) e2.properties.get(4);
                if (upAxisSign != 1) {
                    logger.log(Level.WARNING, "FBX model isn't using correct up axis sign. Orientation could be incorrect");
                }
            } else if (propName.equals("FrontAxis")) {
                Integer frontAxis = (Integer) e2.properties.get(4);
                if (frontAxis != 2) {
                    logger.log(Level.WARNING, "FBX model isn't using Z as forward axis. Orientation could be incorrect");
                }
            } else if (propName.equals("FrontAxisSign")) {
                Integer frontAxisSign = (Integer) e2.properties.get(4);
                if (frontAxisSign != -1) {
                    logger.log(Level.WARNING, "FBX model isn't using correct forward axis sign. Orientation could be incorrect");
                }
            }
        }
        
        Float fps = timeModeToFps.get(timeMode);
        if (fps != null) {
            if (fps == -1f) {
                // Using custom framerate
                frameRate = customFrameRate;
            } else {
                // Use FPS from time mode.
                frameRate = fps;
            }
        }
    }
}
