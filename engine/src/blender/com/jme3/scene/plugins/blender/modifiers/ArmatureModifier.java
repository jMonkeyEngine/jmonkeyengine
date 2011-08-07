package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.math.Matrix4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.constraints.Constraint;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This modifier allows to add bone animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ArmatureModifier extends Modifier {

	/**
	 * This constructor is only temporary. It will be removed when object
	 * animation is implemented in jme. TODO!!!!!!!
	 */
	/* package */ArmatureModifier() {
	}

	/**
	 * This constructor reads animation data from the object structore. The
	 * stored data is the AnimData and additional data is armature's OMA.
	 * 
	 * @param objectStructure
	 *            the structure of the object
	 * @param modifierStructure
	 *            the structure of the modifier
	 * @param dataRepository
	 *            the data repository
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ArmatureModifier(Structure objectStructure, Structure modifierStructure, DataRepository dataRepository) throws BlenderFileException {
		Pointer pArmatureObject = (Pointer) modifierStructure.getFieldValue("object");
		if (pArmatureObject.isNotNull()) {
			ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
			Structure armatureObject = (Structure) dataRepository.getLoadedFeature(pArmatureObject.getOldMemoryAddress(),
							LoadedFeatureDataType.LOADED_STRUCTURE);
			if (armatureObject == null) {// we check this first not to fetch the
											// structure unnecessary
				armatureObject = pArmatureObject.fetchData(dataRepository.getInputStream()).get(0);
				objectHelper.toObject(armatureObject, dataRepository);
			}
			additionalData = armatureObject.getOldMemoryAddress();
			ArmatureHelper armatureHelper = dataRepository
					.getHelper(ArmatureHelper.class);

			// changing bones matrices so that they fit the current object (that
			// is why we need a copy of a skeleton)
			Matrix4f armatureObjectMatrix = objectHelper.getTransformationMatrix(armatureObject);
			Matrix4f inverseMeshObjectMatrix = objectHelper.getTransformationMatrix(objectStructure).invert();
			Matrix4f additionalRootBoneTransformation = inverseMeshObjectMatrix.multLocal(armatureObjectMatrix);
			Bone[] bones = armatureHelper.buildBonesStructure(Long.valueOf(0L), additionalRootBoneTransformation);

			// setting the bones structure inside the skeleton (thus completing
			// its loading)
			Skeleton skeleton = new Skeleton(bones);
			dataRepository.addLoadedFeatures(armatureObject.getOldMemoryAddress(), armatureObject.getName(), armatureObject, skeleton);

			String objectName = objectStructure.getName();
			Set<String> animationNames = dataRepository.getBlenderKey().getAnimationNames(objectName);
			if (animationNames != null && animationNames.size() > 0) {
				ArrayList<Animation> animations = new ArrayList<Animation>();
				List<FileBlockHeader> actionHeaders = dataRepository.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
				for (FileBlockHeader header : actionHeaders) {
					Structure actionStructure = header.getStructure(dataRepository);
					String actionName = actionStructure.getName();
					if (animationNames.contains(actionName)) {
						int[] animationFrames = dataRepository.getBlenderKey().getAnimationFrames(objectName, actionName);
						int fps = dataRepository.getBlenderKey().getFps();
						float start = (float) animationFrames[0] / (float) fps;
						float stop = (float) animationFrames[1] / (float) fps;
						BoneAnimation boneAnimation = new BoneAnimation(actionName, stop - start);
						boneAnimation.setTracks(armatureHelper.getTracks(actionStructure, dataRepository, objectName, actionName));
						animations.add(boneAnimation);
					}
				}
				jmeModifierRepresentation = new AnimData(new Skeleton(bones), animations);
			}
		}
	}

	@Override
	public Node apply(Node node, DataRepository dataRepository) {
		if(jmeModifierRepresentation == null) {
			return node;
		}
		AnimData ad = (AnimData) jmeModifierRepresentation;
		ArrayList<Animation> animList = ad.anims;
		Long modifierArmatureObject = (Long) additionalData;
		if (animList != null && animList.size() > 0) {
			List<Constraint> constraints = dataRepository.getConstraints(modifierArmatureObject);
			HashMap<String, Animation> anims = new HashMap<String, Animation>();
			for (int i = 0; i < animList.size(); ++i) {
				BoneAnimation boneAnimation = (BoneAnimation) animList.get(i).clone();

				// baking constraints into animations
				if (constraints != null && constraints.size() > 0) {
					for (Constraint constraint : constraints) {
						constraint.affectAnimation(ad.skeleton, boneAnimation);
					}
				}

				anims.put(boneAnimation.getName(), boneAnimation);
			}

			// getting meshes
			Mesh[] meshes = null;
			List<Mesh> meshesList = new ArrayList<Mesh>();
			List<Spatial> children = node.getChildren();
			for (Spatial child : children) {
				if (child instanceof Geometry) {
					meshesList.add(((Geometry) child).getMesh());
				}
			}
			if (meshesList.size() > 0) {
				meshes = meshesList.toArray(new Mesh[meshesList.size()]);
			}

			// applying the control to the node
			SkeletonControl skeletonControl = new SkeletonControl(meshes, ad.skeleton);
			AnimControl control = node.getControl(AnimControl.class);

			if (control == null) {
				control = new AnimControl(ad.skeleton);
			} else {
				// merging skeletons
				Skeleton controlSkeleton = control.getSkeleton();
				int boneIndexIncrease = controlSkeleton.getBoneCount();
				Skeleton skeleton = this.merge(controlSkeleton, ad.skeleton);

				// merging animations
				HashMap<String, Animation> animations = new HashMap<String, Animation>();
				for (String animationName : control.getAnimationNames()) {
					animations.put(animationName,
							control.getAnim(animationName));
				}
				for (Entry<String, Animation> animEntry : anims.entrySet()) {
					BoneAnimation ba = (BoneAnimation) animEntry.getValue();
					for (int i = 0; i < ba.getTracks().length; ++i) {
						BoneTrack bt = ba.getTracks()[i];
						int newBoneIndex = bt.getTargetBoneIndex()
								+ boneIndexIncrease;
						ba.getTracks()[i] = new BoneTrack(newBoneIndex,
								bt.getTimes(), bt.getTranslations(),
								bt.getRotations(), bt.getScales());
					}
					animations.put(animEntry.getKey(), animEntry.getValue());
				}

				// replacing the control
				node.removeControl(control);
				control = new AnimControl(skeleton);
			}
			control.setAnimations(anims);
			node.addControl(control);
			node.addControl(skeletonControl);
		}
		return node;
	}

	@Override
	public String getType() {
		return Modifier.ARMATURE_MODIFIER_DATA;
	}

	/**
	 * This method merges two skeletons into one. I assume that each skeleton's
	 * 0-indexed bone is objectAnimationBone so only one such bone should be
	 * placed in the result
	 * 
	 * @param s1
	 *            first skeleton
	 * @param s2
	 *            second skeleton
	 * @return merged skeleton
	 */
	protected Skeleton merge(Skeleton s1, Skeleton s2) {
		List<Bone> bones = new ArrayList<Bone>(s1.getBoneCount()
				+ s2.getBoneCount());
		for (int i = 0; i < s1.getBoneCount(); ++i) {
			bones.add(s1.getBone(i));
		}
		for (int i = 1; i < s2.getBoneCount(); ++i) {// ommit
														// objectAnimationBone
			bones.add(s2.getBone(i));
		}
		return new Skeleton(bones.toArray(new Bone[bones.size()]));
	}
}
