package com.jme3.scene.plugins.blender.textures.io;

import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;

/**
 * Implemens read/write operations for luminance images.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class LuminancePixelInputOutput implements PixelInputOutput {
	@Override
	public void read(Image image, TexturePixel pixel, int index) {
		byte intensity = image.getData(0).get(index);
		pixel.fromIntensity(intensity);
	}
	
	@Override
	public void read(Image image, TexturePixel pixel, int x, int y) {
		int index = y * image.getWidth() + x;
		this.read(image, pixel, index);
	}
	
	@Override
	public void write(Image image, TexturePixel pixel, int index) {
		image.getData(0).put(index, pixel.getInt());
	}

	@Override
	public void write(Image image, TexturePixel pixel, int x, int y) {
		int index = y * image.getWidth() + x;
		this.write(image, pixel, index);
	}
}
