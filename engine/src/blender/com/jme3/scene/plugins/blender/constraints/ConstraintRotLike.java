package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.animation.Track;
import com.jme3.math.Quaternion;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Rot like' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintRotLike extends Constraint {
	private static final int ROTLIKE_X = 0x01;
	private static final int ROTLIKE_Y = 0x02;
	private static final int ROTLIKE_Z = 0x04;
	private static final int ROTLIKE_X_INVERT = 0x10;
    private static final int ROTLIKE_Y_INVERT = 0x20;
    private static final int ROTLIKE_Z_INVERT = 0x40;
    private static final int ROTLIKE_OFFSET = 0x80;
    
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
	public ConstraintRotLike(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Animation animation, int targetIndex) {
		Track<?> track = this.getTrack(animation, targetIndex);
		if (track != null) {
			Quaternion targetRotation = this.getTargetRotation();
			int flag = ((Number) data.getFieldValue("flag")).intValue();
			float[] targetAngles = targetRotation.toAngles(null);
			Quaternion[] rotations = track.getRotations();
			int maxFrames = rotations.length;
			for (int frame = 0; frame < maxFrames; ++frame) {
				float[] angles = rotations[frame].toAngles(null);

				Quaternion offset = Quaternion.IDENTITY;
				if ((flag & ROTLIKE_OFFSET) != 0) {//we add the original rotation to the copied rotation
					offset = rotations[frame].clone();
				}

				if ((flag & ROTLIKE_X) != 0) {
					angles[0] = targetAngles[0];
					if ((flag & ROTLIKE_X_INVERT) != 0) {
						angles[0] = -angles[0];
					}
				} else if ((flag & ROTLIKE_Y) != 0) {
					angles[1] = targetAngles[1];
					if ((flag & ROTLIKE_Y_INVERT) != 0) {
						angles[1] = -angles[1];
					}
				} else if ((flag & ROTLIKE_Z) != 0) {
					angles[2] = targetAngles[2];
					if ((flag & ROTLIKE_Z_INVERT) != 0) {
						angles[2] = -angles[2];
					}
				}
				rotations[frame].fromAngles(angles).multLocal(offset);//TODO: ipo influence
			}
			track.setKeyframes(track.getTimes(), track.getTranslations(), rotations, track.getScales());
		}
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_ROTLIKE;
	}
}
