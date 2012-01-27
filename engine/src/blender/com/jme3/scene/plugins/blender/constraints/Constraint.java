package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SpatialTrack;
import com.jme3.animation.Track;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * The implementation of a constraint.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class Constraint {
	/** The name of this constraint. */
	protected final String name;
	/** The constraint's owner. */
	protected final Feature owner;
	/** The constraint's target. */
	protected final Feature target;
	/** The structure with constraint's data. */
	protected final Structure data;
	/** The ipo object defining influence. */
	protected final Ipo ipo;
	/** The blender context. */
	protected final BlenderContext blenderContext;
	
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
	public Constraint(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		this.blenderContext = blenderContext;
		this.name = constraintStructure.getFieldValue("name").toString();
		Pointer pData = (Pointer) constraintStructure.getFieldValue("data");
		if (pData.isNotNull()) {
			data = pData.fetchData(blenderContext.getInputStream()).get(0);
			Pointer pTar = (Pointer)data.getFieldValue("tar");
			if(pTar!= null && pTar.isNotNull()) {
				Structure targetStructure = pTar.fetchData(blenderContext.getInputStream()).get(0);
				Long targetOMA = pTar.getOldMemoryAddress();
				Space targetSpace = Space.valueOf(((Number) constraintStructure.getFieldValue("tarspace")).byteValue());
				ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
				Spatial target = (Spatial) objectHelper.toObject(targetStructure, blenderContext);
				this.target = new Feature(target, targetSpace, targetOMA, blenderContext);
			} else {
				this.target = null;
			}
		} else {
			throw new BlenderFileException("The constraint has no data specified!");
		}
		Space ownerSpace = Space.valueOf(((Number) constraintStructure.getFieldValue("ownspace")).byteValue());
		this.owner = new Feature(ownerSpace, ownerOMA, blenderContext);
		this.ipo = influenceIpo;
	}

	/**
	 * This method bakes the required sontraints into its owner.
	 */
	public void bake() {
		this.owner.update();
		if(this.target != null) {
			this.target.update();
		}
		this.bakeConstraint();
	}
	
	/**
	 * Bake the animation's constraints into its owner.
	 */
	protected abstract void bakeConstraint();
	
    /**
     * This method returns the bone traces for the bone that is affected by the given constraint.
     * @param skeleton
     *        the skeleton containing bones
     * @param boneAnimation
     *        the bone animation that affects the skeleton
     * @return the bone track for the bone that is being affected by the constraint
     */
    protected BlenderTrack getTrack(Object owner, Skeleton skeleton, Animation animation) {
    	if(owner instanceof Bone) {
    		int boneIndex = skeleton.getBoneIndex((Bone) owner);
    		for (Track track : animation.getTracks()) {
                if (((BoneTrack) track).getTargetBoneIndex() == boneIndex) {
                    return new BlenderTrack(((BoneTrack) track));
                }
            }
    		throw new IllegalStateException("Cannot find track for: " + owner);
    	} else {
    		return new BlenderTrack((SpatialTrack)animation.getTracks()[0]);
    	}
    }
    
	/**
	 * The space of target or owner transformation.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	public static enum Space {

		CONSTRAINT_SPACE_WORLD, CONSTRAINT_SPACE_LOCAL, CONSTRAINT_SPACE_POSE, CONSTRAINT_SPACE_PARLOCAL, CONSTRAINT_SPACE_INVALID;

		/**
		 * This method returns the enum instance when given the appropriate
		 * value from the blend file.
		 * 
		 * @param c
		 *            the blender's value of the space modifier
		 * @return the scape enum instance
		 */
		public static Space valueOf(byte c) {
			switch (c) {
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