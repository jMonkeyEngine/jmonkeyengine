package com.jme3.scene.plugins.blender.modifiers;

import java.util.List;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;

/**
 * This class represents an object's modifier. The modifier object can be varied
 * and the user needs to know what is the type of it for the specified type
 * name. For example "ArmatureModifierData" type specified in blender is
 * represented by AnimData object from jMonkeyEngine.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class Modifier {
    public static final String ARRAY_MODIFIER_DATA            = "ArrayModifierData";
    public static final String ARMATURE_MODIFIER_DATA         = "ArmatureModifierData";
    public static final String PARTICLE_MODIFIER_DATA         = "ParticleSystemModifierData";
    public static final String MIRROR_MODIFIER_DATA           = "MirrorModifierData";
    public static final String SUBSURF_MODIFIER_DATA          = "SubsurfModifierData";
    public static final String OBJECT_ANIMATION_MODIFIER_DATA = "ObjectAnimationModifierData";

    /** This variable indicates if the modifier is invalid (<b>true</b>) or not (<b>false</b>). */
    protected boolean          invalid;
    /**
     * A variable that tells if the modifier causes modification. Some modifiers like ArmatureModifier might have no
     * Armature object attached and thus not really modifying the feature. In such cases it is good to know if it is
     * sense to add the modifier to the list of object's modifiers.
     */
    protected boolean          modifying                      = true;

    /**
     * This method applies the modifier to the given node.
     * 
     * @param node
     *            the node that will have modifier applied
     * @param blenderContext
     *            the blender context
     */
    public abstract void apply(Node node, BlenderContext blenderContext);

    /**
     * The method that is called when geometries are already created.
     * @param node
     *            the node that will have the modifier applied
     * @param blenderContext
     *            the blender context
     */
    public void postMeshCreationApply(Node node, BlenderContext blenderContext) {
    }

    /**
     * Determines if the modifier can be applied multiple times over one mesh.
     * At this moment only armature and object animation modifiers cannot be
     * applied multiple times.
     * 
     * @param modifierType
     *            the type name of the modifier
     * @return <b>true</b> if the modifier can be applied many times and
     *         <b>false</b> otherwise
     */
    public static boolean canBeAppliedMultipleTimes(String modifierType) {
        return !(ARMATURE_MODIFIER_DATA.equals(modifierType) || OBJECT_ANIMATION_MODIFIER_DATA.equals(modifierType));
    }

    protected boolean validate(Structure modifierStructure, BlenderContext blenderContext) {
        Structure modifierData = (Structure) modifierStructure.getFieldValue("modifier");
        Pointer pError = (Pointer) modifierData.getFieldValue("error");
        invalid = pError.isNotNull();
        return !invalid;
    }

    /**
     * @return <b>true</b> if the modifier causes feature's modification or <b>false</b> if not
     */
    public boolean isModifying() {
        return modifying;
    }

    protected TemporalMesh getTemporalMesh(Node node) {
        List<Spatial> children = node.getChildren();
        if (children != null && children.size() == 1 && children.get(0) instanceof TemporalMesh) {
            return (TemporalMesh) children.get(0);
        }
        return null;
    }
}
