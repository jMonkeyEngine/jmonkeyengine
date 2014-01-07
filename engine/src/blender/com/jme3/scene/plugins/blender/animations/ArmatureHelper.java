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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.curves.BezierCurve;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;

/**
 * This class defines the methods to calculate certain aspects of animation and
 * armature functionalities.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class ArmatureHelper extends AbstractBlenderHelper {
    private static final Logger LOGGER               = Logger.getLogger(ArmatureHelper.class.getName());

    public static final String  ARMATURE_NODE_MARKER = "armature-node";

    /**
     * This constructor parses the given blender version and stores the result.
     * Some functionalities may differ in different blender versions.
     * 
     * @param blenderVersion
     *            the version read from the blend file
     * @param blenderContext
     *            the blender context
     */
    public ArmatureHelper(String blenderVersion, BlenderContext blenderContext) {
        super(blenderVersion, blenderContext);
    }

    /**
     * This method builds the object's bones structure.
     * 
     * @param armatureObjectOMA
     *            the OMa of the armature node
     * @param boneStructure
     *            the structure containing the bones' data
     * @param parent
     *            the parent bone
     * @param result
     *            the list where the newly created bone will be added
     * @param spatialOMA
     *            the OMA of the spatial that will own the skeleton
     * @param blenderContext
     *            the blender context
     * @throws BlenderFileException
     *             an exception is thrown when there is problem with the blender
     *             file
     */
    public void buildBones(Long armatureObjectOMA, Structure boneStructure, Bone parent, List<Bone> result, Long spatialOMA, BlenderContext blenderContext) throws BlenderFileException {
        BoneContext bc = new BoneContext(armatureObjectOMA, boneStructure, blenderContext);
        bc.buildBone(result, spatialOMA, blenderContext);
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
    public Map<Integer, Integer> getGroupToBoneIndexMap(Structure defBaseStructure, Skeleton skeleton) throws BlenderFileException {
        Map<Integer, Integer> result = null;
        if (skeleton.getBoneCount() != 0) {
            result = new HashMap<Integer, Integer>();
            List<Structure> deformGroups = defBaseStructure.evaluateListBase();// bDeformGroup
            int groupIndex = 0;
            for (Structure deformGroup : deformGroups) {
                String deformGroupName = deformGroup.getFieldValue("name").toString();
                int boneIndex = skeleton.getBoneIndex(deformGroupName);
                if (boneIndex >= 0) {
                    result.put(groupIndex, boneIndex);
                }
                ++groupIndex;
            }
        }
        return result;
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
        LOGGER.log(Level.FINE, "Getting tracks!");
        IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
        int fps = blenderContext.getBlenderKey().getFps();
        Structure groups = (Structure) actionStructure.getFieldValue("groups");
        List<Structure> actionGroups = groups.evaluateListBase();// bActionGroup
        List<BoneTrack> tracks = new ArrayList<BoneTrack>();
        for (Structure actionGroup : actionGroups) {
            String name = actionGroup.getFieldValue("name").toString();
            int boneIndex = skeleton.getBoneIndex(name);
            if (boneIndex >= 0) {
                List<Structure> channels = ((Structure) actionGroup.getFieldValue("channels")).evaluateListBase();
                BezierCurve[] bezierCurves = new BezierCurve[channels.size()];
                int channelCounter = 0;
                for (Structure c : channels) {
                    int type = ipoHelper.getCurveType(c, blenderContext);
                    Pointer pBezTriple = (Pointer) c.getFieldValue("bezt");
                    List<Structure> bezTriples = pBezTriple.fetchData();
                    bezierCurves[channelCounter++] = new BezierCurve(type, bezTriples, 2);
                }

                Bone bone = skeleton.getBone(boneIndex);
                Ipo ipo = new Ipo(bezierCurves, fixUpAxis, blenderContext.getBlenderVersion());
                tracks.add((BoneTrack) ipo.calculateTrack(boneIndex, bone.getLocalPosition(), bone.getLocalRotation(), bone.getLocalScale(), 1, ipo.getLastFrame(), fps, false));
            }
        }
        this.equaliseBoneTracks(tracks);
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
        LOGGER.log(Level.FINE, "Getting tracks!");
        IpoHelper ipoHelper = blenderContext.getHelper(IpoHelper.class);
        int fps = blenderContext.getBlenderKey().getFps();
        Structure chanbase = (Structure) actionStructure.getFieldValue("chanbase");
        List<Structure> actionChannels = chanbase.evaluateListBase();// bActionChannel
        List<BoneTrack> tracks = new ArrayList<BoneTrack>();
        for (Structure bActionChannel : actionChannels) {
            String name = bActionChannel.getFieldValue("name").toString();
            int boneIndex = skeleton.getBoneIndex(name);
            if (boneIndex >= 0) {
                Pointer p = (Pointer) bActionChannel.getFieldValue("ipo");
                if (!p.isNull()) {
                    Structure ipoStructure = p.fetchData().get(0);

                    Bone bone = skeleton.getBone(boneIndex);
                    Ipo ipo = ipoHelper.fromIpoStructure(ipoStructure, blenderContext);
                    if (ipo != null) {
                        tracks.add((BoneTrack) ipo.calculateTrack(boneIndex, bone.getLocalPosition(), bone.getLocalRotation(), bone.getLocalScale(), 1, ipo.getLastFrame(), fps, false));
                    }
                }
            }
        }
        this.equaliseBoneTracks(tracks);
        return tracks.toArray(new BoneTrack[tracks.size()]);
    }

    /**
     * The method makes all the tracks to have equal frame lengths.
     * @param tracks
     *            the tracks to be equalized
     */
    private void equaliseBoneTracks(List<BoneTrack> tracks) {
        // first compute the maximum amount of frames
        int maximumFrameCount = -1;
        float[] maximumTrackTimes = null;
        for (BoneTrack track : tracks) {
            if (track.getTimes().length > maximumFrameCount) {
                maximumTrackTimes = track.getTimes();
                maximumFrameCount = maximumTrackTimes.length;
            }
        }

        // now widen all the tracks that have less frames by repeating the last values in the frame
        for (BoneTrack track : tracks) {
            int currentTrackLength = track.getTimes().length;
            if (currentTrackLength < maximumFrameCount) {
                Vector3f[] translations = new Vector3f[maximumFrameCount];
                Quaternion[] rotations = new Quaternion[maximumFrameCount];
                Vector3f[] scales = new Vector3f[maximumFrameCount];

                Vector3f[] currentTranslations = track.getTranslations();
                Quaternion[] currentRotations = track.getRotations();
                Vector3f[] currentScales = track.getScales();
                for (int i = 0; i < currentTrackLength; ++i) {
                    translations[i] = currentTranslations[i];
                    rotations[i] = currentRotations[i];
                    scales[i] = currentScales[i];
                }

                for (int i = currentTrackLength; i < maximumFrameCount; ++i) {
                    translations[i] = currentTranslations[currentTranslations.length - 1];
                    rotations[i] = currentRotations[currentRotations.length - 1];
                    scales[i] = currentScales[currentScales.length - 1];
                }

                track.setKeyframes(maximumTrackTimes, translations, rotations, scales);
            }
        }
    }
}
