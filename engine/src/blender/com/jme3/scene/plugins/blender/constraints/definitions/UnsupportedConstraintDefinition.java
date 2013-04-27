package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Transform;

/**
 * This class represents a constraint that is defined by blender but not
 * supported by either importer ot jme. It only wirtes down a warning when
 * baking is called.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class UnsupportedConstraintDefinition extends ConstraintDefinition {
    private String typeName;

    public UnsupportedConstraintDefinition(String typeName) {
        super(null, null, null);
        this.typeName = typeName;
    }

    @Override
    public void bake(Transform ownerTransform, Transform targetTransform, float influence) {
    }

    @Override
    public boolean isImplemented() {
        return false;
    }

    @Override
    public String getConstraintTypeName() {
        return typeName;
    }
}
