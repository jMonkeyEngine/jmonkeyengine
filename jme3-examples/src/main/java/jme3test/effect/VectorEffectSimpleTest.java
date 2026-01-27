/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package jme3test.effect;

import com.jme3.vectoreffect.EaseVectorEffect;
import com.jme3.vectoreffect.VectorEffectManagerState;
import com.jme3.vectoreffect.VectorGroup;
import com.jme3.vectoreffect.SequencedVectorEffect;
import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Easing;
import com.jme3.math.FastMath;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;

/**
 *
 * @author yaRnMcDonuts
 */
public class VectorEffectSimpleTest extends SimpleApplication {

    private ColorRGBA colorToShift = new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f);

    private VectorEffectManagerState vectorEffectManagerState;

    public static void main(String[] args) {
        VectorEffectSimpleTest app = new VectorEffectSimpleTest();
        AppSettings settings = new AppSettings(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        restart();
        flyCam.setMoveSpeed(10f);

        vectorEffectManagerState = new VectorEffectManagerState();
        stateManager.attach(vectorEffectManagerState);

        initBloom();
        initPbrRoom(13);

        initLightAndEmissiveSphere();

        // initiate VectorEffectManagerState
        vectorEffectManagerState = new VectorEffectManagerState();
        stateManager.attach(vectorEffectManagerState);
        vectorEffectManagerState.setEnabled(true);

        // create gradient effect :
        VectorGroup vg = new VectorGroup(colorToShift);
        SequencedVectorEffect colorGradientEffect = new SequencedVectorEffect(
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Cyan), 0.75f),
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Yellow), 0.75f),
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Blue), 0.75f),
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Magenta), 0.75f),
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Green), 0.75f),
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Red), 0.75f));

        // create red flashing effect :
        SequencedVectorEffect blinkEffect = new SequencedVectorEffect(
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Black), 0.25f, Easing.inOutQuad),
                new EaseVectorEffect(vg, new VectorGroup(ColorRGBA.Red), 0.175f, Easing.outQuart));
        blinkEffect.setRepeatNumberOfTimes(10);

        // put both effects into a looping SequencedVectorEffect
        SequencedVectorEffect finalLoopingEffect = new SequencedVectorEffect(colorGradientEffect,
                blinkEffect);
        finalLoopingEffect.setLooping(true);

        // register the effect:
        vectorEffectManagerState.registerVectorEffect(finalLoopingEffect);

    }

    private void initBloom() {

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Scene);
        bloom.setBloomIntensity(5f);
        bloom.setExposurePower(4.5f);
        bloom.setExposureCutOff(0.2f);
        bloom.setBlurScale(2);
        fpp.addFilter(bloom);
        viewPort.addProcessor(fpp);
    }

    private void initLightAndEmissiveSphere() {

        // make point light
        PointLight light = new PointLight();
        light.setRadius(10);
        colorToShift = light.getColor();

        // make sphere with Emissive color
        Sphere sphereMesh = new Sphere(32, 32, 0.5f);
        Geometry glowingSphere = new Geometry("ShakingSphere", sphereMesh);

        Material sphereMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");
        sphereMat.setColor("BaseColor", ColorRGBA.DarkGray);
        sphereMat.setFloat("Roughness", 0.04f);
        sphereMat.setFloat("Metallic", 0.98f);
        sphereMat.setBoolean("UseVertexColor", false);
        glowingSphere.setMaterial(sphereMat);

        // assign the same colorToShift vector to both the light and emissive value (important not to clone)
        light.setColor(colorToShift);
        sphereMat.setColor("Emissive", colorToShift);

        rootNode.attachChild(glowingSphere);
        rootNode.addLight(light);

    }

    public void initPbrRoom(float size) {

        float half = size * 0.5f;

        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/PBRLighting.j3md");

        wallMat.setColor("BaseColor", new ColorRGBA(1, 1, 1, 1f));
        wallMat.setFloat("Roughness", 0.12f);
        wallMat.setFloat("Metallic", 0.02f);

        // Floor
        Geometry floor = new Geometry("Floor", new Quad(size, size));
        floor.setMaterial(wallMat);
        floor.rotate(-FastMath.HALF_PI, 0, 0);
        floor.setLocalTranslation(-half, -half, half);
        rootNode.attachChild(floor);

        // Ceiling
        Geometry ceiling = new Geometry("Ceiling", new Quad(size, size));
        ceiling.setMaterial(wallMat);
        ceiling.rotate(FastMath.HALF_PI, 0, 0);
        ceiling.setLocalTranslation(-half, size - half, -half);
        rootNode.attachChild(ceiling);

        // Back wall
        Geometry backWall = new Geometry("BackWall", new Quad(size, size));
        backWall.setMaterial(wallMat);
        backWall.setLocalTranslation(-half, -half, -half);
        rootNode.attachChild(backWall);

        // Left wall
        Geometry leftWall = new Geometry("LeftWall", new Quad(size, size));
        leftWall.setMaterial(wallMat);
        leftWall.rotate(0, FastMath.HALF_PI, 0);
        leftWall.setLocalTranslation(-half, -half, half);
        rootNode.attachChild(leftWall);

        // Right wall
        Geometry rightWall = new Geometry("RightWall", new Quad(size, size));
        rightWall.setMaterial(wallMat);
        rightWall.rotate(0, -FastMath.HALF_PI, 0);
        rightWall.setLocalTranslation(half, -half, -half);
        rootNode.attachChild(rightWall);

    }
}