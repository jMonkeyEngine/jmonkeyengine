package com.jme3.scene.plugins.blender.constraints.definitions;

import java.util.Set;

import com.jme3.animation.Bone;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedDataType;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * A base class for all constraint definitions.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class ConstraintDefinition {
    protected ConstraintHelper constraintHelper;
    /** Constraints flag. Used to load user's options applied to the constraint. */
    protected int              flag;
    /** The constraint's owner. Loaded during runtime. */
    private Object             owner;
    /** The blender context. */
    protected BlenderContext   blenderContext;
    /** The constraint's owner OMA. */
    protected Long             ownerOMA;
    /** Stores the OMA addresses of all features whose transform had been altered beside the constraint owner. */
    protected Set<Long>        alteredOmas;

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
        constraintHelper = (ConstraintHelper) (blenderContext == null ? null : blenderContext.getHelper(ConstraintHelper.class));
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
            owner = blenderContext.getLoadedFeature(ownerOMA, LoadedDataType.FEATURE);
            if (owner == null) {
                throw new IllegalStateException("Cannot load constraint's owner for constraint type: " + this.getClass().getName());
            }
        }
        return owner;
    }

    /**
     * The method gets the owner's transformation. The owner can be either bone or spatial.
     * @param ownerSpace
     *            the space in which the computed transformation is given
     * @return the constraint owner's transformation
     */
    protected Transform getOwnerTransform(Space ownerSpace) {
        if (this.getOwner() instanceof Bone) {
            BoneContext boneContext = blenderContext.getBoneContext(ownerOMA);
            return constraintHelper.getTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), ownerSpace);
        }
        return constraintHelper.getTransform(ownerOMA, null, ownerSpace);
    }

    /**
     * The method applies the given transformation to the owner.
     * @param ownerTransform
     *            the transformation to apply to the owner
     * @param ownerSpace
     *            the space that defines which owner's transformation (ie. global, local, etc. will be set)
     */
    protected void applyOwnerTransform(Transform ownerTransform, Space ownerSpace) {
        if (this.getOwner() instanceof Bone) {
            BoneContext boneContext = blenderContext.getBoneContext(ownerOMA);
            constraintHelper.applyTransform(boneContext.getArmatureObjectOMA(), boneContext.getBone().getName(), ownerSpace, ownerTransform);
        } else {
            constraintHelper.applyTransform(ownerOMA, null, ownerSpace, ownerTransform);
        }
    }

    /**
     * @return <b>true</b> if the definition is implemented and <b>false</b>
     *         otherwise
     */
    public boolean isImplemented() {
        return true;
    }

    /**
     * @return a list of all OMAs of the features that the constraint had altered beside its owner
     */
    public Set<Long> getAlteredOmas() {
        return alteredOmas;
    }

    /**
     * @return the type name of the constraint
     */
    public abstract String getConstraintTypeName();

    /**
     * Bakes the constraint for the current feature (bone or spatial) position.
     * 
     * @param ownerSpace
     *            the space where owner transform will be evaluated in
     * @param targetSpace
     *            the space where target transform will be evaluated in
     * @param targetTransform
     *            the target transform used by some of the constraints
     * @param influence
     *            the influence of the constraint (from range <0; 1>)
     */
    public abstract void bake(Space ownerSpace, Space targetSpace, Transform targetTransform, float influence);
}
