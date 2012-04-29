package com.jme3.scene.plugins.blender.textures.io;

import java.util.HashMap;
import java.util.Map;

import com.jme3.texture.Image.Format;

/**
 * This class creates a pixel IO object for the specified image format.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class PixelIOFactory {
	private static final Map<Format, PixelInputOutput>	PIXEL_INPUT_OUTPUT	= new HashMap<Format, PixelInputOutput>();

	/**
	 * This method returns pixel IO object for the specified format.
	 * 
	 * @param format
	 *            the format of the image
	 * @return pixel IO object
	 */
	public static PixelInputOutput getPixelIO(Format format) {
		PixelInputOutput result = PIXEL_INPUT_OUTPUT.get(format);
		if (result == null) {
			switch (format) {
				case ABGR8:
				case RGBA8:
				case BGR8:
					result = new AWTPixelInputOutput();
					break;
				case Luminance8:
					result = new LuminancePixelInputOutput();
					break;
				case DXT1:
				case DXT1A:
				case DXT3:
				case DXT5:
					result = new DDSPixelInputOutput();
					break;
				default:
					throw new IllegalStateException("Unsupported image format for IO operations: " + format);
			}
			synchronized (PIXEL_INPUT_OUTPUT) {
				PIXEL_INPUT_OUTPUT.put(format, result);
			}
		}
		return result;
	}
}
