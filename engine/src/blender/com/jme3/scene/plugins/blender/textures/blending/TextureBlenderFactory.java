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
package com.jme3.scene.plugins.blender.textures.blending;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.texture.Texture;
import com.jme3.texture.Image.Format;

/**
 * This class creates the texture blending class depending on the texture type.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureBlenderFactory {
	private static final Logger	LOGGER	= Logger.getLogger(TextureBlenderFactory.class.getName());

	/**
	 * This method creates the blending class.
	 * 
	 * @param format
	 *            the texture format
	 * @return texture blending class
	 */
	public static TextureBlender createTextureBlender(Format format) {
		switch (format) {
			case Luminance8:
			case Luminance8Alpha8:
			case Luminance16:
			case Luminance16Alpha16:
			case Luminance16F:
			case Luminance16FAlpha16F:
			case Luminance32F:
				return new TextureBlenderLuminance();
			case RGBA8:
			case ABGR8:
			case BGR8:
			case RGB8:
			case RGB10:
			case RGB111110F:
			case RGB16:
			case RGB16F:
			case RGB16F_to_RGB111110F:
			case RGB16F_to_RGB9E5:
			case RGB32F:
			case RGB565:
			case RGB5A1:
			case RGB9E5:
			case RGBA16:
			case RGBA16F:
			case RGBA32F:
				return new TextureBlenderAWT();
			case DXT1:
			case DXT1A:
			case DXT3:
			case DXT5:
				return new TextureBlenderDDS();
			case Alpha16:
			case Alpha8:
			case ARGB4444:
			case Depth:
			case Depth16:
			case Depth24:
			case Depth32:
			case Depth32F:
			case Intensity16:
			case Intensity8:
			case LATC:
			case LTC:
				LOGGER.log(Level.WARNING, "Image type not yet supported for blending: {0}. Returning a blender that does not change the texture.", format);
				return new TextureBlender() {
					@Override
					public Texture blend(float[] materialColor, Texture texture, float[] color, float affectFactor, int blendType, boolean neg, BlenderContext blenderContext) {
						return texture;
					}
				};
			default:
				throw new IllegalStateException("Unknown image format type: " + format);
		}
	}
}
