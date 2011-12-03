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

package jme3test.android;

import android.media.SoundPool;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.math.Vector3f;

public class TestAmbient extends SimpleApplication {

    private AudioNode footsteps, beep;
    private AudioNode nature, waves;
    
    SoundPool soundPool;
    
//    private PointAudioSource waves;
    private float time = 0;
    private float nextTime = 1;

    public static void main(String[] args){
        TestAmbient test = new TestAmbient();
        test.start();
    }
    

    @Override
    public void simpleInitApp()
    {     
        /*
        footsteps  = new AudioNode(audioRenderer, assetManager, "Sound/Effects/Foot steps.ogg", true);
        
        footsteps.setPositional(true);
        footsteps.setLocalTranslation(new Vector3f(4, -1, 30));
        footsteps.setMaxDistance(5);
        footsteps.setRefDistance(1);
        footsteps.setLooping(true);

        beep = new AudioNode(audioRenderer, assetManager, "Sound/Effects/Beep.ogg", true);
        beep.setVolume(3);
        beep.setLooping(true);
        
        audioRenderer.playSourceInstance(footsteps);
        audioRenderer.playSource(beep);
        */
        
        waves  = new AudioNode(assetManager, "Sound/Environment/Ocean Waves.ogg", true);
        waves.setPositional(true);

        nature = new AudioNode(assetManager, "Sound/Environment/Nature.ogg", true);
        
        waves.setLocalTranslation(new Vector3f(4, -1, 30));
        waves.setMaxDistance(5);
        waves.setRefDistance(1);
        
        nature.setVolume(3);
        audioRenderer.playSourceInstance(waves);
        audioRenderer.playSource(nature);
    }

    @Override
    public void simpleUpdate(float tpf)
    {

    }

}
