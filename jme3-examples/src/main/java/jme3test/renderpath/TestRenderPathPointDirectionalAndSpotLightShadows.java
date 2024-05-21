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
package jme3test.renderpath;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.TechniqueDef;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.FrameGraphFactory;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.*;

/**
 * This example shows all shadow types, check rendering performance under different rendering paths
 * @author JohnKkk
 */
public class TestRenderPathPointDirectionalAndSpotLightShadows extends SimpleApplication {
    public static final int SHADOWMAP_SIZE = 512;

    public static void main(String[] args) {
        TestRenderPathPointDirectionalAndSpotLightShadows app = new TestRenderPathPointDirectionalAndSpotLightShadows();
        app.start();
    }
    private Node lightNode;
    private SpotLight spotLight;
    
    @Override
    public void simpleInitApp() {
        
        //FrameGraph graph = new FrameGraph(assetManager, renderManager);
        //graph.setConstructor(new DeferredGraphConstructor());
        FrameGraph graph = FrameGraphFactory.deferred(assetManager, renderManager, false);
        viewPort.setFrameGraph(graph);
        //renderManager.setFrameGraph(RenderPipelineFactory.create(this, RenderManager.RenderPath.Deferred));
        
        // Note that for this j3o Cube model, the value of vLightDir passed from vs to ps in MultPass LightModel is different from using SinglePass. See the lightComputeDir() function, there will be some differences when this function calculates in world space and view space. It's an existing bug in JME, so here we set it to use SinglePass instead.
        renderManager.setPreferredLightMode(TechniqueDef.LightMode.SinglePass);
        flyCam.setMoveSpeed(10);
        cam.setLocation(new Vector3f(0.040581334f, 1.7745866f, 6.155161f));
        cam.setRotation(new Quaternion(4.3868728E-5f, 0.9999293f, -0.011230096f, 0.0039059948f));


        Node scene = (Node) assetManager.loadModel("Models/Test/CornellBox.j3o");
        scene.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(scene);
        rootNode.getChild("Cube").setShadowMode(RenderQueue.ShadowMode.Receive);
        lightNode = (Node) rootNode.getChild("Lamp");
        Geometry lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
        //Geometry  lightMdl = new Geometry("Light", new Box(.1f,.1f,.1f));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.setShadowMode(RenderQueue.ShadowMode.Off);
        lightNode.attachChild(lightMdl);
        //lightMdl.setLocalTranslation(lightNode.getLocalTranslation());


        Geometry box = new Geometry("box", new Box(0.2f, 0.2f, 0.2f));
        //Geometry  lightMdl = new Geometry("Light", new Box(.1f,.1f,.1f));
        box.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        box.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        rootNode.attachChild(box);
        box.setLocalTranslation(-1f, 0.5f, -2);

        scene.getLocalLightList().get(0).setColor(ColorRGBA.Red);

        PointLightShadowFilter plsf
                = new PointLightShadowFilter(assetManager, SHADOWMAP_SIZE);
        plsf.setLight((PointLight) scene.getLocalLightList().get(0));     
        plsf.setEdgeFilteringMode(EdgeFilteringMode.PCF4);

        //DIRECTIONAL LIGHT
        DirectionalLight directionalLight = new DirectionalLight();
        rootNode.addLight(directionalLight);
        directionalLight.setColor(ColorRGBA.Blue);
        directionalLight.setDirection(new Vector3f(-1f, -.2f, 0f));

        DirectionalLightShadowFilter dlsf
                = new DirectionalLightShadowFilter(assetManager, SHADOWMAP_SIZE*2, 4);
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.PCF4);
        dlsf.setLight(directionalLight);        

        //SPOT LIGHT
        spotLight = new SpotLight();
        spotLight.setDirection(new Vector3f(1f,-1f,0f));
        spotLight.setPosition(new Vector3f(-1f,3f,0f));
        spotLight.setSpotOuterAngle(0.5f);
        spotLight.setColor(ColorRGBA.Green);
        Sphere sphere = new Sphere(8, 8, .1f);
        Geometry sphereGeometry = new Geometry("Sphere", sphere);
        sphereGeometry.setLocalTranslation(-1f, 3f, 0f);
        sphereGeometry.setMaterial(assetManager.loadMaterial("Common/Materials/WhiteColor.j3m"));
        rootNode.attachChild(sphereGeometry);
        rootNode.addLight(spotLight);

        SpotLightShadowFilter slsf
                = new SpotLightShadowFilter(assetManager, SHADOWMAP_SIZE);
        slsf.setLight(spotLight);
        slsf.setEdgeFilteringMode(EdgeFilteringMode.PCF4);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(plsf);
        fpp.addFilter(dlsf);
        fpp.addFilter(slsf);
        viewPort.addProcessor(fpp);

    }

    private float timeElapsed = 0.0f;
    @Override
    public void simpleUpdate(float tpf) {
        timeElapsed += tpf;
       lightNode.setLocalTranslation(FastMath.cos(timeElapsed), lightNode.getLocalTranslation().y, FastMath.sin(timeElapsed));
       spotLight.setDirection(new Vector3f(FastMath.cos(-timeElapsed*.7f), -1.0f, FastMath.sin(-timeElapsed*.7f)));
    }
}