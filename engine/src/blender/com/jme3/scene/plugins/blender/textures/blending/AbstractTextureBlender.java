package com.jme3.scene.plugins.blender.textures.blending;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;

/**
 * An abstract class that contains the basic methods used by the classes that
 * will derive from it.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */abstract class AbstractTextureBlender implements TextureBlender {
	/**
	 * This method blends the single pixel depending on the blending type.
	 * 
	 * @param result
	 *            the result pixel
	 * @param materialColor
	 *            the material color
	 * @param pixelColor
	 *            the pixel color
	 * @param blendFactor
	 *            the blending factor
	 * @param blendtype
	 *            the blending type
	 * @param blenderContext
	 *            the blender context
	 */
	protected void blendPixel(float[] result, float[] materialColor, float[] pixelColor, float blendFactor, int blendtype, BlenderContext blenderContext) {
		float oneMinusFactor = 1.0f - blendFactor, col;

		switch (blendtype) {
			case MTEX_BLEND:
				result[0] = blendFactor * pixelColor[0] + oneMinusFactor * materialColor[0];
				result[1] = blendFactor * pixelColor[1] + oneMinusFactor * materialColor[1];
				result[2] = blendFactor * pixelColor[2] + oneMinusFactor * materialColor[2];
				break;
			case MTEX_MUL:
				result[0] = (oneMinusFactor + blendFactor * materialColor[0]) * pixelColor[0];
				result[1] = (oneMinusFactor + blendFactor * materialColor[1]) * pixelColor[1];
				result[2] = (oneMinusFactor + blendFactor * materialColor[2]) * pixelColor[2];
				break;
			case MTEX_DIV:
				if (pixelColor[0] != 0.0) {
					result[0] = (oneMinusFactor * materialColor[0] + blendFactor * materialColor[0] / pixelColor[0]) * 0.5f;
				}
				if (pixelColor[1] != 0.0) {
					result[1] = (oneMinusFactor * materialColor[1] + blendFactor * materialColor[1] / pixelColor[1]) * 0.5f;
				}
				if (pixelColor[2] != 0.0) {
					result[2] = (oneMinusFactor * materialColor[2] + blendFactor * materialColor[2] / pixelColor[2]) * 0.5f;
				}
				break;
			case MTEX_SCREEN:
				result[0] = 1.0f - (oneMinusFactor + blendFactor * (1.0f - materialColor[0])) * (1.0f - pixelColor[0]);
				result[1] = 1.0f - (oneMinusFactor + blendFactor * (1.0f - materialColor[1])) * (1.0f - pixelColor[1]);
				result[2] = 1.0f - (oneMinusFactor + blendFactor * (1.0f - materialColor[2])) * (1.0f - pixelColor[2]);
				break;
			case MTEX_OVERLAY:
				if (materialColor[0] < 0.5f) {
					result[0] = pixelColor[0] * (oneMinusFactor + 2.0f * blendFactor * materialColor[0]);
				} else {
					result[0] = 1.0f - (oneMinusFactor + 2.0f * blendFactor * (1.0f - materialColor[0])) * (1.0f - pixelColor[0]);
				}
				if (materialColor[1] < 0.5f) {
					result[1] = pixelColor[1] * (oneMinusFactor + 2.0f * blendFactor * materialColor[1]);
				} else {
					result[1] = 1.0f - (oneMinusFactor + 2.0f * blendFactor * (1.0f - materialColor[1])) * (1.0f - pixelColor[1]);
				}
				if (materialColor[2] < 0.5f) {
					result[2] = pixelColor[2] * (oneMinusFactor + 2.0f * blendFactor * materialColor[2]);
				} else {
					result[2] = 1.0f - (oneMinusFactor + 2.0f * blendFactor * (1.0f - materialColor[2])) * (1.0f - pixelColor[2]);
				}
				break;
			case MTEX_SUB:
				result[0] = materialColor[0] - blendFactor * pixelColor[0];
				result[1] = materialColor[1] - blendFactor * pixelColor[1];
				result[2] = materialColor[2] - blendFactor * pixelColor[2];
				result[0] = FastMath.clamp(result[0], 0.0f, 1.0f);
				result[1] = FastMath.clamp(result[1], 0.0f, 1.0f);
				result[2] = FastMath.clamp(result[2], 0.0f, 1.0f);
				break;
			case MTEX_ADD:
				result[0] = (blendFactor * pixelColor[0] + materialColor[0]) * 0.5f;
				result[1] = (blendFactor * pixelColor[1] + materialColor[1]) * 0.5f;
				result[2] = (blendFactor * pixelColor[2] + materialColor[2]) * 0.5f;
				break;
			case MTEX_DIFF:
				result[0] = oneMinusFactor * materialColor[0] + blendFactor * Math.abs(materialColor[0] - pixelColor[0]);
				result[1] = oneMinusFactor * materialColor[1] + blendFactor * Math.abs(materialColor[1] - pixelColor[1]);
				result[2] = oneMinusFactor * materialColor[2] + blendFactor * Math.abs(materialColor[2] - pixelColor[2]);
				break;
			case MTEX_DARK:
				col = blendFactor * pixelColor[0];
				result[0] = col < materialColor[0] ? col : materialColor[0];
				col = blendFactor * pixelColor[1];
				result[1] = col < materialColor[1] ? col : materialColor[1];
				col = blendFactor * pixelColor[2];
				result[2] = col < materialColor[2] ? col : materialColor[2];
				break;
			case MTEX_LIGHT:
				col = blendFactor * pixelColor[0];
				result[0] = col > materialColor[0] ? col : materialColor[0];
				col = blendFactor * pixelColor[1];
				result[1] = col > materialColor[1] ? col : materialColor[1];
				col = blendFactor * pixelColor[2];
				result[2] = col > materialColor[2] ? col : materialColor[2];
				break;
			case MTEX_BLEND_HUE:
			case MTEX_BLEND_SAT:
			case MTEX_BLEND_VAL:
			case MTEX_BLEND_COLOR:
				System.arraycopy(materialColor, 0, result, 0, 3);
				this.blendHSV(blendtype, result, blendFactor, pixelColor, blenderContext);
				break;
			default:
				throw new IllegalStateException("Unknown blend type: " + blendtype);
		}
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
			case MTEX_BLEND_HUE: {// FIXME: not working well for image textures
									// (works fine for generated textures)
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
					materialHelper.hsvToRgb(h, (oneMinusFactor * s + fac * colorTransformResult[1]), v, materialRGB);
				}
				break;
			}
			case MTEX_BLEND_VAL: {
				float[] rgbToHsv = new float[3];
				float[] colToHsv = new float[3];
				materialHelper.rgbToHsv(materialRGB[0], materialRGB[1], materialRGB[2], rgbToHsv);
				materialHelper.rgbToHsv(pixelColor[0], pixelColor[1], pixelColor[2], colToHsv);
				materialHelper.hsvToRgb(rgbToHsv[0], rgbToHsv[1], (oneMinusFactor * rgbToHsv[2] + fac * colToHsv[2]), materialRGB);
				break;
			}
			case MTEX_BLEND_COLOR: {// FIXME: not working well for image
									// textures (works fine for generated
									// textures)
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
}
