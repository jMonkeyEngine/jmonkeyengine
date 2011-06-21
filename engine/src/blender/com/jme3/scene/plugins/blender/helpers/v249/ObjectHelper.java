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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.plugins.blender.data.FileBlockHeader;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.structures.Ipo;
import com.jme3.scene.plugins.blender.structures.Modifier;
import com.jme3.scene.plugins.blender.structures.Properties;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * A class that is used in object calculations.
 * @author Marcin Roguski
 */
public class ObjectHelper extends AbstractBlenderHelper {
	private static final Logger			LOGGER		= Logger.getLogger(ObjectHelper.class.getName());

	protected static final int		OBJECT_TYPE_EMPTY			= 0;
	protected static final int		OBJECT_TYPE_MESH			= 1;
	protected static final int		OBJECT_TYPE_CURVE			= 2;
	protected static final int		OBJECT_TYPE_SURF			= 3;
	protected static final int		OBJECT_TYPE_TEXT			= 4;
	protected static final int		OBJECT_TYPE_METABALL		= 5;
	protected static final int		OBJECT_TYPE_LAMP			= 10;
	protected static final int		OBJECT_TYPE_CAMERA			= 11;
	protected static final int		OBJECT_TYPE_WAVE			= 21;
	protected static final int		OBJECT_TYPE_LATTICE			= 22;
	protected static final int		OBJECT_TYPE_ARMATURE		= 25;
	
	/** This variable indicates if the Y asxis is the UP axis or not. */
	protected boolean						fixUpAxis;
	/** Quaternion used to rotate data when Y is up axis. */
	protected Quaternion					upAxisRotationQuaternion;

	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in
	 * different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public ObjectHelper(String blenderVersion) {
		super(blenderVersion);
	}

	/**
	 * This method sets the Y is UP axis. By default the UP axis is Z (just like in blender).
	 * @param fixUpAxis
	 *        a variable that indicates if the Y asxis is the UP axis or not
	 */
	public void setyIsUpAxis(boolean fixUpAxis) {
		this.fixUpAxis = fixUpAxis;
		if(fixUpAxis) {
			upAxisRotationQuaternion = new Quaternion().fromAngles(-FastMath.HALF_PI, 0, 0);
		}
	}

	/**
	 * This method reads the given structure and createn an object that represents the data.
	 * @param objectStructure
	 *            the object's structure
	 * @param dataRepository
	 *            the data repository
	 * @return blener's object representation
	 * @throws BlenderFileException
	 *             an exception is thrown when the given data is inapropriate
	 */
	public Object toObject(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
		Object loadedResult = dataRepository.getLoadedFeature(objectStructure.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
		if(loadedResult != null) {
			return loadedResult;
		}

		dataRepository.pushParent(objectStructure);

		ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
		ModifierHelper modifierHelper = dataRepository.getHelper(ModifierHelper.class);
		ArmatureHelper armatureHelper = dataRepository.getHelper(ArmatureHelper.class);
		ConstraintHelper constraintHelper = dataRepository.getHelper(ConstraintHelper.class);

		//get object data
		int type = ((Number)objectStructure.getFieldValue("type")).intValue();
		String name = objectStructure.getName();
		LOGGER.log(Level.INFO, "Loading obejct: {0}", name);

		//reading modifiers
		modifierHelper.readModifiers(objectStructure, dataRepository);
		Modifier objectAnimationModifier = objectHelper.readObjectAnimation(objectStructure, dataRepository);

		//loading constraints connected with this object
		constraintHelper.loadConstraints(objectStructure, dataRepository);

		int restrictflag = ((Number)objectStructure.getFieldValue("restrictflag")).intValue();
		boolean visible = (restrictflag & 0x01) != 0;
		Object result = null;

		Pointer pParent = (Pointer)objectStructure.getFieldValue("parent");
		Object parent = dataRepository.getLoadedFeature(pParent.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
		if(parent == null && pParent.isNotNull()) {
			Structure parentStructure = pParent.fetchData(dataRepository.getInputStream()).get(0);//TODO: moze byc wiecej rodzicow
			parent = this.toObject(parentStructure, dataRepository);
		}

		Transform t = objectHelper.getTransformation(objectStructure);
		
		try {
			switch(type) {
				case OBJECT_TYPE_EMPTY:
					LOGGER.log(Level.INFO, "Importing empty.");
					Node empty = new Node(name);
					empty.setLocalTransform(t);
					result = empty;
					break;
				case OBJECT_TYPE_MESH:
					LOGGER.log(Level.INFO, "Importing mesh.");
					Node node = new Node(name);
					node.setCullHint(visible ? CullHint.Always : CullHint.Inherit);

					//reading mesh
					MeshHelper meshHelper = dataRepository.getHelper(MeshHelper.class);
					Pointer pMesh = (Pointer)objectStructure.getFieldValue("data");
					List<Structure> meshesArray = pMesh.fetchData(dataRepository.getInputStream());
					List<Geometry> geometries = meshHelper.toMesh(meshesArray.get(0), dataRepository);
					for(Geometry geometry : geometries) {
						node.attachChild(geometry);
					}
					node.setLocalTransform(t);

					//applying all modifiers
					List<Modifier> modifiers = dataRepository.getModifiers(objectStructure.getOldMemoryAddress(), null);
					for(Modifier modifier : modifiers) {
						modifierHelper.applyModifier(node, modifier, dataRepository);
					}
					//adding object animation modifier
					if(objectAnimationModifier != null) {
						node = modifierHelper.applyModifier(node, objectAnimationModifier, dataRepository);
					}

					//setting the parent
					if(parent instanceof Node) {
						((Node)parent).attachChild(node);
					}
					node.updateModelBound();//I prefer do calculate bounding box here than read it from the file
					result = node;
					break;
				case OBJECT_TYPE_SURF:
				case OBJECT_TYPE_CURVE:
					LOGGER.log(Level.INFO, "Importing curve/nurb.");
					Pointer pCurve = (Pointer)objectStructure.getFieldValue("data");
					if(pCurve.isNotNull()) {
						CurvesHelper curvesHelper = dataRepository.getHelper(CurvesHelper.class);
						Structure curveData = pCurve.fetchData(dataRepository.getInputStream()).get(0);
						List<Geometry> curves = curvesHelper.toCurve(curveData, dataRepository);
						result = new Node(name);
						for(Geometry curve : curves) {
							((Node)result).attachChild(curve);
						}
						((Node)result).setLocalTransform(t);
					}
					break;
				case OBJECT_TYPE_LAMP:
					LOGGER.log(Level.INFO, "Importing lamp.");
					Pointer pLamp = (Pointer)objectStructure.getFieldValue("data");
					if(pLamp.isNotNull()) {
						LightHelper lightHelper = dataRepository.getHelper(LightHelper.class);
						List<Structure> lampsArray = pLamp.fetchData(dataRepository.getInputStream());
						Light light = lightHelper.toLight(lampsArray.get(0), dataRepository);
						if(light!=null) {
							light.setName(name);
						}
						if(light instanceof PointLight) {
							((PointLight)light).setPosition(t.getTranslation());
						} else if(light instanceof DirectionalLight) {
							Quaternion quaternion = t.getRotation();
							Vector3f[] axes = new Vector3f[3];
							quaternion.toAxes(axes);
							((DirectionalLight)light).setDirection(axes[2].negate());//-Z is the direction axis of area lamp in blender
						} else {
							LOGGER.log(Level.WARNING, "Unknown type of light: {0}", light);
						}
						result = light;
					}
					break;
				case OBJECT_TYPE_CAMERA:
					Pointer pCamera = (Pointer)objectStructure.getFieldValue("data");
					if(pCamera.isNotNull()) {
						CameraHelper cameraHelper = dataRepository.getHelper(CameraHelper.class);
						List<Structure> camerasArray = pCamera.fetchData(dataRepository.getInputStream());
						Camera camera = cameraHelper.toCamera(camerasArray.get(0));
						camera.setLocation(t.getTranslation());
						camera.setRotation(t.getRotation());
						result = camera;
					}
					break;
				case OBJECT_TYPE_ARMATURE:
					LOGGER.log(Level.INFO, "Importing armature.");
					Pointer pArmature = (Pointer)objectStructure.getFieldValue("data");
					List<Structure> armaturesArray = pArmature.fetchData(dataRepository.getInputStream());//TODO: moze byc wiecej???
					result = armatureHelper.toArmature(armaturesArray.get(0), dataRepository);
					break;
				default:
					LOGGER.log(Level.WARNING, "Unknown object type: {0}", type);
			}
		} finally {
			dataRepository.popParent();
		}
		
		//reading custom properties
		Properties properties = this.loadProperties(objectStructure, dataRepository);
		if(properties != null && properties.getValue() != null) {
			((Node)result).setUserData("properties", properties);
		}
		
		if(result != null) {
			dataRepository.addLoadedFeatures(objectStructure.getOldMemoryAddress(), name, objectStructure, result);
		}
		return result;
	}
	
	/**
	 * This method calculates local transformation for the object. Parentage is taken under consideration.
	 * @param objectStructure
	 *        the object's structure
	 * @return objects transformation relative to its parent
	 */
	@SuppressWarnings("unchecked")
	public Transform getTransformation(Structure objectStructure) {
		DynamicArray<Number> loc = (DynamicArray<Number>)objectStructure.getFieldValue("loc");
		DynamicArray<Number> size = (DynamicArray<Number>)objectStructure.getFieldValue("size");
		DynamicArray<Number> rot = (DynamicArray<Number>)objectStructure.getFieldValue("rot");

		Pointer parent = (Pointer) objectStructure.getFieldValue("parent");
		Matrix4f parentInv = parent.isNull() ? Matrix4f.IDENTITY : this.getMatrix(objectStructure, "parentinv");
		
		Matrix4f globalMatrix = new Matrix4f();
		globalMatrix.setTranslation(loc.get(0).floatValue(), loc.get(1).floatValue(), loc.get(2).floatValue());
		globalMatrix.setRotationQuaternion(new Quaternion().fromAngles(rot.get(0).floatValue(), rot.get(1).floatValue(), rot.get(2).floatValue()));
		Matrix4f localMatrix = parentInv.mult(globalMatrix);

		Vector3f translation = localMatrix.toTranslationVector();
		Quaternion rotation = localMatrix.toRotationQuat();
		//getting the scale
		float scaleX = (float) Math.sqrt(parentInv.m00 * parentInv.m00 + parentInv.m10 * parentInv.m10 + parentInv.m20 * parentInv.m20);
		float scaleY = (float) Math.sqrt(parentInv.m01 * parentInv.m01 + parentInv.m11 * parentInv.m11 + parentInv.m21 * parentInv.m21);
		float scaleZ = (float) Math.sqrt(parentInv.m02 * parentInv.m02 + parentInv.m12 * parentInv.m12 + parentInv.m22 * parentInv.m22);
		Vector3f scale = new Vector3f(size.get(0).floatValue() * scaleX, 
									  size.get(1).floatValue() * scaleY, 
									  size.get(2).floatValue() * scaleZ);
		if(fixUpAxis) {
			float y = translation.y;
			translation.y = translation.z;
			translation.z = y;
			rotation.multLocal(this.upAxisRotationQuaternion);
		}
		Transform t = new Transform(translation, rotation);
		t.setScale(scale);
		return t;
	}

	/**
	 * This method returns the transformation matrix of the given object structure.
	 * @param objectStructure
	 *        the structure with object's data
	 * @return object's transformation matrix
	 */
	public Matrix4f getTransformationMatrix(Structure objectStructure) {
		return this.getMatrix(objectStructure, "obmat");
	}

	/**
	 * This method returns the matrix of a given name for the given object structure.
	 * @param objectStructure
	 *        the structure with object's data
	 * @param matrixName
	 * 		  the name of the matrix structure
	 * @return object's matrix
	 */
	@SuppressWarnings("unchecked")
	protected Matrix4f getMatrix(Structure objectStructure, String matrixName) {
		Matrix4f result = new Matrix4f();
		DynamicArray<Number> obmat = (DynamicArray<Number>)objectStructure.getFieldValue(matrixName);
		for(int i = 0; i < 4; ++i) {
			for(int j = 0; j < 4; ++j) {
				result.set(i, j, obmat.get(j, i).floatValue());
			}
		}
		return result;
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
	public Modifier readObjectAnimation(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
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
			Transform t = this.getTransformation(objectStructure);
			Bone bone = new Bone(null);
			bone.setBindTransforms(t.getTranslation(), t.getRotation(), t.getScale());

			return new Modifier(Modifier.ARMATURE_MODIFIER_DATA, new AnimData(new Skeleton(new Bone[] {bone}), animations), null);
		}
		return null;
	}

	@Override
	public void clearState() {
		fixUpAxis = false;
	}
}
