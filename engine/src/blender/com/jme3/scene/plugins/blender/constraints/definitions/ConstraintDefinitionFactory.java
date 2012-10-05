/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.constraints.definitions;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

public class ConstraintDefinitionFactory {
	private static final Map<String, Class<? extends ConstraintDefinition>> CONSTRAINT_CLASSES = new HashMap<String, Class<? extends ConstraintDefinition>>();
	static {
		CONSTRAINT_CLASSES.put("bActionConstraint", ConstraintDefinitionAction.class);
		CONSTRAINT_CLASSES.put("bChildOfConstraint", ConstraintDefinitionChildOf.class);
		CONSTRAINT_CLASSES.put("bClampToConstraint", ConstraintDefinitionClampTo.class);
		CONSTRAINT_CLASSES.put("bDistLimitConstraint", ConstraintDefinitionDistLimit.class);
		CONSTRAINT_CLASSES.put("bFollowPathConstraint", ConstraintDefinitionFollowPath.class);
		CONSTRAINT_CLASSES.put("bKinematicConstraint", ConstraintDefinitionInverseKinematics.class);
		CONSTRAINT_CLASSES.put("bLockTrackConstraint", ConstraintDefinitionLockTrack.class);
		CONSTRAINT_CLASSES.put("bLocateLikeConstraint", ConstraintDefinitionLocLike.class);
		CONSTRAINT_CLASSES.put("bLocLimitConstraint", ConstraintDefinitionLocLimit.class);
		CONSTRAINT_CLASSES.put("bMinMaxConstraint", ConstraintDefinitionMinMax.class);
		CONSTRAINT_CLASSES.put("bNullConstraint", ConstraintDefinitionNull.class);
		CONSTRAINT_CLASSES.put("bPythonConstraint", ConstraintDefinitionPython.class);
		CONSTRAINT_CLASSES.put("bRigidBodyJointConstraint", ConstraintDefinitionRigidBodyJoint.class);
		CONSTRAINT_CLASSES.put("bRotateLikeConstraint", ConstraintDefinitionRotLike.class);
		CONSTRAINT_CLASSES.put("bShrinkWrapConstraint", ConstraintDefinitionShrinkWrap.class);
		CONSTRAINT_CLASSES.put("bSizeLikeConstraint", ConstraintDefinitionSizeLike.class);
		CONSTRAINT_CLASSES.put("bSizeLimitConstraint", ConstraintDefinitionSizeLimit.class);
		CONSTRAINT_CLASSES.put("bStretchToConstraint", ConstraintDefinitionStretchTo.class);
		CONSTRAINT_CLASSES.put("bTransformConstraint", ConstraintDefinitionTransform.class);
		CONSTRAINT_CLASSES.put("bRotLimitConstraint", ConstraintDefinitionRotLimit.class);
		//Blender 2.50+
		CONSTRAINT_CLASSES.put("bSplineIKConstraint", ConstraintDefinitionSplineInverseKinematic.class);
		CONSTRAINT_CLASSES.put("bDampTrackConstraint", ConstraintDefinitionDampTrack.class);
		CONSTRAINT_CLASSES.put("bPivotConstraint", ConstraintDefinitionDampTrack.class);
		//Blender 2.56+
		CONSTRAINT_CLASSES.put("bTrackToConstraint", ConstraintDefinitionTrackTo.class);
		CONSTRAINT_CLASSES.put("bSameVolumeConstraint", ConstraintDefinitionSameVolume.class);
		CONSTRAINT_CLASSES.put("bTransLikeConstraint", ConstraintDefinitionTransLike.class);
	}
	
	/**
	 * This method creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49). If the value is null the NullConstraint is created.
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public static ConstraintDefinition createConstraintDefinition(Structure constraintStructure, BlenderContext blenderContext) throws BlenderFileException {
		if(constraintStructure == null) {
			return new ConstraintDefinitionNull(null, blenderContext);
		}
		String constraintClassName = constraintStructure.getType();
		Class<? extends ConstraintDefinition> constraintDefinitionClass = CONSTRAINT_CLASSES.get(constraintClassName);
		if(constraintDefinitionClass != null) {
			try {
				return (ConstraintDefinition) constraintDefinitionClass.getDeclaredConstructors()[0].newInstance(constraintStructure, blenderContext);
			} catch (IllegalArgumentException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (SecurityException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (InstantiationException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (IllegalAccessException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (InvocationTargetException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			}
		} else {
			throw new BlenderFileException("Unknown constraint type: " + constraintClassName);
		}
	}
}
