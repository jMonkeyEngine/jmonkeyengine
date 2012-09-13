package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Trans like' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDefinitionTransLike extends ConstraintDefinition {
	private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionTransLike.class.getName());
	
	public ConstraintDefinitionTransLike(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
	}
	
	@Override
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
		// TODO: implement 'Trans like' constraint
		LOGGER.log(Level.WARNING, "'Trans like' constraint NOT implemented!");
	}
}
