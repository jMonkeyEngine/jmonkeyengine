/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.bullet.control.ragdoll;

import com.jme3.bullet.joints.SixDofJoint;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nehon
 */
public abstract class RagdollPreset {

    /**
     * message logger for this class
     */
    protected static final Logger logger = Logger.getLogger(RagdollPreset.class.getName());
    /**
     * map bone names to joint presets
     */
    protected Map<String, JointPreset> boneMap = new HashMap<>();
    /**
     * lexicon to map bone names to entries
     */
    protected Map<String, LexiconEntry> lexicon = new HashMap<>();

    /**
     * Initialize the map from bone names to joint presets.
     */
    protected abstract void initBoneMap();

    /**
     * Initialize the lexicon.
     */
    protected abstract void initLexicon();

    /**
     * Apply the preset for the named bone to the specified joint.
     *
     * @param boneName name
     * @param joint where to apply the preset (not null, modified)
     */
    public void setupJointForBone(String boneName, SixDofJoint joint) {

        if (boneMap.isEmpty()) {
            initBoneMap();
        }
        if (lexicon.isEmpty()) {
            initLexicon();
        }
        String resultName = "";
        int resultScore = 0;

        for (String key : lexicon.keySet()) {
        
            int score = lexicon.get(key).getScore(boneName);        
            if (score > resultScore) {
                resultScore = score;
                resultName = key;
            }
            
        }
        
        JointPreset preset = boneMap.get(resultName);

        if (preset != null && resultScore >= 50) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Found matching joint for bone {0} : {1} with score {2}", new Object[]{boneName, resultName, resultScore});
            }
            preset.setupJoint(joint);
        } else {
            logger.log(Level.FINE, "No joint match found for bone {0}", boneName);
            if (resultScore > 0 && logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Best match found is {0} with score {1}", new Object[]{resultName, resultScore});
            }
            new JointPreset().setupJoint(joint);
        }

    }

    /**
     * Range of motion for a joint.
     */
    protected class JointPreset {

        private float maxX, minX, maxY, minY, maxZ, minZ;

        /**
         * Instantiate a preset with no motion allowed.
         */
        public JointPreset() {
        }

        /**
         * Instantiate a preset with the specified range of motion.
         *
         * @param maxX the maximum rotation on the X axis (in radians)
         * @param minX the minimum rotation on the X axis (in radians)
         * @param maxY the maximum rotation on the Y axis (in radians)
         * @param minY the minimum rotation on the Y axis (in radians)
         * @param maxZ the maximum rotation on the Z axis (in radians)
         * @param minZ the minimum rotation on the Z axis (in radians)
         */
        public JointPreset(float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {
            this.maxX = maxX;
            this.minX = minX;
            this.maxY = maxY;
            this.minY = minY;
            this.maxZ = maxZ;
            this.minZ = minZ;
        }

        /**
         * Apply this preset to the specified joint.
         *
         * @param joint where to apply (not null, modified)
         */
        public void setupJoint(SixDofJoint joint) {
            joint.getRotationalLimitMotor(0).setHiLimit(maxX);
            joint.getRotationalLimitMotor(0).setLoLimit(minX);
            joint.getRotationalLimitMotor(1).setHiLimit(maxY);
            joint.getRotationalLimitMotor(1).setLoLimit(minY);
            joint.getRotationalLimitMotor(2).setHiLimit(maxZ);
            joint.getRotationalLimitMotor(2).setLoLimit(minZ);
        }
    }

    /**
     * One entry in a bone lexicon.
     */
    protected class LexiconEntry extends HashMap<String, Integer> {

        /**
         * Add a synonym with the specified score.
         *
         * @param word a substring that might occur in a bone name (not null)
         * @param score larger value means more likely to correspond
         */
        public void addSynonym(String word, int score) {
            put(word.toLowerCase(), score);
        }

        /**
         * Calculate a total score for the specified bone name.
         *
         * @param word the name of a bone (not null)
         * @return total score: larger value means more likely to correspond
         */
        public int getScore(String word) {
            int score = 0;
            String searchWord = word.toLowerCase();
            for (String key : this.keySet()) {
                if (searchWord.indexOf(key) >= 0) {
                    score += get(key);
                }
            }
            return score;
        }
    }
}
