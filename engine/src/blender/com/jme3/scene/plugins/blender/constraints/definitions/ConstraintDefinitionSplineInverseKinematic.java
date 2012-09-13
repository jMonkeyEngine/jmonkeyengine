package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * The spline inverse kinematic constraint. Available for blender 2.50+.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDefinitionSplineInverseKinematic extends ConstraintDefinition {
	private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionSplineInverseKinematic.class.getName());
	
	public ConstraintDefinitionSplineInverseKinematic(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
	}
	
	@Override
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
		// TODO Auto-generated method stub
		LOGGER.log(Level.WARNING, "'Splie IK' constraint NOT implemented!");		
	}
}
