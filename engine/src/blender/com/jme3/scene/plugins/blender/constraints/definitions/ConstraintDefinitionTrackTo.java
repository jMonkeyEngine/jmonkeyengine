package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Track to' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionTrackTo extends ConstraintDefinition {
	private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionTrackTo.class.getName());

	public ConstraintDefinitionTrackTo(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
	}
	
	@Override
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
		// TODO: implement 'Track to' constraint
		LOGGER.log(Level.WARNING, "'Track to' constraint NOT implemented!");
	}
}
