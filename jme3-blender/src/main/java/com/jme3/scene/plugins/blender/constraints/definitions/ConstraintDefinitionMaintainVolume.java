package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.animation.Bone;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Maintain volume' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ConstraintDefinitionMaintainVolume extends ConstraintDefinition {
    private static final int FLAG_MASK_X = 0;
    private static final int FLAG_MASK_Y = 1;
    private static final int FLAG_MASK_Z = 2;

    private float            volume;

    public ConstraintDefinitionMaintainVolume(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        volume = (float) Math.sqrt(((Number) constraintData.getFieldValue("volume")).floatValue());
        trackToBeChanged = volume != 1 && (flag & (FLAG_MASK_X | FLAG_MASK_Y | FLAG_MASK_Z)) != 0;
    }

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (trackToBeChanged && influence > 0) {
            // the maintain volume constraint is applied directly to object's scale, so no need to do it again
            // but in case of bones we need to make computations
            if (this.getOwner() instanceof Bone) {
                Transform ownerTransform = this.getOwnerTransform(ownerSpace);
                switch (flag) {
                    case FLAG_MASK_X:
                        ownerTransform.getScale().multLocal(1, volume, volume);
                        break;
                    case FLAG_MASK_Y:
                        ownerTransform.getScale().multLocal(volume, 1, volume);
                        break;
                    case FLAG_MASK_Z:
                        ownerTransform.getScale().multLocal(volume, volume, 1);
                        break;
                    default:
                        throw new IllegalStateException("Unknown flag value: " + flag);
                }
                this.applyOwnerTransform(ownerTransform, ownerSpace);
            }
        }
    }

    @Override
    public String getConstraintTypeName() {
        return "Maintain volume";
    }

    @Override
    public boolean isTargetRequired() {
        return false;
    }
}
