package com.jme3.scene.plugins.blender.textures.blending;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

/**
 * The class that is responsible for blending the following texture types:
 * <li> RGBA8
 * <li> ABGR8
 * <li> BGR8
 * <li> RGB8
 * Not yet supported (but will be):
 * <li> ARGB4444:
 * <li> RGB10:
 * <li> RGB111110F:
 * <li> RGB16:
 * <li> RGB16F:
 * <li> RGB16F_to_RGB111110F:
 * <li> RGB16F_to_RGB9E5:
 * <li> RGB32F:
 * <li> RGB565:
 * <li> RGB5A1:
 * <li> RGB9E5:
 * <li> RGBA16:
 * <li> RGBA16F
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureBlenderAWT extends AbstractTextureBlender {
	public TextureBlenderAWT(int flag, boolean negateTexture, int blendType, float[] materialColor, float[] color, float blendFactor) {
		super(flag, negateTexture, blendType, materialColor, color, blendFactor);
	}
	
	public Image blend(Image image, Image baseImage, BlenderContext blenderContext) {
		float[] pixelColor = new float[] { color[0], color[1], color[2], 1.0f };
		Format format = image.getFormat();
		ByteBuffer data = image.getData(0);
		
		PixelInputOutput basePixelIO = null, pixelReader = PixelIOFactory.getPixelIO(format);
		TexturePixel basePixel = null, pixel = new TexturePixel();
		float[] materialColor = this.materialColor;
		if(baseImage != null) {
			basePixelIO = PixelIOFactory.getPixelIO(baseImage.getFormat());
			materialColor = new float[this.materialColor.length];
			basePixel = new TexturePixel();
		}
		
		int width = image.getWidth();
		int height = image.getHeight();
		int depth = image.getDepth();
		if (depth == 0) {
			depth = 1;
		}
		ByteBuffer newData = BufferUtils.createByteBuffer(width * height * depth * 4);
		
		float[] resultPixel = new float[4];
		int dataIndex = 0, x = 0, y = 0, index = 0;
		while (index < data.limit()) {
			//getting the proper material color if the base texture is applied
			if(basePixelIO != null) {
				basePixelIO.read(baseImage, basePixel, x, y);
				basePixel.toRGBA(materialColor);
			}			
			
			//reading the current texture's pixel
			pixelReader.read(image, pixel, index);
			index += image.getFormat().getBitsPerPixel() >> 3;
			pixel.toRGBA(pixelColor);
			if (negateTexture) {
				pixel.negate();
			}
			
			this.blendPixel(resultPixel, materialColor, pixelColor, blenderContext);
			newData.put(dataIndex++, (byte) (resultPixel[0] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[1] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[2] * 255.0f));
			newData.put(dataIndex++, (byte) (pixelColor[3] * 255.0f));
			
			++x;
			if(x >= width) {
				x = 0;
				++y;
			}
		}
		if(depth > 1) {
			ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
			dataArray.add(newData);
			return new Image(Format.RGBA8, width, height, depth, dataArray);
		} else {
			return new Image(Format.RGBA8, width, height, newData);
		}
	}

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
	protected void blendPixel(float[] result, float[] materialColor, float[] pixelColor, BlenderContext blenderContext) {
		float blendFactor = this.blendFactor * pixelColor[3];
		float oneMinusFactor = 1.0f - blendFactor, col;

		switch (blendType) {
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
				this.blendHSV(blendType, result, blendFactor, pixelColor, blenderContext);
				break;
			default:
				throw new IllegalStateException("Unknown blend type: " + blendType);
		}
	}
}
