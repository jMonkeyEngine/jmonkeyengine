package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Rot limit' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionRotLimit extends ConstraintDefinition {
    private static final int    LIMIT_XROT = 0x01;
    private static final int    LIMIT_YROT = 0x02;
    private static final int    LIMIT_ZROT = 0x04;

    private transient float[][] limits     = new float[3][2];
    private transient float[]   angles     = new float[3];

    public ConstraintDefinitionRotLimit(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        if (blenderContext.getBlenderKey().isFixUpAxis()) {
            limits[0][0] = ((Number) constraintData.getFieldValue("xmin")).floatValue();
            limits[0][1] = ((Number) constraintData.getFieldValue("xmax")).floatValue();
            limits[2][0] = ((Number) constraintData.getFieldValue("ymin")).floatValue();
            limits[2][1] = ((Number) constraintData.getFieldValue("ymax")).floatValue();
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
            for (int i = 0; i < 3; ++i) {
                limits[i][0] *= FastMath.DEG_TO_RAD;
                limits[i][1] *= FastMath.DEG_TO_RAD;
            }
        }

        // make sure that the limits are always in range [0, 2PI)
        // TODO: left it here because it is essential to make sure all cases
        // work poperly
        // but will do it a little bit later ;)
        /*
         * for (int i = 0; i < 3; ++i) { for (int j = 0; j < 2; ++j) { int
         * multFactor = (int)Math.abs(limits[i][j] / FastMath.TWO_PI) ; if
         * (limits[i][j] < 0) { limits[i][j] += FastMath.TWO_PI * (multFactor +
         * 1); } else { limits[i][j] -= FastMath.TWO_PI * multFactor; } } //make
         * sure the lower limit is not greater than the upper one
         * if(limits[i][0] > limits[i][1]) { float temp = limits[i][0];
         * limits[i][0] = limits[i][1]; limits[i][1] = temp; } }
         */

        trackToBeChanged = (flag & (LIMIT_XROT | LIMIT_YROT | LIMIT_ZROT)) != 0;
    }

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (influence == 0 || !trackToBeChanged) {
            return;
        }
        Transform ownerTransform = this.getOwnerTransform(ownerSpace);

        ownerTransform.getRotation().toAngles(angles);
        // make sure that the rotations are always in range [0, 2PI)
        // TODO: same comment as in constructor
        /*
         * for (int i = 0; i < 3; ++i) { int multFactor =
         * (int)Math.abs(angles[i] / FastMath.TWO_PI) ; if(angles[i] < 0) {
         * angles[i] += FastMath.TWO_PI * (multFactor + 1); } else { angles[i]
         * -= FastMath.TWO_PI * multFactor; } }
         */
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
        ownerTransform.getRotation().fromAngles(angles);

        this.applyOwnerTransform(ownerTransform, ownerSpace);
    }

    @Override
    public String getConstraintTypeName() {
        return "Limit rotation";
    }

    @Override
    public boolean isTargetRequired() {
        return false;
    }
}
