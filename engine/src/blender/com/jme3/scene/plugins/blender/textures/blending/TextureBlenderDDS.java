package com.jme3.scene.plugins.blender.textures.blending;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3tools.converters.RGB565;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;

/**
 * The class that is responsible for blending the following texture types:
 * <li> DXT1
 * <li> DXT3
 * <li> DXT5
 * Not yet supported (but will be):
 * <li> DXT1A:
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureBlenderDDS extends AbstractTextureBlender {
	private static final Logger	LOGGER	= Logger.getLogger(TextureBlenderDDS.class.getName());

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
		ByteBuffer newData = BufferUtils.createByteBuffer(data.remaining());

		float[] resultPixel = new float[4];
		float[] pixelColor = new float[4];
		TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel() };
		int dataIndex = 0;
		while (data.hasRemaining()) {
			switch (format) {
				case DXT3:
				case DXT5:
					newData.putLong(dataIndex, data.getLong());// just copy the
																// 8 bytes of
																// alphas
					dataIndex += 8;
				case DXT1:
					int col0 = RGB565.RGB565_to_ARGB8(data.getShort());
					int col1 = RGB565.RGB565_to_ARGB8(data.getShort());
					colors[0].fromARGB8(col0);
					colors[1].fromARGB8(col1);
					break;
				case DXT1A:
					LOGGER.log(Level.WARNING, "Image type not yet supported for blending: {0}", format);
					break;
				default:
					throw new IllegalStateException("Invalid image format type for DDS texture blender: " + format);
			}

			// blending colors
			for (int i = 0; i < colors.length; ++i) {
				if (neg) {
					colors[i].negate();
				}
				colors[i].toRGBA(pixelColor);
				this.blendPixel(resultPixel, materialColor, pixelColor, affectFactor, blendType, blenderContext);
				colors[i].fromARGB8(1, resultPixel[0], resultPixel[1], resultPixel[2]);
				int argb8 = colors[i].toARGB8();
				short rgb565 = RGB565.ARGB8_to_RGB565(argb8);
				newData.putShort(dataIndex, rgb565);
				dataIndex += 2;
			}

			// just copy the remaining 4 bytes of the current texel
			newData.putInt(dataIndex, data.getInt());
			dataIndex += 4;
		}
		if (texture.getType() == Texture.Type.TwoDimensional) {
			return new Texture2D(new Image(format, width, height, newData));
		} else {
			ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
			dataArray.add(newData);
			return new Texture3D(new Image(format, width, height, depth, dataArray));
		}
	}
}
