package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Rot limit' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintRotLimit extends Constraint {
	private static final int LIMIT_XROT = 0x01;
	private static final int LIMIT_YROT = 0x02;
	private static final int LIMIT_ZROT = 0x04;
    
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
	public ConstraintRotLimit(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation) {
		BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation);
		if (boneTrack != null) {
			int flag = ((Number) data.getFieldValue("flag")).intValue();
			Quaternion[] rotations = boneTrack.getRotations();
			int maxFrames = rotations.length;
			for (int frame = 0; frame < maxFrames; ++frame) {
				float[] angles = rotations[frame].toAngles(null);
				float influence = ipo.calculateValue(frame);
				if ((flag & LIMIT_XROT) != 0) {
					float xmin = ((Number) data.getFieldValue("xmin")).floatValue() * FastMath.DEG_TO_RAD;
					float xmax = ((Number) data.getFieldValue("xmax")).floatValue() * FastMath.DEG_TO_RAD;
					float difference = 0.0f;
					if (angles[0] < xmin) {
						difference = (angles[0] - xmin) * influence;
					} else if (angles[0] > xmax) {
						difference = (angles[0] - xmax) * influence;
					}
					angles[0] -= difference;
				}
				if ((flag & LIMIT_YROT) != 0) {
					float ymin = ((Number) data.getFieldValue("ymin")).floatValue() * FastMath.DEG_TO_RAD;
					float ymax = ((Number) data.getFieldValue("ymax")).floatValue() * FastMath.DEG_TO_RAD;
					float difference = 0.0f;
					if (angles[1] < ymin) {
						difference = (angles[1] - ymin) * influence;
					} else if (angles[1] > ymax) {
						difference = (angles[1] - ymax) * influence;
					}
					angles[1] -= difference;
				}
				if ((flag & LIMIT_ZROT) != 0) {
					float zmin = ((Number) data.getFieldValue("zmin")).floatValue() * FastMath.DEG_TO_RAD;
					float zmax = ((Number) data.getFieldValue("zmax")).floatValue() * FastMath.DEG_TO_RAD;
					float difference = 0.0f;
					if (angles[2] < zmin) {
						difference = (angles[2] - zmin) * influence;
					} else if (angles[2] > zmax) {
						difference = (angles[2] - zmax) * influence;
					}
					angles[2] -= difference;
				}
				rotations[frame].fromAngles(angles);//TODO: consider constraint space !!!
			}
			boneTrack.setKeyframes(boneTrack.getTimes(), boneTrack.getTranslations(), rotations, boneTrack.getScales());
		}
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_ROTLIMIT;
	}
}
