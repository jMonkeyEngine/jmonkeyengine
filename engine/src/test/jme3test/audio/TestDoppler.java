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

package jme3test.audio;

import com.jme3.audio.AudioNode;
import com.jme3.math.Vector3f;

/**
 * Test Doppler Effect
 */
public class TestDoppler extends AudioApp {

    private AudioNode ufo;

    private float location = 0;
    private float rate = 1;

    public static void main(String[] args){
        TestDoppler test = new TestDoppler();
        test.start();
    }

    @Override
    public void initAudioApp(){
        ufo  = new AudioNode(audioRenderer, assetManager, "Sound/Effects/Beep.ogg", false);
        ufo.setPositional(true);
        ufo.setLooping(true);
        audioRenderer.playSource(ufo);
    }

    @Override
    public void updateAudioApp(float tpf){
        // move the location variable left and right
        if (location > 10){
            location = 10;
            rate = -rate;
            ufo.setVelocity(new Vector3f(rate*10, 0, 0));
        }else if (location < -10){
            location = -10;
            rate = -rate;
            ufo.setVelocity(new Vector3f(rate*10, 0, 0));
        }else{
            location += rate * tpf * 10;
        }
        ufo.setLocalTranslation(location, 0, 2);
        ufo.updateGeometricState();
    }

}
