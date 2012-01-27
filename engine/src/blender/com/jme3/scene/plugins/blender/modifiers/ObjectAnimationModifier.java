package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.SpatialTrack;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This modifier allows to add animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ObjectAnimationModifier extends Modifier {
	private static final Logger	LOGGER	= Logger.getLogger(ObjectAnimationModifier.class.getName());

	/** Loaded animation data. */
	private AnimData			animData;

	/**
	 * This constructor reads animation of the object itself (without bones) and
	 * stores it as an ArmatureModifierData modifier. The animation is returned
	 * as a modifier. It should be later applied regardless other modifiers. The
	 * reason for this is that object may not have modifiers added but it's
	 * animation should be working. The stored modifier is an anim data and
	 * additional data is given object's OMA.
	 * 
	 * @param ipo
	 *            the object's interpolation curves
	 * @param objectAnimationName
	 *            the name of object's animation
	 * @param objectOMA
	 *            the OMA of the object
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ObjectAnimationModifier(Ipo ipo, String objectAnimationName, Long objectOMA, BlenderContext blenderContext) throws BlenderFileException {
		int fps = blenderContext.getBlenderKey().getFps();

		// calculating track
		SpatialTrack track = (SpatialTrack) ipo.calculateTrack(-1, 0, ipo.getLastFrame(), fps, true);

		Animation animation = new Animation(objectAnimationName, ipo.getLastFrame() / fps);
		animation.setTracks(new SpatialTrack[] { track });
		ArrayList<Animation> animations = new ArrayList<Animation>(1);
		animations.add(animation);

		animData = new AnimData(null, animations);
		blenderContext.setAnimData(objectOMA, animData);
	}

	@Override
	public Node apply(Node node, BlenderContext blenderContext) {
		if (invalid) {
			LOGGER.log(Level.WARNING, "Armature modifier is invalid! Cannot be applied to: {0}", node.getName());
		}// if invalid, animData will be null
		if (animData != null) {
			// INFO: constraints for this modifier are applied in the
			// ObjectHelper when the whole object is loaded
			ArrayList<Animation> animList = animData.anims;
			if (animList != null && animList.size() > 0) {
				HashMap<String, Animation> anims = new HashMap<String, Animation>();
				for (int i = 0; i < animList.size(); ++i) {
					Animation animation = animList.get(i);
					anims.put(animation.getName(), animation);
				}

				AnimControl control = new AnimControl(null);
				control.setAnimations(anims);
				node.addControl(control);
			}
		}
		return node;
	}

	@Override
	public String getType() {
		return Modifier.OBJECT_ANIMATION_MODIFIER_DATA;
	}
}
