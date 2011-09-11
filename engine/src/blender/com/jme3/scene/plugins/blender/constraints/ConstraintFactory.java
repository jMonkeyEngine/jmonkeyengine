package com.jme3.scene.plugins.blender.constraints;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * A factory class to create new instances of constraints depending on the type from the constraint's structure.
 * This class has a package scope.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ final class ConstraintFactory {
	
	/**
	 * This method creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param boneOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public static Constraint createConstraint(Structure constraintStructure, Long boneOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		int type = ((Number)constraintStructure.getFieldValue("type")).intValue();
		ConstraintType constraintType = ConstraintType.valueOf(type);
			switch(constraintType) {
				case CONSTRAINT_TYPE_ACTION:
					return new ConstraintAction(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_CHILDOF:
					return new ConstraintChildOf(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_CLAMPTO:
					return new ConstraintClampTo(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_DISTLIMIT:
					return new ConstraintDistLimit(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_FOLLOWPATH:
					return new ConstraintFollowPath(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_KINEMATIC:
					return new ConstraintInverseKinematics(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_LOCKTRACK:
					return new ConstraintLockTrack(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_LOCLIKE:
					return new ConstraintLocLike(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_LOCLIMIT:
					return new ConstraintLocLimit(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_MINMAX:
					return new ConstraintMinMax(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_NULL:
					return new ConstraintNull(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_PYTHON:
					return new ConstraintPython(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_RIGIDBODYJOINT:
					return new ConstraintRigidBodyJoint(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_ROTLIKE:
					return new ConstraintRotLike(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_ROTLIMIT:
					return new ConstraintRotLimit(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_SHRINKWRAP:
					return new ConstraintShrinkWrap(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_SIZELIKE:
					return new ConstraintSizeLike(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_SIZELIMIT:
					return new ConstraintSizeLimit(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_STRETCHTO:
					return new ConstraintStretchTo(constraintStructure, boneOMA, influenceIpo, blenderContext);
				case CONSTRAINT_TYPE_TRANSFORM:
					return new ConstraintTransform(constraintStructure, boneOMA, influenceIpo, blenderContext);
				default:
					throw new IllegalStateException("Unknown constraint type: " + constraintType);
		}
	}
}
