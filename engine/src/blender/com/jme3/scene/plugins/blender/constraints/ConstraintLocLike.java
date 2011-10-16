package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Track;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Loc like' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintLocLike extends Constraint {
	private static final int LOCLIKE_X = 0x01;
	private static final int LOCLIKE_Y = 0x02;
	private static final int LOCLIKE_Z = 0x04;
	/* LOCLIKE_TIP is a depreceated option... use headtail=1.0f instead */
    //protected static final int LOCLIKE_TIP = 0x08;
    private static final int LOCLIKE_X_INVERT = 0x10;
    private static final int LOCLIKE_Y_INVERT = 0x20;
    private static final int LOCLIKE_Z_INVERT = 0x40;
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
	public ConstraintLocLike(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Animation animation, int targetIndex) {
		BoneTrack track = (BoneTrack) this.getTrack(animation, targetIndex);
		if (track != null) {
			Vector3f targetLocation = this.getTargetLocation();
			int flag = ((Number) data.getFieldValue("flag")).intValue();
			Vector3f[] translations = track.getTranslations();
			int maxFrames = translations.length;
			for (int frame = 0; frame < maxFrames; ++frame) {
				Vector3f offset = Vector3f.ZERO;
				if ((flag & LOCLIKE_OFFSET) != 0) {//we add the original location to the copied location
					offset = translations[frame].clone();
				}

				if ((flag & LOCLIKE_X) != 0) {
					translations[frame].x = targetLocation.x;
					if ((flag & LOCLIKE_X_INVERT) != 0) {
						translations[frame].x = -translations[frame].x;
					}
				} else if ((flag & LOCLIKE_Y) != 0) {
					translations[frame].y = targetLocation.y;
					if ((flag & LOCLIKE_Y_INVERT) != 0) {
						translations[frame].y = -translations[frame].y;
					}
				} else if ((flag & LOCLIKE_Z) != 0) {
					translations[frame].z = targetLocation.z;
					if ((flag & LOCLIKE_Z_INVERT) != 0) {
						translations[frame].z = -translations[frame].z;
					}
				}
				translations[frame].addLocal(offset);//TODO: ipo influence
			}
			track.setKeyframes(track.getTimes(), translations, track.getRotations(), track.getScales());
		}
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_LOCLIKE;
	}
}
