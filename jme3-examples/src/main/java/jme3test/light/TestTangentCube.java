/*
 * Copyright (c) 2009-2015 jMonkeyEngine
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
package jme3test.light;

import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.util.TangentBinormalGenerator;

/**
 *
 * @author Nehon
 */
public class TestTangentCube extends SimpleApplication {

    public static void main(String... args) {
        TestTangentCube app = new TestTangentCube();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Box aBox = new Box(1, 1, 1);
        Geometry aGeometry = new Geometry("Box", aBox);
        TangentBinormalGenerator.generate(aBox);

        Material aMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        aMaterial.setTexture("DiffuseMap",
                assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg"));
        aMaterial.setTexture("NormalMap",
                assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall_normal.jpg"));
        aMaterial.setBoolean("UseMaterialColors", false);
        aMaterial.setColor("Diffuse", ColorRGBA.White);
        aMaterial.setColor("Specular", ColorRGBA.White);
        aMaterial.setFloat("Shininess", 64f);
        aGeometry.setMaterial(aMaterial);

        // Rotate 45 degrees to see multiple faces
        aGeometry.rotate(FastMath.QUARTER_PI, FastMath.QUARTER_PI, 0.0f);
        rootNode.attachChild(aGeometry);

        /*
         * Must add a light to make the lit object visible!
         */
        PointLight aLight = new PointLight();
        aLight.setPosition(new Vector3f(0, 3, 3));
        aLight.setColor(ColorRGBA.Red);
        rootNode.addLight(aLight);
//
//        AmbientLight bLight = new AmbientLight();
//        bLight.setColor(ColorRGBA.Gray);
//        rootNode.addLight(bLight);

        
        ChaseCameraAppState chaser = new ChaseCameraAppState();
        chaser.setTarget(aGeometry);
        getStateManager().attach(chaser);
    }

}
