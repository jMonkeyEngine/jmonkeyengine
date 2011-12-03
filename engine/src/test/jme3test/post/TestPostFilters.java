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
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ColorOverlayFilter;
import com.jme3.post.filters.FadeFilter;
import com.jme3.post.filters.RadialBlurFilter;
import com.jme3.renderer.Caps;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

public class TestPostFilters extends SimpleApplication implements ActionListener {

    private FilterPostProcessor fpp;
    private Vector3f lightDir = new Vector3f(-1, -1, .5f).normalizeLocal();
    FadeFilter fade;

    public static void main(String[] args) {
        TestPostFilters app = new TestPostFilters();
        app.start();
    }

    public void setupFilters() {
        if (renderer.getCaps().contains(Caps.GLSL100)) {
            fpp = new FilterPostProcessor(assetManager);
            // fpp.setNumSamples(4);
            fpp.addFilter(new ColorOverlayFilter(ColorRGBA.LightGray));
            fpp.addFilter(new RadialBlurFilter());
            //fade=new FadeFilter(1.0f);
            //fpp.addFilter(fade);


            viewPort.addProcessor(fpp);
        }
    }

    public void setupSkyBox() {
        Texture envMap;
        if (renderer.getCaps().contains(Caps.FloatTexture)) {
            envMap = assetManager.loadTexture("Textures/Sky/St Peters/StPeters.hdr");
        } else {
            envMap = assetManager.loadTexture("Textures/Sky/St Peters/StPeters.jpg");
        }
        rootNode.attachChild(SkyFactory.createSky(assetManager, envMap, new Vector3f(-1, -1, -1), true));
    }

    public void setupLighting() {

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(lightDir);

        dl.setColor(new ColorRGBA(.9f, .9f, .9f, 1));

        rootNode.addLight(dl);

        dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, 0, -1).normalizeLocal());

        dl.setColor(new ColorRGBA(.4f, .4f, .4f, 1));

        rootNode.addLight(dl);
    }

    public void setupFloor() {
        Material mat = assetManager.loadMaterial("Textures/Terrain/BrickWall/BrickWall.j3m");
        mat.getTextureParam("DiffuseMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.getTextureParam("NormalMap").getTextureValue().setWrap(WrapMode.Repeat);
        mat.getTextureParam("ParallaxMap").getTextureValue().setWrap(WrapMode.Repeat);
        Box floor = new Box(Vector3f.ZERO, 50, 1f, 50);
        TangentBinormalGenerator.generate(floor);
        floor.scaleTextureCoordinates(new Vector2f(5, 5));
        Geometry floorGeom = new Geometry("Floor", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(floorGeom);
    }

    public void setupSignpost() {
        Spatial signpost = assetManager.loadModel("Models/Sign Post/Sign Post.mesh.xml");
        Material mat = assetManager.loadMaterial("Models/Sign Post/Sign Post.j3m");
        signpost.setMaterial(mat);
        signpost.rotate(0, FastMath.HALF_PI, 0);
        signpost.setLocalTranslation(12, 3.5f, 30);
        signpost.setLocalScale(4);
        signpost.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(signpost);
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(-32.295086f, 54.80136f, 79.59805f));
        cam.setRotation(new Quaternion(0.074364014f, 0.92519957f, -0.24794696f, 0.27748522f));
        cam.update();

        cam.setFrustumFar(300);
        flyCam.setMoveSpeed(30);

        rootNode.setCullHint(CullHint.Never);

        setupLighting();
        setupSkyBox();


        setupFloor();

        setupSignpost();

        setupFilters();

        initInput();

    }

    protected void initInput() {
        flyCam.setMoveSpeed(3);
        //init input
        inputManager.addMapping("fadein", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addListener(this, "fadein");
        inputManager.addMapping("fadeout", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addListener(this, "fadeout");

    }

    public void onAction(String name, boolean value, float tpf) {
        if (name.equals("fadein") && value) {
            fade.fadeIn();
            System.out.println("fade in");

        }
        if (name.equals("fadeout") && value) {
            fade.fadeOut();
            System.out.println("fade out");
        }
    }
}
