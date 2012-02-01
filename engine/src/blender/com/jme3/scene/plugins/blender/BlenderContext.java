/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Skeleton;
import com.jme3.asset.AssetManager;
import com.jme3.asset.BlenderKey;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.blender.animations.BoneContext;
import com.jme3.scene.plugins.blender.animations.Ipo;
import com.jme3.scene.plugins.blender.constraints.Constraint;
import com.jme3.scene.plugins.blender.file.BlenderInputStream;
import com.jme3.scene.plugins.blender.file.DnaBlockData;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.meshes.MeshContext;
import com.jme3.scene.plugins.blender.modifiers.Modifier;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * The class that stores temporary data and manages it during loading the belnd
 * file. This class is intended to be used in a single loading thread. It holds
 * the state of loading operations.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class BlenderContext {
	private static final Logger					LOGGER					= Logger.getLogger(BlenderContext.class.getName());

	/** The blender file version. */
	private int									blenderVersion;
	/** The blender key. */
	private BlenderKey							blenderKey;
	/** The header of the file block. */
	private DnaBlockData						dnaBlockData;
	/** The input stream of the blend file. */
	private BlenderInputStream					inputStream;
	/** The asset manager. */
	private AssetManager						assetManager;
	/**
	 * A map containing the file block headers. The key is the old pointer
	 * address.
	 */
	private Map<Long, FileBlockHeader>			fileBlockHeadersByOma	= new HashMap<Long, FileBlockHeader>();
	/** A map containing the file block headers. The key is the block code. */
	private Map<Integer, List<FileBlockHeader>>	fileBlockHeadersByCode	= new HashMap<Integer, List<FileBlockHeader>>();
	/**
	 * This map stores the loaded features by their old memory address. The
	 * first object in the value table is the loaded structure and the second -
	 * the structure already converted into proper data.
	 */
	private Map<Long, Object[]>					loadedFeatures			= new HashMap<Long, Object[]>();
	/**
	 * This map stores the loaded features by their name. Only features with ID
	 * structure can be stored here. The first object in the value table is the
	 * loaded structure and the second - the structure already converted into
	 * proper data.
	 */
	private Map<String, Object[]>				loadedFeaturesByName	= new HashMap<String, Object[]>();
	/** A stack that hold the parent structure of currently loaded feature. */
	private Stack<Structure>					parentStack				= new Stack<Structure>();
	/**
	 * A map storing loaded ipos. The key is the ipo's owner old memory address
	 * and the value is the ipo.
	 */
	private Map<Long, Ipo>						loadedIpos				= new HashMap<Long, Ipo>();
	/** A list of modifiers for the specified object. */
	protected Map<Long, List<Modifier>>			modifiers				= new HashMap<Long, List<Modifier>>();
	/** A list of constraints for the specified object. */
	protected Map<Long, List<Constraint>>		constraints				= new HashMap<Long, List<Constraint>>();
	/** Anim data loaded for features. */
	private Map<Long, AnimData>					animData				= new HashMap<Long, AnimData>();
	/** Loaded skeletons. */
	private Map<Long, Skeleton>					skeletons				= new HashMap<Long, Skeleton>();
	/** A map of mesh contexts. */
	protected Map<Long, MeshContext>			meshContexts			= new HashMap<Long, MeshContext>();
	/** A map of bone contexts. */
	protected Map<Long, BoneContext>			boneContexts			= new HashMap<Long, BoneContext>();
	/** A map of material contexts. */
	protected Map<Material, MaterialContext>	materialContexts		= new HashMap<Material, MaterialContext>();
	/** A map og helpers that perform loading. */
	private Map<String, AbstractBlenderHelper>	helpers					= new HashMap<String, AbstractBlenderHelper>();

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
		fileBlockHeadersByOma.put(oldMemoryAddress, fileBlockHeader);
		List<FileBlockHeader> headers = fileBlockHeadersByCode.get(Integer.valueOf(fileBlockHeader.getCode()));
		if (headers == null) {
			headers = new ArrayList<FileBlockHeader>();
			fileBlockHeadersByCode.put(Integer.valueOf(fileBlockHeader.getCode()), headers);
		}
		headers.add(fileBlockHeader);
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
	public List<FileBlockHeader> getFileBlocks(Integer code) {
		return fileBlockHeadersByCode.get(code);
	}

	/**
	 * This method clears the saved block headers stored in the features map.
	 */
	public void clearFileBlocks() {
		fileBlockHeadersByOma.clear();
		fileBlockHeadersByCode.clear();
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
	 * @param featureName
	 *            the name of the feature
	 * @param structure
	 *            the filled structure of the feature
	 * @param feature
	 *            the feature we want to store
	 */
	public void addLoadedFeatures(Long oldMemoryAddress, String featureName, Structure structure, Object feature) {
		if (oldMemoryAddress == null || structure == null || feature == null) {
			throw new IllegalArgumentException("One of the given arguments is null!");
		}
		Object[] storedData = new Object[] { structure, feature };
		loadedFeatures.put(oldMemoryAddress, storedData);
		if (featureName != null) {
			loadedFeaturesByName.put(featureName, storedData);
		}
	}

	/**
	 * This method returns the feature of a given memory address. If the feature
	 * is not yet loaded then null is returned.
	 * 
	 * @param oldMemoryAddress
	 *            the address of the feature
	 * @param loadedFeatureDataType
	 *            the type of data we want to retreive it can be either filled
	 *            structure or already converted feature
	 * @return loaded feature or null if it was not yet loaded
	 */
	public Object getLoadedFeature(Long oldMemoryAddress, LoadedFeatureDataType loadedFeatureDataType) {
		Object[] result = loadedFeatures.get(oldMemoryAddress);
		if (result != null) {
			return result[loadedFeatureDataType.getIndex()];
		}
		return null;
	}

	/**
	 * This method returns the feature of a given name. If the feature is not
	 * yet loaded then null is returned.
	 * 
	 * @param featureName
	 *            the name of the feature
	 * @param loadedFeatureDataType
	 *            the type of data we want to retreive it can be either filled
	 *            structure or already converted feature
	 * @return loaded feature or null if it was not yet loaded
	 */
	public Object getLoadedFeature(String featureName, LoadedFeatureDataType loadedFeatureDataType) {
		Object[] result = loadedFeaturesByName.get(featureName);
		if (result != null) {
			return result[loadedFeatureDataType.getIndex()];
		}
		return null;
	}

	/**
	 * This method clears the saved features stored in the features map.
	 */
	public void clearLoadedFeatures() {
		loadedFeatures.clear();
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
	 * This method retreives the structure at the top of the parent's stack but
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
	 * This method adds new ipo curve for the feature.
	 * 
	 * @param ownerOMA
	 *            the OMA of blender feature that owns the ipo
	 * @param ipo
	 *            the ipo to be added
	 */
	public void addIpo(Long ownerOMA, Ipo ipo) {
		loadedIpos.put(ownerOMA, ipo);
	}

	/**
	 * This method removes the ipo curve from the feature.
	 * 
	 * @param ownerOma
	 *            the OMA of blender feature that owns the ipo
	 */
	public Ipo removeIpo(Long ownerOma) {
		return loadedIpos.remove(ownerOma);
	}

	/**
	 * This method returns the ipo curve of the feature.
	 * 
	 * @param ownerOMA
	 *            the OMA of blender feature that owns the ipo
	 */
	public Ipo getIpo(Long ownerOMA) {
		return loadedIpos.get(ownerOMA);
	}

	/**
	 * This method adds a new modifier to the list.
	 * 
	 * @param ownerOMA
	 *            the owner's old memory address
	 * @param modifier
	 *            the object's modifier
	 */
	public void addModifier(Long ownerOMA, Modifier modifier) {
		List<Modifier> objectModifiers = this.modifiers.get(ownerOMA);
		if (objectModifiers == null) {
			objectModifiers = new ArrayList<Modifier>();
			this.modifiers.put(ownerOMA, objectModifiers);
		}
		objectModifiers.add(modifier);
	}

	/**
	 * This method returns modifiers for the object specified by its old memory
	 * address and the modifier type. If no modifiers are found - empty list is
	 * returned. If the type is null - all modifiers for the object are
	 * returned.
	 * 
	 * @param objectOMA
	 *            object's old memory address
	 * @param type
	 *            the type of the modifier
	 * @return the list of object's modifiers
	 */
	public List<Modifier> getModifiers(Long objectOMA, String type) {
		List<Modifier> result = new ArrayList<Modifier>();
		List<Modifier> readModifiers = modifiers.get(objectOMA);
		if (readModifiers != null && readModifiers.size() > 0) {
			for (Modifier modifier : readModifiers) {
				if (type == null || type.isEmpty() || modifier.getType().equals(type)) {
					result.add(modifier);
				}
			}
		}
		return result;
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
	 * This method returns constraints for the object specified by its old
	 * memory address. If no modifiers are found - <b>null</b> is returned.
	 * 
	 * @param objectOMA
	 *            object's old memory address
	 * @return the list of object's modifiers or null
	 */
	public List<Constraint> getConstraints(Long objectOMA) {
		return objectOMA == null ? null : constraints.get(objectOMA);
	}

	/**
	 * This method sets the anim data for the specified OMA of its owner.
	 * 
	 * @param ownerOMA
	 *            the owner's old memory address
	 * @param animData
	 *            the animation data for the feature specified by ownerOMA
	 */
	public void setAnimData(Long ownerOMA, AnimData animData) {
		this.animData.put(ownerOMA, animData);
	}

	/**
	 * This method returns the animation data for the specified owner.
	 * 
	 * @param ownerOMA
	 *            the old memory address of the animation data owner
	 * @return the animation data or null if none exists
	 */
	public AnimData getAnimData(Long ownerOMA) {
		return this.animData.get(ownerOMA);
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
		this.skeletons.put(skeletonOMA, skeleton);
	}

	/**
	 * This method returns the skeleton for the specified OMA of its owner.
	 * 
	 * @param skeletonOMA
	 *            the skeleton's old memory address
	 * @return the skeleton specified by the given OMA
	 */
	public Skeleton getSkeleton(Long skeletonOMA) {
		return this.skeletons.get(skeletonOMA);
	}

	/**
	 * This method sets the mesh context for the given mesh old memory address.
	 * If the context is already set it will be replaced.
	 * 
	 * @param meshOMA
	 *            the mesh's old memory address
	 * @param meshContext
	 *            the mesh's context
	 */
	public void setMeshContext(Long meshOMA, MeshContext meshContext) {
		this.meshContexts.put(meshOMA, meshContext);
	}

	/**
	 * This method returns the mesh context for the given mesh old memory
	 * address. If no context exists then <b>null</b> is returned.
	 * 
	 * @param meshOMA
	 *            the mesh's old memory address
	 * @return mesh's context
	 */
	public MeshContext getMeshContext(Long meshOMA) {
		return this.meshContexts.get(meshOMA);
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
		this.boneContexts.put(boneOMA, boneContext);
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
	 * This method sets the material context for the given material. If the
	 * context is already set it will be replaced.
	 * 
	 * @param material
	 *            the material
	 * @param materialContext
	 *            the material's context
	 */
	public void setMaterialContext(Material material, MaterialContext materialContext) {
		this.materialContexts.put(material, materialContext);
	}

	/**
	 * This method returns the material context for the given material. If no
	 * context exists then <b>null</b> is returned.
	 * 
	 * @param material
	 *            the material
	 * @return material's context
	 */
	public MaterialContext getMaterialContext(Material material) {
		return materialContexts.get(material);
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

	public void dispose() {
		try {
			inputStream.close();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
		}
		loadedFeatures.clear();
		loadedFeaturesByName.clear();
	}

	/**
	 * This enum defines what loaded data type user wants to retreive. It can be
	 * either filled structure or already converted data.
	 * 
	 * @author Marcin Roguski
	 */
	public static enum LoadedFeatureDataType {

		LOADED_STRUCTURE(0), LOADED_FEATURE(1);
		private int	index;

		private LoadedFeatureDataType(int index) {
			this.index = index;
		}

		public int getIndex() {
			return index;
		}
	}
}
