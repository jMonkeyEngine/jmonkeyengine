package com.jme3.scene.plugins.blender.constraints.definitions;

import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * A base class for all constraint definitions.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class ConstraintDefinition {
    /** Constraints flag. Used to load user's options applied to the constraint. */
    protected int          flag;
    /** The constraint's owner. Loaded during runtime. */
    private Object         owner;
    /** The blender context. */
    private BlenderContext blenderContext;
    /** The constraint's owner OMA. */
    private Long           ownerOMA;

    /**
     * Loads a constraint definition based on the constraint definition
     * structure.
     * 
     * @param constraintData
     *            the constraint definition structure
     * @param ownerOMA
     *            the constraint's owner OMA
     * @param blenderContext
     *            the blender context
     */
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
    protected Object getOwner() {
        if (ownerOMA != null && owner == null) {
            owner = blenderContext.getLoadedFeature(ownerOMA, LoadedFeatureDataType.LOADED_FEATURE);
            if (owner == null) {
                throw new IllegalStateException("Cannot load constraint's owner for constraint type: " + this.getClass().getName());
            }
        }
        return owner;
    }

    /**
     * @return <b>true</b> if the definition is implemented and <b>false</b>
     *         otherwise
     */
    public boolean isImplemented() {
        return true;
    }

    /**
     * @return the type name of the constraint
     */
    public abstract String getConstraintTypeName();

    /**
     * Bakes the constraint for the current feature (bone or spatial) position.
     * 
     * @param ownerTransform
     *            the input transform (here the result is stored)
     * @param targetTransform
     *            the target transform used by some of the constraints
     * @param influence
     *            the influence of the constraint (from range <0; 1>)
     */
    public abstract void bake(Transform ownerTransform, Transform targetTransform, float influence);
}
