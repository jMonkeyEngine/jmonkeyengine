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

import com.jme3.app.SimpleApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

/**
 * Particle that moves in a circle.
 *
 * @author Kirill Vainer
 */
public class TestMovingParticle extends SimpleApplication {
    
    private ParticleEmitter emit;
    private float angle = 0;
    
    public static void main(String[] args) {
        TestMovingParticle app = new TestMovingParticle();
        app.start();
    }
    
    @Override
    public void simpleInitApp() {
        emit = new ParticleEmitter("Emitter", Type.Triangle, 300);
        emit.setGravity(0, 0, 0);
        emit.setVelocityVariation(1);
        emit.setLowLife(1);
        emit.setHighLife(1);
        emit.setInitialVelocity(new Vector3f(0, .5f, 0));
        emit.setImagesX(15);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
        emit.setMaterial(mat);
        
        rootNode.attachChild(emit);
        
        AmbientLight al = new AmbientLight();
        al.setColor(new ColorRGBA(0.84f, 0.80f, 0.80f, 1.0f));
        rootNode.addLight(al);
        
        
        
        inputManager.addListener(new ActionListener() {
            
            public void onAction(String name, boolean isPressed, float tpf) {
                if ("setNum".equals(name) && isPressed) {
                    emit.setNumParticles(1000);
                }
            }
        }, "setNum");
        
        inputManager.addMapping("setNum", new KeyTrigger(KeyInput.KEY_SPACE));
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        angle += tpf;
        angle %= FastMath.TWO_PI;
        float x = FastMath.cos(angle) * 2;
        float y = FastMath.sin(angle) * 2;
        emit.setLocalTranslation(x, 0, y);
    }
}
