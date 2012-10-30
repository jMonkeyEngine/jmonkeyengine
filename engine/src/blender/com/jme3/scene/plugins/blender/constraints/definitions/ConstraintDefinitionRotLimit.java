package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Rot limit' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionRotLimit extends ConstraintDefinition {
	private static final int	LIMIT_XROT	= 0x01;
	private static final int	LIMIT_YROT	= 0x02;
	private static final int	LIMIT_ZROT	= 0x04;

	private transient float[][]	limits		= new float[3][2];
	private transient float[]	angles 		= new float[3];
	
	public ConstraintDefinitionRotLimit(Structure constraintData, BlenderContext blenderContext) {
		super(constraintData, blenderContext);
		if (blenderContext.getBlenderKey().isFixUpAxis()/* && owner.spatial != null*/) {//FIXME: !!!!!!!!
			limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
			limits[2][0] = -((Number) constraintData.getFieldValue("ymin")).floatValue();
			limits[2][1] = -((Number) constraintData.getFieldValue("ymax")).floatValue();
			limits[1][0] = ((Number) constraintData.getFieldValue("zmin")).floatValue();
			limits[1][1] = ((Number) constraintData.getFieldValue("zmax")).floatValue();

			// swapping Y and X limits flag in the bitwise flag
			int limitY = flag & LIMIT_YROT;
			int limitZ = flag & LIMIT_ZROT;
			flag &= LIMIT_XROT;// clear the other flags to swap them
			flag |= limitY << 1;
			flag |= limitZ >> 1;
		} else {
			limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
			limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
			limits[1][0] = ((Number) constraintData.getFieldValue("ymin")).floatValue();
			limits[1][1] = ((Number) constraintData.getFieldValue("ymax")).floatValue();
			limits[2][0] = ((Number) constraintData.getFieldValue("zmin")).floatValue();
			limits[2][1] = ((Number) constraintData.getFieldValue("zmax")).floatValue();
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
	public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
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
}
