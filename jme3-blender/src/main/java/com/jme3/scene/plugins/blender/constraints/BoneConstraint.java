package com.jme3.scene.plugins.blender.constraints;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;

/**
 * Constraint applied on the bone.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class BoneConstraint extends Constraint {
    private static final Logger LOGGER = Logger.getLogger(BoneConstraint.class.getName());

    /**
     * The bone constraint constructor.
     * 
     * @param constraintStructure
     *            the constraint's structure
     * @param ownerOMA
     *            the OMA of the bone that owns the constraint
     * @param influenceIpo
     *            the influence interpolation curve
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             exception thrown when problems with blender file occur
     */
    public BoneConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
        super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
    }

    @Override
    public boolean validate() {
        if (targetOMA != null) {
            Spatial nodeTarget = (Spatial) blenderContext.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE);
            if (nodeTarget == null) {
                LOGGER.log(Level.WARNING, "Cannot find target for constraint: {0}.", name);
                return false;
            }
            // the second part of the if expression verifies if the found node
            // (if any) is an armature node
            if (blenderContext.getMarkerValue(ObjectHelper.ARMATURE_NODE_MARKER, nodeTarget) != null) {
                if (subtargetName.trim().isEmpty()) {
                    LOGGER.log(Level.WARNING, "No bone target specified for constraint: {0}.", name);
                    return false;
                }
                // if the target is not an object node then it is an Armature,
                // so make sure the bone is in the current skeleton
                BoneContext boneContext = blenderContext.getBoneContext(ownerOMA);
                if (targetOMA.longValue() != boneContext.getArmatureObjectOMA().longValue()) {
                    LOGGER.log(Level.WARNING, "Bone constraint {0} must target bone in the its own skeleton! Targeting bone in another skeleton is not supported!", name);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void apply(int frame) {
        super.apply(frame);
        blenderContext.getBoneContext(ownerOMA).getBone().updateModelTransforms();
    }
}
