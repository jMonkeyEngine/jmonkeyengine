package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Track;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.constraints.Constraint;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This modifier allows to add animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ObjectAnimationModifier extends Modifier {
	private static final Logger LOGGER = Logger.getLogger(ObjectAnimationModifier.class.getName());

	/** Loaded animation data. */
	private AnimData animData;
	/** Old memory address of the object structure that will have the modifier applied. */
	private Long objectOMA;
	
	/**
	 * This constructor reads animation of the object itself (without bones) and
	 * stores it as an ArmatureModifierData modifier. The animation is returned
	 * as a modifier. It should be later applied regardless other modifiers. The
	 * reason for this is that object may not have modifiers added but it's
	 * animation should be working. The stored modifier is an anim data and
	 * additional data is given object's OMA.
	 * 
	 * @param objectStructure
	 *            the structure of the object
	 * @param blenderContext
	 *            the blender context
	 * @return animation modifier is returned, it should be separately applied
	 *         when the object is loaded
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ObjectAnimationModifier(Structure objectStructure, BlenderContext blenderContext) throws BlenderFileException {
		LOGGER.warning("Object animation modifier not yet implemented!");
		
		Pointer pIpo = (Pointer) objectStructure.getFieldValue("ipo");
		if (pIpo.isNotNull()) {
			// check if there is an action name connected with this ipo
			String objectAnimationName = null;
			List<FileBlockHeader> actionBlocks = blenderContext.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
			for (FileBlockHeader actionBlock : actionBlocks) {
				Structure action = actionBlock.getStructure(blenderContext);
				List<Structure> actionChannels = ((Structure) action.getFieldValue("chanbase")).evaluateListBase(blenderContext);
				if (actionChannels.size() == 1) {// object's animtion action has only one channel
					Pointer pChannelIpo = (Pointer) actionChannels.get(0).getFieldValue("ipo");
					if (pChannelIpo.equals(pIpo)) {
						objectAnimationName = action.getName();
						break;
					}
				}
			}

			String objectName = objectStructure.getName();
			if (objectAnimationName == null) {// set the object's animation name to object's name
				objectAnimationName = objectName;
			}

			IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
			Structure ipoStructure = pIpo.fetchData(blenderContext.getInputStream()).get(0);
			Ipo ipo = ipoHelper.createIpo(ipoStructure, blenderContext);
			int fps = blenderContext.getBlenderKey().getFps();

			// calculating track for the only bone in this skeleton
			Track<?> track = ipo.calculateTrack(-1, 0, ipo.getLastFrame(), fps);
			
			Animation animation = new Animation(objectAnimationName, ipo.getLastFrame() / fps);
			animation.setTracks(new Track<?>[] { track });
			ArrayList<Animation> animations = new ArrayList<Animation>(1);
			animations.add(animation);

			animData = new AnimData(null, animations);
			objectOMA = objectStructure.getOldMemoryAddress();
		}
	}
	
	@Override
	public Node apply(Node node, BlenderContext blenderContext) {
		if(invalid) {
			LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
		}//if invalid, animData will be null
		if(animData == null) {
			return node;
		}
		
		ArrayList<Animation> animList = animData.anims;
		if (animList != null && animList.size() > 0) {
			List<Constraint> constraints = blenderContext.getConstraints(this.objectOMA);
			HashMap<String, Animation> anims = new HashMap<String, Animation>();
			for (int i = 0; i < animList.size(); ++i) {
				Animation animation = (Animation) animList.get(i).clone();

				// baking constraints into animations
				if (constraints != null && constraints.size() > 0) {
					for (Constraint constraint : constraints) {
						constraint.affectAnimation(animation, 0);
					}
				}

				anims.put(animation.getName(), animation);
			}

			AnimControl control = new AnimControl(null);
			control.setAnimations(anims);
			node.addControl(control);
		}
		return node;
	}

	@Override
	public String getType() {
		return Modifier.OBJECT_ANIMATION_MODIFIER_DATA;
	}
}
