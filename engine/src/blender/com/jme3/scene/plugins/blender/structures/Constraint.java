package com.jme3.scene.plugins.blender.structures;

import com.jme3.animation.BoneAnimation;
import com.jme3.animation.Skeleton;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.Pointer;

/**
 * The implementation of a constraint.
 * @author Marcin Roguski
 */
public class Constraint {
	/** The type of this constraint. */
	private final ConstraintType			type;
	/** The name of this constraint. */
	private final String					name;
	/** The old memory address of the constraint's owner. */
	private final Long						boneOMA;

	private final Space						ownerSpace;

	private final Space						targetSpace;
	/** The structure with constraint's data. */
	private final Structure					data;
	/** The ipo object defining influence. */
	private final Ipo						ipo;
	/** The influence function of this constraint. */
	private final AbstractInfluenceFunction	influenceFunction;

	/**
	 * This constructor creates the constraint instance.
	 * @param constraintStructure
	 *        the constraint's structure (bConstraint clss in blender 2.49).
	 * @param influenceFunction
	 *        the constraint's influence function (taken from ConstraintHelper)
	 * @param boneOMA
	 *        the old memory address of the constraint owner
	 * @param influenceIpo
	 *        the ipo curve of the influence factor
	 * @param dataRepository
	 *        the data repository
	 * @throws BlenderFileException
	 */
	public Constraint(Structure constraintStructure, AbstractInfluenceFunction influenceFunction, Long boneOMA, Space ownerSpace, Space targetSpace, Ipo influenceIpo, DataRepository dataRepository) throws BlenderFileException {
		if(influenceFunction == null) {
			throw new IllegalArgumentException("Influence function is not defined!");
		}
		Pointer pData = (Pointer)constraintStructure.getFieldValue("data");
		if(!pData.isNull()) {
			data = pData.fetchData(dataRepository.getInputStream()).get(0);
		} else {
			throw new BlenderFileException("The constraint has no data specified!");
		}
		this.boneOMA = boneOMA;
		this.type = ConstraintType.valueOf(((Number)constraintStructure.getFieldValue("type")).intValue());
		this.name = constraintStructure.getFieldValue("name").toString();
		this.ownerSpace = ownerSpace;
		this.targetSpace = targetSpace;
		this.ipo = influenceIpo;
		this.influenceFunction = influenceFunction;
	}

	/**
	 * This method returns the name of the constraint.
	 * @return the name of the constraint
	 */
	public String getName() {
		return name;
	}

	/**
	 * This method returns the old memoty address of the bone this constraint affects.
	 * @return the old memory address of the bone this constraint affects
	 */
	public Long getBoneOMA() {
		return boneOMA;
	}

	/**
	 * This method returns owner's transform space.
	 * @return owner's transform space
	 */
	public Space getOwnerSpace() {
		return ownerSpace;
	}

	/**
	 * This method returns target's transform space.
	 * @return target's transform space
	 */
	public Space getTargetSpace() {
		return targetSpace;
	}

	/**
	 * This method returns the type of the constraint.
	 * @return the type of the constraint
	 */
	public ConstraintType getType() {
		return type;
	}

	/**
	 * This method returns the constraint's data structure.
	 * @return the constraint's data structure
	 */
	public Structure getData() {
		return data;
	}

	/**
	 * This method returns the constraint's influcence curve.
	 * @return the constraint's influcence curve
	 */
	public Ipo getIpo() {
		return ipo;
	}

	/**
	 * This method affects the bone animation tracks for the given skeleton.
	 * @param skeleton
	 *        the skeleton containing the affected bones by constraint
	 * @param boneAnimation
	 *        the bone animation baked traces
	 * @param constraint
	 *        the constraint
	 */
	public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation) {
		influenceFunction.affectAnimation(skeleton, boneAnimation, this);
	}

	/**
	 * The space of target or owner transformation.
	 * @author Marcin Roguski
	 */
	public static enum Space {
		CONSTRAINT_SPACE_WORLD, CONSTRAINT_SPACE_LOCAL, CONSTRAINT_SPACE_POSE, CONSTRAINT_SPACE_PARLOCAL, CONSTRAINT_SPACE_INVALID;

		/**
		 * This method returns the enum instance when given the appropriate value from the blend file.
		 * @param c
		 *        the blender's value of the space modifier
		 * @return the scape enum instance
		 */
		public static Space valueOf(byte c) {
			switch(c) {
				case 0:
					return CONSTRAINT_SPACE_WORLD;
				case 1:
					return CONSTRAINT_SPACE_LOCAL;
				case 2:
					return CONSTRAINT_SPACE_POSE;
				case 3:
					return CONSTRAINT_SPACE_PARLOCAL;
				default:
					return CONSTRAINT_SPACE_INVALID;
			}
		}
	}
}