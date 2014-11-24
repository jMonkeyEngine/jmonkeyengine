package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Rot like' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionRotLike extends ConstraintDefinition {
    private static final int  ROTLIKE_X        = 0x01;
    private static final int  ROTLIKE_Y        = 0x02;
    private static final int  ROTLIKE_Z        = 0x04;
    private static final int  ROTLIKE_X_INVERT = 0x10;
    private static final int  ROTLIKE_Y_INVERT = 0x20;
    private static final int  ROTLIKE_Z_INVERT = 0x40;
    private static final int  ROTLIKE_OFFSET   = 0x80;

    private transient float[] ownerAngles      = new float[3];
    private transient float[] targetAngles     = new float[3];

    public ConstraintDefinitionRotLike(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
    }

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if(influence == 0 || targetTransform == null) {
            return ;// no need to do anything
        }
        Transform ownerTransform = this.getOwnerTransform(ownerSpace);
        
        Quaternion ownerRotation = ownerTransform.getRotation();
        ownerAngles = ownerRotation.toAngles(ownerAngles);
        targetAngles = targetTransform.getRotation().toAngles(targetAngles);

        Quaternion startRotation = ownerRotation.clone();
        Quaternion offset = Quaternion.IDENTITY;
        if ((flag & ROTLIKE_OFFSET) != 0) {// we add the original rotation to
                                           // the copied rotation
            offset = startRotation;
        }

        if ((flag & ROTLIKE_X) != 0) {
            ownerAngles[0] = targetAngles[0];
            if ((flag & ROTLIKE_X_INVERT) != 0) {
                ownerAngles[0] = -ownerAngles[0];
            }
        }
        if ((flag & ROTLIKE_Y) != 0) {
            ownerAngles[1] = targetAngles[1];
            if ((flag & ROTLIKE_Y_INVERT) != 0) {
                ownerAngles[1] = -ownerAngles[1];
            }
        }
        if ((flag & ROTLIKE_Z) != 0) {
            ownerAngles[2] = targetAngles[2];
            if ((flag & ROTLIKE_Z_INVERT) != 0) {
                ownerAngles[2] = -ownerAngles[2];
            }
        }
        ownerRotation.fromAngles(ownerAngles).multLocal(offset);

        if (influence < 1.0f) {
            // startLocation.subtractLocal(ownerLocation).normalizeLocal().mult(influence);
            // ownerLocation.addLocal(startLocation);
            // TODO
        }
        
        this.applyOwnerTransform(ownerTransform, ownerSpace);
    }

    @Override
    public String getConstraintTypeName() {
        return "Copy rotation";
    }
}
