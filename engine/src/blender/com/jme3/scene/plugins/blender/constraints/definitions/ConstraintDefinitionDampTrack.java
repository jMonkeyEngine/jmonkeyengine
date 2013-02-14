package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * The damp track constraint. Available for blender 2.50+.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionDampTrack extends ConstraintDefinition {
    private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionDampTrack.class.getName());

    public ConstraintDefinitionDampTrack(Structure constraintData, BlenderContext blenderContext) {
        super(constraintData, blenderContext);
    }

    @Override
    public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
        // TODO Auto-generated method stub
        LOGGER.log(Level.WARNING, "'Damp Track' constraint NOT implemented!");
    }
}
