package com.jme3.scene.plugins.blender.constraints;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
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
            return blenderContext.getLoadedFeature(targetOMA, LoadedDataType.FEATURE) != null;
        }
        return true;
    }
}
