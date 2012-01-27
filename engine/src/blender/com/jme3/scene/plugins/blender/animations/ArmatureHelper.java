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
package com.jme3.scene.plugins.blender.animations;

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
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.curves.BezierCurve;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class defines the methods to calculate certain aspects of animation and
 * armature functionalities.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ArmatureHelper extends AbstractBlenderHelper {
	private static final Logger	LOGGER		= Logger.getLogger(ArmatureHelper.class.getName());

	/** A map of bones and their old memory addresses. */
	private Map<Bone, Long>		bonesOMAs	= new HashMap<Bone, Long>();

	/**
	 * This constructor parses the given blender version and stores the result.
	 * Some functionalities may differ in different blender versions.
	 * 
	 * @param blenderVersion
	 *            the version read from the blend file
	 * @param fixUpAxis
	 *            a variable that indicates if the Y asxis is the UP axis or not
	 */
	public ArmatureHelper(String blenderVersion, boolean fixUpAxis) {
		super(blenderVersion, fixUpAxis);
	}

	/**
	 * This method builds the object's bones structure.
	 * 
	 * @param boneStructure
	 *            the structure containing the bones' data
	 * @param parent
	 *            the parent bone
	 * @param result
	 *            the list where the newly created bone will be added
	 * @param bonesPoseChannels
	 *            a map of bones poses channels
	 * @param blenderContext
	 *            the blender context
	 * @throws BlenderFileException
	 *             an exception is thrown when there is problem with the blender
	 *             file
	 */
	public void buildBones(Structure boneStructure, Bone parent, List<Bone> result, Matrix4f arbt, final Map<Long, Structure> bonesPoseChannels, BlenderContext blenderContext) throws BlenderFileException {
		BoneContext bc = new BoneContext(boneStructure, arbt, bonesPoseChannels, blenderContext);
		bc.buildBone(result, bonesOMAs, blenderContext);
	}

	/**
	 * This method returns the old memory address of a bone. If the bone does
	 * not exist in the blend file - zero is returned.
	 * 
	 * @param bone
	 *            the bone whose old memory address we seek
	 * @return the old memory address of the given bone
	 */
	public Long getBoneOMA(Bone bone) {
		Long result = bonesOMAs.get(bone);
		if (result == null) {
			result = Long.valueOf(0);
		}
		return result;
	}

	/**
	 * This method returns a map where the key is the object's group index that
	 * is used by a bone and the key is the bone index in the armature.
	 * 
	 * @param defBaseStructure
	 *            a bPose structure of the object
	 * @return bone group-to-index map
	 * @throws BlenderFileException
	 *             this exception is thrown when the blender file is somehow
	 *             corrupted
	 */
	public Map<Integer, Integer> getGroupToBoneIndexMap(Structure defBaseStructure, Skeleton skeleton, BlenderContext blenderContext) throws BlenderFileException {
		Map<Integer, Integer> result = null;
		if (skeleton.getBoneCount() != 0) {
			result = new HashMap<Integer, Integer>();
			List<Structure> deformGroups = defBaseStructure.evaluateListBase(blenderContext);// bDeformGroup
			int groupIndex = 0;
			for (Structure deformGroup : deformGroups) {
				String deformGroupName = deformGroup.getFieldValue("name").toString();
				Integer boneIndex = this.getBoneIndex(skeleton, deformGroupName);
				if (boneIndex != null) {
					result.put(Integer.valueOf(groupIndex), boneIndex);
				}
				++groupIndex;
			}
		}
		return result;
	}

	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return true;
	}

	/**
	 * This method retuns the bone tracks for animation.
	 * 
	 * @param actionStructure
	 *            the structure containing the tracks
	 * @param blenderContext
	 *            the blender context
	 * @return a list of tracks for the specified animation
	 * @throws BlenderFileException
	 *             an exception is thrown when there are problems with the blend
	 *             file
	 */
	public BoneTrack[] getTracks(Structure actionStructure, Skeleton skeleton, BlenderContext blenderContext) throws BlenderFileException {
		if (blenderVersion < 250) {
			return this.getTracks249(actionStructure, skeleton, blenderContext);
		} else {
			return this.getTracks250(actionStructure, skeleton, blenderContext);
		}
	}

	/**
	 * This method retuns the bone tracks for animation for blender version 2.50
	 * and higher.
	 * 
	 * @param actionStructure
	 *            the structure containing the tracks
	 * @param blenderContext
	 *            the blender context
	 * @return a list of tracks for the specified animation
	 * @throws BlenderFileException
	 *             an exception is thrown when there are problems with the blend
	 *             file
	 */
	private BoneTrack[] getTracks250(Structure actionStructure, Skeleton skeleton, BlenderContext blenderContext) throws BlenderFileException {
		LOGGER.log(Level.INFO, "Getting tracks!");
		IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
		int fps = blenderContext.getBlenderKey().getFps();
		Structure groups = (Structure) actionStructure.getFieldValue("groups");
		List<Structure> actionGroups = groups.evaluateListBase(blenderContext);// bActionGroup
		List<BoneTrack> tracks = new ArrayList<BoneTrack>();
		for (Structure actionGroup : actionGroups) {
			String name = actionGroup.getFieldValue("name").toString();
			int boneIndex = this.getBoneIndex(skeleton, name);
			if (boneIndex >= 0) {
				List<Structure> channels = ((Structure) actionGroup.getFieldValue("channels")).evaluateListBase(blenderContext);
				BezierCurve[] bezierCurves = new BezierCurve[channels.size()];
				int channelCounter = 0;
				for (Structure c : channels) {
					int type = ipoHelper.getCurveType(c, blenderContext);
					Pointer pBezTriple = (Pointer) c.getFieldValue("bezt");
					List<Structure> bezTriples = pBezTriple.fetchData(blenderContext.getInputStream());
					bezierCurves[channelCounter++] = new BezierCurve(type, bezTriples, 2);
				}

				Ipo ipo = new Ipo(bezierCurves, fixUpAxis);
				tracks.add((BoneTrack) ipo.calculateTrack(boneIndex, 0, ipo.getLastFrame(), fps, false));
			}
		}
		return tracks.toArray(new BoneTrack[tracks.size()]);
	}

	/**
	 * This method retuns the bone tracks for animation for blender version 2.49
	 * (and probably several lower versions too).
	 * 
	 * @param actionStructure
	 *            the structure containing the tracks
	 * @param blenderContext
	 *            the blender context
	 * @return a list of tracks for the specified animation
	 * @throws BlenderFileException
	 *             an exception is thrown when there are problems with the blend
	 *             file
	 */
	private BoneTrack[] getTracks249(Structure actionStructure, Skeleton skeleton, BlenderContext blenderContext) throws BlenderFileException {
		LOGGER.log(Level.INFO, "Getting tracks!");
		IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
		int fps = blenderContext.getBlenderKey().getFps();
		Structure chanbase = (Structure) actionStructure.getFieldValue("chanbase");
		List<Structure> actionChannels = chanbase.evaluateListBase(blenderContext);// bActionChannel
		List<BoneTrack> tracks = new ArrayList<BoneTrack>();
		for (Structure bActionChannel : actionChannels) {
			String name = bActionChannel.getFieldValue("name").toString();
			int boneIndex = this.getBoneIndex(skeleton, name);
			if (boneIndex >= 0) {
				Pointer p = (Pointer) bActionChannel.getFieldValue("ipo");
				if (!p.isNull()) {
					Structure ipoStructure = p.fetchData(blenderContext.getInputStream()).get(0);
					Ipo ipo = ipoHelper.fromIpoStructure(ipoStructure, blenderContext);
					tracks.add((BoneTrack) ipo.calculateTrack(boneIndex, 0, ipo.getLastFrame(), fps, false));
				}
			}
		}
		return tracks.toArray(new BoneTrack[tracks.size()]);
	}

	/**
	 * This method returns the index of the bone in the given skeleton.
	 * 
	 * @param skeleton
	 *            the skeleton
	 * @param boneName
	 *            the name of the bone
	 * @return the index of the bone
	 */
	private int getBoneIndex(Skeleton skeleton, String boneName) {
		int result = -1;
		for (int i = 0; i < skeleton.getBoneCount() && result == -1; ++i) {
			if (boneName.equals(skeleton.getBone(i).getName())) {
				result = i;
			}
		}
		return result;
	}
}
