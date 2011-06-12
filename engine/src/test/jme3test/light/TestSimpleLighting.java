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

package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TangentBinormalGenerator;

public class TestSimpleLighting extends SimpleApplication {

    float angle;
    PointLight pl;
    Geometry lightMdl;

    public static void main(String[] args){
        TestSimpleLighting app = new TestSimpleLighting();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        Geometry teapot = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        TangentBinormalGenerator.generate(teapot.getMesh(), true);

        teapot.setLocalScale(2f);
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
//        mat.selectTechnique("GBuf");
        mat.setFloat("Shininess", 12);
        mat.setBoolean("UseMaterialColors", true);

//        mat.setTexture("ColorRamp", assetManager.loadTexture("Textures/ColorRamp/cloudy.png"));
//
//        mat.setBoolean("VTangent", true);
//        mat.setBoolean("Minnaert", true);
//        mat.setBoolean("WardIso", true);
//        mat.setBoolean("VertexLighting", true);
//        mat.setBoolean("LowQuality", true);
//        mat.setBoolean("HighQuality", true);

        mat.setColor("Ambient",  ColorRGBA.Black);
        mat.setColor("Diffuse",  ColorRGBA.Gray);
        mat.setColor("Specular", ColorRGBA.Gray);
        
        teapot.setMaterial(mat);
        rootNode.attachChild(teapot);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.getMesh().setStatic();
        rootNode.attachChild(lightMdl);

        pl = new PointLight();
        pl.setColor(ColorRGBA.White);
        pl.setRadius(4f);
        rootNode.addLight(pl);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.Green);
        rootNode.addLight(dl);
    }

    @Override
    public void simpleUpdate(float tpf){
//        cam.setLocation(new Vector3f(2.0632997f, 1.9493936f, 2.6885238f));
//        cam.setRotation(new Quaternion(-0.053555284f, 0.9407851f, -0.17754152f, -0.28378546f));

        angle += tpf;
        angle %= FastMath.TWO_PI;
        
        pl.setPosition(new Vector3f(FastMath.cos(angle) * 2f, 0.5f, FastMath.sin(angle) * 2f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }

}
