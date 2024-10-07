/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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

import com.jme3.math.FastMath;

/**
 * Example ragdoll presets for a typical humanoid skeleton.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author Nehon
 */
public class HumanoidRagdollPreset extends RagdollPreset {

    /**
     * Initialize the map from bone names to joint presets.
     */
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

    /**
     * Initialize the lexicon.
     */
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
        entry.addSynonym("shin", 100);
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
