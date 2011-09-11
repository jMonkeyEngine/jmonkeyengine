package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Size limit' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class ConstraintSizeLimit extends Constraint {
	private static final int LIMIT_XMIN = 0x01;
	private static final int LIMIT_XMAX = 0x02;
	private static final int LIMIT_YMIN = 0x04;
	private static final int LIMIT_YMAX = 0x08;
	private static final int LIMIT_ZMIN = 0x10;
	private static final int LIMIT_ZMAX = 0x20;
	
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
	public ConstraintSizeLimit(Structure constraintStructure, Long boneOMA,
			Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, boneOMA, influenceIpo, blenderContext);
	}

	@Override
	public void affectAnimation(Skeleton skeleton, BoneAnimation boneAnimation) {
		BoneTrack boneTrack = this.getBoneTrack(skeleton, boneAnimation);
		if (boneTrack != null) {
			int flag = ((Number) data.getFieldValue("flag")).intValue();
			Vector3f[] scales = boneTrack.getScales();
			int maxFrames = scales.length;
			for (int frame = 0; frame < maxFrames; ++frame) {
				float influence = ipo.calculateValue(frame);
				if ((flag & LIMIT_XMIN) != 0) {
					float xmin = ((Number) data.getFieldValue("xmin")).floatValue();
					if (scales[frame].x < xmin) {
						scales[frame].x -= (scales[frame].x - xmin) * influence;
					}
				}
				if ((flag & LIMIT_XMAX) != 0) {
					float xmax = ((Number) data.getFieldValue("xmax")).floatValue();
					if (scales[frame].x > xmax) {
						scales[frame].x -= (scales[frame].x - xmax) * influence;
					}
				}
				if ((flag & LIMIT_YMIN) != 0) {
					float ymin = ((Number) data.getFieldValue("ymin")).floatValue();
					if (scales[frame].y < ymin) {
						scales[frame].y -= (scales[frame].y - ymin) * influence;
					}
				}
				if ((flag & LIMIT_YMAX) != 0) {
					float ymax = ((Number) data.getFieldValue("ymax")).floatValue();
					if (scales[frame].y > ymax) {
						scales[frame].y -= (scales[frame].y - ymax) * influence;
					}
				}
				if ((flag & LIMIT_ZMIN) != 0) {
					float zmin = ((Number) data.getFieldValue("zmin")).floatValue();
					if (scales[frame].z < zmin) {
						scales[frame].z -= (scales[frame].z - zmin) * influence;
					}
				}
				if ((flag & LIMIT_ZMAX) != 0) {
					float zmax = ((Number) data.getFieldValue("zmax")).floatValue();
					if (scales[frame].z > zmax) {
						scales[frame].z -= (scales[frame].z - zmax) * influence;
					}
				}//TODO: consider constraint space !!!
			}
			boneTrack.setKeyframes(boneTrack.getTimes(), boneTrack.getTranslations(), boneTrack.getRotations(), scales);
		}
	}
	
	@Override
	public ConstraintType getType() {
		return ConstraintType.CONSTRAINT_TYPE_SIZELIMIT;
	}
}
