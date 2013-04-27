package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.file.Structure;

public abstract class ConstraintDefinition {
    protected int          flag;
    private Object         owner;
    private BlenderContext blenderContext;
    private Long           ownerOMA;

    public ConstraintDefinition(Structure constraintData, Long ownerOMA, BlenderContext blenderContext) {
        if (constraintData != null) {// Null constraint has no data
            Number flag = (Number) constraintData.getFieldValue("flag");
            if (flag != null) {
                this.flag = flag.intValue();
            }
        }
        this.blenderContext = blenderContext;
        this.ownerOMA = ownerOMA;
    }

    /**
     * This method is here because we have no guarantee that the owner is loaded
     * when constraint is being created. So use it to get the owner when it is
     * needed for computations.
     * 
     * @return the owner of the constraint or null if none is set
     */
    public Object getOwner() {
        if (ownerOMA != null && owner == null) {
            owner = blenderContext.getLoadedFeature(ownerOMA, LoadedFeatureDataType.LOADED_FEATURE);
            if (owner == null) {
                throw new IllegalStateException("Cannot load constraint's owner for constraint type: " + this.getClass().getName());
            }
        }
        return owner;
    }

    public boolean isImplemented() {
        return true;
    }

    public abstract String getConstraintTypeName();

    public abstract void bake(Transform ownerTransform, Transform targetTransform, float influence);
}
