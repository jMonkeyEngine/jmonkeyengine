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
package com.jme3.scene.plugins.blender;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.Properties;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * A purpose of the helper class is to split calculation code into several classes. Each helper after use should be cleared because it can
 * hold the state of the calculations.
 * @author Marcin Roguski
 */
public abstract class AbstractBlenderHelper {

	/** The version of the blend file. */
	protected final int	blenderVersion;
	/** This variable indicates if the Y asxis is the UP axis or not. */
	protected boolean						fixUpAxis;
	/** Quaternion used to rotate data when Y is up axis. */
	protected Quaternion					upAxisRotationQuaternion;
	
	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
	 * versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
	 */
	public AbstractBlenderHelper(String blenderVersion, boolean fixUpAxis) {
		this.blenderVersion = Integer.parseInt(blenderVersion);
		this.fixUpAxis = fixUpAxis;
		if(fixUpAxis) {
			upAxisRotationQuaternion = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
		}
	}
	
	/**
	 * This method clears the state of the helper so that it can be used for different calculations of another feature.
	 */
	public void clearState() {}

	/**
	 * This method should be used to check if the text is blank. Avoid using text.trim().length()==0. This causes that more strings are
	 * being created and stored in the memory. It can be unwise especially inside loops.
	 * @param text
	 *        the text to be checked
	 * @return <b>true</b> if the text is blank and <b>false</b> otherwise
	 */
	protected boolean isBlank(String text) {
		if (text != null) {
			for (int i = 0; i < text.length(); ++i) {
				if (!Character.isWhitespace(text.charAt(i))) {
					return false;
				}
			}
		}
		return true;
	}

        /**
	 * Generate a new ByteBuffer using the given array of byte[4] objects. The ByteBuffer will be 4 * data.length
	 * long and contain the vector data as data[0][0], data[0][1], data[0][2], data[0][3], data[1][0]... etc.
	 * @param data
	 *        list of byte[4] objects to place into a new ByteBuffer
	 */
	protected ByteBuffer createByteBuffer(List<byte[]> data) {
		if (data == null) {
			return null;
		}
		ByteBuffer buff = BufferUtils.createByteBuffer(4 * data.size());
		for (byte[] v : data) {
			if (v != null) {
				buff.put(v[0]).put(v[1]).put(v[2]).put(v[3]);
			} else {
				buff.put((byte)0).put((byte)0).put((byte)0).put((byte)0);
			}
		}
		buff.flip();
		return buff;
	}
        
	/**
	 * Generate a new FloatBuffer using the given array of float[4] objects. The FloatBuffer will be 4 * data.length
	 * long and contain the vector data as data[0][0], data[0][1], data[0][2], data[0][3], data[1][0]... etc.
	 * @param data
	 *        list of float[4] objects to place into a new FloatBuffer
	 */
	protected FloatBuffer createFloatBuffer(List<float[]> data) {
		if (data == null) {
			return null;
		}
		FloatBuffer buff = BufferUtils.createFloatBuffer(4 * data.size());
		for (float[] v : data) {
			if (v != null) {
				buff.put(v[0]).put(v[1]).put(v[2]).put(v[3]);
			} else {
				buff.put(0).put(0).put(0).put(0);
			}
		}
		buff.flip();
		return buff;
	}

	/**
	 * This method loads the properties if they are available and defined for the structure.
	 * @param structure
	 *        the structure we read the properties from
	 * @param blenderContext
	 *        the blender context
	 * @return loaded properties or null if they are not available
	 * @throws BlenderFileException
	 *         an exception is thrown when the blend file is somehow corrupted
	 */
	protected Properties loadProperties(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
		Properties properties = null;
		Structure id = (Structure) structure.getFieldValue("ID");
		if (id != null) {
			Pointer pProperties = (Pointer) id.getFieldValue("properties");
			if (pProperties.isNotNull()) {
				Structure propertiesStructure = pProperties.fetchData(blenderContext.getInputStream()).get(0);
				properties = new Properties();
				properties.load(propertiesStructure, blenderContext);
			}
		}
		return properties;
	}
	
	/**
	 * This method analyzes the given structure and the data contained within
	 * blender context and decides if the feature should be loaded.
	 * @param structure
	 *        structure to be analyzed
	 * @param blenderContext
	 *        the blender context
	 * @return <b>true</b> if the feature should be loaded and false otherwise
	 */
	public abstract boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext);
}
