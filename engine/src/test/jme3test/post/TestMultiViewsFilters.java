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
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.*;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.util.SkyFactory;

public class TestMultiViewsFilters extends SimpleApplication {

    public static void main(String[] args) {
        TestMultiViewsFilters app = new TestMultiViewsFilters();
        app.start();
    }
    private boolean filterEnabled = true;

    public void simpleInitApp() {
        // create the geometry and attach it
        Geometry teaGeom = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        teaGeom.scale(3);
        teaGeom.getMaterial().setColor("GlowColor", ColorRGBA.Green);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(Vector3f.UNIT_XYZ.negate());

        rootNode.addLight(dl);
        rootNode.attachChild(teaGeom);

        // Setup first view      
        cam.setViewPort(.5f, 1f, 0f, 0.5f);
        cam.setLocation(new Vector3f(3.3212643f, 4.484704f, 4.2812433f));
        cam.setRotation(new Quaternion(-0.07680723f, 0.92299235f, -0.2564353f, -0.27645364f));

        // Setup second view
        Camera cam2 = cam.clone();
        cam2.setViewPort(0f, 0.5f, 0f, 0.5f);
        cam2.setLocation(new Vector3f(-0.10947256f, 1.5760219f, 4.81758f));
        cam2.setRotation(new Quaternion(0.0010108891f, 0.99857414f, -0.04928594f, 0.020481428f));

        final ViewPort view2 = renderManager.createMainView("Bottom Left", cam2);
        view2.setClearFlags(true, true, true);
        view2.attachScene(rootNode);

        // Setup third view
        Camera cam3 = cam.clone();
        cam3.setName("cam3");
        cam3.setViewPort(0f, .5f, .5f, 1f);
        cam3.setLocation(new Vector3f(0.2846221f, 6.4271426f, 0.23380789f));
        cam3.setRotation(new Quaternion(0.004381671f, 0.72363687f, -0.69015175f, 0.0045953835f));

        final ViewPort view3 = renderManager.createMainView("Top Left", cam3);
        view3.setClearFlags(true, true, true);
        view3.attachScene(rootNode);


        // Setup fourth view
        Camera cam4 = cam.clone();
        cam4.setName("cam4");
        cam4.setViewPort(.5f, 1f, .5f, 1f);

        cam4.setLocation(new Vector3f(4.775564f, 1.4548365f, 0.11491505f));
        cam4.setRotation(new Quaternion(0.02356979f, -0.74957186f, 0.026729556f, 0.66096294f));

        final ViewPort view4 = renderManager.createMainView("Top Right", cam4);
        view4.setClearFlags(true, true, true);
        view4.attachScene(rootNode);

//        Camera cam5 = new Camera(200, 200);
//        cam5.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 1f, 1000f);
//        cam5.setName("cam5");
//        cam5.setViewPort(5.23f, 6.33f, 0.56f, 1.66f);
//          this.setViewPortAreas(5.23f, 6.33f, 0.56f, 1.66f);
//          this.setViewPortCamSize(200, 200);
//          1046,1266,112,332
        Camera cam5 = cam.clone();
        cam5.setName("cam5");
        cam5.setViewPort(1046f/settings.getWidth(), 1266f/settings.getWidth(), 112f/settings.getHeight(), 332f/settings.getHeight());
        cam5.setLocation(new Vector3f(0.2846221f, 6.4271426f, 0.23380789f));
        cam5.setRotation(new Quaternion(0.004381671f, 0.72363687f, -0.69015175f, 0.0045953835f));

        final ViewPort view5 = renderManager.createMainView("center", cam5);
        view5.setClearFlags(true, true, true);
        view5.attachScene(rootNode);

        
        
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));

        final FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        final FilterPostProcessor fpp2 = new FilterPostProcessor(assetManager);
        final FilterPostProcessor fpp3 = new FilterPostProcessor(assetManager);
        final FilterPostProcessor fpp4 = new FilterPostProcessor(assetManager);
        final FilterPostProcessor fpp5 = new FilterPostProcessor(assetManager);


        //  fpp.addFilter(new WaterFilter(rootNode, Vector3f.UNIT_Y.mult(-1)));
        fpp3.addFilter(new CartoonEdgeFilter());

        fpp2.addFilter(new CrossHatchFilter());
        final FogFilter ff = new FogFilter(ColorRGBA.Yellow, 0.7f, 2);
        fpp.addFilter(ff);

        final RadialBlurFilter rbf = new RadialBlurFilter(1, 10);
        //    rbf.setEnabled(false);
        fpp.addFilter(rbf);


        SSAOFilter f = new SSAOFilter(1.8899765f, 20.490374f, 0.4699998f, 0.1f);;
        fpp4.addFilter(f);
        SSAOUI ui = new SSAOUI(inputManager, f);
        
        fpp5.addFilter(new BloomFilter(BloomFilter.GlowMode.Objects));

        viewPort.addProcessor(fpp);
        view2.addProcessor(fpp2);
        view3.addProcessor(fpp3);
        view4.addProcessor(fpp4);
        view5.addProcessor(fpp5);



        inputManager.addListener(new ActionListener() {

            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("press") && isPressed) {
                    if (filterEnabled) {
                        viewPort.removeProcessor(fpp);
                        view2.removeProcessor(fpp2);
                        view3.removeProcessor(fpp3);
                        view4.removeProcessor(fpp4);
                        view5.removeProcessor(fpp5);
                    } else {
                        viewPort.addProcessor(fpp);
                        view2.addProcessor(fpp2);
                        view3.addProcessor(fpp3);
                        view4.addProcessor(fpp4);
                        view5.addProcessor(fpp5);
                    }
                    filterEnabled = !filterEnabled;
                }
                if (name.equals("filter") && isPressed) {
                    ff.setEnabled(!ff.isEnabled());
                    rbf.setEnabled(!rbf.isEnabled());
                }
            }
        }, "press", "filter");

        inputManager.addMapping("press", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("filter", new KeyTrigger(KeyInput.KEY_F));

    }
}
