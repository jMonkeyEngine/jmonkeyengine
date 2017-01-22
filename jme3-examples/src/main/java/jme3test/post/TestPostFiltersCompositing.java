/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.post.filters.ComposeFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.SkyFactory;

/**
 * This test showcases the possibility to compose the post filtered outputs of several viewports.
 * The usual use case is when you want to apply some post process to the main viewport and then other post process to the gui viewport
 * @author Nehon
 */
public class TestPostFiltersCompositing extends SimpleApplication {

    public static void main(String[] args) {
        TestPostFiltersCompositing app = new TestPostFiltersCompositing();
//        AppSettings settings = new AppSettings(true);
//        settings.putBoolean("GraphicsDebug", false);
//        app.setSettings(settings);
        app.start();        
        
    }

    public void simpleInitApp() {
        this.flyCam.setMoveSpeed(10);
        cam.setLocation(new Vector3f(0.028406568f, 2.015769f, 7.386517f));
        cam.setRotation(new Quaternion(-1.0729783E-5f, 0.9999721f, -0.0073241726f, -0.0014647911f));


        makeScene();

        //Creating the main view port post processor
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        fpp.addFilter(new ColorOverlayFilter(ColorRGBA.Blue));
        viewPort.addProcessor(fpp);

        //creating a frame buffer for the mainviewport
        FrameBuffer mainVPFrameBuffer = new FrameBuffer(cam.getWidth(), cam.getHeight(), 1);
        Texture2D mainVPTexture = new Texture2D(cam.getWidth(), cam.getHeight(), Image.Format.RGBA8);
        mainVPFrameBuffer.addColorTexture(mainVPTexture);
        mainVPFrameBuffer.setDepthBuffer(Image.Format.Depth);
        viewPort.setOutputFrameBuffer(mainVPFrameBuffer);

        //creating the post processor for the gui viewport
        final FilterPostProcessor guifpp = new FilterPostProcessor(assetManager);
        guifpp.setFrameBufferFormat(Image.Format.RGBA8);
        guifpp.addFilter(new ColorOverlayFilter(ColorRGBA.Red));
        //this will compose the main viewport texture with the guiviewport back buffer.
        //Note that you can switch the order of the filters so that guiviewport filters are applied or not to the main viewport texture
        guifpp.addFilter(new ComposeFilter(mainVPTexture));

        guiViewPort.addProcessor(guifpp);
        
        //compositing is done by mixing texture depending on the alpha channel, 
        //it's important that the guiviewport clear color alpha value is set to 0
        guiViewPort.setBackgroundColor(ColorRGBA.BlackNoAlpha);
        guiViewPort.setClearColor(true);


    }

    private void makeScene() {
        // load sky
        rootNode.attachChild(SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", SkyFactory.EnvMapType.CubeMap));
        //assetManager.registerLocator("http://jmonkeyengine.googlecode.com/files/wildhouse.zip", HttpZipLocator.class);
        Spatial scene = assetManager.loadModel("Models/Test/CornellBox.j3o");
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.4790551f, -0.39247334f, -0.7851566f));
        scene.addLight(sun);
        rootNode.attachChild(scene);

    }
}
