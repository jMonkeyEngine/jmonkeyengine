package com.jme3.scene.plugins.fbx.objects;

import java.util.HashMap;
import java.util.Map;

import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.fbx.RotationOrder;
import com.jme3.scene.plugins.fbx.SceneLoader;
import com.jme3.scene.plugins.fbx.file.FbxElement;

public class FbxNode extends FbxObject {

    public FaceCullMode cullMode = FaceCullMode.Back;
    public Transform localTransform;
    public Node node;
    public FbxNode parentFbxNode;

    public boolean rotationActive = false;
    public RotationOrder rotationOrder = RotationOrder.EULER_XYZ;


    // For bones and animation, in world space
    public Matrix4f bindTransform = null;
    public int boneIndex;
    public Map<Long, FbxAnimNode> animTranslations = new HashMap<>();
    public Map<Long, FbxAnimNode> animRotations = new HashMap<>();
    public Map<Long, FbxAnimNode> animScales = new HashMap<>();
    public Bone bone;
    private FbxAnimNode lastAnimTranslation;
    private FbxAnimNode lastAnimRotation;
    private FbxAnimNode lastAnimScale;
    private FbxMesh mesh;
    public Map<Long, FbxCluster> skinToCluster = new HashMap<>();

    public FbxNode(SceneLoader scene, FbxElement element) {
        super(scene, element);
        node = new Node(name);
        Vector3f translationLocalRaw = new Vector3f();
        Vector3f rotationOffsetRaw = new Vector3f();
        Vector3f rotationPivotRaw = new Vector3f();
        Vector3f rotationPreRaw = new Vector3f();
        Vector3f rotationLocalRaw = new Vector3f();
        Vector3f rotationPostRaw = new Vector3f();
        Vector3f scaleOffsetRaw = new Vector3f();
        Vector3f scalePivotRaw = new Vector3f();
        Vector3f scaleLocalRaw = new Vector3f(1, 1, 1);
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
            case "U":
                String userDataKey = (String) prop.properties.get(0);
                String userDataType = (String) prop.properties.get(1);
                Object userDataValue;
                if(userDataType.equals("KString")) {
                    userDataValue = prop.properties.get(4);
                } else if(userDataType.equals("int")) {
                    userDataValue = prop.properties.get(4);
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
                node.setUserData(userDataKey, userDataValue);
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

        RotationOrder rotOrder = rotationActive ? rotationOrder : RotationOrder.EULER_XYZ;

        Matrix4f transformMatrix = new Matrix4f();
        transformMatrix.setTranslation(translationLocalRaw.x + rotationOffsetRaw.x + rotationPivotRaw.x, translationLocalRaw.y + rotationOffsetRaw.y + rotationPivotRaw.y, translationLocalRaw.z + rotationOffsetRaw.z + rotationPivotRaw.z);

        if(rotationActive) {
            Quaternion postRotation = rotOrder.rotate(rotationPostRaw.x, rotationPostRaw.y, rotationPostRaw.z);
            Quaternion localRotation = rotOrder.rotate(rotationLocalRaw.x, rotationLocalRaw.y, rotationLocalRaw.z);
            Quaternion preRotation = rotOrder.rotate(rotationPreRaw.x, rotationPreRaw.y, rotationPreRaw.z);
            //preRotation.multLocal(localRotation).multLocal(postRotation);
            postRotation.multLocal(localRotation).multLocal(preRotation);
            transformMatrix.multLocal(postRotation);
        } else {
            transformMatrix.multLocal(rotOrder.rotate(rotationLocalRaw.x, rotationLocalRaw.y, rotationLocalRaw.z));
        }

        Matrix4f mat = new Matrix4f();
        mat.setTranslation(scaleOffsetRaw.x + scalePivotRaw.x - rotationPivotRaw.x, scaleOffsetRaw.y + scalePivotRaw.y - rotationPivotRaw.y, scaleOffsetRaw.z + scalePivotRaw.z - rotationPivotRaw.z);
        transformMatrix.multLocal(mat);

        transformMatrix.scale(scaleLocalRaw);
        transformMatrix.scale(new Vector3f(scene.unitSize, scene.unitSize, scene.unitSize));

        mat.setTranslation(scalePivotRaw.negate());
        transformMatrix.multLocal(mat);

        localTransform = new Transform(transformMatrix.toTranslationVector(), transformMatrix.toRotationQuat(), transformMatrix.toScaleVector());

        node.setLocalTransform(localTransform);
    }

    @Override
    public void linkToZero() {
        scene.sceneNode.attachChild(node);
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
        }
    }

    public boolean isLimb() {
        return type.equals("LimbNode");
    }
}
