/*
 * Copyright (c) 2024 jMonkeyEngine
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

import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.RenderObjectMap;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.system.AppSettings;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 * This example demonstrates unified handling of several built-in shading models under the same render path.
 * @author JohnKkk
 */
public class TestShadingModel extends SimpleApplication {
    private DirectionalLight dl;

    private float roughness = 0.0f;
    
    private FrameGraph graph;
    private Node modelNode;
    private int frame = 0;
    private Material pbrMat;
    private Geometry model;
    private Node tex;

    public static void main(String[] args) {
        TestShadingModel app = new TestShadingModel();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(768);
        settings.setHeight(768);
        //settings.setGraphicsDebug(true);
        //settings.setGraphicsTrace(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        
        stateManager.attach(new DetailedProfilerState());
        //flyCam.setEnabled(false);
        flyCam.setDragToRotate(true);
        inputManager.setCursorVisible(true);
        
        //FrameGraph graph = RenderPipelineFactory.create(this, RenderManager.RenderPath.Deferred);
        //graph = FrameGraphFactory.deferred(assetManager, renderManager, false);
        graph = new FrameGraph(assetManager);
        graph.applyData(assetManager.loadFrameGraph("Common/FrameGraphs/Deferred.j3g"));
        //graph.setConstructor(new ForwardGraphConstructor());
        //graph.setConstructor(new TestConstructor());
        //MyFrameGraph graph = RenderPipelineFactory.createBackroundScreenTest(assetManager, renderManager);
        viewPort.setFrameGraph(graph);
        //guiViewPort.setFrameGraph(graph);
        //renderManager.setFrameGraph(graph);
        
        viewPort.setBackgroundColor(ColorRGBA.Green.mult(0.2f));
        //viewPort.setBackgroundColor(ColorRGBA.White);
        
//        FrameBuffer fb = new FrameBuffer(768, 768, 1);
//        Texture2D depth = new Texture2D(768, 768, Image.Format.Depth);
//        fb.setDepthTarget(FrameBuffer.FrameBufferTarget.newTarget(depth));
//        viewPort.setOutputFrameBuffer(fb);
        
        Geometry debugView = new Geometry("debug", new Quad(150, 150));
        debugView.setLocalTranslation(0, 200, 0);
        Material debugMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        //debugMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //debugMat.setTransparent(true);
        debugView.setMaterial(debugMat);
        //debugMat.setTexture("ColorMap", depth);
//        MatParamTargetControl texParam = new MatParamTargetControl("ColorMap", VarType.Texture2D);
//        graph.get(Attribute.class, "OpaqueColor").setTarget(texParam);
//        debugView.addControl(texParam);
//        guiNode.attachChild(debugView);
        
        // UNLIT
        Material unlitMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        unlitMat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
        //unlitMat.setColor("Color", new ColorRGBA(0, 0, 1, .5f));
        //unlitMat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        //unlitMat.getAdditionalRenderState().setDepthWrite(false);
        //unlitMat.getAdditionalRenderState().setDepthTest(false);
        unlitMat.setTransparent(true);
        Sphere sp = new Sphere(15, 15, 1.0f);
        Geometry unlitSphere = new Geometry("unlitSphere", sp);
        unlitSphere.setLocalTranslation(-5, 0, 0);
        unlitSphere.setLocalRotation(new Quaternion(new float[]{(float) Math.toRadians(-90), 0, 0}));
        unlitSphere.setMaterial(unlitMat);
        unlitSphere.setQueueBucket(RenderQueue.Bucket.Transparent);
        rootNode.attachChild(unlitSphere);

        // LEGACY_LIGHTING
        Geometry lightSphere = unlitSphere.clone(false);
        TangentBinormalGenerator.generate(lightSphere.getMesh());
        Material lightMat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        lightSphere.setLocalTranslation(5, 0, 0);
        lightSphere.setMaterial(lightMat);
        lightSphere.setQueueBucket(RenderQueue.Bucket.Inherit);
        rootNode.attachChild(lightSphere);

        // STANDARD_LIGHTING
        roughness = 1.0f;
        assetManager.registerLoader(KTXLoader.class, "ktx");

        modelNode = new Node("modelNode");
        model = (Geometry) assetManager.loadModel("Models/Tank/tank.j3o");
        MikktspaceTangentGenerator.generate(model);
        modelNode.attachChild(model);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        rootNode.addLight(dl);
        dl.setColor(ColorRGBA.White);
        modelNode.setLocalScale(0.3f);
        rootNode.attachChild(modelNode);
        
        AmbientLight al = new AmbientLight(ColorRGBA.White.mult(0.1f));
        rootNode.addLight(al);

        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        int numSamples = context.getSettings().getSamples();
        if (numSamples > 0) {
            fpp.setNumSamples(numSamples);
        }

//        fpp.addFilter(new FXAAFilter());
        //fpp.addFilter(new ToneMapFilter(Vector3f.UNIT_XYZ.mult(1.0f)));
//        fpp.addFilter(new SSAOFilter(0.5f, 3, 0.2f, 0.2f));
        viewPort.addProcessor(fpp);
        
        DirectionalLightShadowRenderer dr = new DirectionalLightShadowRenderer(assetManager, 1024, 2);
        dr.setLight(dl);
        viewPort.addProcessor(dr);

        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Sky_Cloudy.hdr", SkyFactory.EnvMapType.EquirectMap);
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap);
        //Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/road.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);

        pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);

        //new RenderPathHelper(this);
        flyCam.setMoveSpeed(10.0f);
    }

    @Override
    public void simpleRender(RenderManager rm) {
        super.simpleRender(rm);
        frame++;

        if (frame == 2) {
            
            rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));

        }
        if (frame > 10 && modelNode.getParent() == null) {
            rootNode.attachChild(modelNode);
        }
    }
    
}
