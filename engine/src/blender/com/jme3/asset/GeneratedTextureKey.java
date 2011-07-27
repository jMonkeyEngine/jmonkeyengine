package com.jme3.asset;


/**
 * This key is mostly used to distinguish between textures that are loaded from
 * the given assets and those being generated automatically. Every generated
 * texture will have this kind of key attached.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class GeneratedTextureKey extends TextureKey {
	/**
	 * Constructor. Stores the name. Extension and folder name are empty
	 * strings.
	 * 
	 * @param name
	 *            the name of the texture
	 */
	public GeneratedTextureKey(String name) {
		super(name);
	}

	@Override
	public String getExtension() {
		return "";
	}

	@Override
	public String getFolder() {
		return "";
	}

	@Override
	public String toString() {
		return "Generated texture [" + name + "]";
	}
}
