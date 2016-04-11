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

package jme3test.audio;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Torus;

/**
 * Test Doppler Effect
 */
public class TestDoppler extends SimpleApplication {

    private float pos = -5;
    private float vel = 5;
    private AudioNode ufoNode;

    public static void main(String[] args){
        TestDoppler test = new TestDoppler();
        test.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(10);

        Torus torus = new Torus(10, 6, 1, 3);
        Geometry g = new Geometry("Torus Geom", torus);
        g.rotate(-FastMath.HALF_PI, 0, 0);
        g.center();

        g.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
//        rootNode.attachChild(g);

        ufoNode = new AudioNode(assetManager, "Sound/Effects/Beep.ogg", AudioData.DataType.Buffer);
        ufoNode.setLooping(true);
        ufoNode.setPitch(0.5f);
        ufoNode.setRefDistance(1);
        ufoNode.setMaxDistance(100000000);
        ufoNode.setVelocityFromTranslation(true);
        ufoNode.play();

        Geometry ball = new Geometry("Beeper", new Sphere(10, 10, 0.1f));
        ball.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        ufoNode.attachChild(ball);

        rootNode.attachChild(ufoNode);
    }


    @Override
    public void simpleUpdate(float tpf) {
        pos += tpf * vel;
        if (pos < -10 || pos > 10) {
            vel *= -1;
        }
        ufoNode.setLocalTranslation(new Vector3f(pos, 0, 0));
    }
}
