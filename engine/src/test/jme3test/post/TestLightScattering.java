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

package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsView;
import com.jme3.font.BitmapText;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.LightScatteringFilter;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

public class TestLightScattering extends SimpleApplication {

    public static void main(String[] args) {
        TestLightScattering app = new TestLightScattering();
        
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // put the camera in a bad position
        cam.setLocation(new Vector3f(55.35316f, -0.27061665f, 27.092093f));
        cam.setRotation(new Quaternion(0.010414706f, 0.9874893f, 0.13880467f, -0.07409228f));
//        cam.setDirection(new Vector3f(0,-0.5f,1.0f));
//        cam.setLocation(new Vector3f(0, 300, -500));
        //cam.setFrustumFar(1000);
        flyCam.setMoveSpeed(10);
        Material mat = assetManager.loadMaterial("Textures/Terrain/Rocky/Rocky.j3m");
        Spatial scene = assetManager.loadModel("Models/Terrain/Terrain.mesh.xml");
        TangentBinormalGenerator.generate(((Geometry)((Node)scene).getChild(0)).getMesh());
        scene.setMaterial(mat);
        scene.setShadowMode(ShadowMode.CastAndReceive);
        scene.setLocalScale(400);
        scene.setLocalTranslation(0, -10, -120);

        rootNode.attachChild(scene);

        // load sky
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/FullskiesBlueClear03.dds", false));

        DirectionalLight sun = new DirectionalLight();
        Vector3f lightDir = new Vector3f(-0.12f, -0.3729129f, 0.74847335f);
        sun.setDirection(lightDir);
        sun.setColor(ColorRGBA.White.clone().multLocal(2));
        scene.addLight(sun);

        PssmShadowRenderer pssmRenderer = new PssmShadowRenderer(assetManager,1024,4);
        pssmRenderer.setDirection(lightDir);
        pssmRenderer.setShadowIntensity(0.55f);
     //   viewPort.addProcessor(pssmRenderer);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
//        SSAOFilter ssaoFilter= new SSAOFilter(viewPort, new SSAOConfig(0.36f,1.8f,0.84f,0.16f,false,true));
//        fpp.addFilter(ssaoFilter);


//           Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
//        mat2.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
//
//        Sphere lite=new Sphere(8, 8, 10.0f);
//        Geometry lightSphere=new Geometry("lightsphere", lite);
//        lightSphere.setMaterial(mat2);
        Vector3f lightPos = lightDir.multLocal(-3000);
//        lightSphere.setLocalTranslation(lightPos);
        // rootNode.attachChild(lightSphere);
        LightScatteringFilter filter = new LightScatteringFilter(lightPos);
        LightScatteringUI ui = new LightScatteringUI(inputManager, filter);
        fpp.addFilter(filter);
//fpp.setNumSamples(4);
        //fpp.addFilter(new RadialBlurFilter(0.3f,15.0f));
        //    SSAOUI ui=new SSAOUI(inputManager, ssaoFilter.getConfig());

        viewPort.addProcessor(fpp);
    }

    @Override
    public void simpleUpdate(float tpf) {
    }
}
