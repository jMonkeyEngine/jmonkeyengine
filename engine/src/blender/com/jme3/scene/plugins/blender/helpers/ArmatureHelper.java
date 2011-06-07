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
package com.jme3.scene.plugins.blender.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.animation.BoneTrack;
import com.jme3.scene.plugins.blender.data.FileBlockHeader;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.structures.BezierCurve;
import com.jme3.scene.plugins.blender.structures.Ipo;
import com.jme3.scene.plugins.blender.utils.BlenderInputStream;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.Pointer;


/**
 * This class defines the methods to calculate certain aspects of animation and armature functionalities.
 * @author Marcin Roguski
 */
public class ArmatureHelper extends com.jme3.scene.plugins.blender.helpers.v249.ArmatureHelper {
	private static final Logger				LOGGER			= Logger.getLogger(ArmatureHelper.class.getName());
	
	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in
	 * different blender versions.
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public ArmatureHelper(String blenderVersion) {
		super(blenderVersion);
	}
	
	@Override
	public BoneTrack[] getTracks(Structure actionStructure, DataRepository dataRepository, String objectName, String animationName) throws BlenderFileException {
		if(blenderVersion<250) {
			return super.getTracks(actionStructure, dataRepository, objectName, animationName);
		}
		LOGGER.log(Level.INFO, "Getting tracks!");
		int fps = dataRepository.getBlenderKey().getFps();
		int[] animationFrames = dataRepository.getBlenderKey().getAnimationFrames(objectName, animationName);
		Structure groups = (Structure)actionStructure.getFieldValue("groups");
		List<Structure> actionGroups = groups.evaluateListBase(dataRepository);//bActionGroup
		if(actionGroups != null && actionGroups.size() > 0 && (bonesMap == null || bonesMap.size() == 0)) {
			throw new IllegalStateException("No bones found! Cannot proceed to calculating tracks!");
		}
		
		List<BoneTrack> tracks = new ArrayList<BoneTrack>();
		for(Structure actionGroup : actionGroups) {
			String name = actionGroup.getFieldValue("name").toString();
			Integer boneIndex = bonesMap.get(name);
			if(boneIndex != null) {
				List<Structure> channels = ((Structure)actionGroup.getFieldValue("channels")).evaluateListBase(dataRepository);
				BezierCurve[] bezierCurves = new BezierCurve[channels.size()];
				int channelCounter = 0;
				for(Structure c : channels) {
					//reading rna path first
					BlenderInputStream bis = dataRepository.getInputStream();
					int currentPosition = bis.getPosition();
					Pointer pRnaPath = (Pointer) c.getFieldValue("rna_path");
					FileBlockHeader dataFileBlock = dataRepository.getFileBlock(pRnaPath.getOldMemoryAddress());
					bis.setPosition(dataFileBlock.getBlockPosition());
					String rnaPath = bis.readString();
					bis.setPosition(currentPosition);
					int arrayIndex = ((Number)c.getFieldValue("array_index")).intValue();
					int type = this.getCurveType(rnaPath, arrayIndex);

					Pointer pBezTriple = (Pointer)c.getFieldValue("bezt");
					List<Structure> bezTriples = pBezTriple.fetchData(dataRepository.getInputStream());
					bezierCurves[channelCounter++] = new BezierCurve(type, bezTriples, 2);
				}

				Ipo ipo = new Ipo(bezierCurves);
				tracks.add(ipo.calculateTrack(boneIndex.intValue(), animationFrames[0], animationFrames[1], fps));
			}
		}
		return tracks.toArray(new BoneTrack[tracks.size()]);
	}
	
	/**
	 * This method parses the information stored inside the curve rna path and returns the proper type
	 * of the curve.
	 * @param rnaPath the curve's rna path
	 * @param arrayIndex the array index of the stored data
	 * @return the type of the curve
	 */
	protected int getCurveType(String rnaPath, int arrayIndex) {
		if(rnaPath.endsWith(".location")) {
			return Ipo.AC_LOC_X + arrayIndex;
		}
		if(rnaPath.endsWith(".rotation_quaternion")) {
			return Ipo.AC_QUAT_W + arrayIndex;
		}
		if(rnaPath.endsWith(".scale")) {
			return Ipo.AC_SIZE_X + arrayIndex;
		}
		throw new IllegalStateException("Unknown curve rna path: " + rnaPath);
	}
}
