package com.jme3.scene.plugins.blender.constraints;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * The pivot constraint. Available for blender 2.50+.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintPivot extends Constraint {
	private static final Logger LOGGER = Logger.getLogger(ConstraintPivot.class.getName());
	
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
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
	public ConstraintPivot(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo,
			BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void bakeConstraint() {
		// TODO Auto-generated method stub
		LOGGER.log(Level.WARNING, "'Pivot' constraint NOT implemented!");
	}
}
