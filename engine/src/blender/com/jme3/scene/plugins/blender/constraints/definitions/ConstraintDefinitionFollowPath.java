package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Follow path' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionFollowPath extends ConstraintDefinition {
    private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionFollowPath.class.getName());

    public ConstraintDefinitionFollowPath(Structure constraintData, BlenderContext blenderContext) {
        super(constraintData, blenderContext);
    }

    @Override
    public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
        // TODO: implement when curves are implemented
        LOGGER.log(Level.WARNING, "'Follow path' not implemented! Curves not yet implemented!");
    }
}
