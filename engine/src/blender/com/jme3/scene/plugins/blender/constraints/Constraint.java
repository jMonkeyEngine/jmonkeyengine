package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * The implementation of a constraint.
 * 
 * @author Marcin Roguski
 */
public abstract class Constraint {

	/** The name of this constraint. */
	protected final String name;
	/** The old memory address of the constraint's owner. */
	protected final Long boneOMA;
	protected final Space ownerSpace;
	protected final Space targetSpace;
	/** The structure with constraint's data. */
	protected final Structure data;
	/** The ipo object defining influence. */
	protected final Ipo ipo;
	protected DataRepository dataRepository;
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param boneOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param dataRepository
	 *            the data repository
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public Constraint(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, DataRepository dataRepository) throws BlenderFileException {
		this.name = constraintStructure.getFieldValue("name").toString();
		ConstraintType constraintType = ConstraintType.valueOf(((Number)constraintStructure.getFieldValue("type")).intValue());
		if(constraintType != this.getType()) {
			throw new IllegalStateException("Constraint structure does not match its type for constraint: " + name);
		}
		Pointer pData = (Pointer) constraintStructure.getFieldValue("data");
		if (pData.isNotNull()) {
			data = pData.fetchData(dataRepository.getInputStream()).get(0);
		} else {
			throw new BlenderFileException("The constraint has no data specified!");
		}
		this.boneOMA = boneOMA;
		this.ownerSpace = Space.valueOf(((Number) constraintStructure.getFieldValue("ownspace")).byteValue());
		this.targetSpace = Space.valueOf(((Number) constraintStructure.getFieldValue("tarspace")).byteValue());
		this.ipo = influenceIpo;
	}

	/**
	 * This method returns the name of the constraint.
	 * 
	 * @return the name of the constraint
	 */
	public String getName() {
		return name;
	}

	/**
	 * This method returns the old memoty address of the bone this constraint
	 * affects.
	 * 
	 * @return the old memory address of the bone this constraint affects
	 */
	public Long getBoneOMA() {
		return boneOMA;
	}

	/**
	 * This method returns the type of the constraint.
	 * 
	 * @return the type of the constraint
	 */
	public abstract ConstraintType getType();

	/**
     * This method returns the bone traces for the bone that is affected by the given constraint.
     * @param skeleton
     *        the skeleton containing bones
     * @param boneAnimation
     *        the bone animation that affects the skeleton
     * @return the bone track for the bone that is being affected by the constraint
     */
    protected BoneTrack getBoneTrack(Skeleton skeleton, BoneAnimation boneAnimation) {
        Bone bone = (Bone) dataRepository.getLoadedFeature(boneOMA, LoadedFeatureDataType.LOADED_FEATURE);
        int boneIndex = bone==null ? 0 : skeleton.getBoneIndex(bone);//bone==null may mean the object animation
        if (boneIndex != -1) {
            //searching for track for this bone
            for (BoneTrack boneTrack : boneAnimation.getTracks()) {
                if (boneTrack.getTargetBoneIndex() == boneIndex) {
                    return boneTrack;
                }
            }
        }
        return null;
    }

    /**
     * This method returns the target or subtarget object (if specified).
     * @param loadedFeatureDataType
     * @return target or subtarget feature
     * @throws BlenderFileException this exception is thrown if the blend file is somehow corrupted
     */
    protected Object getTarget(LoadedFeatureDataType loadedFeatureDataType) throws BlenderFileException {
    	//load the feature through objectHelper, this way we are certain the object loads and has
    	//his own constraints applied to traces
    	ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
    	//always load the target first
    	Long targetOMA = ((Pointer) data.getFieldValue("tar")).getOldMemoryAddress();
        Structure objectStructure = dataRepository.getFileBlock(targetOMA).getStructure(dataRepository);
        Object result = objectHelper.toObject(objectStructure, dataRepository);
    	
    	//subtarget should be loaded alogn with target
    	Object subtarget = data.getFieldValue("subtarget");
    	String subtargetName = subtarget==null ? null : subtarget.toString();
        if (subtargetName!=null && subtargetName.length() > 0) {
            result = dataRepository.getLoadedFeature(subtargetName, loadedFeatureDataType);
        }
        return result;
    }
	
	/**
     * This method returns target's object location.
     * @return target's object location
     */
    protected Vector3f getTargetLocation() {
        Long targetOMA = ((Pointer) data.getFieldValue("tar")).getOldMemoryAddress();
        Node targetObject = (Node) dataRepository.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE);
        switch (targetSpace) {
            case CONSTRAINT_SPACE_LOCAL:
                return targetObject.getLocalTranslation();
            case CONSTRAINT_SPACE_WORLD:
                return targetObject.getWorldTranslation();
            default:
                throw new IllegalStateException("Invalid space type for target object: " + targetSpace.toString());
        }
    }

    /**
     * This method returns target's object location in the specified frame.
     * @param frame
     *        the frame number
     * @return target's object location
     */
    protected Vector3f getTargetLocation(int frame) {
        return this.getTargetLocation();//TODO: implement getting location in a specified frame
    }

    /**
     * This method returns target's object rotation.
     * @return target's object rotation
     */
    protected Quaternion getTargetRotation() {
        Long targetOMA = ((Pointer) data.getFieldValue("tar")).getOldMemoryAddress();
        Node targetObject = (Node) dataRepository.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE);
        switch (targetSpace) {
            case CONSTRAINT_SPACE_LOCAL:
                return targetObject.getLocalRotation();
            case CONSTRAINT_SPACE_WORLD:
                return targetObject.getWorldRotation();
            default:
                throw new IllegalStateException("Invalid space type for target object: " + targetSpace.toString());
        }
    }

    /**
     * This method returns target's object scale.
     * @return target's object scale
     */
    protected Vector3f getTargetScale() {
        Long targetOMA = ((Pointer) data.getFieldValue("tar")).getOldMemoryAddress();
        Node targetObject = (Node) dataRepository.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE);
        switch (targetSpace) {
            case CONSTRAINT_SPACE_LOCAL:
                return targetObject.getLocalScale();
            case CONSTRAINT_SPACE_WORLD:
                return targetObject.getWorldScale();
            default:
                throw new IllegalStateException("Invalid space type for target object: " + targetSpace.toString());
        }
    }
    
	/**
	 * This method affects the bone animation tracks for the given skeleton.
	 * 
	 * @param skeleton
	 *            the skeleton containing the affected bones by constraint
	 * @param boneAnimation
	 *            the bone animation baked traces
	 * @param constraint
	 *            the constraint
	 */
	public abstract void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation);

	/**
	 * The space of target or owner transformation.
	 * 
	 * @author Marcin Roguski
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