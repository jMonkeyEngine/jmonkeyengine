package com.jme3.scene.plugins.blender.structures;

import java.util.HashMap;
import java.util.Map;

/**
 * Constraint types. Definitions taken from blender sources, file: DNA_constraint_types.h. Constraint id's the same as
 * used in blender. The constraints might have duplicated type ids, depending on the blender version. The purpose of
 * this enum is to combine class name and the constraint type (id).
 * @author Marcin Roguski
 */
public enum ConstraintType {
	/* Invalid/legacy constraint */
	CONSTRAINT_TYPE_NULL(0, "bNullConstraint"),
	/* Unimplemented non longer :) - during constraints recode, Aligorith */
	CONSTRAINT_TYPE_CHILDOF(1, "bChildOfConstraint"), 
	CONSTRAINT_TYPE_KINEMATIC(3, "bKinematicConstraint"), 
	CONSTRAINT_TYPE_FOLLOWPATH(4, "bFollowPathConstraint"),
	/* Unimplemented no longer :) - Aligorith */
	CONSTRAINT_TYPE_ROTLIMIT(5, "bRotLimitConstraint"),
	/* Unimplemented no longer :) - Aligorith */
	CONSTRAINT_TYPE_LOCLIMIT(6, "bLocLimitConstraint"),
	/* Unimplemented no longer :) - Aligorith */
	CONSTRAINT_TYPE_SIZELIMIT(7, "bSizeLimitConstraint"), 
	CONSTRAINT_TYPE_ROTLIKE(8, "bRotateLikeConstraint"), 
	CONSTRAINT_TYPE_LOCLIKE(9, "bLocateLikeConstraint"), 
	CONSTRAINT_TYPE_SIZELIKE(10, "bSizeLikeConstraint"),
	/* Unimplemented no longer :) - Aligorith. Scripts */
	CONSTRAINT_TYPE_PYTHON(11, "bPythonConstraint"), 
	CONSTRAINT_TYPE_ACTION(12, "bActionConstraint"),
	/* New Tracking constraint that locks an axis in place - theeth */
	CONSTRAINT_TYPE_LOCKTRACK(13, "bLockTrackConstraint"),
	/* limit distance */
	CONSTRAINT_TYPE_DISTLIMIT(14, "bDistLimitConstraint"),
	/* claiming this to be mine :) is in tuhopuu bjornmose */
	CONSTRAINT_TYPE_STRETCHTO(15, "bStretchToConstraint"),
	/* floor constraint */
	CONSTRAINT_TYPE_MINMAX(16, "bMinMaxConstraint"),
	/* rigidbody constraint */
	CONSTRAINT_TYPE_RIGIDBODYJOINT(17, "bRigidBodyConstraint"),
	/* clampto constraint */
	CONSTRAINT_TYPE_CLAMPTO(18, "bClampToConstraint"),
	/* transformation (loc/rot/size -> loc/rot/size) constraint */
	CONSTRAINT_TYPE_TRANSFORM(19, "bTransformConstraint"),
	/* shrinkwrap (loc/rot) constraint */
	CONSTRAINT_TYPE_SHRINKWRAP(20, "bShrinkwrapConstraint");

	/** The constraint's id (in blender known as 'type'). */
	private int									constraintId;
	/** The name of constraint class used by blender. */
	private String								className;
	/** The map containing class names and types of constraints. */
	private static Map<String, ConstraintType>	typesMap	= new HashMap<String, ConstraintType>(ConstraintType.values().length);
	/** The map containing class names and types of constraints. */
	private static Map<Integer, ConstraintType>	idsMap	= new HashMap<Integer, ConstraintType>(ConstraintType.values().length);
	static {
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_NULL.constraintId), CONSTRAINT_TYPE_NULL);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_CHILDOF.constraintId), CONSTRAINT_TYPE_CHILDOF);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_KINEMATIC.constraintId), CONSTRAINT_TYPE_KINEMATIC);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_FOLLOWPATH.constraintId), CONSTRAINT_TYPE_FOLLOWPATH);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_ROTLIMIT.constraintId), CONSTRAINT_TYPE_ROTLIMIT);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_LOCLIMIT.constraintId), CONSTRAINT_TYPE_LOCLIMIT);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_SIZELIMIT.constraintId), CONSTRAINT_TYPE_SIZELIMIT);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_ROTLIKE.constraintId), CONSTRAINT_TYPE_ROTLIKE);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_LOCLIKE.constraintId), CONSTRAINT_TYPE_LOCLIKE);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_SIZELIKE.constraintId), CONSTRAINT_TYPE_SIZELIKE);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_PYTHON.constraintId), CONSTRAINT_TYPE_PYTHON);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_ACTION.constraintId), CONSTRAINT_TYPE_ACTION);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_LOCKTRACK.constraintId), CONSTRAINT_TYPE_LOCKTRACK);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_DISTLIMIT.constraintId), CONSTRAINT_TYPE_DISTLIMIT);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_STRETCHTO.constraintId), CONSTRAINT_TYPE_STRETCHTO);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_MINMAX.constraintId), CONSTRAINT_TYPE_MINMAX);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_RIGIDBODYJOINT.constraintId), CONSTRAINT_TYPE_RIGIDBODYJOINT);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_CLAMPTO.constraintId), CONSTRAINT_TYPE_CLAMPTO);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_TRANSFORM.constraintId), CONSTRAINT_TYPE_TRANSFORM);
		idsMap.put(Integer.valueOf(CONSTRAINT_TYPE_SHRINKWRAP.constraintId), CONSTRAINT_TYPE_SHRINKWRAP);
	}
	/**
	 * Constructor. Stores constraint type and class name.
	 * @param constraintId
	 *        the constraint's type
	 * @param className
	 *        the constraint's type name
	 */
	private ConstraintType(int constraintId, String className) {
		this.constraintId = constraintId;
		this.className = className;
	}

	/**
	 * This method returns the type by given constraint id.
	 * @param constraintId
	 *        the id of the constraint
	 * @return the constraint type enum value
	 */
	public static ConstraintType valueOf(int constraintId) {
		return idsMap.get(Integer.valueOf(constraintId));
	}

	/**
	 * This method returns the constraint's id (type).
	 * @return the constraint's id (type)
	 */
	public int getConstraintId() {
		return constraintId;
	}

	/**
	 * This method returns the constraint's class name.
	 * @return the constraint's class name
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * This method returns constraint enum type by the given class name.
	 * @param className
	 *        the blender's constraint class name
	 * @return the constraint enum type of the specified class name
	 */
	public static ConstraintType getByBlenderClassName(String className) {
		ConstraintType result = typesMap.get(className);
		if(result == null) {
			ConstraintType[] constraints = ConstraintType.values();
			for(ConstraintType constraint : constraints) {
				if(constraint.className.equals(className)) {
					return constraint;
				}
			}
		}
		return result;
	}

	/**
	 * This method returns the type value of the last defined constraint. It can be used for allocating tables for
	 * storing constraint procedures since not all type values from 0 to the last value are used.
	 * @return the type value of the last defined constraint
	 */
	public static int getLastDefinedTypeValue() {
		return CONSTRAINT_TYPE_SHRINKWRAP.getConstraintId();
	}
}
