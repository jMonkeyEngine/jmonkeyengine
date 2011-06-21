package com.jme3.scene.plugins.blender.structures;

import java.util.logging.Logger;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.structures.Constraint.Space;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.Pointer;

/**
 * This class is used to calculate the constraint. The following methods should be implemented: affectLocation,
 * affectRotation and affectScale. This class also defines all constants required by known deriving classes.
 * @author Marcin Roguski
 */
public abstract class AbstractInfluenceFunction {

    protected static final Logger LOGGER = Logger.getLogger(AbstractInfluenceFunction.class.getName());
    protected static final float IK_SOLVER_ERROR = 0.5f;
    //DISTLIMIT
    protected static final int LIMITDIST_INSIDE = 0;
    protected static final int LIMITDIST_OUTSIDE = 1;
    protected static final int LIMITDIST_ONSURFACE = 2;
    //CONSTRAINT_TYPE_LOCLIKE
    protected static final int LOCLIKE_X = 0x01;
    protected static final int LOCLIKE_Y = 0x02;
    protected static final int LOCLIKE_Z = 0x04;
    //ROTLIKE
    protected static final int ROTLIKE_X = 0x01;
    protected static final int ROTLIKE_Y = 0x02;
    protected static final int ROTLIKE_Z = 0x04;
    protected static final int ROTLIKE_X_INVERT = 0x10;
    protected static final int ROTLIKE_Y_INVERT = 0x20;
    protected static final int ROTLIKE_Z_INVERT = 0x40;
    protected static final int ROTLIKE_OFFSET = 0x80;
    //SIZELIKE
    protected static final int SIZELIKE_X = 0x01;
    protected static final int SIZELIKE_Y = 0x02;
    protected static final int SIZELIKE_Z = 0x04;
    protected static final int SIZELIKE_OFFSET = 0x80;

    /* LOCLIKE_TIP is a depreceated option... use headtail=1.0f instead */
    //protected static final int LOCLIKE_TIP = 0x08;
    protected static final int LOCLIKE_X_INVERT = 0x10;
    protected static final int LOCLIKE_Y_INVERT = 0x20;
    protected static final int LOCLIKE_Z_INVERT = 0x40;
    protected static final int LOCLIKE_OFFSET = 0x80;
    //LOCLIMIT, SIZELIMIT
    protected static final int LIMIT_XMIN = 0x01;
    protected static final int LIMIT_XMAX = 0x02;
    protected static final int LIMIT_YMIN = 0x04;
    protected static final int LIMIT_YMAX = 0x08;
    protected static final int LIMIT_ZMIN = 0x10;
    protected static final int LIMIT_ZMAX = 0x20;
    //ROTLIMIT
    protected static final int LIMIT_XROT = 0x01;
    protected static final int LIMIT_YROT = 0x02;
    protected static final int LIMIT_ZROT = 0x04;
    /** The type of the constraint. */
    protected ConstraintType constraintType;
    /** The data repository. */
    protected DataRepository dataRepository;

    /**
     * Constructor.
     * @param constraintType
     *        the type of the current constraint
     * @param dataRepository
     *        the data repository
     */
    public AbstractInfluenceFunction(ConstraintType constraintType, DataRepository dataRepository) {
        this.constraintType = constraintType;
        this.dataRepository = dataRepository;
    }

    /**
     * This method validates the constraint type. It throws an IllegalArgumentException if the constraint type of the
     * given structure is invalid.
     * @param constraintStructure
     *        the structure with constraint data
     */
    protected void validateConstraintType(Structure constraintStructure) {
        if (!constraintType.getClassName().equalsIgnoreCase(constraintStructure.getType())) {
            throw new IllegalArgumentException("Invalud structure type (" + constraintStructure.getType() + ") for the constraint: " + constraintType.getClassName() + '!');
        }
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
    public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
    }

    /**
     * This method returns the bone traces for the bone that is affected by the given constraint.
     * @param skeleton
     *        the skeleton containing bones
     * @param boneAnimation
     *        the bone animation that affects the skeleton
     * @param constraint
     *        the affecting constraint
     * @return the bone track for the bone that is being affected by the constraint
     */
    protected BoneTrack getBoneTrack(Skeleton skeleton, BoneAnimation boneAnimation, Constraint constraint) {
        Long boneOMA = constraint.getBoneOMA();
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
     * @param constraint
     *        the constraint instance
     * @return target or subtarget feature
     */
    protected Object getTarget(Constraint constraint, LoadedFeatureDataType loadedFeatureDataType) {
        Long targetOMA = ((Pointer) constraint.getData().getFieldValue("tar")).getOldMemoryAddress();
        Object targetObject = dataRepository.getLoadedFeature(targetOMA, loadedFeatureDataType);
        String subtargetName = constraint.getData().getFieldValue("subtarget").toString();
        if (subtargetName.length() > 0) {
            return dataRepository.getLoadedFeature(subtargetName, loadedFeatureDataType);
        }
        return targetObject;
    }

    /**
     * This method returns target's object location.
     * @param constraint
     *        the constraint instance
     * @return target's object location
     */
    protected Vector3f getTargetLocation(Constraint constraint) {
        Long targetOMA = ((Pointer) constraint.getData().getFieldValue("tar")).getOldMemoryAddress();
        Space targetSpace = constraint.getTargetSpace();
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
     * @param constraint
     *        the constraint instance
     * @param frame
     *        the frame number
     * @return target's object location
     */
    protected Vector3f getTargetLocation(Constraint constraint, int frame) {
        return this.getTargetLocation(constraint);//TODO: implement getting location in a specified frame
    }

    /**
     * This method returns target's object rotation.
     * @param constraint
     *        the constraint instance
     * @return target's object rotation
     */
    protected Quaternion getTargetRotation(Constraint constraint) {
        Long targetOMA = ((Pointer) constraint.getData().getFieldValue("tar")).getOldMemoryAddress();
        Space targetSpace = constraint.getTargetSpace();
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
     * @param constraint
     *        the constraint instance
     * @return target's object scale
     */
    protected Vector3f getTargetScale(Constraint constraint) {
        Long targetOMA = ((Pointer) constraint.getData().getFieldValue("tar")).getOldMemoryAddress();
        Space targetSpace = constraint.getTargetSpace();
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
}
