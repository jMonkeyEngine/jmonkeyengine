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
package com.jme3.scene.plugins.fbx;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.Track;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.math.Matrix4f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.fbx.anim.FbxToJmeTrack;
import com.jme3.scene.plugins.fbx.anim.FbxAnimCurveNode;
import com.jme3.scene.plugins.fbx.anim.FbxAnimLayer;
import com.jme3.scene.plugins.fbx.anim.FbxAnimStack;
import com.jme3.scene.plugins.fbx.anim.FbxBindPose;
import com.jme3.scene.plugins.fbx.anim.FbxLimbNode;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.file.FbxFile;
import com.jme3.scene.plugins.fbx.file.FbxReader;
import com.jme3.scene.plugins.fbx.file.FbxId;
import com.jme3.scene.plugins.fbx.misc.FbxGlobalSettings;
import com.jme3.scene.plugins.fbx.node.FbxNode;
import com.jme3.scene.plugins.fbx.node.FbxRootNode;
import com.jme3.scene.plugins.fbx.obj.FbxObjectFactory;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FbxLoader implements AssetLoader {
    
    private static final Logger logger = Logger.getLogger(FbxLoader.class.getName());
    
    private AssetManager assetManager;
    
    private String sceneName;
    private String sceneFilename;
    private String sceneFolderName;
    private FbxGlobalSettings globalSettings;
    private final Map<FbxId, FbxObject> objectMap = new HashMap<>();
    
    private final List<FbxAnimStack> animStacks = new ArrayList<>();
    private final List<FbxBindPose> bindPoses = new ArrayList<>();
    
    @Override
    public Object load(AssetInfo assetInfo) throws IOException {
        this.assetManager = assetInfo.getManager();
        AssetKey<?> assetKey = assetInfo.getKey();
        if (!(assetKey instanceof ModelKey)) {
            throw new AssetLoadException("Invalid asset key");
        }
        
        InputStream stream = assetInfo.openStream();
        try {
            sceneFilename = assetKey.getName();
            sceneFolderName = assetKey.getFolder();
            String ext = assetKey.getExtension();
            
            sceneName = sceneFilename.substring(0, sceneFilename.length() - ext.length() - 1);
            if (sceneFolderName != null && sceneFolderName.length() > 0) {
                sceneName = sceneName.substring(sceneFolderName.length());
            }
            
            reset();
            
            // Load the data from the stream.
            loadData(stream);
            
            // Bind poses are needed to compute world transforms.
            applyBindPoses();
            
            // Need world transforms for skeleton creation.
            updateWorldTransforms();
            
            // Need skeletons for meshes to be created in scene graph construction.
            // Mesh bone indices require skeletons to determine bone index.
            constructSkeletons();
            
            // Create the jME3 scene graph from the FBX scene graph.
            // Also creates SkeletonControls based on the constructed skeletons.
            Spatial scene = constructSceneGraph();
            
            // Load animations into AnimControls
            constructAnimations();
           
            return scene;
        } finally {
            releaseObjects();
            if (stream != null) {
                stream.close();
            }
        }
    }
    
    private void reset() {
        globalSettings = new FbxGlobalSettings();
    }
    
    private void releaseObjects() {
        globalSettings = null;
        objectMap.clear();
        animStacks.clear();
    }
    
    private void loadData(InputStream stream) throws IOException {
        FbxFile scene = FbxReader.readFBX(stream);

        //FbxDump.dumpFile(scene);

        // TODO: Load FBX object templates
        
        for (FbxElement e : scene.rootElements) {
            if (e.id.equals("FBXHeaderExtension")) {
                loadHeader(e);
            } else if (e.id.equals("GlobalSettings")) {
                loadGlobalSettings(e);
             } else if (e.id.equals("Objects")) {
                loadObjects(e);
            } else if (e.id.equals("Connections")) {
                connectObjects(e);
            }
        }
    }
    
    private void loadHeader(FbxElement element) {
        for (FbxElement e : element.children) {
            if (e.id.equals("FBXVersion")) {
                Integer version = (Integer) e.properties.get(0);
                if (version < 7100) {
                    logger.log(Level.WARNING, "FBX file version is older than 7.1. "
                                            + "Some features may not work.");
                }
            }
        }
    }
    
    private void loadGlobalSettings(FbxElement element) {
        globalSettings = new FbxGlobalSettings();
        globalSettings.fromElement(element);
    }
    
    private void loadObjects(FbxElement element) {
        // Initialize the FBX root element.
        objectMap.put(FbxId.ROOT, new FbxRootNode(assetManager, sceneFolderName));

        for(FbxElement e : element.children) {
            if (e.id.equals("GlobalSettings")) {
                // Old FBX files seem to have the GlobalSettings element
                // under Objects (??)
                globalSettings.fromElement(e);
            } else {
                FbxObject object = FbxObjectFactory.createObject(e, assetManager, sceneFolderName);
                if (object != null) {
                    if (objectMap.containsKey(object.getId())) {
                        logger.log(Level.WARNING, "An object with ID \"{0}\" has "
                                                + "already been defined. "
                                                + "Ignoring.", 
                                                   object.getId());
                    }
                    
                    objectMap.put(object.getId(), object);
                    
                    if (object instanceof FbxAnimStack) {
                        // NOTE: animation stacks are implicitly global.
                        // Capture them here.
                        animStacks.add((FbxAnimStack) object);
                    } else if (object instanceof FbxBindPose) {
                        bindPoses.add((FbxBindPose) object);
                    }
                } else {
                    throw new UnsupportedOperationException("Failed to create FBX object of type: " + e.id);
                }
            }
        }
    }
    
    private void connectObjects(FbxElement element) {
        if (objectMap.isEmpty()) {
            logger.log(Level.WARNING, "FBX file is missing object information");
            return;
        } else if (objectMap.size() == 1) {
            // Only root node (automatically added by jME3)
            logger.log(Level.WARNING, "FBX file has no objects");
            return;
        }
        
        for (FbxElement el : element.children) {
            if (!el.id.equals("C") && !el.id.equals("Connect")) {
                continue;
            }
            String type = (String) el.properties.get(0);
            FbxId childId;
            FbxId parentId;
            if (type.equals("OO")) {
                childId = FbxId.create(el.properties.get(1));
                parentId = FbxId.create(el.properties.get(2));
                FbxObject child = objectMap.get(childId);
                FbxObject parent;

                if (parentId.isNull()) {
                    // TODO: maybe clean this up a bit..
                    parent = objectMap.get(FbxId.ROOT);
                } else {
                    parent = objectMap.get(parentId);
                }

                if (parent == null) {
                    throw new UnsupportedOperationException("Cannot find parent object ID \"" + parentId + "\"");
                }

                parent.connectObject(child);
            } else if (type.equals("OP")) {
                childId = FbxId.create(el.properties.get(1));
                parentId = FbxId.create(el.properties.get(2));
                String propName = (String) el.properties.get(3);
                FbxObject child = objectMap.get(childId);
                if (child == null) {
                    logger.log(Level.WARNING,
                            "Missing child object with ID {0}. Skipping object-"
                                    + "property connection for property \"{1}\"",
                            new Object[]{childId, propName});
                }
                FbxObject parent = objectMap.get(parentId);
                if (parent == null) {
                    logger.log(Level.WARNING,
                            "Missing parent object with ID {0}. Skipping object-"
                                    + "property connection for property \"{1}\"",
                            new Object[]{parentId, propName});
                }
                if (parent != null && child != null) {
                    parent.connectObjectProperty(child, propName);
                }

            } else {
                logger.log(Level.WARNING, "Unknown connection type: {0}. Ignoring.", type);
            }
        }
    }
    
    /**
     * Copies the bind poses from FBX BindPose objects to FBX nodes.
     * Must be called prior to {@link #updateWorldTransforms()}.
     */
    private void applyBindPoses() {
        for (FbxBindPose bindPose : bindPoses) {
            Map<FbxId, Matrix4f> bindPoseData = bindPose.getJmeObject();
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "Applying {0} bind poses", bindPoseData.size());
            }
            for (Map.Entry<FbxId, Matrix4f> entry : bindPoseData.entrySet()) {
                FbxObject obj = objectMap.get(entry.getKey());
                if (obj instanceof FbxNode) {
                    FbxNode node = (FbxNode) obj;
                    node.setWorldBindPose(entry.getValue());
                } else {
                    logger.log(Level.WARNING, "Bind pose can only be applied to FBX nodes. Ignoring.");
                }
            }
        }
    }
    
    /**
     * Updates world transforms and bind poses for the FBX scene graph.
     */
    private void updateWorldTransforms() {
        FbxNode fbxRoot = (FbxNode) objectMap.get(FbxId.ROOT);
        fbxRoot.updateWorldTransforms(null, null);
    }
    
    private void constructAnimations() {
        // In FBX, animation are not attached to any root.
        // They are implicitly global.
        // So, we need to use heuristics to find which node(s) 
        // an animation is associated with, so we can create the AnimControl
        // in the appropriate location in the scene.
        Map<FbxToJmeTrack, FbxToJmeTrack> pairs = new HashMap<>();
        for (FbxAnimStack stack : animStacks) {
            for (FbxAnimLayer layer : stack.getLayers()) {
                for (FbxAnimCurveNode curveNode : layer.getAnimationCurveNodes()) {
                    for (Map.Entry<FbxNode, String> nodePropertyEntry : curveNode.getInfluencedNodeProperties().entrySet()) {
                        FbxToJmeTrack lookupPair = new FbxToJmeTrack();
                        lookupPair.animStack = stack;
                        lookupPair.animLayer = layer;
                        lookupPair.node = nodePropertyEntry.getKey();
                        
                        // Find if this pair is already stored
                        FbxToJmeTrack storedPair = pairs.get(lookupPair);
                        if (storedPair == null) {
                            // If not, store it.
                            storedPair = lookupPair;
                            pairs.put(storedPair, storedPair);
                        }
                        
                        String property = nodePropertyEntry.getValue();
                        storedPair.animCurves.put(property, curveNode);
                    }
                }
            }
        }
        
        // At this point we can construct the animation for all pairs ...
        for (FbxToJmeTrack pair : pairs.values()) {
            if (pair.countKeyframes() == 0) {
                continue;
            }
            String animName = pair.animStack.getName();
            float duration    = pair.animStack.getDuration();
            
            //System.out.println("ANIMATION: " + animName + ", duration = " + duration);
            //System.out.println("NODE: " + pair.node.getName());
            
            duration = pair.getDuration();
            
            if (pair.node instanceof FbxLimbNode) {
                // Find the spatial that has the skeleton for this limb.
                FbxLimbNode limbNode = (FbxLimbNode) pair.node;
                Bone bone = limbNode.getJmeBone();
                Spatial jmeSpatial = limbNode.getSkeletonHolder().getJmeObject();
                Skeleton skeleton = limbNode.getSkeletonHolder().getJmeSkeleton();
                
                // Get the animation control (create if missing).
                AnimControl animControl = jmeSpatial.getControl(AnimControl.class);
                if (animControl.getSkeleton() != skeleton) {
                    throw new UnsupportedOperationException();
                }
                
                // Get the animation (create if missing).
                Animation anim = animControl.getAnim(animName);
                if (anim == null) { 
                    anim = new Animation(animName, duration);
                    animControl.addAnim(anim);
                }
                
                // Find the bone index from the spatial's skeleton.
                int boneIndex = skeleton.getBoneIndex(bone);
                
                // Generate the bone track.
                BoneTrack bt = pair.toJmeBoneTrack(boneIndex, bone.getBindInverseTransform());
                
                // Add the bone track to the animation.
                anim.addTrack(bt);
            } else {
                // Create the spatial animation
                Animation anim = new Animation(animName, duration);
                anim.setTracks(new Track[]{ pair.toJmeSpatialTrack() });

                // Get the animation control (create if missing).
                Spatial jmeSpatial = pair.node.getJmeObject();
                AnimControl animControl = jmeSpatial.getControl(AnimControl.class);
                
                if (animControl == null) { 
                    animControl = new AnimControl(null);
                    jmeSpatial.addControl(animControl);
                }
                
                // Add the spatial animation
                animControl.addAnim(anim);
            }
        }
    }
    
    private void constructSkeletons() {
        FbxNode fbxRoot = (FbxNode) objectMap.get(FbxId.ROOT);
        FbxNode.createSkeletons(fbxRoot);
    }
    
    private Spatial constructSceneGraph() {
        // Acquire the implicit root object.
        FbxNode fbxRoot = (FbxNode) objectMap.get(FbxId.ROOT);

        // Convert it into a jME3 scene
        Node jmeRoot = (Node) FbxNode.createScene(fbxRoot);

        // Fix the name (will probably be set to something like "-node")
        jmeRoot.setName(sceneName + "-scene");

        return jmeRoot;
    }
}
