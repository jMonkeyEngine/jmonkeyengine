package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Stretch to' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDefinitionStretchTo extends ConstraintDefinition {
	private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionStretchTo.class.getName());
	
	public ConstraintDefinitionStretchTo(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
	}

	@Override
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
		// TODO: implement 'Stretch to' constraint
		LOGGER.log(Level.WARNING, "'Stretch to' constraint NOT implemented!");
	}
}
