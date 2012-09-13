package com.jme3.scene.plugins.blender.constraints;

import java.util.Arrays;
import java.util.logging.Logger;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * Constraint applied on the bone.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class BoneConstraint extends Constraint {
	private static final Logger LOGGER = Logger.getLogger(BoneConstraint.class.getName());
	/** The OMA of the target armature. */
	private Long targetArmatureOMA;
	
	public BoneConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext)
			throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		targetArmatureOMA = targetOMA;
		if(targetArmatureOMA != null && targetArmatureOMA <= 0L) {
			targetArmatureOMA = null;
		}
		targetOMA = null;
		if(targetArmatureOMA != null && targetArmatureOMA > 0L && (subtargetName == null || subtargetName.length() == 0)) {
			invalid = true;
		}
	}

	@Override
	public void performBakingOperation() {
		Bone owner = blenderContext.getBoneContext(ownerOMA).getBone();
		Bone target = null;
		
		if(targetArmatureOMA != null) {//first make sure the target is loaded
			ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
			try {
				objectHelper.toObject((Structure) blenderContext.getLoadedFeature(targetArmatureOMA, LoadedFeatureDataType.LOADED_STRUCTURE), blenderContext);
			} catch (BlenderFileException e) {
				LOGGER.warning("Problems occured during target object loading. The constraint " + name + " will not be applied.");
				return ;
			}
			
			BoneContext boneContext = blenderContext.getBoneByName(subtargetName);
			target = boneContext.getBone();
			this.targetOMA = boneContext.getBoneOma();
		}
		
		this.prepareTracksForApplyingConstraints();
		AnimData animData = blenderContext.getAnimData(ownerOMA);
		if(animData != null) {
			for(Animation animation : animData.anims) {
				Transform ownerTransform = constraintHelper.getBoneTransform(ownerSpace, owner);
				Transform targetTransform = target != null ? constraintHelper.getBoneTransform(targetSpace, target) : null;
				
				BoneTrack boneTrack = constraintHelper.getTrack(owner, animData.skeleton, animation);
				BoneTrack targetTrack = target != null ? constraintHelper.getTrack(target, animData.skeleton, animation) : null;
				
				constraintDefinition.bake(ownerTransform, targetTransform, boneTrack, targetTrack, this.ipo);
			}
		}
	}
	
	@Override
	protected void prepareTracksForApplyingConstraints() {
		Long[] bonesOMAs = new Long[] { ownerOMA, targetOMA };
		Space[] spaces = new Space[] { ownerSpace, targetSpace };
		
		//creating animations for current objects if at least on of their parents have an animation
		for(int i=0;i<bonesOMAs.length;++i) {
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
	 * @param boneOMA
	 *            OMA of the bone
	 * @return <b>true</b> if the bone has animations and <b>false</b> otherwise
	 */
	private boolean hasAnimation(Long boneOMA) {
		AnimData animData = blenderContext.getAnimData(boneOMA);
		if(animData != null) {
			Bone bone = blenderContext.getBoneContext(boneOMA).getBone();
			int boneIndex = animData.skeleton.getBoneIndex(bone);
			for(Animation animation : animData.anims) {
				for(Track track : animation.getTracks()) {
					if(track instanceof BoneTrack && ((BoneTrack) track).getTargetBoneIndex() == boneIndex) {
						return true;
					}
				}
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
	private void applyAnimData(BoneContext boneContext, Space space, AnimData referenceAnimData) {
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
