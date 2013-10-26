package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Size like' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionSizeLike extends ConstraintDefinition {
    private static final int SIZELIKE_X     = 0x01;
    private static final int SIZELIKE_Y     = 0x02;
    private static final int SIZELIKE_Z     = 0x04;
    private static final int LOCLIKE_OFFSET = 0x80;

    public ConstraintDefinitionSizeLike(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        if (blenderContext.getBlenderKey().isFixUpAxis()) {
            // swapping Y and X limits flag in the bitwise flag
            int y = flag & SIZELIKE_Y;
            int z = flag & SIZELIKE_Z;
            flag &= SIZELIKE_X | LOCLIKE_OFFSET;// clear the other flags to swap
                                                // them
            flag |= y << 1;
            flag |= z >> 1;
        }
    }
    
    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        BoneContext boneContext = blenderContext.getBoneContext(ownerOMA);
        Transform ownerTransform = constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), ownerSpace);
        
        Vector3f ownerScale = ownerTransform.getScale();
        Vector3f targetScale = targetTransform.getScale();

        Vector3f offset = Vector3f.ZERO;
        if ((flag & LOCLIKE_OFFSET) != 0) {// we add the original scale to the
                                           // copied scale
            offset = ownerScale.clone();
        }

        if ((flag & SIZELIKE_X) != 0) {
            ownerScale.x = targetScale.x * influence + (1.0f - influence) * ownerScale.x;
        }
        if ((flag & SIZELIKE_Y) != 0) {
            ownerScale.y = targetScale.y * influence + (1.0f - influence) * ownerScale.y;
        }
        if ((flag & SIZELIKE_Z) != 0) {
            ownerScale.z = targetScale.z * influence + (1.0f - influence) * ownerScale.z;
        }
        ownerScale.addLocal(offset);
        
        constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), ownerSpace, ownerTransform);
    }

    @Override
    public String getConstraintTypeName() {
        return "Copy scale";
    }
}
