package com.jme3.scene.plugins.blender.constraints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.SpatialTrack;
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
 * Constraint applied on the spatial objects.
 * This includes: nodes, cameras nodes and light nodes.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class SpatialConstraint extends BoneConstraint {
	private static final Logger LOGGER = Logger.getLogger(SpatialConstraint.class.getName());
	
	/** The owner of the constraint. */
	private Spatial owner;
	/** The target of the constraint. */
	private Object target;
	
	public SpatialConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext)
			throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
	}
	
	@Override
	public void performBakingOperation() {
		this.owner = (Spatial) blenderContext.getLoadedFeature(ownerOMA, LoadedFeatureDataType.LOADED_FEATURE);
		this.target = targetOMA != null ? blenderContext.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE) : null;
		this.prepareTracksForApplyingConstraints();
		
		//apply static constraint
		Transform ownerTransform = constraintHelper.getNodeObjectTransform(ownerSpace, ownerOMA, blenderContext);
		Transform targetTransform = targetOMA != null ? constraintHelper.getNodeObjectTransform(targetSpace, targetOMA, blenderContext) : null;
		constraintDefinition.bake(ownerTransform, targetTransform, null, null, this.ipo);
		constraintHelper.applyTransform(owner, ownerSpace, ownerTransform);
		
		//apply dynamic constraint
		AnimData animData = blenderContext.getAnimData(ownerOMA);
		if(animData != null) {
			for(Animation animation : animData.anims) {
				SpatialTrack ownerTrack = constraintHelper.getTrack(owner, animation);
				
				AnimData targetAnimData = blenderContext.getAnimData(targetOMA);
				SpatialTrack targetTrack = null;
				if(targetAnimData != null) {
					targetTrack = constraintHelper.getTrack((Spatial)target, targetAnimData.anims.get(0));
				}
				
				constraintDefinition.bake(ownerTransform, targetTransform, ownerTrack, targetTrack, this.ipo);
			}
		}
	}
	
	@Override
	protected void prepareTracksForApplyingConstraints() {
		Long[] spatialsOMAs = new Long[] { ownerOMA, targetOMA };
		Space[] spaces = new Space[] { ownerSpace, targetSpace };
		
		//creating animations for current objects if at least on of their parents have an animation
		for (int i = 0; i < spatialsOMAs.length; ++i) {
			Long oma = spatialsOMAs[i];
			if(oma != null && oma > 0L) {
				AnimData animData = blenderContext.getAnimData(oma);
				if(animData == null) {
					Spatial currentSpatial = (Spatial)blenderContext.getLoadedFeature(oma, LoadedFeatureDataType.LOADED_FEATURE);
					if(currentSpatial != null) {
						if(currentSpatial.getUserData(ArmatureHelper.ARMETURE_NODE_MARKER) == Boolean.TRUE) {//look for it among bones
							BoneContext currentBoneContext = blenderContext.getBoneByName(subtargetName);
							Bone currentBone = currentBoneContext.getBone();
							Bone parent = currentBone.getParent();
							boolean foundAnimation = false;
							while(parent != null && !foundAnimation) {
								BoneContext boneContext = blenderContext.getBoneByName(parent.getName());
								foundAnimation = this.hasAnimation(boneContext.getBoneOma());
								animData = blenderContext.getAnimData(boneContext.getBoneOma());
								parent = parent.getParent();
							}
							if(foundAnimation) {
								this.applyAnimData(currentBoneContext, spaces[i], animData);
							}
						} else {
							Spatial parent = currentSpatial.getParent();
							while(parent != null && animData == null) {
								Structure parentStructure = (Structure)blenderContext.getLoadedFeature(parent.getName(), LoadedFeatureDataType.LOADED_STRUCTURE);
								if(parentStructure == null) {
									parent = null;
								} else {
									Long parentOma = parentStructure.getOldMemoryAddress();
									animData = blenderContext.getAnimData(parentOma);
									parent = parent.getParent();
								}
							}
							
							if(animData != null) {//create anim data for the current object
								this.applyAnimData(currentSpatial, oma, spaces[i], animData.anims.get(0));
							}
						}
					} else {
						LOGGER.warning("Couldn't find target object for constraint: " + name + 
									   ". Make sure that the target is on layer that is defined to be loaded in blender key!");
					}
				}
			}
		}
		
		//creating animation for owner if it doesn't have one already and if the target has it
		AnimData animData = blenderContext.getAnimData(ownerOMA);
		if(animData == null) {
			AnimData targetAnimData = blenderContext.getAnimData(targetOMA);
			if(targetAnimData != null) {
				this.applyAnimData(owner, ownerOMA, ownerSpace, targetAnimData.anims.get(0));
			}
		}
	}
	
	/**
	 * This method applies spatial transform on each frame of the given
	 * animations.
	 * 
	 * @param spatial
	 *            the spatial
	 * @param spatialOma
	 *            the OMA of the given spatial
	 * @param space
	 *            the space we compute the transform in
	 * @param referenceAnimation
	 *            the object containing the animations
	 */
	private void applyAnimData(Spatial spatial, Long spatialOma, Space space, Animation referenceAnimation) {
		ConstraintHelper constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
		Transform transform = constraintHelper.getNodeObjectTransform(space, spatialOma, blenderContext);
		
		SpatialTrack parentTrack = (SpatialTrack) referenceAnimation.getTracks()[0];
		
		HashMap<String, Animation> anims = new HashMap<String, Animation>(1);
		Animation animation = new Animation(spatial.getName(), referenceAnimation.getLength());
		anims.put(spatial.getName(), animation);
		
		float[] times = parentTrack.getTimes();
		Vector3f[] translations = new Vector3f[times.length];
		Quaternion[] rotations = new Quaternion[times.length];
		Vector3f[] scales = new Vector3f[times.length];
		Arrays.fill(translations, transform.getTranslation());
		Arrays.fill(rotations, transform.getRotation());
		Arrays.fill(scales, transform.getScale());
		animation.addTrack(new SpatialTrack(times, translations, rotations, scales));
		
		AnimControl control = new AnimControl(null);
		control.setAnimations(anims);
		spatial.addControl(control);
		
		blenderContext.setAnimData(spatialOma, new AnimData(null, new ArrayList<Animation>(anims.values())));
	}
}
