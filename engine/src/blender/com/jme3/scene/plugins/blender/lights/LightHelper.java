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
package com.jme3.scene.plugins.blender.lights;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that is used in light calculations.
 * @author Marcin Roguski
 */
public class LightHelper extends AbstractBlenderHelper {

    private static final Logger LOGGER = Logger.getLogger(LightHelper.class.getName());

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
     */
    public LightHelper(String blenderVersion, boolean fixUpAxis) {
        super(blenderVersion, fixUpAxis);
    }

    public Light toLight(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        Light result = (Light) blenderContext.getLoadedFeature(structure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
        if (result != null) {
            return result;
        }
        int type = ((Number) structure.getFieldValue("type")).intValue();
        switch (type) {
            case 0://Lamp
                result = new PointLight();
                float distance = ((Number) structure.getFieldValue("dist")).floatValue();
                ((PointLight) result).setRadius(distance);
                break;
            case 1://Sun
                LOGGER.log(Level.WARNING, "'Sun' lamp is not supported in jMonkeyEngine.");
                break;
            case 2://Spot
            	result = new SpotLight();
            	//range
            	((SpotLight)result).setSpotRange(((Number) structure.getFieldValue("dist")).floatValue());
            	//outer angle
            	float outerAngle = ((Number) structure.getFieldValue("spotsize")).floatValue()*FastMath.DEG_TO_RAD * 0.5f;
            	((SpotLight)result).setSpotOuterAngle(outerAngle);
            	
            	//inner angle
            	float spotblend = ((Number) structure.getFieldValue("spotblend")).floatValue();
                spotblend = FastMath.clamp(spotblend, 0, 1);
                float innerAngle = outerAngle * (1 - spotblend);
            	((SpotLight)result).setSpotInnerAngle(innerAngle);
                break;
            case 3://Hemi
                LOGGER.log(Level.WARNING, "'Hemi' lamp is not supported in jMonkeyEngine.");
                break;
            case 4://Area
                result = new DirectionalLight();
                break;
            default:
                throw new BlenderFileException("Unknown light source type: " + type);
        }
        if (result != null) {
            float r = ((Number) structure.getFieldValue("r")).floatValue();
            float g = ((Number) structure.getFieldValue("g")).floatValue();
            float b = ((Number) structure.getFieldValue("b")).floatValue();
            result.setColor(new ColorRGBA(r, g, b, 1.0f));
        }
        return result;
    }
    
    @Override
    public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
    	return (blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.LIGHTS) != 0;
    }
}
