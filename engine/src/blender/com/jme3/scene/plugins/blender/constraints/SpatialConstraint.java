package com.jme3.scene.plugins.blender.constraints;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * Constraint applied on the spatial objects. This includes: nodes, cameras
 * nodes and light nodes.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class SpatialConstraint extends Constraint {
    public SpatialConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
        super(constraintStructure, ownerOMA, influenceIpo, blenderContext);
    }

    @Override
    public boolean validate() {
        if (targetOMA != null) {
            return blenderContext.getLoadedFeature(targetOMA, LoadedFeatureDataType.LOADED_FEATURE) != null;
        }
        return true;
    }

    @Override
    public void apply(int frame) {
        Transform ownerTransform = constraintHelper.getTransform(ownerOMA, null, ownerSpace);
        Transform targetTransform = targetOMA != null ? constraintHelper.getTransform(targetOMA, subtargetName, targetSpace) : null;
        constraintDefinition.bake(ownerTransform, targetTransform, this.ipo.calculateValue(frame));
        constraintHelper.applyTransform(ownerOMA, subtargetName, ownerSpace, ownerTransform);
    }
}
