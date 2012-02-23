package com.jme3.scene.plugins.blender.textures.blending;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

/**
 * The class that is responsible for blending the following texture types:
 * <li> Luminance8
 * <li> Luminance8Alpha8
 * Not yet supported (but will be):
 * <li> Luminance16:
 * <li> Luminance16Alpha16:
 * <li> Luminance16F:
 * <li> Luminance16FAlpha16F:
 * <li> Luminance32F:
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureBlenderLuminance extends AbstractTextureBlender {
	private static final Logger	LOGGER	= Logger.getLogger(TextureBlenderLuminance.class.getName());

	@Override
	public Texture blend(float[] materialColor, Texture texture, float[] color, float affectFactor, int blendType, boolean neg, BlenderContext blenderContext) {
		Format format = texture.getImage().getFormat();
		ByteBuffer data = texture.getImage().getData(0);
		data.rewind();

		int width = texture.getImage().getWidth();
		int height = texture.getImage().getHeight();
		int depth = texture.getImage().getDepth();
		if (depth == 0) {
			depth = 1;
		}
		ByteBuffer newData = BufferUtils.createByteBuffer(width * height * depth * 4);

		float[] resultPixel = new float[4];
		float[] tinAndAlpha = new float[2];
		int dataIndex = 0;
		while (data.hasRemaining()) {
			this.getTinAndAlpha(data, format, neg, tinAndAlpha);
			this.blendPixel(resultPixel, materialColor, color, tinAndAlpha[0], affectFactor, blendType, blenderContext);
			newData.put(dataIndex++, (byte) (resultPixel[0] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[1] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[2] * 255.0f));
			newData.put(dataIndex++, (byte) (tinAndAlpha[1] * 255.0f));
		}
		if (texture.getType() == Texture.Type.TwoDimensional) {
			return new Texture2D(new Image(Format.RGBA8, width, height, newData));
		} else {
			ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
			dataArray.add(newData);
			return new Texture3D(new Image(Format.RGBA8, width, height, depth, dataArray));
		}
	}

	/**
	 * This method return texture intensity and alpha value.
	 * 
	 * @param data
	 *            the texture data
	 * @param imageFormat
	 *            the image format
	 * @param neg
	 *            indicates if the texture is negated
	 * @param result
	 *            the table (2 elements) where the result is being stored
	 */
	protected void getTinAndAlpha(ByteBuffer data, Format imageFormat, boolean neg, float[] result) {
		byte pixelValue = data.get();// at least one byte is always taken
		float firstPixelValue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
		switch (imageFormat) {
			case Luminance8:
				result[0] = neg ? 1.0f - firstPixelValue : firstPixelValue;
				result[1] = 1.0f;
				break;
			case Luminance8Alpha8:
				result[0] = neg ? 1.0f - firstPixelValue : firstPixelValue;
				pixelValue = data.get();
				result[1] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case Luminance16:
			case Luminance16Alpha16:
			case Luminance16F:
			case Luminance16FAlpha16F:
			case Luminance32F:
				LOGGER.log(Level.WARNING, "Image type not yet supported for blending: {0}", imageFormat);
				break;
			default:
				throw new IllegalStateException("Invalid image format type for DDS texture blender: " + imageFormat);
		}
	}

	/**
	 * This method blends the texture with an appropriate color.
	 * 
	 * @param result
	 *            the result color (variable 'in' in blender source code)
	 * @param materialColor
	 *            the texture color (variable 'out' in blender source coude)
	 * @param color
	 *            the previous color (variable 'tex' in blender source code)
	 * @param textureIntensity
	 *            texture intensity (variable 'fact' in blender source code)
	 * @param textureFactor
	 *            texture affection factor (variable 'facg' in blender source
	 *            code)
	 * @param blendtype
	 *            the blend type
	 * @param blenderContext
	 *            the blender context
	 */
	protected void blendPixel(float[] result, float[] materialColor, float[] color, float textureIntensity, float textureFactor, int blendtype, BlenderContext blenderContext) {
		float oneMinusFactor, col;
		textureIntensity *= textureFactor;

		switch (blendtype) {
			case MTEX_BLEND:
				oneMinusFactor = 1.0f - textureIntensity;
				result[0] = textureIntensity * color[0] + oneMinusFactor * materialColor[0];
				result[1] = textureIntensity * color[1] + oneMinusFactor * materialColor[1];
				result[2] = textureIntensity * color[2] + oneMinusFactor * materialColor[2];
				break;
			case MTEX_MUL:
				oneMinusFactor = 1.0f - textureFactor;
				result[0] = (oneMinusFactor + textureIntensity * materialColor[0]) * color[0];
				result[1] = (oneMinusFactor + textureIntensity * materialColor[1]) * color[1];
				result[2] = (oneMinusFactor + textureIntensity * materialColor[2]) * color[2];
				break;
			case MTEX_DIV:
				oneMinusFactor = 1.0f - textureIntensity;
				if (color[0] != 0.0) {
					result[0] = (oneMinusFactor * materialColor[0] + textureIntensity * materialColor[0] / color[0]) * 0.5f;
				}
				if (color[1] != 0.0) {
					result[1] = (oneMinusFactor * materialColor[1] + textureIntensity * materialColor[1] / color[1]) * 0.5f;
				}
				if (color[2] != 0.0) {
					result[2] = (oneMinusFactor * materialColor[2] + textureIntensity * materialColor[2] / color[2]) * 0.5f;
				}
				break;
			case MTEX_SCREEN:
				oneMinusFactor = 1.0f - textureFactor;
				result[0] = 1.0f - (oneMinusFactor + textureIntensity * (1.0f - materialColor[0])) * (1.0f - color[0]);
				result[1] = 1.0f - (oneMinusFactor + textureIntensity * (1.0f - materialColor[1])) * (1.0f - color[1]);
				result[2] = 1.0f - (oneMinusFactor + textureIntensity * (1.0f - materialColor[2])) * (1.0f - color[2]);
				break;
			case MTEX_OVERLAY:
				oneMinusFactor = 1.0f - textureFactor;
				if (materialColor[0] < 0.5f) {
					result[0] = color[0] * (oneMinusFactor + 2.0f * textureIntensity * materialColor[0]);
				} else {
					result[0] = 1.0f - (oneMinusFactor + 2.0f * textureIntensity * (1.0f - materialColor[0])) * (1.0f - color[0]);
				}
				if (materialColor[1] < 0.5f) {
					result[1] = color[1] * (oneMinusFactor + 2.0f * textureIntensity * materialColor[1]);
				} else {
					result[1] = 1.0f - (oneMinusFactor + 2.0f * textureIntensity * (1.0f - materialColor[1])) * (1.0f - color[1]);
				}
				if (materialColor[2] < 0.5f) {
					result[2] = color[2] * (oneMinusFactor + 2.0f * textureIntensity * materialColor[2]);
				} else {
					result[2] = 1.0f - (oneMinusFactor + 2.0f * textureIntensity * (1.0f - materialColor[2])) * (1.0f - color[2]);
				}
				break;
			case MTEX_SUB:
				result[0] = materialColor[0] - textureIntensity * color[0];
				result[1] = materialColor[1] - textureIntensity * color[1];
				result[2] = materialColor[2] - textureIntensity * color[2];
				result[0] = FastMath.clamp(result[0], 0.0f, 1.0f);
				result[1] = FastMath.clamp(result[1], 0.0f, 1.0f);
				result[2] = FastMath.clamp(result[2], 0.0f, 1.0f);
				break;
			case MTEX_ADD:
				result[0] = (textureIntensity * color[0] + materialColor[0]) * 0.5f;
				result[1] = (textureIntensity * color[1] + materialColor[1]) * 0.5f;
				result[2] = (textureIntensity * color[2] + materialColor[2]) * 0.5f;
				break;
			case MTEX_DIFF:
				oneMinusFactor = 1.0f - textureIntensity;
				result[0] = oneMinusFactor * materialColor[0] + textureIntensity * Math.abs(materialColor[0] - color[0]);
				result[1] = oneMinusFactor * materialColor[1] + textureIntensity * Math.abs(materialColor[1] - color[1]);
				result[2] = oneMinusFactor * materialColor[2] + textureIntensity * Math.abs(materialColor[2] - color[2]);
				break;
			case MTEX_DARK:
				col = textureIntensity * color[0];
				result[0] = col < materialColor[0] ? col : materialColor[0];
				col = textureIntensity * color[1];
				result[1] = col < materialColor[1] ? col : materialColor[1];
				col = textureIntensity * color[2];
				result[2] = col < materialColor[2] ? col : materialColor[2];
				break;
			case MTEX_LIGHT:
				col = textureIntensity * color[0];
				result[0] = col > materialColor[0] ? col : materialColor[0];
				col = textureIntensity * color[1];
				result[1] = col > materialColor[1] ? col : materialColor[1];
				col = textureIntensity * color[2];
				result[2] = col > materialColor[2] ? col : materialColor[2];
				break;
			case MTEX_BLEND_HUE:
			case MTEX_BLEND_SAT:
			case MTEX_BLEND_VAL:
			case MTEX_BLEND_COLOR:
				System.arraycopy(materialColor, 0, result, 0, 3);
				this.blendHSV(blendtype, result, textureIntensity, color, blenderContext);
				break;
			default:
				throw new IllegalStateException("Unknown blend type: " + blendtype);
		}
	}
}
