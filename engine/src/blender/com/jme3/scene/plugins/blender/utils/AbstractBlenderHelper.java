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
package com.jme3.scene.plugins.blender.utils;

import java.nio.FloatBuffer;
import java.util.List;

import com.jme3.util.BufferUtils;

/**
 * A purpose of the helper class is to split calculation code into several classes. Each helper after use should be cleared because it can
 * hold the state of the calculations.
 * @author Marcin Roguski
 */
public abstract class AbstractBlenderHelper {
	/** The version of the blend file. */
	protected final int blenderVersion;

	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
	 * versions.
	 * @param blenderVersion
	 *            the version read from the blend file
	 */
	public AbstractBlenderHelper(String blenderVersion) {
		this.blenderVersion = Integer.parseInt(blenderVersion);
	}
	
	/**
	 * This method clears the state of the helper so that it can be used for different calculations of another feature.
	 */
	public void clearState() { }

	/**
	 * This method should be used to check if the text is blank. Avoid using text.trim().length()==0. This causes that more strings are
	 * being created and stored in the memory. It can be unwise especially inside loops.
	 * @param text
	 *            the text to be checked
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
	 * Generate a new FloatBuffer using the given array of float[4] objects. The FloatBuffer will be 4 * data.length
	 * long and contain the vector data as data[0][0], data[0][1], data[0][2], data[0][3], data[1][0]... etc.
	 * @param data
	 *        list of float[4] objects to place into a new FloatBuffer
	 */
	protected FloatBuffer createFloatBuffer(List<float[]> data) {
		if(data == null) {
			return null;
		}
		FloatBuffer buff = BufferUtils.createFloatBuffer(4 * data.size());
		for(float[] v : data) {
			if(v != null) {
				buff.put(v[0]).put(v[1]).put(v[2]).put(v[3]);
			} else {
				buff.put(0).put(0).put(0).put(0);
			}
		}
		buff.flip();
		return buff;
	}
}
