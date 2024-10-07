/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.node;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.debug.SkeletonDebugger;
import com.jme3.scene.plugins.fbx.anim.FbxAnimCurveNode;
import com.jme3.scene.plugins.fbx.anim.FbxCluster;
import com.jme3.scene.plugins.fbx.anim.FbxLimbNode;
import com.jme3.scene.plugins.fbx.anim.FbxSkinDeformer;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.material.FbxImage;
import com.jme3.scene.plugins.fbx.material.FbxMaterial;
import com.jme3.scene.plugins.fbx.material.FbxTexture;
import com.jme3.scene.plugins.fbx.mesh.FbxMesh;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import com.jme3.util.IntMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxNode extends FbxObject<Spatial> {

    private static final Logger logger = Logger.getLogger(FbxNode.class.getName());
    
    private static enum InheritMode {
        /**
         * Apply parent scale after child rotation.
         * This is the only mode correctly supported by jME3.
         */
        ScaleAfterChildRotation,
        
        /**
         * Apply parent scale before child rotation.
         * Not supported by jME3, will cause distortion with
         * non-uniform scale. No way around it.
         */
        ScaleBeforeChildRotation,
        
        /**
         * Do not apply parent scale at all.
         * Not supported by jME3, will cause distortion.
         * Could be worked around by via: 
         * <code>jmeChildScale = jmeParentScale / fbxChildScale</code>
         */
        NoParentScale
    }
    
    private InheritMode inheritMode = InheritMode.ScaleAfterChildRotation;

    protected FbxNode parent;
    protected List<FbxNode> children = new ArrayList<>();
    protected List<FbxMaterial> materials = new ArrayList<>();
    protected Map<String, Object> userData = new HashMap<>();
    protected Map<String, List<FbxAnimCurveNode>> propertyToAnimCurveMap = new HashMap<>();
    protected FbxNodeAttribute nodeAttribute;
    protected double visibility = 1.0;
    
    /**
     * For FBX nodes that contain a skeleton (i.e. FBX limbs).
     */
    protected Skeleton skeleton;
    
    protected final Transform jmeWorldNodeTransform = new Transform();
    protected final Transform jmeLocalNodeTransform = new Transform();

    // optional - used for limbs / bones / skeletons
    protected Transform jmeWorldBindPose;
    protected Transform jmeLocalBindPose;
    
    // used for debugging only
    protected Matrix4f cachedWorldBindPose;
    
    public FbxNode(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    public Transform computeFbxLocalTransform() {
        // TODO: implement the actual algorithm, which is this:
        // Render Local Translation =
        //      Inv Scale Pivot * Lcl Scale * Scale Pivot * Scale Offset * Inv Rota Pivot * Post Rotation * Rotation * Pre Rotation * Rotation Pivot * Rotation Offset * Translation
        
        // LclTranslation, 
        // LclRotation, 
        // PreRotation, 
        // PostRotation, 
        // RotationPivot, 
        // RotationOffset, 
        // LclScaling, 
        // ScalingPivot, 
        // ScalingOffset
        
        Matrix4f scaleMat = new Matrix4f();
        scaleMat.setScale(jmeLocalNodeTransform.getScale());
        
        Matrix4f rotationMat = new Matrix4f();
        rotationMat.setRotationQuaternion(jmeLocalNodeTransform.getRotation());
        
        Matrix4f translationMat = new Matrix4f();
        translationMat.setTranslation(jmeLocalNodeTransform.getTranslation());
        
        Matrix4f result = new Matrix4f();
        result.multLocal(scaleMat).multLocal(rotationMat).multLocal(translationMat);
        
        Transform t = new Transform();
        t.fromTransformMatrix(result);
        
        return t;
    }
    
    public void setWorldBindPose(Matrix4f worldBindPose) {
        if (cachedWorldBindPose != null) {
            if (!cachedWorldBindPose.isSimilar(worldBindPose, 1e-6f)) {
                throw new UnsupportedOperationException("Bind poses don't match");
            }
        }
        
        cachedWorldBindPose = worldBindPose;
        
        this.jmeWorldBindPose = new Transform();
        this.jmeWorldBindPose.setTranslation(worldBindPose.toTranslationVector());
        this.jmeWorldBindPose.setRotation(worldBindPose.toRotationQuat());
        this.jmeWorldBindPose.setScale(worldBindPose.toScaleVector());
        
//        System.out.println("\tBind Pose for " + getName());
//        System.out.println(jmeWorldBindPose);
        
        float[] angles = new float[3];
        jmeWorldBindPose.getRotation().toAngles(angles);
//        System.out.println("Angles: " + angles[0] * FastMath.RAD_TO_DEG + ", " +
//                                        angles[1] * FastMath.RAD_TO_DEG + ", " +
//                                        angles[2] * FastMath.RAD_TO_DEG);
    }
    
    public void updateWorldTransforms(Transform jmeParentNodeTransform, Transform parentBindPose) {
        Transform fbxLocalTransform = computeFbxLocalTransform();
        jmeLocalNodeTransform.set(fbxLocalTransform);
        
        if (jmeParentNodeTransform != null) {
            jmeParentNodeTransform = jmeParentNodeTransform.clone();
            switch (inheritMode) {
                case NoParentScale:
                case ScaleAfterChildRotation:
                case ScaleBeforeChildRotation:
                    jmeWorldNodeTransform.set(jmeLocalNodeTransform);
                    jmeWorldNodeTransform.combineWithParent(jmeParentNodeTransform);
                    break;
            }
        } else {
            jmeWorldNodeTransform.set(jmeLocalNodeTransform);
        }
        
        if (jmeWorldBindPose != null) {
            jmeLocalBindPose = new Transform();
            
            // Need to derive local bind pose from world bind pose
            // (this is to be expected for FBX limbs)
            jmeLocalBindPose.set(jmeWorldBindPose);
            jmeLocalBindPose.combineWithParent(parentBindPose.invert());
            
            // It's somewhat odd for the transforms to differ ...
//            System.out.println("Bind Pose for: " + getName());
//            if (!jmeLocalBindPose.equals(jmeLocalNodeTransform)) {
//                System.out.println("Local Bind: " + jmeLocalBindPose);
//                System.out.println("Local Trans: " + jmeLocalNodeTransform);
//            }
//            if (!jmeWorldBindPose.equals(jmeWorldNodeTransform)) {
//                System.out.println("World Bind: " + jmeWorldBindPose);
//                System.out.println("World Trans: " + jmeWorldNodeTransform);
//            }
        } else {
            // World pose derived from local transforms
            // (this is to be expected for FBX nodes)
            jmeLocalBindPose = new Transform();
            jmeWorldBindPose = new Transform();
            
            jmeLocalBindPose.set(jmeLocalNodeTransform);
            if (parentBindPose != null) {
                jmeWorldBindPose.set(jmeLocalNodeTransform);
                jmeWorldBindPose.combineWithParent(parentBindPose);
            } else {
                jmeWorldBindPose.set(jmeWorldNodeTransform);
            }
        }
        
        for (FbxNode child : children) {
            child.updateWorldTransforms(jmeWorldNodeTransform, jmeWorldBindPose);
        }
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        
        Vector3f localTranslation = new Vector3f();
        Quaternion localRotation = new Quaternion();
        Vector3f localScale = new Vector3f(Vector3f.UNIT_XYZ);
        Quaternion preRotation = new Quaternion();
        
        for (FbxElement e2 : element.getFbxProperties()) {
            String propName = (String) e2.properties.get(0);
            String type = (String) e2.properties.get(3);
            if (propName.equals("Lcl Translation")) {
                double x = (Double) e2.properties.get(4);
                double y = (Double) e2.properties.get(5);
                double z = (Double) e2.properties.get(6);
                localTranslation.set((float) x, (float) y, (float) z); //.divideLocal(unitSize);
            } else if (propName.equals("Lcl Rotation")) {
                double x = (Double) e2.properties.get(4);
                double y = (Double) e2.properties.get(5);
                double z = (Double) e2.properties.get(6);
                localRotation.fromAngles((float) x * FastMath.DEG_TO_RAD, (float) y * FastMath.DEG_TO_RAD, (float) z * FastMath.DEG_TO_RAD);
            } else if (propName.equals("Lcl Scaling")) {
                double x = (Double) e2.properties.get(4);
                double y = (Double) e2.properties.get(5);
                double z = (Double) e2.properties.get(6);
                localScale.set((float) x, (float) y, (float) z); //.multLocal(unitSize);
            } else if (propName.equals("PreRotation")) {
                double x = (Double) e2.properties.get(4);
                double y = (Double) e2.properties.get(5);
                double z = (Double) e2.properties.get(6);
                preRotation.set(FbxNodeUtil.quatFromBoneAngles((float) x * FastMath.DEG_TO_RAD, (float) y * FastMath.DEG_TO_RAD, (float) z * FastMath.DEG_TO_RAD));
            } else if (propName.equals("InheritType")) {
                int inheritType = (Integer) e2.properties.get(4);
                inheritMode = InheritMode.values()[inheritType];
            } else if (propName.equals("Visibility")) {
                visibility = (Double) e2.properties.get(4);
            } else if (type.contains("U")) {
                String userDataKey = (String) e2.properties.get(0);
                String userDataType = (String) e2.properties.get(1);
                Object userDataValue;
                
                if (userDataType.equals("KString")) {
                    userDataValue = e2.properties.get(4);
                } else if (userDataType.equals("int")) {
                    userDataValue = e2.properties.get(4);
                } else if (userDataType.equals("double")) {
                    // NOTE: jME3 does not support doubles in UserData.
                    //       Need to convert to float.
                    userDataValue = ((Double) e2.properties.get(4)).floatValue();
                } else if (userDataType.equals("Vector")) {
                    float x = ((Double) e2.properties.get(4)).floatValue();
                    float y = ((Double) e2.properties.get(5)).floatValue();
                    float z = ((Double) e2.properties.get(6)).floatValue();
                    userDataValue = new Vector3f(x, y, z);
                } else {
                    logger.log(Level.WARNING, "Unsupported user data type: {0}. Ignoring.", userDataType);
                    continue;
                }
                
                userData.put(userDataKey, userDataValue);
            }
        }
        
        // Create local transform
        // TODO: take into account Maya-style transforms (pre / post rotation ..)
        jmeLocalNodeTransform.setTranslation(localTranslation);
        jmeLocalNodeTransform.setRotation(localRotation);
        jmeLocalNodeTransform.setScale(localScale);
        
        if (element.getChildById("Vertices") != null) {
            // This is an old-style FBX 6.1
            // Meshes could be embedded inside the node.

            // Inject the mesh into ourselves.
            FbxMesh mesh = new FbxMesh(assetManager, sceneFolderName);
            mesh.fromElement(element);
            connectObject(mesh);
        }
    }
    
    private Spatial tryCreateGeometry(int materialIndex, Mesh jmeMesh, boolean single) {
        // Map meshes without material indices to material 0.
        if (materialIndex == -1) {
            materialIndex = 0;
        }
        
        Material jmeMat;
        if (materialIndex >= materials.size()) {
            // Material index does not exist. Create default material.
            jmeMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            jmeMat.setReceivesShadows(true);
        } else {
            FbxMaterial fbxMat = materials.get(materialIndex);
            jmeMat = fbxMat.getJmeObject();
        }
        
        String geomName = getName();
        if (single) {
            geomName += "-submesh";
        } else {
            geomName += "-mat-" + materialIndex + "-submesh";
        }
        Spatial spatial = new Geometry(geomName, jmeMesh);
        spatial.setMaterial(jmeMat);
        if (jmeMat.isTransparent()) {
            spatial.setQueueBucket(Bucket.Transparent);
        }
        if (jmeMat.isReceivesShadows()) {
            spatial.setShadowMode(ShadowMode.Receive);
        }
        spatial.updateModelBound();
        return spatial;
    }
    
    /**
     * If this geometry node is deformed by a skeleton, this
     * returns the node containing the skeleton.
     * 
     * In jME3, a mesh can be deformed by a skeleton only if it is 
     * a child of the node containing the skeleton. However, this
     * is not a requirement in FBX, so we have to modify the scene graph
     * of the loaded model to adjust for this.
     * This happens automatically in 
     * {@link #createScene(com.jme3.scene.plugins.fbx.node.FbxNode)}.
     * 
     * @return The model this node would like to be a child of, or null
     * if no preferred parent.
     */
    public FbxNode getPreferredParent() {
        if (!(nodeAttribute instanceof FbxMesh)) {
            return null;
        }
        
        FbxMesh fbxMesh = (FbxMesh) nodeAttribute;
        FbxSkinDeformer deformer = fbxMesh.getSkinDeformer();
        FbxNode preferredParent = null;
        
        if (deformer != null) {
            for (FbxCluster cluster : deformer.getJmeObject()) {
                FbxLimbNode limb = cluster.getLimb();
                if (preferredParent == null) {
                    preferredParent = limb.getSkeletonHolder();
                } else if (preferredParent != limb.getSkeletonHolder()) {
                    logger.log(Level.WARNING, "A mesh is being deformed by multiple skeletons. "
                                            + "Only one skeleton will work, ignoring other skeletons.");
                }
            }
        }
        
        return preferredParent;
    }
    
    @Override
    public Spatial toJmeObject() {
        Spatial spatial;
        
        if (nodeAttribute instanceof FbxMesh) {
            FbxMesh fbxMesh = (FbxMesh) nodeAttribute;
            IntMap<Mesh> jmeMeshes = fbxMesh.getJmeObject();
            
            if (jmeMeshes == null || jmeMeshes.size() == 0) {
                // No meshes found on FBXMesh (??)
                logger.log(Level.WARNING, "No meshes could be loaded. Creating empty node.");
                spatial = new Node(getName() + "-node");
            } else {
                // Multiple jME3 geometries required for a single FBXMesh.
                String nodeName;
                if (children.isEmpty()) {
                    nodeName = getName() + "-mesh";
                } else {
                    nodeName = getName() + "-node";
                }
                Node node = new Node(nodeName);
                boolean singleMesh = jmeMeshes.size() == 1;
                for (IntMap.Entry<Mesh> meshInfo : jmeMeshes) {
                    node.attachChild(tryCreateGeometry(meshInfo.getKey(), meshInfo.getValue(), singleMesh));
                }
                spatial = node;
            }
        } else {
            if (nodeAttribute != null) {
                // Just specifies that this is a "null" node.
                nodeAttribute.getJmeObject();
            }
            
            // TODO: handle other node attribute types.
            //       right now everything we don't know about gets converted
            //       to jME3 Node.
            spatial = new Node(getName() + "-node");
        }
        
        if (!children.isEmpty()) {
            // Check uniform scale.
            // Although, if inheritType is 0 (eInheritRrSs)
            // it might not be a problem.
            Vector3f localScale = jmeLocalNodeTransform.getScale();
            if (!FastMath.approximateEquals(localScale.x, localScale.y) || 
                !FastMath.approximateEquals(localScale.x, localScale.z)) {
                logger.log(Level.WARNING, "Non-uniform scale detected on parent node. " +
                                          "The model may appear distorted.");
            }
        }
        
        spatial.setLocalTransform(jmeLocalNodeTransform);
        
        if (visibility == 0.0) {
            spatial.setCullHint(CullHint.Always);
        }
        
        for (Map.Entry<String, Object> userDataEntry : userData.entrySet()) {
            spatial.setUserData(userDataEntry.getKey(), userDataEntry.getValue());
        }
        
        return spatial;
    }
    
    /**
     * Create jME3 Skeleton objects on the scene. 
     * 
     * Goes through the scene graph and finds limbs that are 
     * attached to FBX nodes, then creates a Skeleton on the node
     * based on the child limbs.
     * 
     * Must be called prior to calling 
     * {@link #createScene(com.jme3.scene.plugins.fbx.node.FbxNode)}.
     * 
     * @param fbxNode The root FBX node.
     */
    public static void createSkeletons(FbxNode fbxNode) {
        boolean createSkeleton = false;
        for (FbxNode fbxChild : fbxNode.children) {
            if (fbxChild instanceof FbxLimbNode) {
                createSkeleton = true;
            } else {
                createSkeletons(fbxChild);
            }
        }
        if (createSkeleton) {
            if (fbxNode.skeleton != null) {
                throw new UnsupportedOperationException();
            }
            fbxNode.skeleton = FbxLimbNode.createSkeleton(fbxNode);
//            System.out.println("created skeleton: " + fbxNode.skeleton);
        }
    }
    
    private static void relocateSpatial(Spatial spatial, 
                                        Transform originalWorldTransform, Transform newWorldTransform) {
        Transform localTransform = new Transform();
        localTransform.set(originalWorldTransform);
        localTransform.combineWithParent(newWorldTransform.invert());
        spatial.setLocalTransform(localTransform);
    }
    
    public static Spatial createScene(FbxNode fbxNode) {
        Spatial jmeSpatial = fbxNode.getJmeObject();
        
        if (jmeSpatial instanceof Node) {
            // Attach children to Node
            Node jmeNode = (Node) jmeSpatial;
            for (FbxNode fbxChild : fbxNode.children) {
                if (!(fbxChild instanceof FbxLimbNode)) {
                    createScene(fbxChild);
                    
                    FbxNode preferredParent = fbxChild.getPreferredParent();
                    Spatial jmeChild = fbxChild.getJmeObject();
                    if (preferredParent != null) {
//                        System.out.println("Preferred parent for " + fbxChild + " is " + preferredParent);
                        
                        Node jmePreferredParent = (Node) preferredParent.getJmeObject();
                        relocateSpatial(jmeChild, fbxChild.jmeWorldNodeTransform, 
                                                  preferredParent.jmeWorldNodeTransform);
                        jmePreferredParent.attachChild(jmeChild);
                    } else {
                        jmeNode.attachChild(jmeChild);
                    }
                }
            }
        }
        
        if (fbxNode.skeleton != null) { 
            jmeSpatial.addControl(new AnimControl(fbxNode.skeleton));
            jmeSpatial.addControl(new SkeletonControl(fbxNode.skeleton));
            
            SkeletonDebugger sd = new SkeletonDebugger("debug", fbxNode.skeleton);
            Material mat = new Material(fbxNode.assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.getAdditionalRenderState().setWireframe(true);
            mat.getAdditionalRenderState().setDepthTest(false);
            mat.setColor("Color", ColorRGBA.Green);
            sd.setMaterial(mat);
            
            ((Node)jmeSpatial).attachChild(sd);
        }
        
        return jmeSpatial;
    }
    
//    public SceneLoader.Limb toLimb() {
//        SceneLoader.Limb limb = new SceneLoader.Limb();
//        limb.name = getName();
//        Quaternion rotation = preRotation.mult(localRotation);
//        limb.bindTransform = new Transform(localTranslation, rotation, localScale);
//        return limb;
//    }
    
    public Skeleton getJmeSkeleton() {
        return skeleton;
    }
    
    public List<FbxNode> getChildren() { 
        return children;
    }
    
    @Override
    public void connectObject(FbxObject object) {
        if (object instanceof FbxNode) {
            // Scene Graph Object
            FbxNode childNode = (FbxNode) object;
            if (childNode.parent != null) {
                throw new IllegalStateException("Cannot attach " + childNode
                                              + " to " + this + ". It is already "
                                              + "attached to " + childNode.parent);
            }
            childNode.parent = this;
            children.add(childNode);
        } else if (object instanceof FbxNodeAttribute) {
            // Node Attribute
            if (nodeAttribute != null) { 
                throw new IllegalStateException("An FBXNodeAttribute (" + nodeAttribute + ")" +
                                                " is already attached to " + this + ". " +
                                                "Only one attribute allowed per node.");
            }
            
            nodeAttribute = (FbxNodeAttribute) object;
            if (nodeAttribute instanceof FbxNullAttribute) {
                nodeAttribute.getJmeObject();
            }
        } else if (object instanceof FbxMaterial) {
            materials.add((FbxMaterial) object);
        } else if (object instanceof FbxImage || object instanceof FbxTexture) {
            // Ignore - attaching textures to nodes is legacy feature.
        } else {
            unsupportedConnectObject(object);
        }
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        // Only allowed to connect local transform properties to object
        // (FbxAnimCurveNode)
        if (object instanceof FbxAnimCurveNode) {
            FbxAnimCurveNode curveNode = (FbxAnimCurveNode) object;
            if (property.equals("Lcl Translation")
                    || property.equals("Lcl Rotation")
                    || property.equals("Lcl Scaling")) {
                
                List<FbxAnimCurveNode> curveNodes = propertyToAnimCurveMap.get(property);
                if (curveNodes == null) {
                    curveNodes = new ArrayList<FbxAnimCurveNode>();
                    curveNodes.add(curveNode);
                    propertyToAnimCurveMap.put(property, curveNodes);
                }
                curveNodes.add(curveNode);
                
                // Make sure the curve knows about it animating
                // this node as well. 
                curveNode.addInfluencedNode(this, property);
            } else {
                logger.log(Level.WARNING, "Animating the property ''{0}'' is not "
                                        + "supported. Ignoring.", property);
            }
        } else {
            unsupportedConnectObjectProperty(object, property);
        }
    }
    
}
