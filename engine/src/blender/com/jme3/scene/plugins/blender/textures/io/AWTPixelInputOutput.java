package com.jme3.scene.plugins.blender.textures.io;

import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;

/**
 * Implemens read/write operations for AWT images.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class AWTPixelInputOutput implements PixelInputOutput {
	@Override
	public void read(Image image, TexturePixel pixel, int index) {
		byte r,g,b,a;
		switch(image.getFormat()) {//TODO: add other formats
			case RGBA8:
				r = image.getData(0).get(index);
				g = image.getData(0).get(index + 1);
				b = image.getData(0).get(index + 2);
				a = image.getData(0).get(index + 3);
				break;
			case ABGR8:
				a = image.getData(0).get(index);
				b = image.getData(0).get(index + 1);
				g = image.getData(0).get(index + 2);
				r = image.getData(0).get(index + 3);
				break;
			case BGR8:
				b = image.getData(0).get(index);
				g = image.getData(0).get(index + 1);
				r = image.getData(0).get(index + 2);
				a = (byte)0xFF;
				break;
			default:
				throw new IllegalStateException("Unknown image format: " + image.getFormat());
		}
		pixel.fromARGB8(a, r, g, b);
	}
	
	@Override
	public void read(Image image, TexturePixel pixel, int x, int y) {
		int index = (y * image.getWidth() + x) * (image.getFormat().getBitsPerPixel() >> 3);
		this.read(image, pixel, index);
	}

	@Override
	public void write(Image image, TexturePixel pixel, int index) {
		switch(image.getFormat()) {
			case RGBA8:
				image.getData(0).put(index, pixel.getR8());
				image.getData(0).put(index + 1, pixel.getG8());
				image.getData(0).put(index + 2, pixel.getB8());
				image.getData(0).put(index + 3, pixel.getA8());
				break;
			case ABGR8:
				image.getData(0).put(index, pixel.getA8());
				image.getData(0).put(index + 1, pixel.getB8());
				image.getData(0).put(index + 2, pixel.getG8());
				image.getData(0).put(index + 3, pixel.getR8());
				break;
			case BGR8:
				image.getData(0).put(index, pixel.getB8());
				image.getData(0).put(index + 1, pixel.getG8());
				image.getData(0).put(index + 2, pixel.getR8());
				break;
			default:
				throw new IllegalStateException("Unknown image format: " + image.getFormat());
		}
	}
	
	@Override
	public void write(Image image, TexturePixel pixel, int x, int y) {
		int index = (y * image.getWidth() + x) * (image.getFormat().getBitsPerPixel() >> 3);
		this.write(image, pixel, index);
	}
}
