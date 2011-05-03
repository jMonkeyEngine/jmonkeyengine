/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.bullet.control.ragdoll;

import com.jme3.math.FastMath;

/**
 *
 * @author Nehon
 */
public class HumanoidRagdollPreset extends RagdollPreset {

    @Override
    protected void initBoneMap() {
        boneMap.put("head", new JointPreset(FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI));

        boneMap.put("torso", new JointPreset(FastMath.QUARTER_PI, -FastMath.QUARTER_PI, 0, 0, FastMath.QUARTER_PI, -FastMath.QUARTER_PI));

        boneMap.put("upperleg", new JointPreset(FastMath.PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI/2, -FastMath.QUARTER_PI/2, FastMath.QUARTER_PI, -FastMath.QUARTER_PI));

        boneMap.put("lowerleg", new JointPreset(0, -FastMath.PI, 0, 0, 0, 0));

        boneMap.put("foot", new JointPreset(0, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI));

        boneMap.put("upperarm", new JointPreset(FastMath.HALF_PI, -FastMath.QUARTER_PI, 0, 0, FastMath.HALF_PI, -FastMath.QUARTER_PI));

        boneMap.put("lowerarm", new JointPreset(FastMath.HALF_PI, 0, 0, 0, 0, 0));

        boneMap.put("hand", new JointPreset(FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI, FastMath.QUARTER_PI, -FastMath.QUARTER_PI));

    }

    @Override
    protected void initLexicon() {
        LexiconEntry entry = new LexiconEntry();
        entry.addSynonym("head", 100);        
        lexicon.put("head", entry);

        entry = new LexiconEntry();
        entry.addSynonym("torso", 100);
        entry.addSynonym("chest", 100);
        entry.addSynonym("spine", 45);
        entry.addSynonym("high", 25);
        lexicon.put("torso", entry);

        entry = new LexiconEntry();
        entry.addSynonym("upperleg", 100);
        entry.addSynonym("thigh", 100);
        entry.addSynonym("hip", 75);
        entry.addSynonym("leg", 40);
        entry.addSynonym("high", 10);
        entry.addSynonym("up", 15);
        entry.addSynonym("upper", 15);
        lexicon.put("upperleg", entry);

        entry = new LexiconEntry();
        entry.addSynonym("lowerleg", 100);
        entry.addSynonym("calf", 100);
        entry.addSynonym("knee", 75);
        entry.addSynonym("leg", 50);
        entry.addSynonym("low", 10);
        entry.addSynonym("lower", 10);
        lexicon.put("lowerleg", entry);
        
        entry = new LexiconEntry();
        entry.addSynonym("foot", 100);
        entry.addSynonym("ankle", 75);   
        lexicon.put("foot", entry);
        
        
        entry = new LexiconEntry();
        entry.addSynonym("upperarm", 100);
        entry.addSynonym("humerus", 100); 
        entry.addSynonym("shoulder", 50);
        entry.addSynonym("arm", 40);
        entry.addSynonym("high", 10);
        entry.addSynonym("up", 15);
        entry.addSynonym("upper", 15);
        lexicon.put("upperarm", entry);

        entry = new LexiconEntry();
        entry.addSynonym("lowerarm", 100);
        entry.addSynonym("ulna", 100);
        entry.addSynonym("elbow", 75);
        entry.addSynonym("arm", 50);
        entry.addSynonym("low", 10);
        entry.addSynonym("lower", 10);
        lexicon.put("lowerarm", entry);
        
        entry = new LexiconEntry();
        entry.addSynonym("hand", 100);
        entry.addSynonym("fist", 100);   
        entry.addSynonym("wrist", 75);           
        lexicon.put("hand", entry);

    }
}
