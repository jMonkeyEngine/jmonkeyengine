package com.jme3.scene.plugins.blender.animations;

import java.util.ArrayList;
import java.util.List;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * This class holds the basic data that describes a bone.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BoneContext {
    private BlenderContext    blenderContext;
    /** The OMA of the bone's armature object. */
    private Long              armatureObjectOMA;
    /** The structure of the bone. */
    private Structure         boneStructure;
    /** Bone's name. */
    private String            boneName;
    /** The bone's armature matrix. */
    private Matrix4f          armatureMatrix;
    /** The parent context. */
    private BoneContext       parent;
    /** The children of this context. */
    private List<BoneContext> children = new ArrayList<BoneContext>();
    /** Created bone (available after calling 'buildBone' method). */
    private Bone              bone;
    /** The bone's rest matrix. */
    private Matrix4f          restMatrix;
    /** Bone's total inverse transformation. */
    private Matrix4f          inverseTotalTransformation;
    /** The length of the bone. */
    private float             length;

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
    private BoneContext(Structure boneStructure, Long armatureObjectOMA, BoneContext parent, BlenderContext blenderContext) throws BlenderFileException {
        this.parent = parent;
        this.blenderContext = blenderContext;
        this.boneStructure = boneStructure;
        this.armatureObjectOMA = armatureObjectOMA;
        boneName = boneStructure.getFieldValue("name").toString();
        length = ((Number) boneStructure.getFieldValue("length")).floatValue();
        ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
        armatureMatrix = objectHelper.getMatrix(boneStructure, "arm_mat", blenderContext.getBlenderKey().isFixUpAxis());

        // compute the bone's rest matrix
        restMatrix = armatureMatrix.clone();
        inverseTotalTransformation = restMatrix.invert();
        if (parent != null) {
            restMatrix = parent.inverseTotalTransformation.mult(restMatrix);
        }

        // create the children
        List<Structure> childbase = ((Structure) boneStructure.getFieldValue("childbase")).evaluateListBase(blenderContext);
        for (Structure child : childbase) {
            this.children.add(new BoneContext(child, armatureObjectOMA, this, blenderContext));
        }

        blenderContext.setBoneContext(boneStructure.getOldMemoryAddress(), this);
    }

    /**
     * This method builds the bone. It recursively builds the bone's children.
     * 
     * @param bones
     *            a list of bones where the newly created bone will be added
     * @param objectToArmatureMatrix
     *            object to armature transformation matrix
     * @param blenderContext
     *            the blender context
     * @return newly created bone
     */
    public Bone buildBone(List<Bone> bones, Matrix4f objectToArmatureMatrix, BlenderContext blenderContext) {
        Long boneOMA = boneStructure.getOldMemoryAddress();
        bone = new Bone(boneName);
        bones.add(bone);
        blenderContext.addLoadedFeatures(boneOMA, boneName, boneStructure, bone);

        Vector3f poseLocation = restMatrix.toTranslationVector();
        Quaternion rotation = restMatrix.toRotationQuat().normalizeLocal();
        Vector3f scale = restMatrix.toScaleVector();
        if (parent == null) {
            Quaternion rotationQuaternion = objectToArmatureMatrix.toRotationQuat().normalizeLocal();
            scale.multLocal(objectToArmatureMatrix.toScaleVector());
            rotationQuaternion.multLocal(poseLocation.addLocal(objectToArmatureMatrix.toTranslationVector()));
            rotation.multLocal(rotationQuaternion);
        }

        bone.setBindTransforms(poseLocation, rotation, scale);
        for (BoneContext child : children) {
            bone.addChild(child.buildBone(bones, objectToArmatureMatrix, blenderContext));
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
     * @return the skeleton the bone of this context belongs to
     */
    public Skeleton getSkeleton() {
        return blenderContext.getSkeleton(armatureObjectOMA);
    }
}
