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
package com.jme3.scene.plugins.blender;

import java.util.Arrays;
import java.util.List;

import com.jme3.export.Savable;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.Properties;

/**
 * A purpose of the helper class is to split calculation code into several classes. Each helper after use should be cleared because it can
 * hold the state of the calculations.
 * @author Marcin Roguski
 */
public abstract class AbstractBlenderHelper {
    /** The blender context. */
    protected BlenderContext blenderContext;
    /** The version of the blend file. */
    protected final int  blenderVersion;
    /** This variable indicates if the Y asxis is the UP axis or not. */
    protected boolean    fixUpAxis;
    /** Quaternion used to rotate data when Y is up axis. */
    protected Quaternion upAxisRotationQuaternion;

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
     * versions.
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public AbstractBlenderHelper(String blenderVersion, BlenderContext blenderContext) {
        this.blenderVersion = Integer.parseInt(blenderVersion);
        this.blenderContext = blenderContext;
        fixUpAxis = blenderContext.getBlenderKey().isFixUpAxis();
        if (fixUpAxis) {
            upAxisRotationQuaternion = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
        }
    }

    /**
     * This method loads the properties if they are available and defined for the structure.
     * @param structure
     *            the structure we read the properties from
     * @param blenderContext
     *            the blender context
     * @return loaded properties or null if they are not available
     * @throws BlenderFileException
     *             an exception is thrown when the blend file is somehow corrupted
     */
    protected Properties loadProperties(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        Properties properties = null;
        Structure id = (Structure) structure.getFieldValue("ID");
        if (id != null) {
            Pointer pProperties = (Pointer) id.getFieldValue("properties");
            if (pProperties.isNotNull()) {
                Structure propertiesStructure = pProperties.fetchData().get(0);
                properties = new Properties();
                properties.load(propertiesStructure, blenderContext);
            }
        }
        return properties;
    }

    /**
     * The method applies properties to the given spatial. The Properties
     * instance cannot be directly applied because the end-user might not have
     * the blender plugin jar file and thus receive ClassNotFoundException. The
     * values are set by name instead.
     * 
     * @param spatial
     *            the spatial that is to have properties applied
     * @param properties
     *            the properties to be applied
     */
    public void applyProperties(Spatial spatial, Properties properties) {
        List<String> propertyNames = properties.getSubPropertiesNames();
        if (propertyNames != null && propertyNames.size() > 0) {
            for (String propertyName : propertyNames) {
                Object value = properties.findValue(propertyName);
                if (value instanceof Savable || value instanceof Boolean || value instanceof String || value instanceof Float || value instanceof Integer || value instanceof Long) {
                    spatial.setUserData(propertyName, value);
                } else if (value instanceof Double) {
                    spatial.setUserData(propertyName, ((Double) value).floatValue());
                } else if (value instanceof int[]) {
                    spatial.setUserData(propertyName, Arrays.toString((int[]) value));
                } else if (value instanceof float[]) {
                    spatial.setUserData(propertyName, Arrays.toString((float[]) value));
                } else if (value instanceof double[]) {
                    spatial.setUserData(propertyName, Arrays.toString((double[]) value));
                }
            }
        }
    }
}
