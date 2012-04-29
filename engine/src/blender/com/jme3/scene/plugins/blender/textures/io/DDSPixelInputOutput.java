package com.jme3.scene.plugins.blender.textures.io;

import java.nio.ByteBuffer;

import jme3tools.converters.RGB565;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;

/**
 * Implemens read/write operations for DDS images.
 * This class currently implements only read operation.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class DDSPixelInputOutput implements PixelInputOutput {
	@Override
	public void read(Image image, TexturePixel pixel, int index) {
		throw new UnsupportedOperationException("Cannot get the DXT pixel by index because not every index contains the pixel color!");
	}

	@Override
	public void read(Image image, TexturePixel pixel, int x, int y) {
		int xTexetlIndex = x % image.getWidth() >> 2;
		int yTexelIndex = y % image.getHeight() >> 2;
		int xTexelCount = image.getWidth() >> 2;
		int texelIndex = yTexelIndex * xTexelCount + xTexetlIndex;
		
		TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
		int indexes = 0;
		long alphaIndexes = 0;
		float[] alphas = null;
		ByteBuffer data = image.getData().get(0);
		
		switch (image.getFormat()) {
			case DXT1: {// BC1
				data.position(texelIndex * 8);
				short c0 = data.getShort();
				short c1 = data.getShort();
				int col0 = RGB565.RGB565_to_ARGB8(c0);
				int col1 = RGB565.RGB565_to_ARGB8(c1);
				colors[0].fromARGB8(col0);
				colors[1].fromARGB8(col1);

				if (col0 > col1) {
					// creating color2 = 2/3color0 + 1/3color1
					colors[2].fromPixel(colors[0]);
					colors[2].mult(2);
					colors[2].add(colors[1]);
					colors[2].divide(3);

					// creating color3 = 1/3color0 + 2/3color1;
					colors[3].fromPixel(colors[1]);
					colors[3].mult(2);
					colors[3].add(colors[0]);
					colors[3].divide(3);
				} else {
					// creating color2 = 1/2color0 + 1/2color1
					colors[2].fromPixel(colors[0]);
					colors[2].add(colors[1]);
					colors[2].mult(0.5f);

					colors[3].fromARGB8(0);
				}
				indexes = data.getInt();// 4-byte table with color indexes in decompressed table
				break;
			}
			case DXT3: {// BC2
				data.position(texelIndex * 16);
				long alpha = data.getLong();
				alphas = new float[16];
				for (int i = 0; i < 16; ++i) {
					alphaIndexes |= i << i * 4;
					byte a = (byte) ((alpha >> i * 4 & 0x0F) << 4);
					alphas[i] = a >= 0 ? a / 255.0f : 1.0f - ~a / 255.0f;
				}

				short c0 = data.getShort();
				short c1 = data.getShort();
				int col0 = RGB565.RGB565_to_ARGB8(c0);
				int col1 = RGB565.RGB565_to_ARGB8(c1);
				colors[0].fromARGB8(col0);
				colors[1].fromARGB8(col1);

				// creating color2 = 2/3color0 + 1/3color1
				colors[2].fromPixel(colors[0]);
				colors[2].mult(2);
				colors[2].add(colors[1]);
				colors[2].divide(3);

				// creating color3 = 1/3color0 + 2/3color1;
				colors[3].fromPixel(colors[1]);
				colors[3].mult(2);
				colors[3].add(colors[0]);
				colors[3].divide(3);

				indexes = data.getInt();// 4-byte table with color indexes in decompressed table
				break;
			}
			case DXT5: {// BC3
				data.position(texelIndex * 16);
				alphas = new float[8];
				alphas[0] = data.get() * 255.0f;
				alphas[1] = data.get() * 255.0f;
				alphaIndexes = data.get() | data.get() << 8 | data.get() << 16 | data.get() << 24 | data.get() << 32 | data.get() << 40;
				if (alphas[0] > alphas[1]) {// 6 interpolated alpha values.
					alphas[2] = (6 * alphas[0] + alphas[1]) / 7;
					alphas[3] = (5 * alphas[0] + 2 * alphas[1]) / 7;
					alphas[4] = (4 * alphas[0] + 3 * alphas[1]) / 7;
					alphas[5] = (3 * alphas[0] + 4 * alphas[1]) / 7;
					alphas[6] = (2 * alphas[0] + 5 * alphas[1]) / 7;
					alphas[7] = (alphas[0] + 6 * alphas[1]) / 7;
				} else {
					alphas[2] = (4 * alphas[0] + alphas[1]) * 0.2f;
					alphas[3] = (3 * alphas[0] + 2 * alphas[1]) * 0.2f;
					alphas[4] = (2 * alphas[0] + 3 * alphas[1]) * 0.2f;
					alphas[5] = (alphas[0] + 4 * alphas[1]) * 0.2f;
					alphas[6] = 0;
					alphas[7] = 1;
				}

				short c0 = data.getShort();
				short c1 = data.getShort();
				int col0 = RGB565.RGB565_to_ARGB8(c0);
				int col1 = RGB565.RGB565_to_ARGB8(c1);
				colors[0].fromARGB8(col0);
				colors[1].fromARGB8(col1);

				// creating color2 = 2/3color0 + 1/3color1
				colors[2].fromPixel(colors[0]);
				colors[2].mult(2);
				colors[2].add(colors[1]);
				colors[2].divide(3);

				// creating color3 = 1/3color0 + 2/3color1;
				colors[3].fromPixel(colors[1]);
				colors[3].mult(2);
				colors[3].add(colors[0]);
				colors[3].divide(3);

				indexes = data.getInt();// 4-byte table with color indexes in decompressed table
				break;
			}
			case DXT1A://TODO: implement
				break;
			default:
				throw new IllegalStateException("Unsupported decompression format.");
		}
		
		// coordinates of the pixel in the selected texel
		x = x - 4 * xTexetlIndex;// pixels are arranged from left to right
		y = 3 - y - 4 * yTexelIndex;// pixels are arranged from bottom to top (that is why '3 - ...' is at the start)

		int pixelIndexInTexel = (y * 4 + x) * (int) FastMath.log(colors.length, 2);
		int alphaIndexInTexel = alphas != null ? (y * 4 + x) * (int) FastMath.log(alphas.length, 2) : 0;

		// getting the pixel
		int indexMask = colors.length - 1;
		int colorIndex = indexes >> pixelIndexInTexel & indexMask;
		float alpha = alphas != null ? alphas[(int) (alphaIndexes >> alphaIndexInTexel & 0x07)] : colors[colorIndex].alpha;
		pixel.fromPixel(colors[colorIndex]);
		pixel.alpha = alpha;
	}

	@Override
	public void write(Image image, TexturePixel pixel, int index) {
		throw new UnsupportedOperationException("Cannot put the DXT pixel by index because not every index contains the pixel color!");
	}

	@Override
	public void write(Image image, TexturePixel pixel, int x, int y) {
		throw new UnsupportedOperationException("Writing to DDS texture pixel by pixel is not yet supported!");
	}
}
