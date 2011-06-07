package com.jme3.scene.plugins.blender.structures;

/**
 * This class represents an object's modifier. The modifier object can be varied and the user needs to know what is
 * the type of it for the specified type name. For example "ArmatureModifierData" type specified in blender is
 * represented by AnimData object from jMonkeyEngine.
 * @author Marcin Roguski (Kaelthas)
 */
public class Modifier {
	public static final String	ARRAY_MODIFIER_DATA		= "ArrayModifierData";
	public static final String	ARMATURE_MODIFIER_DATA	= "ArmatureModifierData";
	public static final String	PARTICLE_MODIFIER_DATA	= "ParticleSystemModifierData";

	/** Blender's type of modifier. */
	private String				type;
	/** JME modifier representation object. */
	private Object				jmeModifierRepresentation;
	/** Various additional data used by modifiers.*/
	private Object				additionalData;
	/**
	 * Constructor. Creates modifier object.
	 * @param type
	 *        blender's type of modifier
	 * @param modifier
	 *        JME modifier representation object
	 */
	public Modifier(String type, Object modifier, Object additionalData) {
		this.type = type;
		this.jmeModifierRepresentation = modifier;
		this.additionalData = additionalData;
	}

	/**
	 * This method returns JME modifier representation object.
	 * @return JME modifier representation object
	 */
	public Object getJmeModifierRepresentation() {
		return jmeModifierRepresentation;
	}

	/**
	 * This method returns blender's type of modifier.
	 * @return blender's type of modifier
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * This method returns additional data stored in the modifier.
	 * @return the additional data stored in the modifier
	 */
	public Object getAdditionalData() {
		return additionalData;
	}
}