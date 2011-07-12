/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.plugins.blender.helpers.v249;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.shapes.EmitterMeshVertexShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.data.FileBlockHeader;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.helpers.ParticlesHelper;
import com.jme3.scene.plugins.blender.structures.Constraint;
import com.jme3.scene.plugins.blender.structures.Ipo;
import com.jme3.scene.plugins.blender.structures.Modifier;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;
import com.jme3.scene.plugins.ogre.AnimData;
import com.jme3.scene.shape.Curve;

/**
 * A class that is used in modifiers calculations.
 * @author Marcin Roguski
 */
public class ModifierHelper extends AbstractBlenderHelper {

	private static final Logger	LOGGER	= Logger.getLogger(ModifierHelper.class.getName());

	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in
	 * different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public ModifierHelper(String blenderVersion) {
		super(blenderVersion);
	}

	/**
	 * This method applies modifier to the object.
	 * @param node
	 *        the loaded object
	 * @param modifier
	 *        the modifier to apply
	 * @param dataRepository
	 *        the data repository
	 * @return the node to whom the modifier was applied
	 */
	public Node applyModifier(Node node, Modifier modifier, DataRepository dataRepository) {
		if (Modifier.ARMATURE_MODIFIER_DATA.equals(modifier.getType())) {
			return this.applyArmatureModifierData(node, modifier, dataRepository);
		} else if (Modifier.OBJECT_ANIMATION_MODIFIER_DATA.equals(modifier.getType())) {
			return this.applyObjectAnimationModifier(node, modifier, dataRepository);
		} else if (Modifier.ARRAY_MODIFIER_DATA.equals(modifier.getType())) {
			return this.applyArrayModifierData(node, modifier, dataRepository);
		} else if (Modifier.PARTICLE_MODIFIER_DATA.equals(modifier.getType())) {
			return this.applyParticleSystemModifierData(node, modifier, dataRepository);
		} else if (Modifier.MIRROR_MODIFIER_DATA.equals(modifier.getType())) {
			return this.applyMirrorModifierData(node, modifier, dataRepository);
		} else {
			LOGGER.warning("Modifier: " + modifier.getType() + " not yet implemented!!!");
			return node;
		}
	}

	/**
	 * This method reads the given object's modifiers.
	 * @param objectStructure
	 *        the object structure
	 * @param dataRepository
	 *        the data repository
	 * @param converter
	 *        the converter object (in some cases we need to read an object first before loading the modifier)
	 * @throws BlenderFileException
	 *         this exception is thrown when the blender file is somehow corrupted
	 */
	@SuppressWarnings("unchecked")
	public void readModifiers(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
		Structure modifiersListBase = (Structure) objectStructure.getFieldValue("modifiers");
		List<Structure> modifiers = modifiersListBase.evaluateListBase(dataRepository);
		for (Structure modifier : modifiers) {
			Object loadedModifier = null;
			Object modifierAdditionalData = null;
			if (Modifier.ARRAY_MODIFIER_DATA.equals(modifier.getType())) {// ****************ARRAY MODIFIER
				Map<String, Object> params = new HashMap<String, Object>();

				Number fittype = (Number) modifier.getFieldValue("fit_type");
				params.put("fittype", fittype);
				switch (fittype.intValue()) {
					case 0:// FIXED COUNT
						params.put("count", modifier.getFieldValue("count"));
						break;
					case 1:// FIXED LENGTH
						params.put("length", modifier.getFieldValue("length"));
						break;
					case 2:// FITCURVE
						Pointer pCurveOb = (Pointer) modifier.getFieldValue("curve_ob");
						float length = 0;
						if(pCurveOb.isNotNull()) {
							Structure curveStructure = pCurveOb.fetchData(dataRepository.getInputStream()).get(0);
							ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
							Node curveObject = (Node)objectHelper.toObject(curveStructure, dataRepository);
							Set<Number> referencesToCurveLengths = new HashSet<Number>(curveObject.getChildren().size());
							for(Spatial spatial : curveObject.getChildren()) {
								if(spatial instanceof Geometry) {
									Mesh mesh = ((Geometry) spatial).getMesh();
									if(mesh instanceof Curve) {
										length += ((Curve) mesh).getLength();
									} else {
										//if bevel object has several parts then each mesh will have the same reference
										//to length value (and we should use only one)
										Number curveLength = spatial.getUserData("curveLength");
										if(curveLength!=null && !referencesToCurveLengths.contains(curveLength)) {
											length += curveLength.floatValue();
											referencesToCurveLengths.add(curveLength);
										}
									}
								}
							}
						}
						params.put("length", Float.valueOf(length));
						params.put("fittype", Integer.valueOf(1));// treat it like FIXED LENGTH
						break;
					default:
						assert false : "Unknown array modifier fit type: " + fittype;
				}

				// offset parameters
				int offsettype = ((Number) modifier.getFieldValue("offset_type")).intValue();
				if ((offsettype & 0x01) != 0) {// Constant offset
					DynamicArray<Number> offsetArray = (DynamicArray<Number>) modifier.getFieldValue("offset");
					float[] offset = new float[] { offsetArray.get(0).floatValue(), offsetArray.get(1).floatValue(), offsetArray.get(2).floatValue() };
					params.put("offset", offset);
				}
				if ((offsettype & 0x02) != 0) {// Relative offset
					DynamicArray<Number> scaleArray = (DynamicArray<Number>) modifier.getFieldValue("scale");
					float[] scale = new float[] { scaleArray.get(0).floatValue(), scaleArray.get(1).floatValue(), scaleArray.get(2).floatValue() };
					params.put("scale", scale);
				}
				if ((offsettype & 0x04) != 0) {// Object offset
					Pointer pOffsetObject = (Pointer) modifier.getFieldValue("offset_ob");
					if (pOffsetObject.isNotNull()) {
						params.put("offsetob", pOffsetObject);
					}
				}

				// start cap and end cap
				Pointer pStartCap = (Pointer) modifier.getFieldValue("start_cap");
				if (pStartCap.isNotNull()) {
					params.put("startcap", pStartCap);
				}
				Pointer pEndCap = (Pointer) modifier.getFieldValue("end_cap");
				if (pEndCap.isNotNull()) {
					params.put("endcap", pEndCap);
				}
				loadedModifier = params;
			} else if (Modifier.MIRROR_MODIFIER_DATA.equals(modifier.getType())) {// ****************MIRROR MODIFIER
				Map<String, Object> params = new HashMap<String, Object>();

				params.put("flag", modifier.getFieldValue("flag"));
				params.put("tolerance", modifier.getFieldValue("tolerance"));
				Pointer pMirrorOb = (Pointer) modifier.getFieldValue("mirror_ob");
				if (pMirrorOb.isNotNull()) {
					params.put("mirrorob", pMirrorOb);
				}
				loadedModifier = params;
			} else if (Modifier.ARMATURE_MODIFIER_DATA.equals(modifier.getType())) {// ****************ARMATURE MODIFIER
				Pointer pArmatureObject = (Pointer) modifier.getFieldValue("object");
				if (pArmatureObject.isNotNull()) {
					ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
					Structure armatureObject = (Structure) dataRepository.getLoadedFeature(pArmatureObject.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_STRUCTURE);
					if (armatureObject == null) {// we check this first not to fetch the structure unnecessary
						armatureObject = pArmatureObject.fetchData(dataRepository.getInputStream()).get(0);
					}
					modifierAdditionalData = armatureObject.getOldMemoryAddress();
					ArmatureHelper armatureHelper = dataRepository.getHelper(ArmatureHelper.class);

					// changing bones matrices so that they fit the current object (that is why we need a copy of a skeleton)
					Matrix4f armatureObjectMatrix = objectHelper.getTransformationMatrix(armatureObject);
					Matrix4f inverseMeshObjectMatrix = objectHelper.getTransformationMatrix(objectStructure).invert();
					Matrix4f additionalRootBoneTransformation = inverseMeshObjectMatrix.multLocal(armatureObjectMatrix);
					Bone[] bones = armatureHelper.buildBonesStructure(Long.valueOf(0L), additionalRootBoneTransformation);

					//setting the bones structure inside the skeleton (thus completing its loading)
					Skeleton skeleton = new Skeleton(bones);
					dataRepository.addLoadedFeatures(armatureObject.getOldMemoryAddress(), armatureObject.getName(), armatureObject, skeleton);
					
					String objectName = objectStructure.getName();
					Set<String> animationNames = dataRepository.getBlenderKey().getAnimationNames(objectName);
					if (animationNames != null && animationNames.size() > 0) {
						ArrayList<BoneAnimation> animations = new ArrayList<BoneAnimation>();
						List<FileBlockHeader> actionHeaders = dataRepository.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
						for (FileBlockHeader header : actionHeaders) {
							Structure actionStructure = header.getStructure(dataRepository);
							String actionName = actionStructure.getName();
							if (animationNames.contains(actionName)) {
								int[] animationFrames = dataRepository.getBlenderKey().getAnimationFrames(objectName, actionName);
								int fps = dataRepository.getBlenderKey().getFps();
								float start = (float) animationFrames[0] / (float) fps;
								float stop = (float) animationFrames[1] / (float) fps;
								BoneAnimation boneAnimation = new BoneAnimation(actionName, stop - start);
								boneAnimation.setTracks(armatureHelper.getTracks(actionStructure, dataRepository, objectName, actionName));
								animations.add(boneAnimation);
							}
						}
						loadedModifier = new AnimData(new Skeleton(bones), animations);
					}
				} else {
					LOGGER.warning("Unsupported modifier type: " + modifier.getType());
				}
			} else if (Modifier.PARTICLE_MODIFIER_DATA.equals(modifier.getType())) {// ****************PARTICLES MODIFIER
				Pointer pParticleSystem = (Pointer) modifier.getFieldValue("psys");
				if (pParticleSystem.isNotNull()) {
					ParticlesHelper particlesHelper = dataRepository.getHelper(ParticlesHelper.class);
					Structure particleSystem = pParticleSystem.fetchData(dataRepository.getInputStream()).get(0);
					loadedModifier = particlesHelper.toParticleEmitter(particleSystem, dataRepository);
				}
			}
			// adding modifier to the modifier's lists
			if (loadedModifier != null) {
				dataRepository.addModifier(objectStructure.getOldMemoryAddress(), modifier.getType(), loadedModifier, modifierAdditionalData);
				modifierAdditionalData = null;
			}
		}
		
		//at the end read object's animation modifier
		Modifier objectAnimationModifier =  this.readObjectAnimation(objectStructure, dataRepository);
		if(objectAnimationModifier != null) {
			dataRepository.addModifier(objectStructure.getOldMemoryAddress(), 
									   objectAnimationModifier.getType(), 
									   objectAnimationModifier.getJmeModifierRepresentation(), 
									   objectAnimationModifier.getAdditionalData());
		}
	}
	
	/**
	 * This method reads animation of the object itself (without bones) and stores it as an ArmatureModifierData
	 * modifier. The animation is returned as a modifier. It should be later applied regardless other modifiers. The
	 * reason for this is that object may not have modifiers added but it's animation should be working.
	 * @param objectStructure
	 *        the structure of the object
	 * @param dataRepository
	 *        the data repository
	 * @return animation modifier is returned, it should be separately applied when the object is loaded
	 * @throws BlenderFileException
	 *         this exception is thrown when the blender file is somehow corrupted
	 */
	protected Modifier readObjectAnimation(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
		Pointer pIpo = (Pointer)objectStructure.getFieldValue("ipo");
		if(pIpo.isNotNull()) {
			//check if there is an action name connected with this ipo
			String objectAnimationName = null;
			List<FileBlockHeader> actionBlocks = dataRepository.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
			for(FileBlockHeader actionBlock : actionBlocks) {
				Structure action = actionBlock.getStructure(dataRepository);
				List<Structure> actionChannels = ((Structure)action.getFieldValue("chanbase")).evaluateListBase(dataRepository);
				if(actionChannels.size() == 1) {//object's animtion action has only one channel
					Pointer pChannelIpo = (Pointer)actionChannels.get(0).getFieldValue("ipo");
					if(pChannelIpo.equals(pIpo)) {
						objectAnimationName = action.getName();
						break;
					}
				}
			}
			
			String objectName = objectStructure.getName();
			if(objectAnimationName == null) {//set the object's animation name to object's name
				objectAnimationName = objectName;
			}

			IpoHelper ipoHelper = dataRepository.getHelper(IpoHelper.class);
			Structure ipoStructure = pIpo.fetchData(dataRepository.getInputStream()).get(0);
			Ipo ipo = ipoHelper.createIpo(ipoStructure, dataRepository);
			int[] animationFrames = dataRepository.getBlenderKey().getAnimationFrames(objectName, objectAnimationName);
			if(animationFrames == null) {//if the name was created here there are no frames set for the animation
				animationFrames = new int[] {1, ipo.getLastFrame()};
			}
			int fps = dataRepository.getBlenderKey().getFps();
			float start = (float)animationFrames[0] / (float)fps;
			float stop = (float)animationFrames[1] / (float)fps;

			//calculating track for the only bone in this skeleton
			BoneTrack[] tracks = new BoneTrack[1];
			tracks[0] = ipo.calculateTrack(0, animationFrames[0], animationFrames[1], fps);

			BoneAnimation boneAnimation = new BoneAnimation(objectAnimationName, stop - start);
			boneAnimation.setTracks(tracks);
			ArrayList<BoneAnimation> animations = new ArrayList<BoneAnimation>(1);
			animations.add(boneAnimation);

			//preparing the object's bone
			ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
			Transform t = objectHelper.getTransformation(objectStructure);
			Bone bone = new Bone(null);
			bone.setBindTransforms(t.getTranslation(), t.getRotation(), t.getScale());

			return new Modifier(Modifier.OBJECT_ANIMATION_MODIFIER_DATA, new AnimData(new Skeleton(new Bone[] {bone}), animations), objectStructure.getOldMemoryAddress());
		}
		return null;
	}

	/**
	 * This method applies particles emitter to the given node.
	 * @param node
	 *        the particles emitter node
	 * @param modifier
	 *        the modifier containing the emitter data
	 * @param dataRepository
	 *        the data repository
	 * @return node with particles' emitter applied
	 */
	protected Node applyParticleSystemModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
		MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
		ParticleEmitter emitter = (ParticleEmitter) modifier.getJmeModifierRepresentation();
		emitter = emitter.clone();

		// veryfying the alpha function for particles' texture
		Integer alphaFunction = MaterialHelper.ALPHA_MASK_HYPERBOLE;
		char nameSuffix = emitter.getName().charAt(emitter.getName().length() - 1);
		if (nameSuffix == 'B' || nameSuffix == 'N') {
			alphaFunction = MaterialHelper.ALPHA_MASK_NONE;
		}
		// removing the type suffix from the name
		emitter.setName(emitter.getName().substring(0, emitter.getName().length() - 1));

		// applying emitter shape
		EmitterShape emitterShape = emitter.getShape();
		List<Mesh> meshes = new ArrayList<Mesh>();
		for (Spatial spatial : node.getChildren()) {
			if (spatial instanceof Geometry) {
				Mesh mesh = ((Geometry) spatial).getMesh();
				if (mesh != null) {
					meshes.add(mesh);
					Material material = materialHelper.getParticlesMaterial(((Geometry) spatial).getMaterial(), alphaFunction, dataRepository);
					emitter.setMaterial(material);// TODO: divide into several pieces
				}
			}
		}
		if (meshes.size() > 0 && emitterShape instanceof EmitterMeshVertexShape) {
			((EmitterMeshVertexShape) emitterShape).setMeshes(meshes);
		}

		node.attachChild(emitter);
		return node;
	}

	/**
	 * This method applies ArmatureModifierData to the loaded object.
	 * @param node
	 *        the loaded object
	 * @param modifier
	 *        the modifier to apply
	 * @param dataRepository
	 *        the data repository
	 * @return the node to whom the modifier was applied
	 */
	protected Node applyArmatureModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
		AnimData ad = (AnimData) modifier.getJmeModifierRepresentation();
		ArrayList<BoneAnimation> animList = ad.anims;
		Long modifierArmatureObject = (Long) modifier.getAdditionalData();
		if (animList != null && animList.size() > 0) {
			ConstraintHelper constraintHelper = dataRepository.getHelper(ConstraintHelper.class);
			Constraint[] constraints = constraintHelper.getConstraints(modifierArmatureObject);
			HashMap<String, BoneAnimation> anims = new HashMap<String, BoneAnimation>();
			for (int i = 0; i < animList.size(); ++i) {
				BoneAnimation boneAnimation = animList.get(i).clone();

				// baking constraints into animations
				if (constraints != null && constraints.length > 0) {
					for (Constraint constraint : constraints) {
						constraint.affectAnimation(ad.skeleton, boneAnimation);
					}
				}

				anims.put(boneAnimation.getName(), boneAnimation);
			}

			// getting meshes
			Mesh[] meshes = null;
			List<Mesh> meshesList = new ArrayList<Mesh>();
			List<Spatial> children = node.getChildren();
			for (Spatial child : children) {
				if (child instanceof Geometry) {
					meshesList.add(((Geometry) child).getMesh());
				}
			}
			if (meshesList.size() > 0) {
				meshes = meshesList.toArray(new Mesh[meshesList.size()]);
			}

			// applying the control to the node
			SkeletonControl skeletonControl = new SkeletonControl(meshes, ad.skeleton);
			AnimControl control = node.getControl(AnimControl.class);

			if (control == null) {
				control = new AnimControl(ad.skeleton);
			} else {
				// merging skeletons
				Skeleton controlSkeleton = control.getSkeleton();
				int boneIndexIncrease = controlSkeleton.getBoneCount();
				Skeleton skeleton = this.merge(controlSkeleton, ad.skeleton);

				// merging animations
				HashMap<String, BoneAnimation> animations = new HashMap<String, BoneAnimation>();
				for (String animationName : control.getAnimationNames()) {
					animations.put(animationName, control.getAnim(animationName));
				}
				for (Entry<String, BoneAnimation> animEntry : anims.entrySet()) {
					BoneAnimation ba = animEntry.getValue();
					for (int i = 0; i < ba.getTracks().length; ++i) {
						BoneTrack bt = ba.getTracks()[i];
						int newBoneIndex = bt.getTargetBoneIndex() + boneIndexIncrease;
						ba.getTracks()[i] = new BoneTrack(newBoneIndex, bt.getTimes(), bt.getTranslations(), bt.getRotations(), bt.getScales());
					}
					animations.put(animEntry.getKey(), animEntry.getValue());
				}

				// replacing the control
				node.removeControl(control);
				control = new AnimControl(skeleton);
			}
			control.setAnimations(anims);
			node.addControl(control);
			node.addControl(skeletonControl);
		}
		return node;
	}

	protected Node applyObjectAnimationModifier(Node node, Modifier modifier, DataRepository dataRepository) {
		AnimData ad = (AnimData) modifier.getJmeModifierRepresentation();
		ad.skeleton.getBone(0).setAttachNode(node);
		return this.applyArmatureModifierData(node, modifier, dataRepository);
	}
	
	/**
	 * This method applies the array modifier to the node.
	 * @param node
	 *        the object the modifier will be applied to
	 * @param modifier
	 *        the modifier to be applied
	 * @param dataRepository
	 *        the data repository
	 * @return object node with array modifier applied
	 */
	@SuppressWarnings("unchecked")
	protected Node applyArrayModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
		Map<String, Object> modifierData = (Map<String, Object>) modifier.getJmeModifierRepresentation();
		int fittype = ((Number) modifierData.get("fittype")).intValue();
		float[] offset = (float[]) modifierData.get("offset");
		if (offset == null) {// the node will be repeated several times in the same place
			offset = new float[] { 0.0f, 0.0f, 0.0f };
		}
		float[] scale = (float[]) modifierData.get("scale");
		if (scale == null) {// the node will be repeated several times in the same place
			scale = new float[] { 0.0f, 0.0f, 0.0f };
		} else {
			// getting bounding box
			node.updateModelBound();
			BoundingVolume boundingVolume = node.getWorldBound();
			if (boundingVolume instanceof BoundingBox) {
				scale[0] *= ((BoundingBox) boundingVolume).getXExtent() * 2.0f;
				scale[1] *= ((BoundingBox) boundingVolume).getYExtent() * 2.0f;
				scale[2] *= ((BoundingBox) boundingVolume).getZExtent() * 2.0f;
			} else if (boundingVolume instanceof BoundingSphere) {
				float radius = ((BoundingSphere) boundingVolume).getRadius();
				scale[0] *= radius * 2.0f;
				scale[1] *= radius * 2.0f;
				scale[2] *= radius * 2.0f;
			} else {
				throw new IllegalStateException("Unknown bounding volume type: " + boundingVolume.getClass().getName());
			}
		}

		// adding object's offset
		float[] objectOffset = new float[] { 0.0f, 0.0f, 0.0f };
		Pointer pOffsetObject = (Pointer) modifierData.get("offsetob");
		if (pOffsetObject != null) {
			FileBlockHeader offsetObjectBlock = dataRepository.getFileBlock(pOffsetObject.getOldMemoryAddress());
			ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
			try {// we take the structure in case the object was not yet loaded
				Structure offsetStructure = offsetObjectBlock.getStructure(dataRepository);
				Vector3f translation = objectHelper.getTransformation(offsetStructure).getTranslation();
				objectOffset[0] = translation.x;
				objectOffset[1] = translation.y;
				objectOffset[2] = translation.z;
			} catch (BlenderFileException e) {
				LOGGER.warning("Problems in blender file structure! Object offset cannot be applied! The problem: " + e.getMessage());
			}
		}

		// getting start and end caps
		Node[] caps = new Node[] { null, null };
		Pointer[] pCaps = new Pointer[] { (Pointer) modifierData.get("startcap"), (Pointer) modifierData.get("endcap") };
		for (int i = 0; i < pCaps.length; ++i) {
			if (pCaps[i] != null) {
				caps[i] = (Node) dataRepository.getLoadedFeature(pCaps[i].getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
				if (caps[i] != null) {
					caps[i] = (Node) caps[i].clone();
				} else {
					FileBlockHeader capBlock = dataRepository.getFileBlock(pOffsetObject.getOldMemoryAddress());
					try {// we take the structure in case the object was not yet loaded
						Structure capStructure = capBlock.getStructure(dataRepository);
						ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
						caps[i] = (Node) objectHelper.toObject(capStructure, dataRepository);
						if (caps[i] == null) {
							LOGGER.warning("Cap object '" + capStructure.getName() + "' couldn't be loaded!");
						}
					} catch (BlenderFileException e) {
						LOGGER.warning("Problems in blender file structure! Cap object cannot be applied! The problem: " + e.getMessage());
					}
				}
			}
		}

		Vector3f translationVector = new Vector3f(offset[0] + scale[0] + objectOffset[0], offset[1] + scale[1] + objectOffset[1], offset[2] + scale[2] + objectOffset[2]);

		// getting/calculating repeats amount
		int count = 0;
		if (fittype == 0) {// Fixed count
			count = ((Number) modifierData.get("count")).intValue() - 1;
		} else if (fittype == 1) {// Fixed length
			float length = ((Number) modifierData.get("length")).floatValue();
			if (translationVector.length() > 0.0f) {
				count = (int) (length / translationVector.length()) - 1;
			}
		} else if (fittype == 2) {// Fit curve
			throw new IllegalStateException("Fit curve should be transformed to Fixed Length array type!");
		} else {
			throw new IllegalStateException("Unknown fit type: " + fittype);
		}

		// adding translated nodes and caps
		if (count > 0) {
			Node[] arrayNodes = new Node[count];
			Vector3f newTranslation = new Vector3f();
			for (int i = 0; i < count; ++i) {
				newTranslation.addLocal(translationVector);
				Node nodeClone = (Node) node.clone();
				nodeClone.setLocalTranslation(newTranslation);
				arrayNodes[i] = nodeClone;
			}
			for (Node nodeClone : arrayNodes) {
				node.attachChild(nodeClone);
			}
			if (caps[0] != null) {
				caps[0].getLocalTranslation().set(node.getLocalTranslation()).subtractLocal(translationVector);
				node.attachChild(caps[0]);
			}
			if (caps[1] != null) {
				caps[1].getLocalTranslation().set(newTranslation).addLocal(translationVector);
				node.attachChild(caps[1]);
			}
		}
		return node;
	}

	/**
	 * This method applies the mirror modifier to the node.
	 * @param node
	 *        the object the modifier will be applied to
	 * @param modifier
	 *        the modifier to be applied
	 * @param dataRepository
	 *        the data repository
	 * @return object node with mirror modifier applied
	 */
	@SuppressWarnings("unchecked")
	protected Node applyMirrorModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
		Map<String, Object> modifierData = (Map<String, Object>) modifier.getJmeModifierRepresentation();
		int flag = ((Number) modifierData.get("flag")).intValue();
		float[] mirrorFactor = new float[] { 
				(flag & 0x08) != 0 ? -1.0f : 1.0f,
				(flag & 0x10) != 0 ? -1.0f : 1.0f,
				(flag & 0x20) != 0 ? -1.0f : 1.0f
		};
		float[] center = new float[] { 0.0f, 0.0f, 0.0f };
		Pointer pObject = (Pointer) modifierData.get("mirrorob");
		if (pObject != null) {
			Structure objectStructure;
			try {
				objectStructure = pObject.fetchData(dataRepository.getInputStream()).get(0);
				ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
				Node object = (Node) objectHelper.toObject(objectStructure, dataRepository);
				if (object != null) {
					Vector3f translation = object.getWorldTranslation();
					center[0] = translation.x;
					center[1] = translation.y;
					center[2] = translation.z;
				}
			} catch (BlenderFileException e) {
				LOGGER.severe("Cannot load mirror's reference object. Cause: " + e.getLocalizedMessage());
			}
		}
		float tolerance = ((Number) modifierData.get("tolerance")).floatValue();
		boolean mirrorU = (flag & 0x01) != 0;
		boolean mirrorV = (flag & 0x02) != 0;
//		boolean mirrorVGroup = (flag & 0x20) != 0;
		
		List<Geometry> geometriesToAdd = new ArrayList<Geometry>();
		for (int mirrorIndex = 0; mirrorIndex < 3; ++mirrorIndex) {
			if (mirrorFactor[mirrorIndex] == -1.0f) {
				for (Spatial spatial : node.getChildren()) {
					if (spatial instanceof Geometry) {
						Mesh mesh = ((Geometry) spatial).getMesh();
						Mesh clone = mesh.deepClone();

						// getting buffers
						FloatBuffer position = mesh.getFloatBuffer(Type.Position);
						FloatBuffer bindPosePosition = mesh.getFloatBuffer(Type.BindPosePosition);

						FloatBuffer clonePosition = clone.getFloatBuffer(Type.Position);
						FloatBuffer cloneBindPosePosition = clone.getFloatBuffer(Type.BindPosePosition);
						FloatBuffer cloneNormals = clone.getFloatBuffer(Type.Normal);
						FloatBuffer cloneBindPoseNormals = clone.getFloatBuffer(Type.BindPoseNormal);
						ShortBuffer cloneIndexes = (ShortBuffer) clone.getBuffer(Type.Index).getData();
						
						// modyfying data
						for (int i = mirrorIndex; i < clonePosition.limit(); i += 3) {
							float value = clonePosition.get(i);
							float d = center[mirrorIndex] - value;

							if (Math.abs(d) <= tolerance) {
								clonePosition.put(i, center[mirrorIndex]);
								cloneBindPosePosition.put(i, center[mirrorIndex]);
								position.put(i, center[mirrorIndex]);
								bindPosePosition.put(i, center[mirrorIndex]);
							} else {
								clonePosition.put(i, value + 2.0f * d);
								cloneBindPosePosition.put(i, value + 2.0f * d);
							}
							cloneNormals.put(i, -cloneNormals.get(i));
							cloneBindPoseNormals.put(i, -cloneNormals.get(i));
							
							//modifying clone indexes
							int vertexIndex = (i - mirrorIndex) / 3;
							if(vertexIndex % 3 == 0) {
								short index = cloneIndexes.get(vertexIndex + 2);
								cloneIndexes.put(vertexIndex + 2, cloneIndexes.get(vertexIndex + 1));
								cloneIndexes.put(vertexIndex + 1, index);
							}
						}
						
						if(mirrorU) {
							FloatBuffer cloneUVs = (FloatBuffer) clone.getBuffer(Type.TexCoord).getData();
							for(int i=0;i<cloneUVs.limit();i+=2) {
								cloneUVs.put(i, 1.0f - cloneUVs.get(i));
							}
						}
						if(mirrorV) {
							FloatBuffer cloneUVs = (FloatBuffer) clone.getBuffer(Type.TexCoord).getData();
							for(int i=1;i<cloneUVs.limit();i+=2) {
								cloneUVs.put(i, 1.0f - cloneUVs.get(i));
							}
						}
						
						Geometry geometry = new Geometry(null, clone);
						geometry.setMaterial(((Geometry) spatial).getMaterial());
						geometriesToAdd.add(geometry);
					}
				}

				// adding meshes to node
				for (Geometry geometry : geometriesToAdd) {
					node.attachChild(geometry);
				}
				geometriesToAdd.clear();
			}
		}
		return node;
	}

	/**
	 * This method merges two skeletons into one. I assume that each skeleton's 0-indexed bone is objectAnimationBone so
	 * only one such bone should be placed in the result
	 * @param s1
	 *        first skeleton
	 * @param s2
	 *        second skeleton
	 * @return merged skeleton
	 */
	protected Skeleton merge(Skeleton s1, Skeleton s2) {
		List<Bone> bones = new ArrayList<Bone>(s1.getBoneCount() + s2.getBoneCount());
		for (int i = 0; i < s1.getBoneCount(); ++i) {
			bones.add(s1.getBone(i));
		}
		for (int i = 1; i < s2.getBoneCount(); ++i) {// ommit objectAnimationBone
			bones.add(s2.getBone(i));
		}
		return new Skeleton(bones.toArray(new Bone[bones.size()]));
	}
}
