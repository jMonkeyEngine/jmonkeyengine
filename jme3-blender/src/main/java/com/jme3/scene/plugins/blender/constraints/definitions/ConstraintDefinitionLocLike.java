package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.animation.Bone;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Loc like' constraint type in blender.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionLocLike extends ConstraintDefinition {
    private static final int LOCLIKE_X        = 0x01;
    private static final int LOCLIKE_Y        = 0x02;
    private static final int LOCLIKE_Z        = 0x04;
    // protected static final int LOCLIKE_TIP = 0x08;//this is deprecated in
    // blender
    private static final int LOCLIKE_X_INVERT = 0x10;
    private static final int LOCLIKE_Y_INVERT = 0x20;
    private static final int LOCLIKE_Z_INVERT = 0x40;
    private static final int LOCLIKE_OFFSET   = 0x80;

    public ConstraintDefinitionLocLike(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        super(constraintData, ownerOMA, blenderContext);
        if (blenderContext.getBlenderKey().isFixUpAxis()) {
            // swapping Y and X limits flag in the bitwise flag
            int y = flag & LOCLIKE_Y;
            int invY = flag & LOCLIKE_Y_INVERT;
            int z = flag & LOCLIKE_Z;
            int invZ = flag & LOCLIKE_Z_INVERT;
            // clear the other flags to swap them
            flag &= LOCLIKE_X | LOCLIKE_X_INVERT | LOCLIKE_OFFSET;

            flag |= y << 1;
            flag |= invY << 1;
            flag |= z >> 1;
            flag |= invZ >> 1;

            trackToBeChanged = (flag & LOCLIKE_X) != 0 || (flag & LOCLIKE_Y) != 0 || (flag & LOCLIKE_Z) != 0;
        }
    }

    @Override
    public boolean isTrackToBeChanged() {
        // location copy does not work on bones who are connected to their parent
        return trackToBeChanged && !(this.getOwner() instanceof Bone && ((Bone) this.getOwner()).getParent() != null && blenderContext.getBoneContext(ownerOMA).is(BoneContext.CONNECTED_TO_PARENT));
    }

    @Override
    public void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence) {
        if (influence == 0 || targetTransform == null || !this.isTrackToBeChanged()) {
            return;
        }

        Transform ownerTransform = this.getOwnerTransform(ownerSpace);

        Vector3f ownerLocation = ownerTransform.getTranslation();
        Vector3f targetLocation = targetTransform.getTranslation();

        Vector3f startLocation = ownerTransform.getTranslation().clone();
        Vector3f offset = Vector3f.ZERO;
        if ((flag & LOCLIKE_OFFSET) != 0) {// we add the original location to the copied location
            offset = startLocation;
        }

        if ((flag & LOCLIKE_X) != 0) {
            ownerLocation.x = targetLocation.x;
            if ((flag & LOCLIKE_X_INVERT) != 0) {
                ownerLocation.x = -ownerLocation.x;
            }
        }
        if ((flag & LOCLIKE_Y) != 0) {
            ownerLocation.y = targetLocation.y;
            if ((flag & LOCLIKE_Y_INVERT) != 0) {
                ownerLocation.y = -ownerLocation.y;
            }
        }
        if ((flag & LOCLIKE_Z) != 0) {
            ownerLocation.z = targetLocation.z;
            if ((flag & LOCLIKE_Z_INVERT) != 0) {
                ownerLocation.z = -ownerLocation.z;
            }
        }
        ownerLocation.addLocal(offset);

        if (influence < 1.0f) {
            startLocation.subtractLocal(ownerLocation).normalizeLocal().mult(influence);
            ownerLocation.addLocal(startLocation);
        }

        this.applyOwnerTransform(ownerTransform, ownerSpace);
    }

    @Override
    public String getConstraintTypeName() {
        return "Copy location";
    }

    @Override
    public boolean isTargetRequired() {
        return true;
    }
}
