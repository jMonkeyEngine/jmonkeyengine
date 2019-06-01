/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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
package com.jme3.scene.plugins.blender;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import com.jme3.animation.Animation;
import com.jme3.animation.Bone;
import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.asset.BlenderKey;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.plugins.blender.animations.BlenderAction;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.constraints.Constraint;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.DnaBlockData;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.FileBlockHeader.BlockCode;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.meshes.TemporalMesh;
import com.jme3.texture.Texture;

/**
 * The class that stores temporary data and manages it during loading the belnd
 * file. This class is intended to be used in a single loading thread. It holds
 * the state of loading operations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BlenderContext {
    /** The blender file version. */
    private int                                    blenderVersion;
    /** The blender key. */
    private BlenderKey                             blenderKey;
    /** The header of the file block. */
    private DnaBlockData                           dnaBlockData;
    /** The scene structure. */
    private Structure                              sceneStructure;
    /** The input stream of the blend file. */
    private BlenderInputStream                     inputStream;
    /** The asset manager. */
    private AssetManager                           assetManager;
    /** The blocks read from the file. */
    protected List<FileBlockHeader>                blocks                 = new ArrayList<FileBlockHeader>();
    /**
     * A map containing the file block headers. The key is the old memory address.
     */
    private Map<Long, FileBlockHeader>             fileBlockHeadersByOma  = new HashMap<Long, FileBlockHeader>();
    /** A map containing the file block headers. The key is the block code. */
    private Map<BlockCode, List<FileBlockHeader>>  fileBlockHeadersByCode = new HashMap<BlockCode, List<FileBlockHeader>>();
    /**
     * This map stores the loaded features by their old memory address. The
     * first object in the value table is the loaded structure and the second -
     * the structure already converted into proper data.
     */
    private Map<Long, Map<LoadedDataType, Object>> loadedFeatures         = new HashMap<Long, Map<LoadedDataType, Object>>();
    /** Features loaded from external blender files. The key is the file path and the value is a map between feature name and loaded feature. */
    private Map<String, Map<String, Object>>       linkedFeatures         = new HashMap<String, Map<String, Object>>();
    /** A stack that hold the parent structure of currently loaded feature. */
    private Stack<Structure>                       parentStack            = new Stack<Structure>();
    /** A list of constraints for the specified object. */
    protected Map<Long, List<Constraint>>          constraints            = new HashMap<Long, List<Constraint>>();
    /** Animations loaded for features. */
    private Map<Long, List<Animation>>             animations             = new HashMap<Long, List<Animation>>();
    /** Loaded skeletons. */
    private Map<Long, Skeleton>                    skeletons              = new HashMap<Long, Skeleton>();
    /** A map between skeleton and node it modifies. */
    private Map<Skeleton, Node>                    nodesWithSkeletons     = new HashMap<Skeleton, Node>();
    /** A map of bone contexts. */
    protected Map<Long, BoneContext>               boneContexts           = new HashMap<Long, BoneContext>();
    /** A map og helpers that perform loading. */
    private Map<String, AbstractBlenderHelper>     helpers                = new HashMap<String, AbstractBlenderHelper>();
    /** Markers used by loading classes to store some custom data. This is made to avoid putting this data into user properties. */
    private Map<String, Map<Object, Object>>       markers                = new HashMap<String, Map<Object, Object>>();
    /** A map of blender actions. The key is the action name and the value is the action itself. */
    private Map<String, BlenderAction>             actions                = new HashMap<String, BlenderAction>();

    /**
     * This method sets the blender file version.
     * 
     * @param blenderVersion
     *            the blender file version
     */
    public void setBlenderVersion(String blenderVersion) {
        this.blenderVersion = Integer.parseInt(blenderVersion);
    }

    /**
     * @return the blender file version
     */
    public int getBlenderVersion() {
        return blenderVersion;
    }

    /**
     * This method sets the blender key.
     * 
     * @param blenderKey
     *            the blender key
     */
    public void setBlenderKey(BlenderKey blenderKey) {
        this.blenderKey = blenderKey;
    }

    /**
     * This method returns the blender key.
     * 
     * @return the blender key
     */
    public BlenderKey getBlenderKey() {
        return blenderKey;
    }

    /**
     * This method sets the dna block data.
     * 
     * @param dnaBlockData
     *            the dna block data
     */
    public void setBlockData(DnaBlockData dnaBlockData) {
        this.dnaBlockData = dnaBlockData;
    }

    /**
     * This method returns the dna block data.
     * 
     * @return the dna block data
     */
    public DnaBlockData getDnaBlockData() {
        return dnaBlockData;
    }

    /**
     * This method sets the scene structure data.
     * 
     * @param sceneStructure
     *            the scene structure data
     */
    public void setSceneStructure(Structure sceneStructure) {
        this.sceneStructure = sceneStructure;
    }

    /**
     * This method returns the scene structure data.
     * 
     * @return the scene structure data
     */
    public Structure getSceneStructure() {
        return sceneStructure;
    }

    /**
     * This method returns the asset manager.
     * 
     * @return the asset manager
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }

    /**
     * This method sets the asset manager.
     * 
     * @param assetManager
     *            the asset manager
     */
    public void setAssetManager(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * This method returns the input stream of the blend file.
     * 
     * @return the input stream of the blend file
     */
    public BlenderInputStream getInputStream() {
        return inputStream;
    }

    /**
     * This method sets the input stream of the blend file.
     * 
     * @param inputStream
     *            the input stream of the blend file
     */
    public void setInputStream(BlenderInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * This method adds a file block header to the map. Its old memory address
     * is the key.
     * 
     * @param oldMemoryAddress
     *            the address of the block header
     * @param fileBlockHeader
     *            the block header to store
     */
    public void addFileBlockHeader(Long oldMemoryAddress, FileBlockHeader fileBlockHeader) {
        blocks.add(fileBlockHeader);
        fileBlockHeadersByOma.put(oldMemoryAddress, fileBlockHeader);
        List<FileBlockHeader> headers = fileBlockHeadersByCode.get(fileBlockHeader.getCode());
        if (headers == null) {
            headers = new ArrayList<FileBlockHeader>();
            fileBlockHeadersByCode.put(fileBlockHeader.getCode(), headers);
        }
        headers.add(fileBlockHeader);
    }

    /**
     * @return the block headers
     */
    public List<FileBlockHeader> getBlocks() {
        return blocks;
    }

    /**
     * This method returns the block header of a given memory address. If the
     * header is not present then null is returned.
     * 
     * @param oldMemoryAddress
     *            the address of the block header
     * @return loaded header or null if it was not yet loaded
     */
    public FileBlockHeader getFileBlock(Long oldMemoryAddress) {
        return fileBlockHeadersByOma.get(oldMemoryAddress);
    }

    /**
     * This method returns a list of file blocks' headers of a specified code.
     * 
     * @param code
     *            the code of file blocks
     * @return a list of file blocks' headers of a specified code
     */
    public List<FileBlockHeader> getFileBlocks(BlockCode code) {
        return fileBlockHeadersByCode.get(code);
    }

    /**
     * This method adds a helper instance to the helpers' map.
     * 
     * @param <T>
     *            the type of the helper
     * @param clazz
     *            helper's class definition
     * @param helper
     *            the helper instance
     */
    public <T> void putHelper(Class<T> clazz, AbstractBlenderHelper helper) {
        helpers.put(clazz.getSimpleName(), helper);
    }

    @SuppressWarnings("unchecked")
    public <T> T getHelper(Class<?> clazz) {
        return (T) helpers.get(clazz.getSimpleName());
    }

    /**
     * This method adds a loaded feature to the map. The key is its unique old
     * memory address.
     * 
     * @param oldMemoryAddress
     *            the address of the feature
     * @param featureDataType
     * @param feature
     *            the feature we want to store
     */
    public void addLoadedFeatures(Long oldMemoryAddress, LoadedDataType featureDataType, Object feature) {
        if (oldMemoryAddress == null || featureDataType == null || feature == null) {
            throw new IllegalArgumentException("One of the given arguments is null!");
        }
        Map<LoadedDataType, Object> map = loadedFeatures.get(oldMemoryAddress);
        if (map == null) {
            map = new HashMap<BlenderContext.LoadedDataType, Object>();
            loadedFeatures.put(oldMemoryAddress, map);
        }
        map.put(featureDataType, feature);
    }

    /**
     * This method returns the feature of a given memory address. If the feature
     * is not yet loaded then null is returned.
     * 
     * @param oldMemoryAddress
     *            the address of the feature
     * @param loadedFeatureDataType
     *            the type of data we want to retrieve it can be either filled
     *            structure or already converted feature
     * @return loaded feature or null if it was not yet loaded
     */
    public Object getLoadedFeature(Long oldMemoryAddress, LoadedDataType loadedFeatureDataType) {
        Map<LoadedDataType, Object> result = loadedFeatures.get(oldMemoryAddress);
        if (result != null) {
            return result.get(loadedFeatureDataType);
        }
        return null;
    }

    /**
     * The method adds linked content to the blender context.
     * @param blenderFilePath
     *            the path of linked blender file
     * @param featureGroup
     *            the linked feature group (ie. scenes, materials, meshes, etc.)
     * @param feature
     *            the linked feature
     */
    @Deprecated
    public void addLinkedFeature(String blenderFilePath, String featureGroup, Object feature) {
        // the method is deprecated and empty at the moment
    }

    /**
     * The method returns linked feature of a given name from the specified blender path.
     * @param blenderFilePath
     *            the blender file path
     * @param featureName
     *            the feature name we want to get
     * @return linked feature or null if none was found
     */
    @SuppressWarnings("unchecked")
    public Object getLinkedFeature(String blenderFilePath, String featureName) {
        Map<String, Object> linkedFeatures = this.linkedFeatures.get(blenderFilePath);
        if(linkedFeatures != null) {
            String namePrefix = (featureName.charAt(0) + "" + featureName.charAt(1)).toUpperCase();
            featureName = featureName.substring(2);
            
            if("SC".equals(namePrefix)) {
                List<Node> scenes = (List<Node>) linkedFeatures.get("scenes");
                if(scenes != null) {
                    for(Node scene : scenes) {
                        if(featureName.equals(scene.getName())) {
                            return scene;
                        }
                    }
                }
            } else if("OB".equals(namePrefix)) {
                List<Node> features = (List<Node>) linkedFeatures.get("objects");
                if(features != null) {
                    for(Node feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("ME".equals(namePrefix)) {
                List<TemporalMesh> temporalMeshes = (List<TemporalMesh>) linkedFeatures.get("meshes");
                if(temporalMeshes != null) {
                    for(TemporalMesh temporalMesh : temporalMeshes) {
                        if(featureName.equals(temporalMesh.getName())) {
                            return temporalMesh;
                        }
                    }
                }
            } else if("MA".equals(namePrefix)) {
                List<MaterialContext> features = (List<MaterialContext>) linkedFeatures.get("materials");
                if(features != null) {
                    for(MaterialContext feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("TX".equals(namePrefix)) {
                List<Texture> features = (List<Texture>) linkedFeatures.get("textures");
                if(features != null) {
                    for(Texture feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("IM".equals(namePrefix)) {
                List<Texture> features = (List<Texture>) linkedFeatures.get("images");
                if(features != null) {
                    for(Texture feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("AC".equals(namePrefix)) {
                List<Animation> features = (List<Animation>) linkedFeatures.get("animations");
                if(features != null) {
                    for(Animation feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("CA".equals(namePrefix)) {
                List<Camera> features = (List<Camera>) linkedFeatures.get("cameras");
                if(features != null) {
                    for(Camera feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("LA".equals(namePrefix)) {
                List<Light> features = (List<Light>) linkedFeatures.get("lights");
                if(features != null) {
                    for(Light feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            } else if("FI".equals(featureName)) {
                List<Filter> features = (List<Filter>) linkedFeatures.get("lights");
                if(features != null) {
                    for(Filter feature : features) {
                        if(featureName.equals(feature.getName())) {
                            return feature;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @return all linked features for the current blend file
     */
    public Map<String, Map<String, Object>> getLinkedFeatures() {
        return linkedFeatures;
    }

    /**
     * This method adds the structure to the parent stack.
     * 
     * @param parent
     *            the structure to be added to the stack
     */
    public void pushParent(Structure parent) {
        parentStack.push(parent);
    }

    /**
     * This method removes the structure from the top of the parent's stack.
     * 
     * @return the structure that was removed from the stack
     */
    public Structure popParent() {
        try {
            return parentStack.pop();
        } catch (EmptyStackException e) {
            return null;
        }
    }

    /**
     * This method retrieves the structure at the top of the parent's stack but
     * does not remove it.
     * 
     * @return the structure from the top of the stack
     */
    public Structure peekParent() {
        try {
            return parentStack.peek();
        } catch (EmptyStackException e) {
            return null;
        }
    }

    /**
     * This method adds a new modifier to the list.
     * 
     * @param ownerOMA
     *            the owner's old memory address
     * @param constraints
     *            the object's constraints
     */
    public void addConstraints(Long ownerOMA, List<Constraint> constraints) {
        List<Constraint> objectConstraints = this.constraints.get(ownerOMA);
        if (objectConstraints == null) {
            objectConstraints = new ArrayList<Constraint>();
            this.constraints.put(ownerOMA, objectConstraints);
        }
        objectConstraints.addAll(constraints);
    }

    /**
     * Returns constraints applied to the feature of the given OMA.
     * @param ownerOMA
     *            the constraints' owner OMA
     * @return a list of constraints or <b>null</b> if no constraints are applied to the feature
     */
    public List<Constraint> getConstraints(Long ownerOMA) {
        return constraints.get(ownerOMA);
    }

    /**
     * @return all available constraints
     */
    public List<Constraint> getAllConstraints() {
        List<Constraint> result = new ArrayList<Constraint>();
        for (Entry<Long, List<Constraint>> entry : constraints.entrySet()) {
            result.addAll(entry.getValue());
        }
        return result;
    }

    /**
     * This method adds the animation for the specified OMA of its owner.
     * 
     * @param ownerOMA
     *            the owner's old memory address
     * @param animation
     *            the animation for the feature specified by ownerOMA
     */
    public void addAnimation(Long ownerOMA, Animation animation) {
        List<Animation> animList = animations.get(ownerOMA);
        if (animList == null) {
            animList = new ArrayList<Animation>();
            animations.put(ownerOMA, animList);
        }
        animList.add(animation);
    }

    /**
     * This method returns the animation data for the specified owner.
     * 
     * @param ownerOMA
     *            the old memory address of the animation data owner
     * @return the animation or null if none exists
     */
    public List<Animation> getAnimations(Long ownerOMA) {
        return animations.get(ownerOMA);
    }

    /**
     * This method sets the skeleton for the specified OMA of its owner.
     * 
     * @param skeletonOMA
     *            the skeleton's old memory address
     * @param skeleton
     *            the skeleton specified by the given OMA
     */
    public void setSkeleton(Long skeletonOMA, Skeleton skeleton) {
        skeletons.put(skeletonOMA, skeleton);
    }

    /**
     * The method stores a binding between the skeleton and the proper armature
     * node.
     * 
     * @param skeleton
     *            the skeleton
     * @param node
     *            the armature node
     */
    public void setNodeForSkeleton(Skeleton skeleton, Node node) {
        nodesWithSkeletons.put(skeleton, node);
    }

    /**
     * This method returns the armature node that is defined for the skeleton.
     * 
     * @param skeleton
     *            the skeleton
     * @return the armature node that defines the skeleton in blender
     */
    public Node getControlledNode(Skeleton skeleton) {
        return nodesWithSkeletons.get(skeleton);
    }

    /**
     * This method returns the skeleton for the specified OMA of its owner.
     * 
     * @param skeletonOMA
     *            the skeleton's old memory address
     * @return the skeleton specified by the given OMA
     */
    public Skeleton getSkeleton(Long skeletonOMA) {
        return skeletons.get(skeletonOMA);
    }

    /**
     * This method sets the bone context for the given bone old memory address.
     * If the context is already set it will be replaced.
     * 
     * @param boneOMA
     *            the bone's old memory address
     * @param boneContext
     *            the bones's context
     */
    public void setBoneContext(Long boneOMA, BoneContext boneContext) {
        boneContexts.put(boneOMA, boneContext);
    }

    /**
     * This method returns the bone context for the given bone old memory
     * address. If no context exists then <b>null</b> is returned.
     * 
     * @param boneOMA
     *            the bone's old memory address
     * @return bone's context
     */
    public BoneContext getBoneContext(Long boneOMA) {
        return boneContexts.get(boneOMA);
    }

    /**
     * Returns bone by given name.
     * 
     * @param skeletonOMA
     *            the OMA of the skeleton where the bone will be searched
     * @param name
     *            the name of the bone
     * @return found bone or null if none bone of a given name exists
     */
    public BoneContext getBoneByName(Long skeletonOMA, String name) {
        for (Entry<Long, BoneContext> entry : boneContexts.entrySet()) {
            if (entry.getValue().getArmatureObjectOMA().equals(skeletonOMA)) {
                Bone bone = entry.getValue().getBone();
                if (bone != null && name.equals(bone.getName())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Returns bone context for the given bone.
     * 
     * @param bone
     *            the bone
     * @return the bone's bone context
     */
    public BoneContext getBoneContext(Bone bone) {
        for (Entry<Long, BoneContext> entry : boneContexts.entrySet()) {
            if (entry.getValue().getBone().getName().equals(bone.getName())) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("Cannot find context for bone: " + bone);
    }

    /**
     * This metod returns the default material.
     * 
     * @return the default material
     */
    public synchronized Material getDefaultMaterial() {
        if (blenderKey.getDefaultMaterial() == null) {
            Material defaultMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            defaultMaterial.setColor("Color", ColorRGBA.DarkGray);
            blenderKey.setDefaultMaterial(defaultMaterial);
        }
        return blenderKey.getDefaultMaterial();
    }

    /**
     * Adds a custom marker for scene's feature.
     * 
     * @param marker
     *            the marker name
     * @param feature
     *            te scene's feature (can be node, material or texture or
     *            anything else)
     * @param markerValue
     *            the marker value
     */
    public void addMarker(String marker, Object feature, Object markerValue) {
        if (markerValue == null) {
            throw new IllegalArgumentException("The marker's value cannot be null.");
        }
        Map<Object, Object> markersMap = markers.get(marker);
        if (markersMap == null) {
            markersMap = new HashMap<Object, Object>();
            markers.put(marker, markersMap);
        }
        markersMap.put(feature, markerValue);
    }

    /**
     * Returns the marker value. The returned value is null if no marker was
     * defined for the given feature.
     * 
     * @param marker
     *            the marker name
     * @param feature
     *            the scene's feature
     * @return marker value or null if it was not defined
     */
    public Object getMarkerValue(String marker, Object feature) {
        Map<Object, Object> markersMap = markers.get(marker);
        return markersMap == null ? null : markersMap.get(feature);
    }

    /**
     * Adds blender action to the context.
     * @param action
     *            the action loaded from the blend file
     */
    public void addAction(BlenderAction action) {
        actions.put(action.getName(), action);
    }

    /**
     * @return a map of blender actions; the key is the action name and the value is action itself
     */
    public Map<String, BlenderAction> getActions() {
        return actions;
    }

    /**
     * This enum defines what loaded data type user wants to retrieve. It can be
     * either filled structure or already converted data.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static enum LoadedDataType {
        STRUCTURE, FEATURE, TEMPORAL_MESH;
    }
    
    @Override
    public String toString() {
        return blenderKey == null ? "BlenderContext [key = null]" : "BlenderContext [ key = " + blenderKey.toString() + " ]";
    }
}
