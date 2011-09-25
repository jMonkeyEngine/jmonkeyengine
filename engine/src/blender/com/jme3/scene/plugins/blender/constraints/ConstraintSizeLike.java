package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.animation.Track;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Size like' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintSizeLike extends Constraint {
	private static final int SIZELIKE_X = 0x01;
	private static final int SIZELIKE_Y = 0x02;
	private static final int SIZELIKE_Z = 0x04;
	private static final int LOCLIKE_OFFSET = 0x80;
    
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param boneOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ConstraintSizeLike(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Animation animation, int targetIndex) {
		Vector3f targetScale = this.getTargetLocation();
		Track<?> track = this.getTrack(animation, targetIndex);
		if (track != null) {
			int flag = ((Number) data.getFieldValue("flag")).intValue();
			Vector3f[] scales = track.getScales();
			int maxFrames = scales.length;
			for (int frame = 0; frame < maxFrames; ++frame) {
				Vector3f offset = Vector3f.ZERO;
				if ((flag & LOCLIKE_OFFSET) != 0) {//we add the original scale to the copied scale
					offset = scales[frame].clone();
				}

				if ((flag & SIZELIKE_X) != 0) {
					scales[frame].x = targetScale.x;
				} else if ((flag & SIZELIKE_Y) != 0) {
					scales[frame].y = targetScale.y;
				} else if ((flag & SIZELIKE_Z) != 0) {
					scales[frame].z = targetScale.z;
				}
				scales[frame].addLocal(offset);//TODO: ipo influence
				//TODO: add or multiply???
			}
			track.setKeyframes(track.getTimes(), track.getTranslations(), track.getRotations(), scales);
		}
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_SIZELIKE;
	}
}
