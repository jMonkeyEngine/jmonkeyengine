package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.Transform;

/**
 * This class represents a constraint that is defined by blender but not supported by either importer
 * ot jme. It only wirtes down a warning when baking is called.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */ class UnsupportedConstraintDefinition extends ConstraintDefinition {
    private static final Logger LOGGER = Logger.getLogger(UnsupportedConstraintDefinition.class.getName());
    
    private String name;
    
    public UnsupportedConstraintDefinition(String name) {
        super(null, null);
        this.name = name;
    }
    
    @Override
    protected void bake(Transform ownerTransform, Transform targetTransform, float influence) {
        LOGGER.log(Level.WARNING, "'{0}' constraint NOT implemented!", name);
    }
}
