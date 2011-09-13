package com.jme3.scene.plugins.blender.modifiers;

import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class represents an object's modifier. The modifier object can be varied
 * and the user needs to know what is the type of it for the specified type
 * name. For example "ArmatureModifierData" type specified in blender is
 * represented by AnimData object from jMonkeyEngine.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class Modifier {

	public static final String ARRAY_MODIFIER_DATA = "ArrayModifierData";
	public static final String ARMATURE_MODIFIER_DATA = "ArmatureModifierData";
	public static final String PARTICLE_MODIFIER_DATA = "ParticleSystemModifierData";
	public static final String MIRROR_MODIFIER_DATA = "MirrorModifierData";
	public static final String SUBSURF_MODIFIER_DATA = "SubsurfModifierData";
	public static final String OBJECT_ANIMATION_MODIFIER_DATA = "ObjectAnimationModifierData";

	/** This variable indicates if the modifier is invalid (<b>true</b>) or not (<b>false</b>). */
	protected boolean invalid;
	
	/**
	 * This method applies the modifier to the given node.
	 * 
	 * @param node
	 *            the node that will have modifier applied
	 * @param blenderContext
	 *            the blender context
	 * @return the node with applied modifier
	 */
	public abstract Node apply(Node node, BlenderContext blenderContext);

	/**
	 * This method returns blender's type of modifier.
	 * 
	 * @return blender's type of modifier
	 */
	public abstract String getType();
	
	protected boolean validate(Structure modifierStructure, BlenderContext blenderContext) {
		Structure modifierData = (Structure)modifierStructure.getFieldValue("modifier");
		Pointer pError = (Pointer) modifierData.getFieldValue("error");
		invalid = pError.isNotNull();
		return !invalid;
	}
}
