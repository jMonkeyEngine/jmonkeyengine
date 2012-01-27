package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This class represents 'Dist limit' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintDistLimit extends Constraint {
	private static final int LIMITDIST_INSIDE = 0;
	private static final int LIMITDIST_OUTSIDE = 1;
	private static final int LIMITDIST_ONSURFACE = 2;
    
	protected int mode;
	protected float dist;
	
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param ownerOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintDistLimit(Structure constraintStructure, Long ownerOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
		
		mode = ((Number) data.getFieldValue("mode")).intValue();
		dist = ((Number) data.getFieldValue("dist")).floatValue();
	}

	@Override
	protected void bakeConstraint() {
		Object owner = this.owner.getObject();
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if(animData != null) {
			if(owner instanceof Spatial) {
				Vector3f targetLocation = ((Spatial) owner).getWorldTranslation();
				for(Animation animation : animData.anims) {
					BlenderTrack blenderTrack = this.getTrack(owner, animData.skeleton, animation);
					int maxFrames = blenderTrack.getTimes().length;
					Vector3f[] translations = blenderTrack.getTranslations();
					for (int frame = 0; frame < maxFrames; ++frame) {
						Vector3f v = translations[frame].subtract(targetLocation);
						this.distLimit(v, targetLocation, ipo.calculateValue(frame));
						translations[frame].addLocal(v);
					}
					blenderTrack.setKeyframes(blenderTrack.getTimes(), translations, blenderTrack.getRotations(), blenderTrack.getScales());
				}
			}
		}
		
		// apply static constraint only to spatials
		if(owner instanceof Spatial) {
			Matrix4f targetWorldMatrix = target.getWorldTransformMatrix();
			Vector3f targetLocation = targetWorldMatrix.toTranslationVector();
			Matrix4f m = this.owner.getParentWorldTransformMatrix();
			m.invertLocal();
			Matrix4f ownerWorldMatrix = this.owner.getWorldTransformMatrix();
			Vector3f ownerLocation = ownerWorldMatrix.toTranslationVector();
			this.distLimit(ownerLocation, targetLocation, ipo.calculateValue(0));
			((Spatial) owner).setLocalTranslation(m.mult(ownerLocation));
		}
	}
	
	/**
	 * 
	 * @param currentLocation
	 * @param targetLocation
	 * @param influence
	 */
	private void distLimit(Vector3f currentLocation, Vector3f targetLocation, float influence) {
		Vector3f v = currentLocation.subtract(targetLocation);
		float currentDistance = v.length();
		
		switch (mode) {
			case LIMITDIST_INSIDE:
				if (currentDistance >= dist) {
					v.normalizeLocal();
					v.multLocal(dist + (currentDistance - dist) * (1.0f - influence));
					currentLocation.set(v.addLocal(targetLocation));
				}
				break;
			case LIMITDIST_ONSURFACE:
				if (currentDistance > dist) {
					v.normalizeLocal();
					v.multLocal(dist + (currentDistance - dist) * (1.0f - influence));
					currentLocation.set(v.addLocal(targetLocation));
				} else if(currentDistance < dist) {
					v.normalizeLocal().multLocal(dist * influence);
					currentLocation.set(targetLocation.add(v));
				}
				break;
			case LIMITDIST_OUTSIDE:
				if (currentDistance <= dist) {
					v = targetLocation.subtract(currentLocation).normalizeLocal().multLocal(dist * influence);
					currentLocation.set(targetLocation.add(v));
				}
				break;
			default:
				throw new IllegalStateException("Unknown distance limit constraint mode: " + mode);
		}
	}
}
