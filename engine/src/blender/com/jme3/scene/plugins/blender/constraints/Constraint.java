package com.jme3.scene.plugins.blender.constraints;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper.Space;
import com.jme3.scene.plugins.blender.constraints.definitions.ConstraintDefinition;
import com.jme3.scene.plugins.blender.constraints.definitions.ConstraintDefinitionFactory;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * The implementation of a constraint.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class Constraint {
	private static final Logger LOGGER = Logger.getLogger(Constraint.class.getName());
	
	/** Indicates if the constraint is invalid. */
	protected boolean invalid;
	/** The name of this constraint. */
	protected final String name;
	/** Indicates if the constraint is already baked or not. */
	protected boolean baked;
	
	protected Space ownerSpace;
	protected final ConstraintDefinition constraintDefinition;
	protected Long ownerOMA;
	
	protected Long targetOMA;
	protected Space targetSpace;
	protected String subtargetName;
	
	/** The ipo object defining influence. */
	protected final Ipo ipo;
	/** The blender context. */
	protected final BlenderContext blenderContext;
	protected final ConstraintHelper constraintHelper;
	
	/**
	 * This constructor creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param ownerOMA
	 *            the old memory address of the constraint owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public Constraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, BlenderContext blenderContext) throws BlenderFileException {
		this.blenderContext = blenderContext;
		this.name = constraintStructure.getFieldValue("name").toString();
		Pointer pData = (Pointer) constraintStructure.getFieldValue("data");
		if (pData.isNotNull()) {
			Structure data = pData.fetchData(blenderContext.getInputStream()).get(0);
			constraintDefinition = ConstraintDefinitionFactory.createConstraintDefinition(data, blenderContext);
			Pointer pTar = (Pointer)data.getFieldValue("tar");
			if(pTar!= null && pTar.isNotNull()) {
				this.targetOMA = pTar.getOldMemoryAddress();
				this.targetSpace = Space.valueOf(((Number) constraintStructure.getFieldValue("tarspace")).byteValue());
				Object subtargetValue = data.getFieldValue("subtarget");
				if(subtargetValue != null) {//not all constraint data have the subtarget field
					subtargetName = subtargetValue.toString();
				}
			}
		} else {
			//Null constraint has no data, so create it here
			constraintDefinition = ConstraintDefinitionFactory.createConstraintDefinition(null, blenderContext);
		}
		this.ownerSpace = Space.valueOf(((Number) constraintStructure.getFieldValue("ownspace")).byteValue());
		this.ipo = influenceIpo;
		this.ownerOMA = ownerOMA;
		this.constraintHelper = blenderContext.getHelper(ConstraintHelper.class);
	}
	
	/**
	 * This method bakes the required sontraints into its owner. It checks if the constraint is invalid
	 * or if it isn't yet baked. It also performs baking of its target constraints so that the proper baking
	 * order is kept.
	 */
	public void bake() {
		if(invalid) {
			LOGGER.warning("The constraint " + name + " is invalid and will not be applied.");
		} else if(!baked) {
			if(targetOMA != null) {
				List<Constraint> targetConstraints = blenderContext.getConstraints(targetOMA);
				if(targetConstraints != null && targetConstraints.size() > 0) {
					LOGGER.log(Level.FINE, "Baking target constraints of constraint: {0}", name);
					for(Constraint targetConstraint : targetConstraints) {
						targetConstraint.bake();
					}
				}
			}
			
			LOGGER.log(Level.FINE, "Performing baking of constraint: {0}", name);
			this.performBakingOperation();
			baked = true;
		}
	}
	
	/**
	 * This method should be overwridden and perform the baking opertion.
	 */
	protected abstract void performBakingOperation();
	
	/**
	 * This method prepares the tracks for both owner and parent. If either owner or parent have no track while its parent has - 
	 * the tracks are created. The tracks will not modify the owner/target movement but will be there ready for applying constraints.
	 * For example if the owner is a spatial and has no animation but its parent is moving then the track is created for the owner
	 * that will have non modifying values for translation, rotation and scale and will have the same amount of frames as its parent has.
	 */
	protected abstract void prepareTracksForApplyingConstraints();
}