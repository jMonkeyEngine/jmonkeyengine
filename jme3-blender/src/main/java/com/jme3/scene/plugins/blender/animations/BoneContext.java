package com.jme3.scene.plugins.blender.animations;

import java.util.ArrayList;
import java.util.List;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * This class holds the basic data that describes a bone.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BoneContext {
    // the flags of the bone
    public static final int      SELECTED                            = 0x0001;
    public static final int      CONNECTED_TO_PARENT                 = 0x0010;
    public static final int      DEFORM                              = 0x1000;

    /**
     * The bones' matrices have, unlike objects', the coordinate system identical to JME's (Y axis is UP, X to the right and Z toward us).
     * So in order to have them loaded properly we need to transform their armature matrix (which blender sees as rotated) to make sure we get identical results.
     */
    public static final Matrix4f BONE_ARMATURE_TRANSFORMATION_MATRIX = new Matrix4f(1, 0, 0, 0, 0, 0, 1, 0, 0, -1, 0, 0, 0, 0, 0, 1);

    private static final int     IKFLAG_LOCK_X                       = 0x01;
    private static final int     IKFLAG_LOCK_Y                       = 0x02;
    private static final int     IKFLAG_LOCK_Z                       = 0x04;
    private static final int     IKFLAG_LIMIT_X                      = 0x08;
    private static final int     IKFLAG_LIMIT_Y                      = 0x10;
    private static final int     IKFLAG_LIMIT_Z                      = 0x20;

    private BlenderContext       blenderContext;
    /** The OMA of the bone's armature object. */
    private Long                 armatureObjectOMA;
    /** The OMA of the model that owns the bone's skeleton. */
    private Long                 skeletonOwnerOma;
    /** The structure of the bone. */
    private Structure            boneStructure;
    /** Bone's name. */
    private String               boneName;
    /** The bone's flag. */
    private int                  flag;
    /** The bone's matrix in world space. */
    private Matrix4f             globalBoneMatrix;
    /** The bone's matrix in the model space. */
    private Matrix4f             boneMatrixInModelSpace;
    /** The parent context. */
    private BoneContext          parent;
    /** The children of this context. */
    private List<BoneContext>    children                            = new ArrayList<BoneContext>();
    /** Created bone (available after calling 'buildBone' method). */
    private Bone                 bone;
    /** The length of the bone. */
    private float                length;
    /** The bone's deform envelope. */
    private BoneEnvelope         boneEnvelope;

    // The below data is used only for IK constraint computations.

    /** The bone's stretch value. */
    private float                ikStretch;
    /** Bone's rotation minimum values. */
    private Vector3f             limitMin;
    /** Bone's rotation maximum values. */
    private Vector3f             limitMax;
    /** The bone's stiffness values (how much it rotates during IK computations. */
    private Vector3f             stiffness;
    /** Values that indicate if any axis' rotation should be limited by some angle. */
    private boolean[]            limits;
    /** Values that indicate if any axis' rotation should be disabled during IK computations. */
    private boolean[]            locks;

    /**
     * Constructor. Creates the basic set of bone's data.
     * 
     * @param armatureObjectOMA
     *            the OMA of the bone's armature object
     * @param boneStructure
     *            the bone's structure
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when problem with blender data reading
     *             occurs
     */
    public BoneContext(Long armatureObjectOMA, Structure boneStructure, BlenderContext blenderContext) throws BlenderFileException {
        this(boneStructure, armatureObjectOMA, null, blenderContext);
    }

    /**
     * Constructor. Creates the basic set of bone's data.
     * 
     * @param boneStructure
     *            the bone's structure
     * @param armatureObjectOMA
     *            the OMA of the bone's armature object
     * @param parent
     *            bone's parent (null if the bone is the root bone)
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when problem with blender data reading
     *             occurs
     */
    @SuppressWarnings("unchecked")
    private BoneContext(Structure boneStructure, Long armatureObjectOMA, BoneContext parent, BlenderContext blenderContext) throws BlenderFileException {
        this.parent = parent;
        this.blenderContext = blenderContext;
        this.boneStructure = boneStructure;
        this.armatureObjectOMA = armatureObjectOMA;
        boneName = boneStructure.getFieldValue("name").toString();
        flag = ((Number) boneStructure.getFieldValue("flag")).intValue();
        length = ((Number) boneStructure.getFieldValue("length")).floatValue();
        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);

        // first get the bone matrix in its armature space
        globalBoneMatrix = objectHelper.getMatrix(boneStructure, "arm_mat", blenderContext.getBlenderKey().isFixUpAxis());
        if (blenderContext.getBlenderKey().isFixUpAxis()) {
            // then make sure it is rotated in a proper way to fit the jme bone transformation conventions
            globalBoneMatrix.multLocal(BONE_ARMATURE_TRANSFORMATION_MATRIX);
        }

        Structure armatureStructure = blenderContext.getFileBlock(armatureObjectOMA).getStructure(blenderContext);
        Spatial armature = (Spatial) objectHelper.toObject(armatureStructure, blenderContext);
        ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
        Matrix4f armatureWorldMatrix = constraintHelper.toMatrix(armature.getWorldTransform(), new Matrix4f());

        // and now compute the final bone matrix in world space
        globalBoneMatrix = armatureWorldMatrix.mult(globalBoneMatrix);

        // load the bone deformation envelope if necessary
        if ((flag & DEFORM) == 0) {// if the flag is NOT set then the DEFORM is in use
            boneEnvelope = new BoneEnvelope(boneStructure, armatureWorldMatrix, blenderContext.getBlenderKey().isFixUpAxis());
        }

        // load bone's pose channel data
        Pointer pPose = (Pointer) armatureStructure.getFieldValue("pose");
        if (pPose != null && pPose.isNotNull()) {
            List<Structure> poseChannels = ((Structure) pPose.fetchData().get(0).getFieldValue("chanbase")).evaluateListBase();
            for (Structure poseChannel : poseChannels) {
                Long boneOMA = ((Pointer) poseChannel.getFieldValue("bone")).getOldMemoryAddress();
                if (boneOMA.equals(this.boneStructure.getOldMemoryAddress())) {
                    ikStretch = ((Number) poseChannel.getFieldValue("ikstretch")).floatValue();
                    DynamicArray<Number> limitMin = (DynamicArray<Number>) poseChannel.getFieldValue("limitmin");
                    this.limitMin = new Vector3f(limitMin.get(0).floatValue(), limitMin.get(1).floatValue(), limitMin.get(2).floatValue());

                    DynamicArray<Number> limitMax = (DynamicArray<Number>) poseChannel.getFieldValue("limitmax");
                    this.limitMax = new Vector3f(limitMax.get(0).floatValue(), limitMax.get(1).floatValue(), limitMax.get(2).floatValue());

                    DynamicArray<Number> stiffness = (DynamicArray<Number>) poseChannel.getFieldValue("stiffness");
                    this.stiffness = new Vector3f(stiffness.get(0).floatValue(), stiffness.get(1).floatValue(), stiffness.get(2).floatValue());

                    int ikFlag = ((Number) poseChannel.getFieldValue("ikflag")).intValue();
                    locks = new boolean[] { (ikFlag & IKFLAG_LOCK_X) != 0, (ikFlag & IKFLAG_LOCK_Y) != 0, (ikFlag & IKFLAG_LOCK_Z) != 0 };
                    // limits are enabled when locks are disabled, so we ween to take that into account here
                    limits = new boolean[] { (ikFlag & IKFLAG_LIMIT_X & ~IKFLAG_LOCK_X) != 0, (ikFlag & IKFLAG_LIMIT_Y & ~IKFLAG_LOCK_Y) != 0, (ikFlag & IKFLAG_LIMIT_Z & ~IKFLAG_LOCK_Z) != 0 };
                    break;// we have found what we need, no need to search further
                }
            }
        }

        // create the children
        List<Structure> childbase = ((Structure) boneStructure.getFieldValue("childbase")).evaluateListBase();
        for (Structure child : childbase) {
            children.add(new BoneContext(child, armatureObjectOMA, this, blenderContext));
        }

        blenderContext.setBoneContext(boneStructure.getOldMemoryAddress(), this);
    }

    /**
     * This method builds the bone. It recursively builds the bone's children.
     * 
     * @param bones
     *            a list of bones where the newly created bone will be added
     * @param skeletonOwnerOma
     *            the spatial of the object that will own the skeleton
     * @param blenderContext
     *            the blender context
     * @return newly created bone
     */
    public Bone buildBone(List<Bone> bones, Long skeletonOwnerOma, BlenderContext blenderContext) {
        this.skeletonOwnerOma = skeletonOwnerOma;
        Long boneOMA = boneStructure.getOldMemoryAddress();
        bone = new Bone(boneName);
        bones.add(bone);
        blenderContext.addLoadedFeatures(boneOMA, LoadedDataType.STRUCTURE, boneStructure);
        blenderContext.addLoadedFeatures(boneOMA, LoadedDataType.FEATURE, bone);
        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);

        Structure skeletonOwnerObjectStructure = (Structure) blenderContext.getLoadedFeature(skeletonOwnerOma, LoadedDataType.STRUCTURE);
        // I could load 'imat' here, but apparently in some older blenders there were bugs or unfinished functionalities that stored ZERO matrix in imat field
        // loading 'obmat' and inverting it makes us avoid errors in such cases
        Matrix4f invertedObjectOwnerGlobalMatrix = objectHelper.getMatrix(skeletonOwnerObjectStructure, "obmat", blenderContext.getBlenderKey().isFixUpAxis()).invertLocal();
        if (objectHelper.isParent(skeletonOwnerOma, armatureObjectOMA)) {
            boneMatrixInModelSpace = globalBoneMatrix.mult(invertedObjectOwnerGlobalMatrix);
        } else {
            boneMatrixInModelSpace = invertedObjectOwnerGlobalMatrix.mult(globalBoneMatrix);
        }

        Matrix4f boneLocalMatrix = parent == null ? boneMatrixInModelSpace : parent.boneMatrixInModelSpace.invert().multLocal(boneMatrixInModelSpace);

        Vector3f poseLocation = parent == null || !this.is(CONNECTED_TO_PARENT) ? boneLocalMatrix.toTranslationVector() : new Vector3f(0, parent.length, 0);
        Quaternion rotation = boneLocalMatrix.toRotationQuat().normalizeLocal();
        Vector3f scale = boneLocalMatrix.toScaleVector();

        bone.setBindTransforms(poseLocation, rotation, scale);
        for (BoneContext child : children) {
            bone.addChild(child.buildBone(bones, skeletonOwnerOma, blenderContext));
        }

        return bone;
    }

    /**
     * @return built bone (available after calling 'buildBone' method)
     */
    public Bone getBone() {
        return bone;
    }

    /**
     * @return the old memory address of the bone
     */
    public Long getBoneOma() {
        return boneStructure.getOldMemoryAddress();
    }

    /**
     * The method returns the length of the bone.
     * If you want to use it for bone debugger take model space scale into account and do
     * something like this:
     * <b>boneContext.getLength() * boneContext.getBone().getModelSpaceScale().y</b>.
     * Otherwise the bones might not look as they should in the bone debugger.
     * @return the length of the bone
     */
    public float getLength() {
        return length;
    }

    /**
     * @return OMA of the bone's armature object
     */
    public Long getArmatureObjectOMA() {
        return armatureObjectOMA;
    }

    /**
     * @return the OMA of the model that owns the bone's skeleton
     */
    public Long getSkeletonOwnerOma() {
        return skeletonOwnerOma;
    }

    /**
     * @return the skeleton the bone of this context belongs to
     */
    public Skeleton getSkeleton() {
        return blenderContext.getSkeleton(armatureObjectOMA);
    }

    /**
     * @return the initial bone's matrix in model space
     */
    public Matrix4f getBoneMatrixInModelSpace() {
        return boneMatrixInModelSpace;
    }

    /**
     * @return the vertex assigning envelope of the bone
     */
    public BoneEnvelope getBoneEnvelope() {
        return boneEnvelope;
    }

    /**
     * @return bone's stretch factor
     */
    public float getIkStretch() {
        return ikStretch;
    }

    /**
     * @return indicates if the X rotation should be limited
     */
    public boolean isLimitX() {
        return limits != null ? limits[0] : false;
    }

    /**
     * @return indicates if the Y rotation should be limited
     */
    public boolean isLimitY() {
        return limits != null ? limits[1] : false;
    }

    /**
     * @return indicates if the Z rotation should be limited
     */
    public boolean isLimitZ() {
        return limits != null ? limits[2] : false;
    }

    /**
     * @return indicates if the X rotation should be disabled
     */
    public boolean isLockX() {
        return locks != null ? locks[0] : false;
    }

    /**
     * @return indicates if the Y rotation should be disabled
     */
    public boolean isLockY() {
        return locks != null ? locks[1] : false;
    }

    /**
     * @return indicates if the Z rotation should be disabled
     */
    public boolean isLockZ() {
        return locks != null ? locks[2] : false;
    }

    /**
     * @return the minimum values in rotation limitation (if limitation is enabled for specific axis).
     */
    public Vector3f getLimitMin() {
        return limitMin;
    }

    /**
     * @return the maximum values in rotation limitation (if limitation is enabled for specific axis).
     */
    public Vector3f getLimitMax() {
        return limitMax;
    }

    /**
     * @return the stiffness of the bone
     */
    public Vector3f getStiffness() {
        return stiffness;
    }

    /**
     * Tells if the bone is of specified property defined by its flag.
     * @param flagMask
     *            the mask of the flag (constants defined in this class)
     * @return <b>true</b> if the bone IS of specified proeprty and <b>false</b> otherwise
     */
    public boolean is(int flagMask) {
        return (flag & flagMask) != 0;
    }

    /**
     * @return the root bone context of this bone context
     */
    public BoneContext getRoot() {
        BoneContext result = this;
        while (result.parent != null) {
            result = result.parent;
        }
        return result;
    }

    /**
     * @return a number of bones from this bone to its root
     */
    public int getDistanceFromRoot() {
        int result = 0;
        BoneContext boneContext = this;
        while (boneContext.parent != null) {
            boneContext = boneContext.parent;
            ++result;
        }
        return result;
    }

    @Override
    public String toString() {
        return "BoneContext: " + boneName;
    }
}
