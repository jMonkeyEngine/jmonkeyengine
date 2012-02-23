package com.jme3.scene.plugins.blender.textures.blending;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.texture.Texture;

/**
 * An interface for texture blending classes (the classes that mix the texture
 * pixels with the material colors).
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public interface TextureBlender {
	// types of blending
	int	MTEX_BLEND			= 0;
	int	MTEX_MUL			= 1;
	int	MTEX_ADD			= 2;
	int	MTEX_SUB			= 3;
	int	MTEX_DIV			= 4;
	int	MTEX_DARK			= 5;
	int	MTEX_DIFF			= 6;
	int	MTEX_LIGHT			= 7;
	int	MTEX_SCREEN			= 8;
	int	MTEX_OVERLAY		= 9;
	int	MTEX_BLEND_HUE		= 10;
	int	MTEX_BLEND_SAT		= 11;
	int	MTEX_BLEND_VAL		= 12;
	int	MTEX_BLEND_COLOR	= 13;
	int	MTEX_NUM_BLENDTYPES	= 14;

	/**
	 * This method blends the given texture with material color and the defined
	 * color in 'map to' panel. As a result of this method a new texture is
	 * created. The input texture is NOT.
	 * 
	 * @param materialColor
	 *            the material diffuse color
	 * @param texture
	 *            the texture we use in blending
	 * @param color
	 *            the color defined for the texture
	 * @param affectFactor
	 *            the factor that the color affects the texture (value form 0.0
	 *            to 1.0)
	 * @param blendType
	 *            the blending type
	 * @param blenderContext
	 *            the blender context
	 * @return new texture that was created after the blending
	 */
	Texture blend(float[] materialColor, Texture texture, float[] color, float affectFactor, int blendType, boolean neg, BlenderContext blenderContext);
}
