package com.jme3.scene.plugins.blender.materials;

/**
 * An interface used in calculating alpha mask during particles' texture calculations.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ interface IAlphaMask {
	/**
	 * This method sets the size of the texture's image.
	 * @param width
	 *        the width of the image
	 * @param height
	 *        the height of the image
	 */
	void setImageSize(int width, int height);

	/**
	 * This method returns the alpha value for the specified texture position.
	 * @param x
	 *        the X coordinate of the texture position
	 * @param y
	 *        the Y coordinate of the texture position
	 * @return the alpha value for the specified texture position
	 */
	byte getAlpha(float x, float y);
}