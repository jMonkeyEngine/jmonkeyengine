/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
            logger.log(Level.INFO, "Found matching joint for bone {0} : {1} with score {2}", new Object[]{boneName, resultName, resultScore});
            preset.setupJoint(joint);
        } else {
            logger.log(Level.INFO, "No joint match found for bone {0}", boneName);
            if (resultScore > 0) {
                logger.log(Level.INFO, "Best match found is {0} with score {1}", new Object[]{resultName, resultScore});
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
