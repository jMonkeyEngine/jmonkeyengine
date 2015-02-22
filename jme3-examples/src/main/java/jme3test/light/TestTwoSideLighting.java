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

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.MaterialDebugAppState;
import com.jme3.util.TangentBinormalGenerator;

/**
 * Checks two sided lighting capability.
 * 
 * @author Kirill Vainer
 */
public class TestTwoSideLighting extends SimpleApplication {

    float angle;
    PointLight pl;
    Geometry lightMdl;

    public static void main(String[] args){
        TestTwoSideLighting app = new TestTwoSideLighting();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Two-sided lighting requires single pass.
        renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        renderManager.setSinglePassLightBatchSize(4);
        
        cam.setLocation(new Vector3f(5.936224f, 3.3759952f, -3.3202777f));
        cam.setRotation(new Quaternion(0.16265652f, -0.4811838f, 0.09137692f, 0.8565368f));
        
        Geometry quadGeom = new Geometry("quad", new Quad(1, 1));
        quadGeom.move(1, 0, 0);
        Material mat1 = assetManager.loadMaterial("Textures/BumpMapTest/SimpleBump.j3m");
        
        // Display both front and back faces.
        mat1.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        
        quadGeom.setMaterial(mat1);
        // SimpleBump material requires tangents.
        TangentBinormalGenerator.generate(quadGeom);
        rootNode.attachChild(quadGeom);
        
        Geometry teapot = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.move(-1, 0, 0);
        teapot.setLocalScale(2f);
        Material mat2 = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat2.setFloat("Shininess", 25);
        mat2.setBoolean("UseMaterialColors", true);
        mat2.setColor("Ambient",  ColorRGBA.Black);
        mat2.setColor("Diffuse",  ColorRGBA.Gray);
        mat2.setColor("Specular", ColorRGBA.Gray);
        
        // Only display backfaces.
        mat2.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Front);
        
        teapot.setMaterial(mat2);
        rootNode.attachChild(teapot);

        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.getMesh().setStatic();
        rootNode.attachChild(lightMdl);

        pl = new PointLight();
        pl.setColor(ColorRGBA.White);
        pl.setRadius(4f);
        rootNode.addLight(pl);
    }

    @Override
    public void simpleUpdate(float tpf){
        angle += tpf;
        angle %= FastMath.TWO_PI;
        
        pl.setPosition(new Vector3f(FastMath.cos(angle) * 3f, 0.5f, FastMath.sin(angle) * 3f));
        lightMdl.setLocalTranslation(pl.getPosition());
    }

}
