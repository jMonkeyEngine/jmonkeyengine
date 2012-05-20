package com.jme3.scene.plugins.blender.textures.io;

import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image;

/**
 * Implemens read/write operations for luminance images.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class LuminancePixelInputOutput implements PixelInputOutput {
	public void read(Image image, int layer, TexturePixel pixel, int index) {
		byte intensity = image.getData(layer).get(index);
		pixel.fromIntensity(intensity);
	}
	
	public void read(Image image, int layer, TexturePixel pixel, int x, int y) {
		int index = y * image.getWidth() + x;
		this.read(image, layer, pixel, index);
	}
	
	public void write(Image image, int layer, TexturePixel pixel, int index) {
		image.getData(layer).put(index, pixel.getInt());
	}

	public void write(Image image, int layer, TexturePixel pixel, int x, int y) {
		int index = y * image.getWidth() + x;
		this.write(image, layer,pixel,  index);
	}
}
