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

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Environment;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;

/**
 * Test Doppler Effect
 */
public class TestDoppler extends SimpleApplication {

    private AudioNode ufo;

    private float x = 20, z = 0;
    private float rate     = -0.05f;
    private float xDist    = 20;
    private float zDist    = 5;
    private float angle    = FastMath.TWO_PI;
    
    public static void main(String[] args){
        TestDoppler test = new TestDoppler();
        test.start();
    }

    @Override
    public void simpleInitApp(){
        audioRenderer.setEnvironment(Environment.Dungeon);
        AL10.alDistanceModel(AL11.AL_EXPONENT_DISTANCE);
        
        ufo  = new AudioNode(assetManager, "Sound/Effects/Beep.ogg", false);
        ufo.setPositional(true);
        ufo.setLooping(true);
        ufo.setReverbEnabled(true);
        ufo.setRefDistance(100000000);
        ufo.setMaxDistance(100000000);
        ufo.play();
    }

    @Override
    public void simpleUpdate(float tpf){
        //float x  = (float) (Math.cos(angle) * xDist);
        float dx = (float)  Math.sin(angle) * xDist; 
        
        //float z  = (float) (Math.sin(angle) * zDist);
        float dz = (float)(-Math.cos(angle) * zDist);
        
        x += dx * tpf * 0.05f;
        z += dz * tpf * 0.05f;
        
        angle += tpf * rate;
        
        if (angle > FastMath.TWO_PI){
            angle = FastMath.TWO_PI;
            rate = -rate;
        }else if (angle < -0){
            angle = -0;
            rate = -rate;
        }
        
        ufo.setVelocity(new Vector3f(dx, 0, dz));
        ufo.setLocalTranslation(x, 0, z);
        ufo.updateGeometricState();
        
        System.out.println("LOC: " + (int)x +", " + (int)z + 
                ", VEL: " + (int)dx + ", " + (int)dz);
    }

}
