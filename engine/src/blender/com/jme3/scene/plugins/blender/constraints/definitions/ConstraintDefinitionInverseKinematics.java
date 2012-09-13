package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.animation.Animation;
import com.jme3.animation.Skeleton;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.CalculationBone;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Inverse kinematics' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDefinitionInverseKinematics extends ConstraintDefinition {
	//private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionInverseKinematics.class.getName());
	//private static final float IK_SOLVER_ERROR = 0.5f;
	
	public ConstraintDefinitionInverseKinematics(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
	}

	@Override
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
//		try {
			// IK solver is only attached to bones
//			Bone ownerBone = (Bone) blenderContext.getLoadedFeature(ownerOMA, LoadedFeatureDataType.LOADED_FEATURE);
//			AnimData animData = blenderContext.getAnimData(ownerOMA);
//			if(animData == null) {
				//TODO: to nie moxe byx null, utworzyx dane bez ruchu, w zalexnoxci czy target six rusza
//			}
			
			//prepare a list of all parents of this bone
//			CalculationBone[] bones = this.getBonesToCalculate(skeleton, boneAnimation);
			
			// get the target point
//			Object targetObject = this.getTarget(LoadedFeatureDataType.LOADED_FEATURE);
//			Vector3f pt = null;// Point Target
//			if (targetObject instanceof Bone) {
//				pt = ((Bone) targetObject).getModelSpacePosition();
//			} else if (targetObject instanceof Spatial) {
//				pt = ((Spatial) targetObject).getWorldTranslation();
//			} else if (targetObject instanceof Skeleton) {
//				Structure armatureNodeStructure = (Structure) this.getTarget(LoadedFeatureDataType.LOADED_STRUCTURE);
//				ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
//				Transform transform = objectHelper.getTransformation(armatureNodeStructure, blenderContext);
//				pt = transform.getTranslation();
//			} else {
//				throw new IllegalStateException(
//						"Unknown target object type! Should be Node, Bone or Skeleton and there is: "
//						+ targetObject.getClass().getName());
//			}
			
			//fetching the owner's bone track
//			BoneTrack ownerBoneTrack = null;
//			int boneIndex = skeleton.getBoneIndex(ownerBone);
//			for (int i = 0; i < boneAnimation.getTracks().length; ++i) {
//				if (boneAnimation.getTracks()[i].getTargetBoneIndex() == boneIndex) {
//					ownerBoneTrack = boneAnimation.getTracks()[i];
//					break;
//				}
//			}
//			int ownerBoneFramesCount = ownerBoneTrack==null ? 0 : ownerBoneTrack.getTimes().length;
//			
//			// preparing data
//			int maxIterations = ((Number) data.getFieldValue("iterations")).intValue();
//			CalculationBone[] bones = this.getBonesToCalculate(ownerBone, skeleton, boneAnimation);
//			for (int i = 0; i < bones.length; ++i) {
//				System.out.println(Arrays.toString(bones[i].track.getTranslations()));
//				System.out.println(Arrays.toString(bones[i].track.getRotations()));
//				System.out.println("===============================");
//			}
//			Quaternion rotation = new Quaternion();
//			//all tracks should have the same amount of frames
//			int framesCount = bones[0].getBoneFramesCount();
//			assert framesCount >=1;
//			for (int frame = 0; frame < framesCount; ++frame) {
//				float error = IK_SOLVER_ERROR;
//				int iteration = 0;
//				while (error >= IK_SOLVER_ERROR && iteration <= maxIterations) {
//					// rotating the bones
//					for (int i = 0; i < bones.length - 1; ++i) {
//						Vector3f pe = bones[i].getEndPoint();
//						Vector3f pc = bones[i + 1].getWorldTranslation().clone();
//
//						Vector3f peSUBpc = pe.subtract(pc).normalizeLocal();
//						Vector3f ptSUBpc = pt.subtract(pc).normalizeLocal();
//
//						float theta = FastMath.acos(peSUBpc.dot(ptSUBpc));
//						Vector3f direction = peSUBpc.cross(ptSUBpc).normalizeLocal();
//						bones[i].rotate(rotation.fromAngleAxis(theta, direction), frame);
//					}
//					error = pt.subtract(bones[0].getEndPoint()).length();
//					++iteration;
//				}
//			}
//
//			for (CalculationBone bone : bones) {
//				bone.applyCalculatedTracks();
//			}
//
//			System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
//			for (int i = 0; i < bones.length; ++i) {
//				System.out.println(Arrays.toString(bones[i].track.getTranslations()));
//				System.out.println(Arrays.toString(bones[i].track.getRotations()));
//				System.out.println("===============================");
//			}
//		} catch(BlenderFileException e) {
//			LOGGER.severe(e.getLocalizedMessage());
//		}
	}
	
	/**
	 * This method returns bones used for rotation calculations.
	 * @param bone
	 *        the bone to which the constraint is applied
	 * @param skeleton
	 *        the skeleton owning the bone and its ancestors
	 * @param boneAnimation
	 *        the bone animation data that stores the traces for the skeleton's bones
	 * @return a list of bones to imitate the bone's movement during IK solving
	 */
	private CalculationBone[] getBonesToCalculate(Skeleton skeleton, Animation boneAnimation) {
//		Bone ownerBone = (Bone) blenderContext.getLoadedFeature(ownerOMA, LoadedFeatureDataType.LOADED_FEATURE);
//		List<CalculationBone> bonesList = new ArrayList<CalculationBone>();
//		do {
//			bonesList.add(new CalculationBone(ownerBone, 1));
//			int boneIndex = skeleton.getBoneIndex(ownerBone);
//			for (int i = 0; i < boneAnimation.getTracks().length; ++i) {
//				if (((BoneTrack[])boneAnimation.getTracks())[i].getTargetBoneIndex() == boneIndex) {
//					bonesList.add(new CalculationBone(ownerBone, (BoneTrack)boneAnimation.getTracks()[i]));
//					break;
//				}
//			}
//			ownerBone = ownerBone.getParent();
//		} while (ownerBone != null);
//		//attaching children
//		CalculationBone[] result = bonesList.toArray(new CalculationBone[bonesList.size()]);
//		for (int i = result.length - 1; i > 0; --i) {
//			result[i].attachChild(result[i - 1]);
//		}
//		return result;
		return null;
	}
}
