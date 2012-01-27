package com.jme3.scene.plugins.blender.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jme3.animation.Bone;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * This class holds the basic data that describes a bone.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BoneContext {
	/** The structure of the bone. */
	private Structure			boneStructure;
	/** Bone's pose channel structure. */
	private Structure			poseChannel;
	/** Bone's name. */
	private String				boneName;
	/** This variable indicates if the Y axis should be the UP axis. */
	private boolean				fixUpAxis;
	/** The bone's armature matrix. */
	private Matrix4f			armatureMatrix;
	/** The parent context. */
	private BoneContext			parent;
	/** The children of this context. */
	private List<BoneContext>	children		= new ArrayList<BoneContext>();
	/** Created bone (available after calling 'buildBone' method). */
	private Bone				bone;
	/** Bone's pose transform (available after calling 'buildBone' method). */
	private Transform			poseTransform	= new Transform();
	/** The bone's rest matrix. */
	private Matrix4f			restMatrix;
	/** Bone's total inverse transformation. */
	private Matrix4f			inverseTotalTransformation;
	/** Bone's parent inverse matrix. */
	private Matrix4f			inverseParentMatrix;

	/**
	 * Constructor. Creates the basic set of bone's data.
	 * 
	 * @param boneStructure
	 *            the bone's structure
	 * @param objectToArmatureMatrix
	 *            object-to-armature transformation matrix
	 * @param bonesPoseChannels
	 *            a map of pose channels for each bone OMA
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             an exception is thrown when problem with blender data reading
	 *             occurs
	 */
	public BoneContext(Structure boneStructure, Matrix4f objectToArmatureMatrix, final Map<Long, Structure> bonesPoseChannels, BlenderContext blenderContext) throws BlenderFileException {
		this(boneStructure, null, objectToArmatureMatrix, bonesPoseChannels, blenderContext);
	}

	/**
	 * Constructor. Creates the basic set of bone's data.
	 * 
	 * @param boneStructure
	 *            the bone's structure
	 * @param parent
	 *            bone's parent (null if the bone is the root bone)
	 * @param objectToArmatureMatrix
	 *            object-to-armature transformation matrix
	 * @param bonesPoseChannels
	 *            a map of pose channels for each bone OMA
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             an exception is thrown when problem with blender data reading
	 *             occurs
	 */
	private BoneContext(Structure boneStructure, BoneContext parent, Matrix4f objectToArmatureMatrix, final Map<Long, Structure> bonesPoseChannels, BlenderContext blenderContext) throws BlenderFileException {
		this.parent = parent;
		this.boneStructure = boneStructure;
		boneName = boneStructure.getFieldValue("name").toString();
		ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
		armatureMatrix = objectHelper.getMatrix(boneStructure, "arm_mat", true);

		fixUpAxis = blenderContext.getBlenderKey().isFixUpAxis();
		this.computeRestMatrix(objectToArmatureMatrix);
		List<Structure> childbase = ((Structure) boneStructure.getFieldValue("childbase")).evaluateListBase(blenderContext);
		for (Structure child : childbase) {
			this.children.add(new BoneContext(child, this, objectToArmatureMatrix, bonesPoseChannels, blenderContext));
		}

		poseChannel = bonesPoseChannels.get(boneStructure.getOldMemoryAddress());

		blenderContext.setBoneContext(boneStructure.getOldMemoryAddress(), this);
	}

	/**
	 * This method computes the rest matrix for the bone.
	 * 
	 * @param objectToArmatureMatrix
	 *            object-to-armature transformation matrix
	 */
	private void computeRestMatrix(Matrix4f objectToArmatureMatrix) {
		if (parent != null) {
			inverseParentMatrix = parent.inverseTotalTransformation.clone();
		} else if (fixUpAxis) {
			inverseParentMatrix = objectToArmatureMatrix.clone();
		} else {
			inverseParentMatrix = Matrix4f.IDENTITY.clone();
		}

		restMatrix = armatureMatrix.clone();
		inverseTotalTransformation = restMatrix.invert();

		restMatrix = inverseParentMatrix.mult(restMatrix);

		for (BoneContext child : this.children) {
			child.computeRestMatrix(objectToArmatureMatrix);
		}
	}

	/**
	 * This method computes the pose transform for the bone.
	 */
	@SuppressWarnings("unchecked")
	private void computePoseTransform() {
		DynamicArray<Number> loc = (DynamicArray<Number>) poseChannel.getFieldValue("loc");
		DynamicArray<Number> size = (DynamicArray<Number>) poseChannel.getFieldValue("size");
		DynamicArray<Number> quat = (DynamicArray<Number>) poseChannel.getFieldValue("quat");
		if (fixUpAxis) {
			poseTransform.setTranslation(loc.get(0).floatValue(), -loc.get(2).floatValue(), loc.get(1).floatValue());
			poseTransform.setRotation(new Quaternion(quat.get(1).floatValue(), quat.get(3).floatValue(), -quat.get(2).floatValue(), quat.get(0).floatValue()));
			poseTransform.setScale(size.get(0).floatValue(), size.get(2).floatValue(), size.get(1).floatValue());
		} else {
			poseTransform.setTranslation(loc.get(0).floatValue(), loc.get(1).floatValue(), loc.get(2).floatValue());
			poseTransform.setRotation(new Quaternion(quat.get(0).floatValue(), quat.get(1).floatValue(), quat.get(2).floatValue(), quat.get(3).floatValue()));
			poseTransform.setScale(size.get(0).floatValue(), size.get(1).floatValue(), size.get(2).floatValue());
		}

		Transform localTransform = new Transform(bone.getLocalPosition(), bone.getLocalRotation());
		localTransform.setScale(bone.getLocalScale());
		localTransform.getTranslation().addLocal(poseTransform.getTranslation());
		localTransform.getRotation().multLocal(poseTransform.getRotation());
		localTransform.getScale().multLocal(poseTransform.getScale());

		poseTransform.set(localTransform);
	}

	/**
	 * This method builds the bone. It recursively builds the bone's children.
	 * 
	 * @param bones
	 *            a list of bones where the newly created bone will be added
	 * @param boneOMAs
	 *            the map between bone and its old memory address
	 * @param blenderContext
	 *            the blender context
	 * @return newly created bone
	 */
	public Bone buildBone(List<Bone> bones, Map<Bone, Long> boneOMAs, BlenderContext blenderContext) {
		Long boneOMA = boneStructure.getOldMemoryAddress();
		bone = new Bone(boneName);
		bones.add(bone);
		boneOMAs.put(bone, boneOMA);
		blenderContext.addLoadedFeatures(boneOMA, boneName, boneStructure, bone);

		Matrix4f pose = this.restMatrix.clone();
		ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);

		Vector3f poseLocation = pose.toTranslationVector();
		Quaternion rotation = pose.toRotationQuat();
		Vector3f scale = objectHelper.getScale(pose);

		bone.setBindTransforms(poseLocation, rotation, scale);
		for (BoneContext child : children) {
			bone.addChild(child.buildBone(bones, boneOMAs, blenderContext));
		}

		this.computePoseTransform();

		return bone;
	}

	/**
	 * @return bone's pose transformation
	 */
	public Transform getPoseTransform() {
		return poseTransform;
	}

	/**
	 * @return built bone (available after calling 'buildBone' method)
	 */
	public Bone getBone() {
		return bone;
	}
}
