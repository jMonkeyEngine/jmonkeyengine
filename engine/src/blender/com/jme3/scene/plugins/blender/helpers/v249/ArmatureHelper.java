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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.Bone;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.structures.Ipo;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;

/**
 * This class defines the methods to calculate certain aspects of animation and armature functionalities.
 * @author Marcin Roguski
 */
public class ArmatureHelper extends AbstractBlenderHelper {
	private static final Logger				LOGGER			= Logger.getLogger(ArmatureHelper.class.getName());

	/**
	 * The map of the bones. Maps a bone name to its index in the armature. Should be cleared after the object had been
	 * read. TODO: probably bones can have identical names in different armatures
	 */
	protected Map<String, Integer>				bonesMap		= new HashMap<String, Integer>();
	/** A map of bones and their old memory addresses. */
	protected Map<Bone, Long>					bonesOMAs		= new HashMap<Bone, Long>();
	/** This list contains bones hierarchy and their matrices. It is later converted into jme bones. */
	protected List<BoneTransformationData>		boneDataRoots	= new ArrayList<BoneTransformationData>();
	
	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in
	 * different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public ArmatureHelper(String blenderVersion) {
		super(blenderVersion);
	}

	/**
	 * This method returns the old memory address of a bone. If the bone does not exist in the blend file - zero is
	 * returned.
	 * @param bone
	 *        the bone whose old memory address we seek
	 * @return the old memory address of the given bone
	 */
	public Long getBoneOMA(Bone bone) {
		Long result = bonesOMAs.get(bone);
		if(result == null) {
			result = Long.valueOf(0);
		}
		return result;
	}

	/**
	 * This method reads the bones and returns an empty skeleton. Bones should be assigned later.
	 * @param structure
	 *        armature structure
	 * @param dataRepository
	 *        the data repository
	 * @return an empty skeleton, bones are stored within the helper object
	 * @throws BlenderFileException
	 *         this exception is thrown when the blender file is somehow corrupted
	 */
	public Skeleton toArmature(Structure structure, DataRepository dataRepository) throws BlenderFileException {
		LOGGER.log(Level.INFO, "Converting structure to Armature!");
		Structure bonebase = (Structure)structure.getFieldValue("bonebase");
		List<Structure> bonesStructures = bonebase.evaluateListBase(dataRepository);
		for(Structure boneStructure : bonesStructures) {
			BoneTransformationData rootBoneTransformationData = this.readBoneAndItsChildren(boneStructure, null, dataRepository);
			boneDataRoots.add(rootBoneTransformationData);
		}
		return new Skeleton();//bones are assigned later
	}

	/**
	 * This method returns a map where the key is the object's group index that is used by a bone and the key is the
	 * bone index in the armature.
	 * @param poseStructure
	 *        a bPose structure of the object
	 * @return bone group-to-index map
	 * @throws BlenderFileException
	 *         this exception is thrown when the blender file is somehow corrupted
	 */
	public Map<Integer, Integer> getGroupToBoneIndexMap(Structure defBaseStructure, DataRepository dataRepository) throws BlenderFileException {
		Map<Integer, Integer> result = null;
		if(bonesMap != null && bonesMap.size() != 0) {
			result = new HashMap<Integer, Integer>();
			List<Structure> deformGroups = defBaseStructure.evaluateListBase(dataRepository);//bDeformGroup
			int groupIndex = 0;//TODO: consider many armatures attached to one object in the future !!!
			for(Structure deformGroup : deformGroups) {
				String deformGroupName = deformGroup.getFieldValue("name").toString();
				Integer boneIndex = bonesMap.get(deformGroupName);
				if(boneIndex != null) {
					result.put(Integer.valueOf(groupIndex), boneIndex);
				}
				++groupIndex;
			}
		}
		return result;
	}

	/**
	 * This method reads the tracks of the armature object.
	 * @param actionStructure
	 * @param dataRepository
	 *        the data repository
	 * @param objectName
	 *        the name of animation owner
	 * @param animationName
	 *        the name of the animation
	 * @return a list of tracks for the armature
	 * @throws BlenderFileException
	 *         this exception is thrown when the blender file is somehow corrupted
	 */
	public BoneTrack[] getTracks(Structure actionStructure, DataRepository dataRepository, String objectName, String animationName) throws BlenderFileException {
		LOGGER.log(Level.INFO, "Getting tracks!");
		IpoHelper ipoHelper = dataRepository.getHelper(IpoHelper.class);
		int fps = dataRepository.getBlenderKey().getFps();
		int[] animationFrames = dataRepository.getBlenderKey().getAnimationFrames(objectName, animationName);
		Structure chanbase = (Structure)actionStructure.getFieldValue("chanbase");
		List<Structure> actionChannels = chanbase.evaluateListBase(dataRepository);//bActionChannel
		if(actionChannels != null && actionChannels.size() > 0 && (bonesMap == null || bonesMap.size() == 0)) {
			throw new IllegalStateException("No bones found! Cannot proceed to calculating tracks!");
		}
		List<BoneTrack> tracks = new ArrayList<BoneTrack>();
		for(Structure bActionChannel : actionChannels) {
			String name = bActionChannel.getFieldValue("name").toString();
			Integer boneIndex = bonesMap.get(name);
			if(boneIndex != null) {
				Pointer p = (Pointer)bActionChannel.getFieldValue("ipo");
				if(!p.isNull()) {
					Structure ipoStructure = p.fetchData(dataRepository.getInputStream()).get(0);
					Ipo ipo = ipoHelper.createIpo(ipoStructure, dataRepository);
					tracks.add(ipo.calculateTrack(boneIndex.intValue(), animationFrames[0], animationFrames[1], fps));
				}
			}
		}
		return tracks.toArray(new BoneTrack[tracks.size()]);
	}

	/**
	 * This bone returns transformation matrix of the bone that is relative to
	 * its armature object.
	 * @param boneStructure the bone's structure
	 * @return bone's transformation matrix in armature space
	 */
	@SuppressWarnings("unchecked")
	protected Matrix4f getArmatureMatrix(Structure boneStructure) {
		DynamicArray<Number> boneMat = (DynamicArray<Number>)boneStructure.getFieldValue("arm_mat");
		Matrix4f m = new Matrix4f();
		for(int i = 0; i < 4; ++i) {
			for(int j = 0; j < 4; ++j) {
				m.set(i, j, boneMat.get(j, i).floatValue());
			}
		}
		return m;
	}

	/**
	 * This method reads the bone with its children.
	 * @param boneStructure
	 *        a structure containing the bone data
	 * @param parent
	 *        the bone parent; if null then we read the root bone
	 * @param dataRepository
	 *        the data repository
	 * @return the bone transformation data; contains bone chierarchy and the bone's matrices
	 * @throws BlenderFileException
	 *         this exception is thrown when the blender file is somehow corrupted
	 */
	@SuppressWarnings("unchecked")
	protected BoneTransformationData readBoneAndItsChildren(Structure boneStructure, BoneTransformationData parent, DataRepository dataRepository) throws BlenderFileException {
		String name = boneStructure.getFieldValue("name").toString();
		Bone bone = new Bone(name);
		int bonesAmount = bonesOMAs.size();
		bonesOMAs.put(bone, boneStructure.getOldMemoryAddress());
		if(bonesAmount == bonesOMAs.size()) {
			throw new IllegalStateException("Two bones has the same hash value and thereforw a bone was overriden in the bones<->OMA map! Improve the hash algorithm!");
		}
		Matrix4f boneArmatureMatrix = this.getArmatureMatrix(boneStructure);
		DynamicArray<Float> sizeArray = (DynamicArray<Float>) boneStructure.getFieldValue("size");
		Vector3f size = new Vector3f(sizeArray.get(0), sizeArray.get(1), sizeArray.get(2));
		BoneTransformationData boneTransformationData = new BoneTransformationData(boneArmatureMatrix, size, bone, parent);
		dataRepository.addLoadedFeatures(boneStructure.getOldMemoryAddress(), name, boneStructure, bone);

		Structure childbase = (Structure)boneStructure.getFieldValue("childbase");
		List<Structure> children = childbase.evaluateListBase(dataRepository);//Bone
		for(Structure boneChild : children) {
			this.readBoneAndItsChildren(boneChild, boneTransformationData, dataRepository);
		}
		return boneTransformationData;
	}

	/**
	 * This method assigns transformations to the bone.
	 * @param btd
	 *        the bone data containing the bone we assign transformation to
	 * @param additionalRootBoneTransformation
	 *        additional bone transformation which indicates it's mesh parent and armature object transformations
	 * @param boneList
	 *        a list of all read bones
	 */
	protected void assignBonesMatrices(BoneTransformationData btd, Matrix4f additionalRootBoneTransformation, List<Bone> boneList) {
		LOGGER.info("[" + btd.bone.getName() + "]  additionalRootBoneTransformation =\n" + additionalRootBoneTransformation);
		Matrix4f totalInverseParentMatrix = btd.parent != null ? btd.parent.totalInverseBoneParentMatrix : Matrix4f.IDENTITY;
		LOGGER.info("[" + btd.bone.getName() + "]  totalInverseParentMatrix =\n" + totalInverseParentMatrix);
		Matrix4f restMatrix = additionalRootBoneTransformation.mult(btd.boneArmatureMatrix);
		LOGGER.info("[" + btd.bone.getName() + "]  restMatrix =\n" + restMatrix);
		btd.totalInverseBoneParentMatrix = restMatrix.clone().invert();
		restMatrix = totalInverseParentMatrix.mult(restMatrix);
		LOGGER.info("[" + btd.bone.getName() + "]  resultMatrix =\n" + restMatrix);
		btd.bone.setBindTransforms(restMatrix.toTranslationVector(), restMatrix.toRotationQuat(), btd.size);
		boneList.add(btd.bone);
		bonesMap.put(btd.bone.getName(), Integer.valueOf(boneList.size() - 1));
		if(btd.children != null && btd.children.size() > 0) {
			for(BoneTransformationData child : btd.children) {
				this.assignBonesMatrices(child, additionalRootBoneTransformation, boneList);
				btd.bone.addChild(child.bone);
			}
		}
	}

	/**
	 * This method returns bone transformation data for the bone of a given name.
	 * @param boneName
	 *        the name of the bone
	 * @return bone's transformation data
	 */
	public BoneTransformationData getBoneTransformationDataRoot(int index) {
		return boneDataRoots.get(index);
	}

	/**
	 * This method returns the amount of bones transformations roots.
	 * @return the amount of bones transformations roots
	 */
	public int getBoneTransformationDataRootsSize() {
		return boneDataRoots.size();
	}

	/**
	 * This class holds the data needed later for bone transformation calculation and to bind parent with children.
	 * @author Marcin Roguski
	 */
	private static class BoneTransformationData {
		/** Inverse matrix of bone's parent bone. */
		private Matrix4f						totalInverseBoneParentMatrix;
		/** Bone's matrix in armature's space. */
		private Matrix4f						boneArmatureMatrix;
		/** Bone's size (apparently it is held outside the transformation matrix. */
		private Vector3f						size;
		/** The bone the data applies to. */
		private Bone							bone;
		/** The parent of the above mentioned bone (not assigned yet). */
		private BoneTransformationData			parent;
		/** The children of the current bone. */
		private List<BoneTransformationData>	children;

		/**
		 * Private constructor creates the object and assigns the given data.
		 * @param boneArmatureMatrix
		 *        the matrix of the current bone
		 * @param size
		 * 		  the bone's size
		 * @param bone
		 *        the current bone
		 * @param parent
		 *        the parent structure of the bone
		 */
		private BoneTransformationData(Matrix4f boneArmatureMatrix, Vector3f size, Bone bone, BoneTransformationData parent) {
			this.boneArmatureMatrix = boneArmatureMatrix;
			this.size = size;
			this.bone = bone;
			this.parent = parent;
			this.children = new ArrayList<ArmatureHelper.BoneTransformationData>();
			if(this.parent != null) {
				this.parent.children.add(this);
			}
		}
	}

	/**
	 * This method creates the whole bones structure. Assignes transformations to bones and combines children with
	 * parents.
	 * @param armatureOMA
	 *        old memory address of bones' armature object
	 * @param additionalRootBoneTransformation
	 *        additional bone transformation which indicates it's mesh parent and armature object transformations
	 * @return
	 */
	public Bone[] buildBonesStructure(Long armatureOMA, Matrix4f additionalRootBoneTransformation) {//TODO: uwzględnić wiele szkieletów
		List<Bone> bones = new ArrayList<Bone>(boneDataRoots.size() + 1);
		bones.add(new Bone(null));
		for(BoneTransformationData btd : boneDataRoots) {
			this.assignBonesMatrices(btd, additionalRootBoneTransformation, bones);
		}
		return bones.toArray(new Bone[bones.size()]);
	}
	
	/**
	 * This method assigns an immovable bone to vertices that have no bone assigned. They have the bone index with the
	 * value -1.
	 * @param immovableBoneIndex
	 *        the ondex of immovable bone
	 * @param meshes
	 *        a list of meshes whose vertices will be assigned to immovable bone
	 */
	public void assignBoneToOrphanedVertices(byte immovableBoneIndex, Mesh[] meshes) {
		//bone indices are common for all the object's meshes (vertex indices specify which are to be used)
		VertexBuffer boneIndices = meshes[0].getBuffer(Type.BoneIndex);//common buffer to all the meshes
		ByteBuffer data = (ByteBuffer)boneIndices.getData();
		for(int i = 0; i < boneIndices.getNumElements(); ++i) {
			if(data.get(i) == -1) {
				data.put(i, immovableBoneIndex);
			}
		}
	}

	@Override
	public void clearState() {
		bonesMap.clear();
		boneDataRoots.clear();
	}
}
