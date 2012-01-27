package com.jme3.scene.plugins.blender.constraints;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class should be used for constraint calculations.
 * @author Marcin Roguski (Kaelthas)
 */
public class ConstraintHelper extends AbstractBlenderHelper {
	private static final Logger LOGGER = Logger.getLogger(ConstraintHelper.class.getName());
	
	private static final Map<String, Class<? extends Constraint>> constraintClasses = new HashMap<String, Class<? extends Constraint>>(22);
	static {
		constraintClasses.put("bActionConstraint", ConstraintAction.class);
		constraintClasses.put("bChildOfConstraint", ConstraintChildOf.class);
		constraintClasses.put("bClampToConstraint", ConstraintClampTo.class);
		constraintClasses.put("bDistLimitConstraint", ConstraintDistLimit.class);
		constraintClasses.put("bFollowPathConstraint", ConstraintFollowPath.class);
		constraintClasses.put("bKinematicConstraint", ConstraintInverseKinematics.class);
		constraintClasses.put("bLockTrackConstraint", ConstraintLockTrack.class);
		constraintClasses.put("bLocateLikeConstraint", ConstraintLocLike.class);
		constraintClasses.put("bLocLimitConstraint", ConstraintLocLimit.class);
		constraintClasses.put("bMinMaxConstraint", ConstraintMinMax.class);
		constraintClasses.put("bNullConstraint", ConstraintNull.class);
		constraintClasses.put("bPythonConstraint", ConstraintPython.class);
		constraintClasses.put("bRigidBodyJointConstraint", ConstraintRigidBodyJoint.class);
		constraintClasses.put("bRotateLikeConstraint", ConstraintRotLike.class);
		constraintClasses.put("bShrinkWrapConstraint", ConstraintShrinkWrap.class);
		constraintClasses.put("bSizeLikeConstraint", ConstraintSizeLike.class);
		constraintClasses.put("bSizeLimitConstraint", ConstraintSizeLimit.class);
		constraintClasses.put("bStretchToConstraint", ConstraintStretchTo.class);
		constraintClasses.put("bTransformConstraint", ConstraintTransform.class);
		constraintClasses.put("bRotLimitConstraint", ConstraintRotLimit.class);
		//Blender 2.50+
		constraintClasses.put("bSplineIKConstraint", ConstraintSplineInverseKinematic.class);
		constraintClasses.put("bDampTrackConstraint", ConstraintDampTrack.class);
		constraintClasses.put("bPivotConstraint", ConstraintDampTrack.class);
	}
	
	/**
	 * Helper constructor. It's main task is to generate the affection functions. These functions are common to all
	 * ConstraintHelper instances. Unfortunately this constructor might grow large. If it becomes too large - I shall
	 * consider refactoring. The constructor parses the given blender version and stores the result. Some
	 * functionalities may differ in different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
	 */
	public ConstraintHelper(String blenderVersion, BlenderContext blenderContext, boolean fixUpAxis) {
		super(blenderVersion, fixUpAxis);
	}

	/**
	 * This method reads constraints for for the given structure. The
	 * constraints are loaded only once for object/bone.
	 * 
	 * @param objectStructure
	 *            the structure we read constraint's for
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 */
	public void loadConstraints(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
		LOGGER.fine("Loading constraints.");
		// reading influence ipos for the constraints
		IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
		Map<String, Map<String, Ipo>> constraintsIpos = new HashMap<String, Map<String, Ipo>>();
		Pointer pActions = (Pointer) objectStructure.getFieldValue("action");
		if (pActions.isNotNull()) {
			List<Structure> actions = pActions.fetchData(blenderContext.getInputStream());
			for (Structure action : actions) {
				Structure chanbase = (Structure) action.getFieldValue("chanbase");
				List<Structure> actionChannels = chanbase.evaluateListBase(blenderContext);
				for (Structure actionChannel : actionChannels) {
					Map<String, Ipo> ipos = new HashMap<String, Ipo>();
					Structure constChannels = (Structure) actionChannel.getFieldValue("constraintChannels");
					List<Structure> constraintChannels = constChannels.evaluateListBase(blenderContext);
					for (Structure constraintChannel : constraintChannels) {
						Pointer pIpo = (Pointer) constraintChannel.getFieldValue("ipo");
						if (pIpo.isNotNull()) {
							String constraintName = constraintChannel.getFieldValue("name").toString();
							Ipo ipo = ipoHelper.fromIpoStructure(pIpo.fetchData(blenderContext.getInputStream()).get(0), blenderContext);
							ipos.put(constraintName, ipo);
						}
					}
					String actionName = actionChannel.getFieldValue("name").toString();
					constraintsIpos.put(actionName, ipos);
				}
			}
		}
		
		//loading constraints connected with the object's bones
		Pointer pPose = (Pointer) objectStructure.getFieldValue("pose");
		if (pPose.isNotNull()) {
			List<Structure> poseChannels = ((Structure) pPose.fetchData(blenderContext.getInputStream()).get(0).getFieldValue("chanbase")).evaluateListBase(blenderContext);
			for (Structure poseChannel : poseChannels) {
				List<Constraint> constraintsList = new ArrayList<Constraint>();
				Long boneOMA = Long.valueOf(((Pointer) poseChannel.getFieldValue("bone")).getOldMemoryAddress());
				
				//the name is read directly from structure because bone might not yet be loaded
				String name = blenderContext.getFileBlock(boneOMA).getStructure(blenderContext).getFieldValue("name").toString();
				List<Structure> constraints = ((Structure) poseChannel.getFieldValue("constraints")).evaluateListBase(blenderContext);
				for (Structure constraint : constraints) {
					String constraintName = constraint.getFieldValue("name").toString();
					Map<String, Ipo> ipoMap = constraintsIpos.get(name);
					Ipo ipo = ipoMap==null ? null : ipoMap.get(constraintName);
					if (ipo == null) {
						float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
						ipo = ipoHelper.fromValue(enforce);
					}
					constraintsList.add(this.createConstraint(constraint, boneOMA, ipo, blenderContext));
				}
				blenderContext.addConstraints(boneOMA, constraintsList);
			}
		}

		//loading constraints connected with the object itself
		List<Structure> constraints = ((Structure)objectStructure.getFieldValue("constraints")).evaluateListBase(blenderContext);
		List<Constraint> constraintsList = new ArrayList<Constraint>(constraints.size());
		
		for(Structure constraint : constraints) {
			String constraintName = constraint.getFieldValue("name").toString();
			String objectName = objectStructure.getName();
			
			Map<String, Ipo> objectConstraintsIpos = constraintsIpos.get(objectName);
			Ipo ipo = objectConstraintsIpos!=null ? objectConstraintsIpos.get(constraintName) : null;
			if (ipo == null) {
				float enforce = ((Number) constraint.getFieldValue("enforce")).floatValue();
				ipo = ipoHelper.fromValue(enforce);
			}
			constraintsList.add(this.createConstraint(constraint, objectStructure.getOldMemoryAddress(), ipo, blenderContext));
		}
		blenderContext.addConstraints(objectStructure.getOldMemoryAddress(), constraintsList);
	}
	
	/**
	 * This method creates the constraint instance.
	 * 
	 * @param constraintStructure
	 *            the constraint's structure (bConstraint clss in blender 2.49).
	 * @param ownerOMA
	 *            the old memory address of the constraint's owner
	 * @param influenceIpo
	 *            the ipo curve of the influence factor
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	protected Constraint createConstraint(Structure constraintStructure, Long ownerOMA, Ipo influenceIpo, 
						BlenderContext blenderContext) throws BlenderFileException {
		String constraintClassName = this.getConstraintClassName(constraintStructure, blenderContext);
		Class<? extends Constraint> constraintClass = constraintClasses.get(constraintClassName);
		if(constraintClass != null) {
			try {
				return (Constraint) constraintClass.getDeclaredConstructors()[0].newInstance(constraintStructure, ownerOMA, influenceIpo, 
						blenderContext);
			} catch (IllegalArgumentException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (SecurityException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (InstantiationException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (IllegalAccessException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			} catch (InvocationTargetException e) {
				throw new BlenderFileException(e.getLocalizedMessage(), e);
			}
		} else {
			throw new BlenderFileException("Unknown constraint type: " + constraintClassName);
		}
	}
	
	protected String getConstraintClassName(Structure constraintStructure, BlenderContext blenderContext) throws BlenderFileException {
		Pointer pData = (Pointer)constraintStructure.getFieldValue("data");
		if(pData.isNotNull()) {
			Structure data = pData.fetchData(blenderContext.getInputStream()).get(0);
			return data.getType();
			
		}
		return constraintStructure.getType();
	}
	
	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return true;
	}
}
