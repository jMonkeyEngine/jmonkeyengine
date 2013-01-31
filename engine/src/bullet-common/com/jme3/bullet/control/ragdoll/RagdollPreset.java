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

    protected static final Logger logger = Logger.getLogger(RagdollPreset.class.getName());
    protected Map<String, JointPreset> boneMap = new HashMap<String, JointPreset>();
    protected Map<String, LexiconEntry> lexicon = new HashMap<String, LexiconEntry>();

    protected abstract void initBoneMap();

    protected abstract void initLexicon();

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
            logger.log(Level.FINE, "Found matching joint for bone {0} : {1} with score {2}", new Object[]{boneName, resultName, resultScore});
            preset.setupJoint(joint);
        } else {
            logger.log(Level.FINE, "No joint match found for bone {0}", boneName);
            if (resultScore > 0) {
                logger.log(Level.FINE, "Best match found is {0} with score {1}", new Object[]{resultName, resultScore});
            }
            new JointPreset().setupJoint(joint);
        }

    }

    protected class JointPreset {

        private float maxX, minX, maxY, minY, maxZ, minZ;

        public JointPreset() {
        }

        public JointPreset(float maxX, float minX, float maxY, float minY, float maxZ, float minZ) {
            this.maxX = maxX;
            this.minX = minX;
            this.maxY = maxY;
            this.minY = minY;
            this.maxZ = maxZ;
            this.minZ = minZ;
        }

        public void setupJoint(SixDofJoint joint) {
            joint.getRotationalLimitMotor(0).setHiLimit(maxX);
            joint.getRotationalLimitMotor(0).setLoLimit(minX);
            joint.getRotationalLimitMotor(1).setHiLimit(maxY);
            joint.getRotationalLimitMotor(1).setLoLimit(minY);
            joint.getRotationalLimitMotor(2).setHiLimit(maxZ);
            joint.getRotationalLimitMotor(2).setLoLimit(minZ);
        }
    }

    protected class LexiconEntry extends HashMap<String, Integer> {

        public void addSynonym(String word, int score) {
            put(word.toLowerCase(), score);
        }

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
