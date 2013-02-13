package com.jme3.scene.plugins.blender.constraints;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * Constraint applied on the bone.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class BoneConstraint extends Constraint {
	private static final Logger LOGGER = Logger.getLogger(BoneConstraint.class.getName());
	
	protected boolean isNodeTarget;
	
	/**
	 * The bone constraint constructor.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure
	 * @param ownerOMA
	 *            the OMA of the bone that owns the constraint
	 * @param influenceIpo
	 *            the influence interpolation curve
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             exception thrown when problems with blender file occur
	 */
	public BoneConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext)
			throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
	}

	@Override
	protected boolean validate() {
		if(targetOMA != null) {
			Spatial nodeTarget = (Spatial)blenderContext.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE);
			//the second part of the if expression verifies if the found node (if any) is an armature node
			if(nodeTarget == null || nodeTarget.getUserData(ArmatureHelper.ARMETURE_NODE_MARKER) != null) {
				//if the target is not an object node then it is an Armature, so make sure the bone is in the current skeleton
				BoneContext boneContext = blenderContext.getBoneContext(ownerOMA);
				if(targetOMA.longValue() != boneContext.getArmatureObjectOMA().longValue()) {
					LOGGER.log(Level.WARNING, "Bone constraint {0} must target bone in the its own skeleton! Targeting bone in another skeleton is not supported!", name);
					return false;
				}
			} else {
				isNodeTarget = true;
			}
		}
		
		return true;
	}
	
	@Override
	public void performBakingOperation() {
		Bone owner = blenderContext.getBoneContext(ownerOMA).getBone();
		
		if(targetOMA != null) {
			if(isNodeTarget) {
				Spatial target = (Spatial) blenderContext.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE);
				this.prepareTracksForApplyingConstraints();
				AnimData animData = blenderContext.getAnimData(ownerOMA);
				if(animData != null) {
					for(Animation animation : animData.anims) {
						Transform ownerTransform = constraintHelper.getBoneTransform(ownerSpace, owner);
						Transform targetTransform = constraintHelper.getNodeObjectTransform(targetSpace, targetOMA, blenderContext);
						
						Track boneTrack = constraintHelper.getTrack(owner, animData.skeleton, animation);
						Track targetTrack = constraintHelper.getTrack(target, animation);
						
						constraintDefinition.bake(ownerTransform, targetTransform, boneTrack, targetTrack, this.ipo);
					}
				}
			} else {
				BoneContext boneContext = blenderContext.getBoneByName(subtargetName);
				Bone target = boneContext.getBone();
				this.targetOMA = boneContext.getBoneOma();
				
				this.prepareTracksForApplyingConstraints();
				AnimData animData = blenderContext.getAnimData(ownerOMA);
				if(animData != null) {
					for(Animation animation : animData.anims) {
						Transform ownerTransform = constraintHelper.getBoneTransform(ownerSpace, owner);
						Transform targetTransform = constraintHelper.getBoneTransform(targetSpace, target);
						
						Track boneTrack = constraintHelper.getTrack(owner, animData.skeleton, animation);
						Track targetTrack = constraintHelper.getTrack(target, animData.skeleton, animation);
						
						constraintDefinition.bake(ownerTransform, targetTransform, boneTrack, targetTrack, this.ipo);
					}
				}
			}
		} else {
			this.prepareTracksForApplyingConstraints();
			AnimData animData = blenderContext.getAnimData(ownerOMA);
			if(animData != null) {
				for(Animation animation : animData.anims) {
					Transform ownerTransform = constraintHelper.getBoneTransform(ownerSpace, owner);
					Track boneTrack = constraintHelper.getTrack(owner, animData.skeleton, animation);
					
					constraintDefinition.bake(ownerTransform, null, boneTrack, null, this.ipo);
				}
			}
		}
	}
	
	@Override
	protected void prepareTracksForApplyingConstraints() {
		Long[] bonesOMAs = new Long[] { ownerOMA, targetOMA };
		Space[] spaces = new Space[] { ownerSpace, targetSpace };
		
		//creating animations for current objects if at least on of their parents have an animation
		for (int i = 0; i < bonesOMAs.length; ++i) {
			Long oma = bonesOMAs[i];
			if(this.hasAnimation(oma)) {
				Bone currentBone = blenderContext.getBoneContext(oma).getBone();
				Bone parent = currentBone.getParent();
				boolean foundAnimation = false;
				AnimData animData = null;
				while(parent != null && !foundAnimation) {
					BoneContext boneContext = blenderContext.getBoneByName(parent.getName());
					foundAnimation = this.hasAnimation(boneContext.getBoneOma());
					animData = blenderContext.getAnimData(boneContext.getBoneOma());
					parent = parent.getParent();
				}
				
				if(foundAnimation) {
					this.applyAnimData(blenderContext.getBoneContext(oma), spaces[i], animData);
				}
			}
		}
		
		//creating animation for owner if it doesn't have one already and if the target has it
		if(!this.hasAnimation(ownerOMA) && this.hasAnimation(targetOMA)) {
			AnimData targetAnimData = blenderContext.getAnimData(targetOMA);
			this.applyAnimData(blenderContext.getBoneContext(ownerOMA), ownerSpace, targetAnimData);
		}
	}
	
	/**
	 * The method determines if the bone has animations.
	 * 
	 * @param animOwnerOMA
	 *            OMA of the animation's owner
	 * @return <b>true</b> if the target has animations and <b>false</b> otherwise
	 */
	protected boolean hasAnimation(Long animOwnerOMA) {
		AnimData animData = blenderContext.getAnimData(animOwnerOMA);
		if(animData != null) {
			if(!isNodeTarget) {
				Bone bone = blenderContext.getBoneContext(animOwnerOMA).getBone();
				int boneIndex = animData.skeleton.getBoneIndex(bone);
				for(Animation animation : animData.anims) {
					for(Track track : animation.getTracks()) {
						if(track instanceof BoneTrack && ((BoneTrack) track).getTargetBoneIndex() == boneIndex) {
							return true;
						}
					}
				}
			} else {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * The method applies bone's current position to all of the traces of the
	 * given animations.
	 * 
	 * @param boneContext
	 *            the bone context
	 * @param space
	 *            the bone's evaluation space
	 * @param referenceAnimData
	 *            the object containing the animations
	 */
	protected void applyAnimData(BoneContext boneContext, Space space, AnimData referenceAnimData) {
		ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
		Transform transform = constraintHelper.getBoneTransform(space, boneContext.getBone());
		
		AnimData animData = blenderContext.getAnimData(boneContext.getBoneOma());
		
		for(Animation animation : referenceAnimData.anims) {
			BoneTrack parentTrack = (BoneTrack) animation.getTracks()[0];
			
			float[] times = parentTrack.getTimes();
			Vector3f[] translations = new Vector3f[times.length];
			Quaternion[] rotations = new Quaternion[times.length];
			Vector3f[] scales = new Vector3f[times.length];
			Arrays.fill(translations, transform.getTranslation());
			Arrays.fill(rotations, transform.getRotation());
			Arrays.fill(scales, transform.getScale());
			for(Animation anim : animData.anims) {
				anim.addTrack(new BoneTrack(animData.skeleton.getBoneIndex(boneContext.getBone()), times, translations, rotations, scales));
			}
		}
		blenderContext.setAnimData(boneContext.getBoneOma(), animData);
	}
}
