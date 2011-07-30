package com.jme3.scene.plugins.blender.modifiers;

import java.util.ArrayList;
import java.util.List;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Transform;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.animations.IpoHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.objects.ObjectHelper;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * This modifier allows to add animation to the object.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class ObjectAnimationModifier extends ArmatureModifier {

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
	 * @param dataRepository
	 *            the data repository
	 * @return animation modifier is returned, it should be separately applied
	 *         when the object is loaded
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public ObjectAnimationModifier(Structure objectStructure,
			DataRepository dataRepository) throws BlenderFileException {
		Pointer pIpo = (Pointer) objectStructure.getFieldValue("ipo");
		if (pIpo.isNotNull()) {
			// check if there is an action name connected with this ipo
			String objectAnimationName = null;
			List<FileBlockHeader> actionBlocks = dataRepository
					.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
			for (FileBlockHeader actionBlock : actionBlocks) {
				Structure action = actionBlock.getStructure(dataRepository);
				List<Structure> actionChannels = ((Structure) action
						.getFieldValue("chanbase"))
						.evaluateListBase(dataRepository);
				if (actionChannels.size() == 1) {// object's animtion action has
													// only one channel
					Pointer pChannelIpo = (Pointer) actionChannels.get(0)
							.getFieldValue("ipo");
					if (pChannelIpo.equals(pIpo)) {
						objectAnimationName = action.getName();
						break;
					}
				}
			}

			String objectName = objectStructure.getName();
			if (objectAnimationName == null) {// set the object's animation name
												// to object's name
				objectAnimationName = objectName;
			}

			IpoHelper ipoHelper = dataRepository.getHelper(IpoHelper.class);
			Structure ipoStructure = pIpo.fetchData(
					dataRepository.getInputStream()).get(0);
			Ipo ipo = ipoHelper.createIpo(ipoStructure, dataRepository);
			int[] animationFrames = dataRepository.getBlenderKey()
					.getAnimationFrames(objectName, objectAnimationName);
			if (animationFrames == null) {// if the name was created here there
											// are no frames set for the
											// animation
				animationFrames = new int[] { 1, ipo.getLastFrame() };
			}
			int fps = dataRepository.getBlenderKey().getFps();
			float start = (float) animationFrames[0] / (float) fps;
			float stop = (float) animationFrames[1] / (float) fps;

			// calculating track for the only bone in this skeleton
			BoneTrack[] tracks = new BoneTrack[1];
			tracks[0] = ipo.calculateTrack(0, animationFrames[0],
					animationFrames[1], fps);

			BoneAnimation boneAnimation = new BoneAnimation(
					objectAnimationName, stop - start);
			boneAnimation.setTracks(tracks);
			ArrayList<BoneAnimation> animations = new ArrayList<BoneAnimation>(
					1);
			animations.add(boneAnimation);

			// preparing the object's bone
			ObjectHelper objectHelper = dataRepository
					.getHelper(ObjectHelper.class);
			Transform t = objectHelper.getTransformation(objectStructure,
					dataRepository);
			Bone bone = new Bone(null);
			bone.setBindTransforms(t.getTranslation(), t.getRotation(),
					t.getScale());

			jmeModifierRepresentation = new AnimData(new Skeleton(
					new Bone[] { bone }), animations);
			additionalData = objectStructure.getOldMemoryAddress();
		}
	}

	@Override
	public String getType() {
		return Modifier.OBJECT_ANIMATION_MODIFIER_DATA;
	}
}
