package com.jme3.scene.plugins.blender.textures.blending;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.texture.Image;
import java.util.logging.Logger;
import jme3tools.converters.MipMapGenerator;

/**
 * An abstract class that contains the basic methods used by the classes that
 * will derive from it.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */abstract class AbstractTextureBlender implements TextureBlender {
	private static final Logger	LOGGER	= Logger.getLogger(AbstractTextureBlender.class.getName());
	
	protected int flag;
	protected boolean negateTexture;
	protected int blendType;
	protected float[] materialColor;
	protected float[] color;
	protected float blendFactor;
	
	public AbstractTextureBlender(int flag, boolean negateTexture, int blendType, float[] materialColor, float[] color, float blendFactor) {
		this.flag = flag;
		this.negateTexture = negateTexture;
		this.blendType = blendType;
		this.materialColor = materialColor;
		this.color = color;
		this.blendFactor = blendFactor;
	}

	/**
	 * The method that performs the ramp blending.
	 * 
	 * @param type
	 *            the blend type
	 * @param materialRGB
	 *            the rgb value of the material, here the result is stored too
	 * @param fac
	 *            color affection factor
	 * @param pixelColor
	 *            the texture color
	 * @param blenderContext
	 *            the blender context
	 */
	protected void blendHSV(int type, float[] materialRGB, float fac, float[] pixelColor, BlenderContext blenderContext) {
		float oneMinusFactor = 1.0f - fac;
		MaterialHelper materialHelper = blenderContext.getHelper(MaterialHelper.class);

		switch (type) {
			case MTEX_BLEND_HUE: {// FIXME: not working well for image textures (works fine for generated textures)
				float[] colorTransformResult = new float[3];
				materialHelper.rgbToHsv(pixelColor[0], pixelColor[1], pixelColor[2], colorTransformResult);
				if (colorTransformResult[0] != 0.0f) {
					float colH = colorTransformResult[0];
					materialHelper.rgbToHsv(materialRGB[0], materialRGB[1], materialRGB[2], colorTransformResult);
					materialHelper.hsvToRgb(colH, colorTransformResult[1], colorTransformResult[2], colorTransformResult);
					materialRGB[0] = oneMinusFactor * materialRGB[0] + fac * colorTransformResult[0];
					materialRGB[1] = oneMinusFactor * materialRGB[1] + fac * colorTransformResult[1];
					materialRGB[2] = oneMinusFactor * materialRGB[2] + fac * colorTransformResult[2];
				}
				break;
			}
			case MTEX_BLEND_SAT: {
				float[] colorTransformResult = new float[3];
				materialHelper.rgbToHsv(materialRGB[0], materialRGB[1], materialRGB[2], colorTransformResult);
				float h = colorTransformResult[0];
				float s = colorTransformResult[1];
				float v = colorTransformResult[2];
				if (s != 0.0f) {
					materialHelper.rgbToHsv(pixelColor[0], pixelColor[1], pixelColor[2], colorTransformResult);
					materialHelper.hsvToRgb(h, oneMinusFactor * s + fac * colorTransformResult[1], v, materialRGB);
				}
				break;
			}
			case MTEX_BLEND_VAL: {
				float[] rgbToHsv = new float[3];
				float[] colToHsv = new float[3];
				materialHelper.rgbToHsv(materialRGB[0], materialRGB[1], materialRGB[2], rgbToHsv);
				materialHelper.rgbToHsv(pixelColor[0], pixelColor[1], pixelColor[2], colToHsv);
				materialHelper.hsvToRgb(rgbToHsv[0], rgbToHsv[1], oneMinusFactor * rgbToHsv[2] + fac * colToHsv[2], materialRGB);
				break;
			}
			case MTEX_BLEND_COLOR: {// FIXME: not working well for image textures (works fine for generated textures)
				float[] rgbToHsv = new float[3];
				float[] colToHsv = new float[3];
				materialHelper.rgbToHsv(pixelColor[0], pixelColor[1], pixelColor[2], colToHsv);
				if (colToHsv[2] != 0) {
					materialHelper.rgbToHsv(materialRGB[0], materialRGB[1], materialRGB[2], rgbToHsv);
					materialHelper.hsvToRgb(colToHsv[0], colToHsv[1], rgbToHsv[2], rgbToHsv);
					materialRGB[0] = oneMinusFactor * materialRGB[0] + fac * rgbToHsv[0];
					materialRGB[1] = oneMinusFactor * materialRGB[1] + fac * rgbToHsv[1];
					materialRGB[2] = oneMinusFactor * materialRGB[2] + fac * rgbToHsv[2];
				}
				break;
			}
			default:
				throw new IllegalStateException("Unknown ramp type: " + type);
		}
	}
	
	public void copyBlendingData(TextureBlender textureBlender) {
		if(textureBlender instanceof AbstractTextureBlender) {
			this.flag = ((AbstractTextureBlender) textureBlender).flag;
			this.negateTexture = ((AbstractTextureBlender) textureBlender).negateTexture;
			this.blendType = ((AbstractTextureBlender) textureBlender).blendType;
			this.materialColor = ((AbstractTextureBlender) textureBlender).materialColor.clone();
			this.color = ((AbstractTextureBlender) textureBlender).color.clone();
			this.blendFactor = ((AbstractTextureBlender) textureBlender).blendFactor;
		} else {
			LOGGER.warning("Cannot copy blending data from other types than " + this.getClass());
		}
	}
	
	/**
	 * The method prepares images for blending. It generates mipmaps if one of
	 * the images has them defined and the other one has not.
	 * 
	 * @param target
	 *            the image where the blending result is stored
	 * @param source
	 *            the image that is being read only
	 */
	protected void prepareImagesForBlending(Image target, Image source) {
		LOGGER.fine("Generating mipmaps if needed!");
		boolean targetHasMipmaps = target == null ? false : target.getMipMapSizes() != null && target.getMipMapSizes().length > 0;
		boolean sourceHasMipmaps = source == null ? false : source.getMipMapSizes() != null && source.getMipMapSizes().length > 0;
		if (target != null && !targetHasMipmaps && sourceHasMipmaps) {
			MipMapGenerator.generateMipMaps(target);
		} else if (source != null && !sourceHasMipmaps && targetHasMipmaps) {
			MipMapGenerator.generateMipMaps(source);
		}
	}
}
