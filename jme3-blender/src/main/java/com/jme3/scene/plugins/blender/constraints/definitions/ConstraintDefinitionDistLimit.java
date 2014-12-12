package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.animation.Bone;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Dist limit' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionDistLimit extends ConstraintDefinition {
    private static final int LIMITDIST_INSIDE    = 0;
    private static final int LIMITDIST_OUTSIDE   = 1;
    private static final int LIMITDIST_ONSURFACE = 2;

    protected int            mode;
    protected float          dist;

    public ConstraintDefinitionDistLimit(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        mode = ((Number) constraintData.getFieldValue("mode")).intValue();
        dist = ((Number) constraintData.getFieldValue("dist")).floatValue();
    }

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (this.getOwner() instanceof Bone && ((Bone) this.getOwner()).getParent() != null && blenderContext.getBoneContext(ownerOMA).is(BoneContext.CONNECTED_TO_PARENT)) {
            // distance limit does not work on bones who are connected to their parent
            return;
        }
        if (influence == 0 || targetTransform == null) {
            return;// no need to do anything
        }

        Transform ownerTransform = this.getOwnerTransform(ownerSpace);

        Vector3f v = ownerTransform.getTranslation().subtract(targetTransform.getTranslation());
        float currentDistance = v.length();
        switch (mode) {
            case LIMITDIST_INSIDE:
                if (currentDistance >= dist) {
                    v.normalizeLocal();
                    v.multLocal(dist + (currentDistance - dist) * (1.0f - influence));
                    ownerTransform.getTranslation().set(v.addLocal(targetTransform.getTranslation()));
                }
                break;
            case LIMITDIST_ONSURFACE:
                if (currentDistance > dist) {
                    v.normalizeLocal();
                    v.multLocal(dist + (currentDistance - dist) * (1.0f - influence));
                    ownerTransform.getTranslation().set(v.addLocal(targetTransform.getTranslation()));
                } else if (currentDistance < dist) {
                    v.normalizeLocal().multLocal(dist * influence);
                    ownerTransform.getTranslation().set(targetTransform.getTranslation().add(v));
                }
                break;
            case LIMITDIST_OUTSIDE:
                if (currentDistance <= dist) {
                    v = targetTransform.getTranslation().subtract(ownerTransform.getTranslation()).normalizeLocal().multLocal(dist * influence);
                    ownerTransform.getTranslation().set(targetTransform.getTranslation().add(v));
                }
                break;
            default:
                throw new IllegalStateException("Unknown distance limit constraint mode: " + mode);
        }

        this.applyOwnerTransform(ownerTransform, ownerSpace);
    }

    @Override
    public boolean isTargetRequired() {
        return true;
    }

    @Override
    public String getConstraintTypeName() {
        return "Limit distance";
    }
}
