package com.jme3.scene.plugins.blender.textures;

import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureHelper.ColorBand;
import com.jme3.texture.Texture;

/**
 * This class is a base class for texture generators.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */abstract class TextureGenerator {
	private static final Logger	LOGGER	= Logger.getLogger(TextureGenerator.class.getName());

	protected NoiseGenerator	noiseGenerator;

	public TextureGenerator(NoiseGenerator noiseGenerator) {
		this.noiseGenerator = noiseGenerator;
	}

	/**
	 * This method generates the texture.
	 * @param tex
	 *        texture's structure
	 * @param width
	 *        the width of the result texture
	 * @param height
	 *        the height of the result texture
	 * @param depth
	 *        the depth of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return newly generated texture
	 */
	protected abstract Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository);

	/**
	 * This method reads the colorband data from the given texture structure.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param dataRepository
	 *        the data repository
	 * @return read colorband or null if not present
	 */
	protected ColorBand readColorband(Structure tex, DataRepository dataRepository) {
		ColorBand result = null;
		int flag = ((Number) tex.getFieldValue("flag")).intValue();
		if ((flag & NoiseGenerator.TEX_COLORBAND) != 0) {
			Pointer pColorband = (Pointer) tex.getFieldValue("coba");
			Structure colorbandStructure;
			try {
				colorbandStructure = pColorband.fetchData(dataRepository.getInputStream()).get(0);
				result = new ColorBand(colorbandStructure);
			} catch (BlenderFileException e) {
				LOGGER.warning("Cannot fetch the colorband structure. The reason: " + e.getLocalizedMessage());
				// TODO: throw an exception here ???
			}
		}
		return result;
	}
}
