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

package jme3test.helloworld;

import com.jme3.app.SimpleApplication;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;

/** Sample 11 - how to create fire, water, and explosion effects. */
public class HelloEffects extends SimpleApplication {

  public static void main(String[] args) {
    HelloEffects app = new HelloEffects();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    ParticleEmitter fire = 
            new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
    Material mat_red = new Material(assetManager, 
            "Common/MatDefs/Misc/Particle.j3md");
    mat_red.setTexture("Texture", assetManager.loadTexture(
            "Effects/Explosion/flame.png"));
    fire.setMaterial(mat_red);
    fire.setImagesX(2); 
    fire.setImagesY(2); // 2x2 texture animation
    fire.setEndColor(  new ColorRGBA(1f, 0f, 0f, 1f));   // red
    fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
    fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
    fire.setStartSize(1.5f);
    fire.setEndSize(0.1f);
    fire.setGravity(0, 0, 0);
    fire.setLowLife(1f);
    fire.setHighLife(3f);
    fire.getParticleInfluencer().setVelocityVariation(0.3f);
    rootNode.attachChild(fire);

    ParticleEmitter debris = 
            new ParticleEmitter("Debris", ParticleMesh.Type.Triangle, 10);
    Material debris_mat = new Material(assetManager, 
            "Common/MatDefs/Misc/Particle.j3md");
    debris_mat.setTexture("Texture", assetManager.loadTexture(
            "Effects/Explosion/Debris.png"));
    debris.setMaterial(debris_mat);
    debris.setImagesX(3); 
    debris.setImagesY(3); // 3x3 texture animation
    debris.setSelectRandomImage(true);
    debris.setRotateSpeed(4);
    debris.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 4, 0));
    debris.setStartColor(ColorRGBA.White);
    debris.setGravity(0, 6, 0);
    debris.getParticleInfluencer().setVelocityVariation(.60f);
    rootNode.attachChild(debris);
    debris.emitAllParticles();

//    ParticleEmitter water = 
//            new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 20);
//    Material mat_blue = new Material(assetManager, 
//            "Common/MatDefs/Misc/Particle.j3md");
//    mat_blue.setTexture("Texture", assetManager.loadTexture(
//            "Effects/Explosion/flame.png"));
//    water.setMaterial(mat_blue);
//    water.setImagesX(2); 
//    water.setImagesY(2); // 2x2 texture animation
//    water.setStartColor( ColorRGBA.Blue); 
//    water.setEndColor( ColorRGBA.Cyan); 
//    water.getParticleInfluencer().setInitialVelocity(new Vector3f(0, -4, 0));
//    water.setStartSize(1f);
//    water.setEndSize(1.5f);
//    water.setGravity(0,1,0);
//    water.setLowLife(1f);
//    water.setHighLife(1f);
//    water.getParticleInfluencer().setVelocityVariation(0.1f);
//    water.setLocalTranslation(0, 6, 0);
//    rootNode.attachChild(water);

  }
}
