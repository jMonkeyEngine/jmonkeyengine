package com.jme3.scene.plugins.blender.textures.io;

import java.nio.ByteBuffer;

import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;

/**
 * Implemens read/write operations for AWT images.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class AWTPixelInputOutput implements PixelInputOutput {
	public void read(Image image, int layer, TexturePixel pixel, int index) {
		byte r,g,b,a;
		ByteBuffer data = image.getData(layer);
		switch(image.getFormat()) {//TODO: add other formats
			case RGBA8:
				r = data.get(index);
				g = data.get(index + 1);
				b = data.get(index + 2);
				a = data.get(index + 3);
				break;
			case ABGR8:
				a = data.get(index);
				b = data.get(index + 1);
				g = data.get(index + 2);
				r = data.get(index + 3);
				break;
			case BGR8:
				b = data.get(index);
				g = data.get(index + 1);
				r = data.get(index + 2);
				a = (byte)0xFF;
				break;
			default:
				throw new IllegalStateException("Unknown image format: " + image.getFormat());
		}
		pixel.fromARGB8(a, r, g, b);
	}
	
	public void read(Image image, int layer, TexturePixel pixel, int x, int y) {
		int index = (y * image.getWidth() + x) * (image.getFormat().getBitsPerPixel() >> 3);
		this.read(image, layer, pixel, index);
	}

	public void write(Image image, int layer, TexturePixel pixel, int index) {
		ByteBuffer data = image.getData(layer);
		switch(image.getFormat()) {
			case RGBA8:
				data.put(index, pixel.getR8());
				data.put(index + 1, pixel.getG8());
				data.put(index + 2, pixel.getB8());
				data.put(index + 3, pixel.getA8());
				break;
			case ABGR8:
				data.put(index, pixel.getA8());
				data.put(index + 1, pixel.getB8());
				data.put(index + 2, pixel.getG8());
				data.put(index + 3, pixel.getR8());
				break;
			case BGR8:
				data.put(index, pixel.getB8());
				data.put(index + 1, pixel.getG8());
				data.put(index + 2, pixel.getR8());
				break;
			default:
				throw new IllegalStateException("Unknown image format: " + image.getFormat());
		}
	}
	
	public void write(Image image, int layer, TexturePixel pixel, int x, int y) {
		int index = (y * image.getWidth() + x) * (image.getFormat().getBitsPerPixel() >> 3);
		this.write(image, layer, pixel, index);
	}
}
