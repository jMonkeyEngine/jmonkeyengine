package com.jme3.scene.plugins.blender.modifiers;

import java.util.logging.Logger;

import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
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
		/*
		Pointer pIpo = (Pointer) objectStructure.getFieldValue("ipo");
		if (pIpo.isNotNull()) {
			// check if there is an action name connected with this ipo
			String objectAnimationName = null;
			List<FileBlockHeader> actionBlocks = blenderContext
					.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
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
			int[] animationFrames = blenderContext.getBlenderKey().getAnimationFrames(objectName, objectAnimationName);
			if (animationFrames == null) {// if the name was created here there are no frames set for the animation
				animationFrames = new int[] { 1, ipo.getLastFrame() };
			}
			int fps = blenderContext.getBlenderKey().getFps();
			float start = (float) animationFrames[0] / (float) fps;
			float stop = (float) animationFrames[1] / (float) fps;

			// calculating track for the only bone in this skeleton
			BoneTrack[] tracks = new BoneTrack[1];
			tracks[0] = ipo.calculateTrack(0, animationFrames[0], animationFrames[1], fps);

			BoneAnimation boneAnimation = new BoneAnimation(objectAnimationName, stop - start);
			boneAnimation.setTracks(tracks);
			ArrayList<Animation> animations = new ArrayList<Animation>(1);
			animations.add(boneAnimation);

			// preparing the object's bone
			ObjectHelper objectHelper = blenderContext.getHelper(ObjectHelper.class);
			Transform t = objectHelper.getTransformation(objectStructure, blenderContext);
			Bone bone = new Bone(null);
			bone.setBindTransforms(t.getTranslation(), t.getRotation(), t.getScale());

			animData = new AnimData(new Skeleton(new Bone[] { bone }), animations);
			objectOMA = objectStructure.getOldMemoryAddress();
		}
		*/
	}
	
	@Override
	public Node apply(Node node, BlenderContext blenderContext) {
		LOGGER.warning("Object animation modifier not yet implemented!");
		return node;
	}

	@Override
	public String getType() {
		return Modifier.OBJECT_ANIMATION_MODIFIER_DATA;
	}
}
