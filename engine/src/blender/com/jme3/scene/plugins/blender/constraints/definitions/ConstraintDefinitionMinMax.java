package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents 'Min max' constraint type in blender.
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ConstraintDefinitionMinMax extends ConstraintDefinition {
    private static final Logger LOGGER = Logger.getLogger(ConstraintDefinitionMinMax.class.getName());

    public ConstraintDefinitionMinMax(Structure constraintData, BlenderContext blenderContext) {
        super(constraintData, blenderContext);
    }

    @Override
    public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
        // TODO: implement 'Min max' constraint
        LOGGER.log(Level.WARNING, "'Min max' constraint NOT implemented!");
    }
}
