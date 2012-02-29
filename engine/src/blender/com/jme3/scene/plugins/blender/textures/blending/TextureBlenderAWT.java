package com.jme3.scene.plugins.blender.textures.blending;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
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
	private static final Logger	LOGGER	= Logger.getLogger(TextureBlenderAWT.class.getName());

	@Override
	public Texture blend(float[] materialColor, Texture texture, float[] color, float affectFactor, int blendType, boolean neg, BlenderContext blenderContext) {
		float[] pixelColor = new float[] { color[0], color[1], color[2], 1.0f };
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
		int dataIndex = 0;
		while (data.hasRemaining()) {
			this.setupMaterialColor(data, format, neg, pixelColor);
			this.blendPixel(resultPixel, materialColor, pixelColor, affectFactor, blendType, blenderContext);
			newData.put(dataIndex++, (byte) (resultPixel[0] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[1] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[2] * 255.0f));
			newData.put(dataIndex++, (byte) (pixelColor[3] * 255.0f));
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
	 * This method alters the material color in a way dependent on the type of
	 * the image. For example the color remains untouched if the texture is of
	 * Luminance type. The luminance defines the interaction between the
	 * material color and color defined for texture blending. If the type has 3
	 * or more color channels then the material color is replaced with the
	 * texture's color and later blended with the defined blend color. All alpha
	 * values (if present) are ignored and not used during blending.
	 * 
	 * @param data
	 *            the image data
	 * @param imageFormat
	 *            the format of the image
	 * @param neg
	 *            defines it the result color should be nagated
	 * @param materialColor
	 *            the material's color (value may be changed)
	 * @return texture intensity for the current pixel
	 */
	protected float setupMaterialColor(ByteBuffer data, Format imageFormat, boolean neg, float[] materialColor) {
		float tin = 0.0f;
		byte pixelValue = data.get();// at least one byte is always taken :)
		float firstPixelValue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
		switch (imageFormat) {
			case RGBA8:
				materialColor[0] = firstPixelValue;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get();
				materialColor[2] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get();
				materialColor[3] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case ABGR8:
				materialColor[3] = firstPixelValue;
				pixelValue = data.get();
				materialColor[2] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get();
				materialColor[0] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case BGR8:
				materialColor[2] = firstPixelValue;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get();
				materialColor[0] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				materialColor[3] = 1.0f;
				break;
			case RGB8:
				materialColor[0] = firstPixelValue;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get();
				materialColor[2] = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				materialColor[3] = 1.0f;
				break;
			case ARGB4444:
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
			case RGBA32F:// TODO: implement these textures
				LOGGER.log(Level.WARNING, "Image type not yet supported for blending: {0}", imageFormat);
				break;
			default:
				throw new IllegalStateException("Invalid image format type for AWT texture blender: " + imageFormat);
		}
		if (neg) {
			materialColor[0] = 1.0f - materialColor[0];
			materialColor[1] = 1.0f - materialColor[1];
			materialColor[2] = 1.0f - materialColor[2];
		}
		// Blender formula for texture intensity calculation:
		// 0.35*texres.tr+0.45*texres.tg+0.2*texres.tb
		tin = 0.35f * materialColor[0] + 0.45f * materialColor[1] + 0.2f * materialColor[2];
		return tin;
	}
}
