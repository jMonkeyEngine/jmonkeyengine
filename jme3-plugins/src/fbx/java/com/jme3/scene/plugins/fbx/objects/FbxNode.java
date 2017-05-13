package com.jme3.scene.plugins.fbx.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.fbx.InheritType;
import com.jme3.scene.plugins.fbx.RotationOrder;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxNode extends FbxObject {
	
	public Map<String, Object> userData = new HashMap<String, Object>();
	public FaceCullMode cullMode = FaceCullMode.Back;
	public Transform localTransform;
	public Node node;
	public FbxNode parentFbxNode;
	
	public boolean rotationActive = false;
	public RotationOrder rotationOrder = RotationOrder.EULER_XYZ;
	public InheritType inheritType = InheritType.RrSs;
	
	
	// For bones and animation, in world space
	public Matrix4f bindTransform = null;
	public int boneIndex;
	public Map<Long,FbxAnimNode> animTranslations = new HashMap<Long,FbxAnimNode>();
	public Map<Long,FbxAnimNode> animRotations = new HashMap<Long,FbxAnimNode>();
	public Map<Long,FbxAnimNode> animScales = new HashMap<Long,FbxAnimNode>();
	public Bone bone;
	private FbxAnimNode lastAnimTranslation;
	private FbxAnimNode lastAnimRotation;
	private FbxAnimNode lastAnimScale;
	private FbxMesh mesh;
	public Map<Long,FbxCluster> skinToCluster = new HashMap<Long,FbxCluster>();
	public List<FbxNode> children = new ArrayList<FbxNode>();
	
	/**
	 * Cache to store materials if linking order is wrong
	 */
	private List<Material> wrongOrderMaterial = new ArrayList<Material>();
	
	public Vector3f translationLocalRaw = new Vector3f();
	public Vector3f rotationOffsetRaw = new Vector3f();
	public Vector3f rotationPivotRaw = new Vector3f();
	public Vector3f rotationPreRaw = new Vector3f();
	public Vector3f rotationLocalRaw = new Vector3f();
	public Vector3f rotationPostRaw = new Vector3f();
	public Vector3f scaleOffsetRaw = new Vector3f();
	public Vector3f scalePivotRaw = new Vector3f();
	public Vector3f scaleLocalRaw = new Vector3f(1, 1, 1);
	
	public Matrix4f transformMatrix;
	
	public FbxNode(SceneLoader scene, FbxElement element) {
		super(scene, element);
		for(FbxElement prop : element.getFbxProperties()) {
			double x, y, z;
			String propName = (String) prop.properties.get(0);
			switch(propName) {
			case "RotationOrder":
				rotationOrder = RotationOrder.values[(Integer) prop.properties.get(4)];
				break;
			case "Lcl Translation":
				readVectorFromProp(translationLocalRaw, prop);
				break;
			case "Lcl Rotation":
				readVectorFromProp(rotationLocalRaw, prop);
				break;
			case "Lcl Scaling":
				readVectorFromProp(scaleLocalRaw, prop);
				break;
			case "PreRotation":
				readVectorFromProp(rotationPreRaw, prop);
				break;
			case "RotationActive":
				rotationActive = ((Number) prop.properties.get(4)).intValue() == 1;
				break;
			case "RotationPivot":
				readVectorFromProp(rotationPivotRaw, prop);
				break;
			case "PostRotation":
				readVectorFromProp(rotationPostRaw, prop);
				break;
			case "ScaleOffset":
				readVectorFromProp(scaleOffsetRaw, prop);
				break;
			case "ScalePivot":
				readVectorFromProp(scalePivotRaw, prop);
				break;
			case "InheritType":
				inheritType = InheritType.values[(Integer) prop.properties.get(4)];
				break;
			case "U":
				String userDataKey = (String) prop.properties.get(0);
				String userDataType = (String) prop.properties.get(1);
				Object userDataValue;
				if(userDataType.equals("KString")) {
					userDataValue = (String) prop.properties.get(4);
				} else if(userDataType.equals("int")) {
					userDataValue = (Integer) prop.properties.get(4);
				} else if(userDataType.equals("double")) {
					// NOTE: jME3 does not support doubles in UserData.
					//       Need to convert to float.
					userDataValue = ((Double) prop.properties.get(4)).floatValue();
				} else if(userDataType.equals("Vector")) {
					x = (Double) prop.properties.get(4);
					y = (Double) prop.properties.get(5);
					z = (Double) prop.properties.get(6);
					userDataValue = new Vector3f((float) x, (float) y, (float) z);
				} else {
					scene.warning("Unsupported user data type: " + userDataType + ". Ignoring.");
					continue;
				}
				userData.put(userDataKey, userDataValue);
				break;
			}
		}
		
		FbxElement cullingElement = element.getChildById("Culling");
		if(cullingElement != null && cullingElement.properties.get(0).equals("CullingOff"))
			cullMode = FaceCullMode.Off; // TODO Add other variants
		
		/*From http://area.autodesk.com/forum/autodesk-fbx/fbx-sdk/the-makeup-of-the-local-matrix-of-an-kfbxnode/
		
		Local Matrix = LclTranslation * RotationOffset * RotationPivot *
		  PreRotation * LclRotation * PostRotation * RotationPivotInverse *
		  ScalingOffset * ScalingPivot * LclScaling * ScalingPivotInverse
		
		LocalTranslation : translate (xform -query -translation)
		RotationOffset: translation compensates for the change in the rotate pivot point (xform -q -rotateTranslation)
		RotationPivot: current rotate pivot position (xform -q -rotatePivot)
		PreRotation : joint orientation(pre rotation)
		LocalRotation: rotate transform (xform -q -rotation & xform -q -rotateOrder)
		PostRotation : rotate axis (xform -q -rotateAxis)
		RotationPivotInverse: inverse of RotationPivot
		ScalingOffset: translation compensates for the change in the scale pivot point (xform -q -scaleTranslation)
		ScalingPivot: current scale pivot position (xform -q -scalePivot)
		LocalScaling: scale transform (xform -q -scale)
		ScalingPivotInverse: inverse of ScalingPivot
		*/
		
		transformMatrix = computeTransformationMatrix(translationLocalRaw, rotationLocalRaw, scaleLocalRaw, rotationOrder);
		
		localTransform = new Transform(transformMatrix.toTranslationVector(), transformMatrix.toRotationQuat(), transformMatrix.toScaleVector());
			
		node = new Node(name);
		if(userData.size() > 0) {
			Iterator<Entry<String,Object>> iterator = userData.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<String,Object> e = iterator.next();
				node.setUserData(e.getKey(), e.getValue());
			}
		}
		node.setLocalTransform(localTransform);
	}
	
	public Matrix4f computeTransformationMatrix(Vector3f rawTranslation, Vector3f rawRotation, Vector3f rawScale, RotationOrder rotOrder) {
		Matrix4f transformMatrix = new Matrix4f();
		
		Matrix4f mat = new Matrix4f();
		mat.setTranslation(rawTranslation.x + rotationOffsetRaw.x + rotationPivotRaw.x, rawTranslation.y + rotationOffsetRaw.y + rotationPivotRaw.y, rawTranslation.z + rotationOffsetRaw.z + rotationPivotRaw.z);
		transformMatrix.multLocal(mat);
		
		if(rotationActive) {
			// Because of majic, FBX uses rotation order only to Lcl Rotations. Pre Rotations (Joint Orient) uses always XYZ order
			// What is Post Rotations is still a mystery
			Matrix4f preRotation = RotationOrder.EULER_XYZ.rotateToMatrix(rotationPreRaw.x, rotationPreRaw.y, rotationPreRaw.z);
			Matrix4f localRotation = rotOrder.rotateToMatrix(rawRotation.x, rawRotation.y, rawRotation.z);
			Matrix4f postRotation = RotationOrder.EULER_XYZ.rotateToMatrix(rotationPostRaw.x, rotationPostRaw.y, rotationPostRaw.z);
			transformMatrix.multLocal(preRotation);
			transformMatrix.multLocal(localRotation);
			transformMatrix.multLocal(postRotation);
		} else {
			transformMatrix.multLocal(RotationOrder.EULER_XYZ.rotate(rawRotation.x, rawRotation.y, rawRotation.z));
		}
		mat.setTranslation(scaleOffsetRaw.x + scalePivotRaw.x - rotationPivotRaw.x, scaleOffsetRaw.y + scalePivotRaw.y - rotationPivotRaw.y, scaleOffsetRaw.z + scalePivotRaw.z - rotationPivotRaw.z);
		transformMatrix.multLocal(mat);
		transformMatrix.scale(rawScale);
		mat.setTranslation(scalePivotRaw.negate());
		transformMatrix.multLocal(mat);
		
		return transformMatrix;
		
	}
	
	@Override
	public void linkToZero() {
		scene.sceneNode.attachChild(node);
		scene.rootNodes.add(this);
	}
	
	public void setSkeleton(Skeleton skeleton) {
		if(bone != null)
			boneIndex = skeleton.getBoneIndex(bone);
	}
	
	public void buildBindPoseBoneTransform() {
		if(bone != null) {
			Matrix4f t = bindTransform;
			if(t != null) {
				Matrix4f parentMatrix = parentFbxNode != null ? parentFbxNode.bindTransform : Matrix4f.IDENTITY;
				if(parentMatrix == null)
					parentMatrix = node.getLocalToWorldMatrix(null);
				t = parentMatrix.invert().multLocal(t);
				bone.setBindTransforms(t.toTranslationVector(), t.toRotationQuat(), t.toScaleVector());
			} else {
				bone.setBindTransforms(node.getLocalTranslation(), node.getLocalRotation(), node.getLocalScale());
			}
		}
	}
	
	@Override
	public void link(FbxObject child, String propertyName) {
		if(child instanceof FbxAnimNode) {
			FbxAnimNode anim = (FbxAnimNode) child;
			switch(propertyName) {
			case "Lcl Translation":
				animTranslations.put(anim.layerId, anim);
				lastAnimTranslation = anim;
				break;
			case "Lcl Rotation":
				animRotations.put(anim.layerId, anim);
				lastAnimRotation = anim;
				break;
			case "Lcl Scaling":
				animScales.put(anim.layerId, anim);
				lastAnimScale = anim;
				break;
			}
		}
	}
	
	public FbxAnimNode animTranslation(long layerId) {
		if(layerId == 0)
			return lastAnimTranslation;
		return animTranslations.get(layerId);
	}
	
	public FbxAnimNode animRotation(long layerId) {
		if(layerId == 0)
			return lastAnimRotation;
		return animRotations.get(layerId);
	}
	
	public FbxAnimNode animScale(long layerId) {
		if(layerId == 0)
			return lastAnimScale;
		return animScales.get(layerId);
	}
	
	@Override
	public void link(FbxObject otherObject) {
		if(otherObject instanceof FbxMaterial) {
			FbxMaterial m = (FbxMaterial) otherObject;
			Material mat = m.material;
			if(mesh == null) {
				wrongOrderMaterial.add(mat);
				return;
			}
			if(cullMode != FaceCullMode.Back)
				mat.getAdditionalRenderState().setFaceCullMode(cullMode);
			for(Geometry g : mesh.geometries) {
				if(g.getUserData("FBXMaterial") != null) {
					if((Integer) g.getUserData("FBXMaterial") == mesh.lastMaterialId)
						g.setMaterial(mat);
				} else {
					g.setMaterial(mat);
				}
			}
			mesh.lastMaterialId++;
		} else if(otherObject instanceof FbxNode) {
			FbxNode n = (FbxNode) otherObject;
			node.attachChild(n.node);
			children.add(n);
			if(n.inheritType == InheritType.Rrs) {
				Vector3f scale = node.getWorldScale();
				n.node.scale(1f / scale.x, 1f / scale.y, 1f / scale.z);
			}
			n.parentFbxNode = this;
			if(isLimb() && n.isLimb()) {
				if(bone == null)
					bone = new Bone(name);
				if(n.bone == null)
					n.bone = new Bone(n.name);
				bone.addChild(n.bone);
			}
		} else if(otherObject instanceof FbxMesh) {
			FbxMesh m = (FbxMesh) otherObject;
			m.setParent(node);
			m.parent = this;
			mesh = m;
			if(wrongOrderMaterial.size() > 0) {
				for(int i = 0; i < wrongOrderMaterial.size(); ++i) {
					Material mat = wrongOrderMaterial.remove(i--);
					for(Geometry g : mesh.geometries) {
						if(g.getUserData("FBXMaterial") != null) {
							if((Integer) g.getUserData("FBXMaterial") == mesh.lastMaterialId)
								g.setMaterial(mat);
						} else {
							g.setMaterial(mat);
						}
					}
					mesh.lastMaterialId++;
				}
			}
		}
	}
	
	public boolean isLimb() {
		return type.equals("LimbNode");
	}
}
