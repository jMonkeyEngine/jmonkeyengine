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
package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.PssmShadowFilter;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.shadow.PssmShadowRenderer.CompareMode;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;
import java.io.IOException;

public class TestPssmShadow extends SimpleApplication implements ActionListener {

    private Spatial[] obj;
    private Material[] mat;
    private boolean hardwareShadows = false;
    private PssmShadowRenderer pssmRenderer;
    private PssmShadowFilter pssmFilter;
    private Geometry ground;
    private Material matGroundU;
    private Material matGroundL;

    public static void main(String[] args) {
        TestPssmShadow app = new TestPssmShadow();
        app.start();
    }

    public void loadScene() {
        obj = new Spatial[2];
        mat = new Material[2];
        mat[0] = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        mat[1] = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        mat[1].setBoolean("UseMaterialColors", true);
        mat[1].setColor("Ambient", ColorRGBA.White.mult(0.5f));
        mat[1].setColor("Diffuse", ColorRGBA.White.clone());


        obj[0] = new Geometry("sphere", new Sphere(30, 30, 2));
        obj[0].setShadowMode(ShadowMode.CastAndReceive);
        obj[1] = new Geometry("cube", new Box(1.0f, 1.0f, 1.0f));
        obj[1].setShadowMode(ShadowMode.CastAndReceive);
        TangentBinormalGenerator.generate(obj[1]);
        TangentBinormalGenerator.generate(obj[0]);


        for (int i = 0; i < 60; i++) {
            Spatial t = obj[FastMath.nextRandomInt(0, obj.length - 1)].clone(false);
            t.setLocalScale(FastMath.nextRandomFloat() * 10f);
            t.setMaterial(mat[FastMath.nextRandomInt(0, mat.length - 1)]);
            rootNode.attachChild(t);
            t.setLocalTranslation(FastMath.nextRandomFloat() * 200f, FastMath.nextRandomFloat() * 30f + 20, 30f * (i + 2f));
        }

        Box b = new Box(new Vector3f(0, 10, 550), 1000, 2, 1000);
        b.scaleTextureCoordinates(new Vector2f(10, 10));
        ground = new Geometry("soil", b);
        matGroundU = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matGroundU.setColor("Color", ColorRGBA.Green);


        matGroundL = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matGroundL.setTexture("DiffuseMap", grass);

        ground.setMaterial(matGroundL);

        ground.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(ground);

        l = new DirectionalLight();
        l.setDirection(new Vector3f(-1, -1, -1));
        rootNode.addLight(l);

        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.5f));
        rootNode.addLight(al);

        Spatial sky = SkyFactory.createSky(assetManager, "Scenes/Beach/FullskiesSunset0068.dds", false);
        sky.setLocalScale(350);

        rootNode.attachChild(sky);
    }
    DirectionalLight l;

    @Override
    public void simpleInitApp() {
        // put the camera in a bad position
        cam.setLocation(new Vector3f(65.25412f, 44.38738f, 9.087874f));
        cam.setRotation(new Quaternion(0.078139365f, 0.050241485f, -0.003942559f, 0.9956679f));

        flyCam.setMoveSpeed(100);

        loadScene();

        pssmRenderer = new PssmShadowRenderer(assetManager, 1024, 3);
        //pssmRenderer.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        pssmRenderer.setDirection(new Vector3f(-0.5973172f, -0.56583486f, 0.8846725f).normalizeLocal());
        pssmRenderer.setLambda(0.55f);
        pssmRenderer.setShadowIntensity(0.6f);
        pssmRenderer.setCompareMode(CompareMode.Software);
        pssmRenderer.setFilterMode(FilterMode.Dither);
                
        pssmRenderer.displayFrustum();
        viewPort.addProcessor(pssmRenderer);



        pssmFilter = new PssmShadowFilter(assetManager, 1024, 3);
        //pssmFilter.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        pssmRenderer.setDirection(new Vector3f(-0.5973172f, -0.56583486f, 0.8846725f).normalizeLocal());
        pssmFilter.setLambda(0.55f);
        pssmFilter.setShadowIntensity(0.6f);
        pssmFilter.setCompareMode(CompareMode.Software);
        pssmFilter.setFilterMode(FilterMode.Dither);
        pssmFilter.setEnabled(false);
        

//        pssmFilter.setShadowZFadeLength(300);
//        pssmFilter.setShadowZExtend(500);
        
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        //  fpp.setNumSamples(4);
        fpp.addFilter(pssmFilter);

        viewPort.addProcessor(fpp);


        initInputs();
    }
    BitmapText infoText;
    
    private void initInputs() {
        /** Write text on the screen (HUD) */
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        infoText = new BitmapText(guiFont, false);
        infoText.setSize(guiFont.getCharSet().getRenderedSize());


        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("changeFiltering", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("ShadowUp", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("ShadowDown", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("ThicknessUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("ThicknessDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("lambdaUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("lambdaDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("toggleHW", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("switchGroundMat", new KeyTrigger(KeyInput.KEY_M));
        inputManager.addMapping("splits", new KeyTrigger(KeyInput.KEY_X));

        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_NUMPAD8));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_NUMPAD2));
        inputManager.addMapping("right", new KeyTrigger(KeyInput.KEY_NUMPAD6));
        inputManager.addMapping("left", new KeyTrigger(KeyInput.KEY_NUMPAD4));
        inputManager.addMapping("fwd", new KeyTrigger(KeyInput.KEY_PGUP));
        inputManager.addMapping("back", new KeyTrigger(KeyInput.KEY_PGDN));



        inputManager.addListener(this, "lambdaUp", "lambdaDown", "toggleHW", "toggle", "ShadowUp", "ShadowDown", "ThicknessUp", "ThicknessDown", "changeFiltering",
                "switchGroundMat", "splits", "up", "down", "right", "left", "fwd", "back");

    }

    private void print(String str) {
        infoText.setText(str);
        infoText.setLocalTranslation(cam.getWidth() * 0.5f - infoText.getLineWidth() * 0.5f, infoText.getLineHeight(), 0);
        guiNode.attachChild(infoText);
        infoText.removeControl(ctrl);
        infoText.addControl(ctrl);
    }
    AbstractControl ctrl = new AbstractControl() {

        float time;

        @Override
        protected void controlUpdate(float tpf) {
            time += tpf;
            if (time > 3) {
                spatial.removeFromParent();
                spatial.removeControl(this);
            }
        }

        @Override
        public void setSpatial(Spatial spatial) {
            super.setSpatial(spatial);
            time = 0;
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
        }

        public Control cloneForSpatial(Spatial spatial) {
            return null;
        }
    };
    int filteringIndex = 2;
    int renderModeIndex = 0;

    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("toggle") && keyPressed) {
            renderModeIndex += 1;
            renderModeIndex %= 3;

            switch (renderModeIndex) {
                case 0:
                    viewPort.addProcessor(pssmRenderer);
                    break;
                case 1:
                    viewPort.removeProcessor(pssmRenderer);
                    pssmFilter.setEnabled(true);
                    break;
                case 2:
                    pssmFilter.setEnabled(false);
                    break;
            }



        } else if (name.equals("toggleHW") && keyPressed) {
            hardwareShadows = !hardwareShadows;
            pssmRenderer.setCompareMode(hardwareShadows ? CompareMode.Hardware : CompareMode.Software);
            pssmFilter.setCompareMode(hardwareShadows ? CompareMode.Hardware : CompareMode.Software);
            System.out.println("HW Shadows: " + hardwareShadows);
        }
//
//          renderShadows = true;
//                viewPort.addProcessor(pssmRenderer);

        if (name.equals("changeFiltering") && keyPressed) {
            filteringIndex = (filteringIndex + 1) % FilterMode.values().length;
            FilterMode m = FilterMode.values()[filteringIndex];
            pssmRenderer.setFilterMode(m);
            pssmFilter.setFilterMode(m);
            print("Filter mode : " + m.toString());
        }

        if (name.equals("lambdaUp") && keyPressed) {
            pssmRenderer.setLambda(pssmRenderer.getLambda() + 0.01f);
            pssmFilter.setLambda(pssmRenderer.getLambda() + 0.01f);
            System.out.println("Lambda : " + pssmRenderer.getLambda());
        } else if (name.equals("lambdaDown") && keyPressed) {
            pssmRenderer.setLambda(pssmRenderer.getLambda() - 0.01f);
            pssmFilter.setLambda(pssmRenderer.getLambda() - 0.01f);
            System.out.println("Lambda : " + pssmRenderer.getLambda());
        }

        if (name.equals("ShadowUp") && keyPressed) {
            pssmRenderer.setShadowIntensity(pssmRenderer.getShadowIntensity() + 0.1f);
            pssmFilter.setShadowIntensity(pssmRenderer.getShadowIntensity() + 0.1f);
            System.out.println("Shadow intensity : " + pssmRenderer.getShadowIntensity());
        }
        if (name.equals("ShadowDown") && keyPressed) {
            pssmRenderer.setShadowIntensity(pssmRenderer.getShadowIntensity() - 0.1f);
            pssmFilter.setShadowIntensity(pssmRenderer.getShadowIntensity() - 0.1f);
            System.out.println("Shadow intensity : " + pssmRenderer.getShadowIntensity());
        }
        if (name.equals("ThicknessUp") && keyPressed) {
            pssmRenderer.setEdgesThickness(pssmRenderer.getEdgesThickness() + 1);
            pssmFilter.setEdgesThickness(pssmRenderer.getEdgesThickness() + 1);
            System.out.println("Shadow thickness : " + pssmRenderer.getEdgesThickness());
        }
        if (name.equals("ThicknessDown") && keyPressed) {
            pssmRenderer.setEdgesThickness(pssmRenderer.getEdgesThickness() - 1);
            pssmFilter.setEdgesThickness(pssmRenderer.getEdgesThickness() - 1);
            System.out.println("Shadow thickness : " + pssmRenderer.getEdgesThickness());
        }
        if (name.equals("switchGroundMat") && keyPressed) {
            if (ground.getMaterial() == matGroundL) {
                ground.setMaterial(matGroundU);
            } else {
                ground.setMaterial(matGroundL);
            }
        }

//        if (name.equals("splits") && keyPressed) {
//            pssmRenderer.displayFrustum();
//        }


        if (name.equals("up")) {
            up = keyPressed;
        }
        if (name.equals("down")) {
            down = keyPressed;
        }
        if (name.equals("right")) {
            right = keyPressed;
        }
        if (name.equals("left") ) {
            left = keyPressed;
        }
        if (name.equals("fwd")) {
            fwd = keyPressed;
        }
        if (name.equals("back")) {
            back = keyPressed;
        }

    }
    boolean up = false;
    boolean down = false;
    boolean left = false;
    boolean right = false;
    boolean fwd = false;
    boolean back = false;
    float time = 0;
    float s = 1f;

    @Override
    public void simpleUpdate(float tpf) {
        if (up) {
            Vector3f v = l.getDirection();
            v.y += tpf / s;
            setDir(v);
        }
        if (down) {
            Vector3f v = l.getDirection();
            v.y -= tpf / s;
            setDir(v);
        }
        if (right) {
            Vector3f v = l.getDirection();
            v.x += tpf / s;
            setDir(v);
        }
        if (left) {
            Vector3f v = l.getDirection();
            v.x -= tpf / s;
            setDir(v);
        }
        if (fwd) {
            Vector3f v = l.getDirection();
            v.z += tpf / s;
            setDir(v);
        }
        if (back) {
            Vector3f v = l.getDirection();
            v.z -= tpf / s;
            setDir(v);
        }

    }

    private void setDir(Vector3f v) {
        l.setDirection(v);
        pssmFilter.setDirection(v);
        pssmRenderer.setDirection(v);
    }
}
