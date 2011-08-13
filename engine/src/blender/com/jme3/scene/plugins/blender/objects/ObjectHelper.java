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
package com.jme3.scene.plugins.blender.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.BlenderKey.FeaturesToLoad;
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
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.animations.ArmatureHelper;
import com.jme3.scene.plugins.blender.cameras.CameraHelper;
import com.jme3.scene.plugins.blender.constraints.ConstraintHelper;
import com.jme3.scene.plugins.blender.curves.CurvesHelper;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.lights.LightHelper;
import com.jme3.scene.plugins.blender.meshes.MeshHelper;
import com.jme3.scene.plugins.blender.modifiers.Modifier;
import com.jme3.scene.plugins.blender.modifiers.ModifierHelper;

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
	
	/**
	 * This collection contains types of features that are not visible to the user.
	 * These will be loaded regardless the layer they reside in.
	 */
	protected static final Collection<Integer> invisibleTypes = new ArrayList<Integer>();
	static {
		invisibleTypes.add(OBJECT_TYPE_EMPTY);
		invisibleTypes.add(OBJECT_TYPE_ARMATURE);
	}
	
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
		ArmatureHelper armatureHelper = dataRepository.getHelper(ArmatureHelper.class);

		//get object data
		int type = ((Number)objectStructure.getFieldValue("type")).intValue();
		String name = objectStructure.getName();
		LOGGER.log(Level.INFO, "Loading obejct: {0}", name);

		//loading constraints connected with this object
		ConstraintHelper constraintHelper = dataRepository.getHelper(ConstraintHelper.class);
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

		Transform t = objectHelper.getTransformation(objectStructure, dataRepository);
		
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
					if (geometries != null){
                                            for(Geometry geometry : geometries) {
                                                    node.attachChild(geometry);
                                            }
                                        }
					node.setLocalTransform(t);

					//reading and applying all modifiers
					ModifierHelper modifierHelper = dataRepository.getHelper(ModifierHelper.class);
					Collection<Modifier> modifiers = modifierHelper.readModifiers(objectStructure, dataRepository);
					for(Modifier modifier : modifiers) {
						modifier.apply(node, dataRepository);
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
		if(result instanceof Spatial && properties != null && properties.getValue() != null) {
			((Spatial)result).setUserData("properties", properties);
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
	public Transform getTransformation(Structure objectStructure, DataRepository dataRepository) {
		//these are transformations in global space
		DynamicArray<Number> loc = (DynamicArray<Number>)objectStructure.getFieldValue("loc");
		DynamicArray<Number> size = (DynamicArray<Number>)objectStructure.getFieldValue("size");
		DynamicArray<Number> rot = (DynamicArray<Number>)objectStructure.getFieldValue("rot");

		//load parent inverse matrix
		Pointer pParent = (Pointer) objectStructure.getFieldValue("parent");
		Structure parent = null;
		if(pParent.isNotNull()) {
			try {
				parent = pParent.fetchData(dataRepository.getInputStream()).get(0);
			} catch (BlenderFileException e) {
				LOGGER.log(Level.WARNING, "Cannot fetch parent for object! Reason: {0}", e.getLocalizedMessage());
			}
		}
		Matrix4f parentInv = pParent.isNull() ? Matrix4f.IDENTITY : this.getMatrix(objectStructure, "parentinv");
		
		//create the global matrix (without the scale)
		Matrix4f globalMatrix = new Matrix4f();
		globalMatrix.setTranslation(loc.get(0).floatValue(), loc.get(1).floatValue(), loc.get(2).floatValue());
		globalMatrix.setRotationQuaternion(new Quaternion().fromAngles(rot.get(0).floatValue(), rot.get(1).floatValue(), rot.get(2).floatValue()));
		//compute local matrix
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
		
		//the root object is transformed if the Y axis is UP
		if(fixUpAxis && (pParent.isNull() || (parent!=null && !this.shouldBeLoaded(parent, dataRepository)))) {
			float y = translation.y;
			translation.y = translation.z;
			translation.z = -y;
			rotation = this.upAxisRotationQuaternion.mult(rotation);
		}
		
		//create the result
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

	@Override
	public void clearState() {
		fixUpAxis = false;
	}
	
	@Override
	public boolean shouldBeLoaded(Structure structure, DataRepository dataRepository) {
		int lay = ((Number) structure.getFieldValue("lay")).intValue();
        return ((lay & dataRepository.getBlenderKey().getLayersToLoad()) != 0
                && (dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.OBJECTS) != 0);
	}
}
