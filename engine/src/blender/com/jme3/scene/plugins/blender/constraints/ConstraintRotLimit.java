package com.jme3.scene.plugins.blender.constraints;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This class represents 'Rot limit' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintRotLimit extends Constraint {
	private static final int	LIMIT_XROT	= 0x01;
	private static final int	LIMIT_YROT	= 0x02;
	private static final int	LIMIT_ZROT	= 0x04;

	protected float[][]			limits		= new float[3][2];
	protected int				flag;
	protected boolean			updated;

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
	public ConstraintRotLimit(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		super(constraintStructure, ownerOMA, influenceIpo, blenderContext);

		flag = ((Number) data.getFieldValue("flag")).intValue();
		if (blenderContext.getBlenderKey().isFixUpAxis() && owner.spatial != null) {
			limits[0][0] = ((Number) data.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) data.getFieldValue("xmax")).floatValue();
			limits[2][0] = -((Number) data.getFieldValue("ymin")).floatValue();
			limits[2][1] = -((Number) data.getFieldValue("ymax")).floatValue();
			limits[1][0] = ((Number) data.getFieldValue("zmin")).floatValue();
			limits[1][1] = ((Number) data.getFieldValue("zmax")).floatValue();

			// swapping Y and X limits flag in the bitwise flag
			int limitY = flag & LIMIT_YROT;
			int limitZ = flag & LIMIT_ZROT;
			flag &= LIMIT_XROT;// clear the other flags to swap them
			flag |= limitY << 1;
			flag |= limitZ >> 1;
		} else {
			limits[0][0] = ((Number) data.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) data.getFieldValue("xmax")).floatValue();
			limits[1][0] = ((Number) data.getFieldValue("ymin")).floatValue();
			limits[1][1] = ((Number) data.getFieldValue("ymax")).floatValue();
			limits[2][0] = ((Number) data.getFieldValue("zmin")).floatValue();
			limits[2][1] = ((Number) data.getFieldValue("zmax")).floatValue();
		}

		// until blender 2.49 the rotations values were stored in degrees
		if (blenderContext.getBlenderVersion() <= 249) {
			for (int i = 0; i < limits.length; ++i) {
				limits[i][0] *= FastMath.DEG_TO_RAD;
				limits[i][1] *= FastMath.DEG_TO_RAD;
			}
		}
	}

	@Override
	protected void bakeConstraint() {
		this.update();
		Object owner = this.owner.getObject();
		AnimData animData = blenderContext.getAnimData(this.owner.getOma());
		if (animData != null) {
			for (Animation animation : animData.anims) {
				BlenderTrack track = this.getTrack(owner, animData.skeleton, animation);
				Quaternion[] rotations = track.getRotations();
				float[] angles = new float[3];
				int maxFrames = rotations.length;
				for (int frame = 0; frame < maxFrames; ++frame) {
					rotations[frame].toAngles(angles);
					this.rotLimit(angles, ipo.calculateValue(frame));
					rotations[frame].fromAngles(angles);
				}
				track.setKeyframes(track.getTimes(), track.getTranslations(), rotations, track.getScales());
			}
		}

		if (owner instanceof Spatial) {
			Transform ownerTransform = this.owner.getTransform();
			float[] angles = ownerTransform.getRotation().toAngles(null);
			this.rotLimit(angles, ipo.calculateValue(0));
			ownerTransform.getRotation().fromAngles(angles);
			this.owner.applyTransform(ownerTransform);
		}
	}

	/**
	 * This method computes new constrained angles.
	 * 
	 * @param angles
	 *            angles to be altered
	 * @param influence
	 *            the alteration influence
	 */
	private void rotLimit(float[] angles, float influence) {
		if ((flag & LIMIT_XROT) != 0) {
			float difference = 0.0f;
			if (angles[0] < limits[0][0]) {
				difference = (angles[0] - limits[0][0]) * influence;
			} else if (angles[0] > limits[0][1]) {
				difference = (angles[0] - limits[0][1]) * influence;
			}
			angles[0] -= difference;
		}
		if ((flag & LIMIT_YROT) != 0) {
			float difference = 0.0f;
			if (angles[1] < limits[1][0]) {
				difference = (angles[1] - limits[1][0]) * influence;
			} else if (angles[1] > limits[1][1]) {
				difference = (angles[1] - limits[1][1]) * influence;
			}
			angles[1] -= difference;
		}
		if ((flag & LIMIT_ZROT) != 0) {
			float difference = 0.0f;
			if (angles[2] < limits[2][0]) {
				difference = (angles[2] - limits[2][0]) * influence;
			} else if (angles[2] > limits[2][1]) {
				difference = (angles[2] - limits[2][1]) * influence;
			}
			angles[2] -= difference;
		}
	}

	/**
	 * This method is called before baking (performes its operations only once).
	 * It is important to update the state of the limits and owner/target before
	 * baking the constraint.
	 */
	private void update() {
		if (!updated) {
			updated = true;
			if (owner != null) {
				owner.update();
			}
			if (target != null) {
				target.update();
			}
			if (this.owner.getObject() instanceof Bone) {// for bones we need to
															// change the sign
															// of the limits
				for (int i = 0; i < limits.length; ++i) {
					limits[i][0] *= -1;
					limits[i][1] *= -1;
				}
			}

			// sorting the limits (lower is always first)
			for (int i = 0; i < limits.length; ++i) {
				if (limits[i][0] > limits[i][1]) {
					float temp = limits[i][0];
					limits[i][0] = limits[i][1];
					limits[i][1] = temp;
				}
			}
		}
	}
}
