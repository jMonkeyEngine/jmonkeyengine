package com.jme3.scene.plugins.blender.constraints;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents 'Stretch to' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintStretchTo extends Constraint {
	private static final Logger LOGGER = Logger.getLogger(ConstraintStretchTo.class.getName());
	
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure
	 * @param ownerOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintStretchTo(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
	}

	@Override
	protected void bakeConstraint() {
		// TODO: implement 'Stretch to' constraint
		LOGGER.log(Level.WARNING, "'Stretch to' constraint NOT implemented!");
	}
}
