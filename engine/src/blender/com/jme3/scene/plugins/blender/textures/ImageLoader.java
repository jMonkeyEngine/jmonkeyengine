package com.jme3.scene.plugins.blender.textures;

import java.io.InputStream;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.textures.TextureHelper.ImageType;
import com.jme3.texture.Image;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.texture.plugins.DDSLoader;
import com.jme3.texture.plugins.TGALoader;

/**
 * An image loader class. It uses three loaders (AWTLoader, TGALoader and DDSLoader) in an attempt to load the image from the given
 * input stream.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ImageLoader extends AWTLoader {
	private static final Logger	LOGGER		= Logger.getLogger(ImageLoader.class.getName());

	protected DDSLoader			ddsLoader	= new DDSLoader();									// DirectX image loader

	/**
	 * This method loads the image from the blender file itself. It tries each loader to load the image.
	 * 
	 * @param inputStream
	 *        blender input stream
	 * @param startPosition
	 *        position in the stream where the image data starts
	 * @param flipY
	 *        if the image should be flipped (does not work with DirectX image)
	 * @return loaded image or null if it could not be loaded
	 */
	public Image loadImage(BlenderInputStream inputStream, int startPosition, boolean flipY) {
		// loading using AWT loader
		inputStream.setPosition(startPosition);
		Image result = this.loadImage(inputStream, ImageType.AWT, flipY);
		// loading using TGA loader
		if (result == null) {
			inputStream.setPosition(startPosition);
			result = this.loadImage(inputStream, ImageType.TGA, flipY);
		}
		// loading using DDS loader
		if (result == null) {
			inputStream.setPosition(startPosition);
			result = this.loadImage(inputStream, ImageType.DDS, flipY);
		}

		if (result == null) {
			LOGGER.warning("Image could not be loaded by none of available loaders!");
		}

		return result;
	}

	/**
	 * This method loads an image of a specified type from the given input stream.
	 * 
	 * @param inputStream
	 *        the input stream we read the image from
	 * @param imageType
	 *        the type of the image {@link ImageType}
	 * @param flipY
	 *        if the image should be flipped (does not work with DirectX image)
	 * @return loaded image or null if it could not be loaded
	 */
	public Image loadImage(InputStream inputStream, ImageType imageType, boolean flipY) {
		Image result = null;
		switch (imageType) {
			case AWT:
				try {
					result = this.load(inputStream, flipY);
				} catch (Exception e) {
					LOGGER.info("Unable to load image using AWT loader!");
				}
				break;
			case DDS:
				try {
					result = ddsLoader.load(inputStream);
				} catch (Exception e) {
					LOGGER.info("Unable to load image using DDS loader!");
				}
				break;
			case TGA:
				try {
					result = TGALoader.load(inputStream, flipY);
				} catch (Exception e) {
					LOGGER.info("Unable to load image using TGA loader!");
				}
				break;
			default:
				throw new IllegalStateException("Unknown image type: " + imageType);
		}
		return result;
	}
}
