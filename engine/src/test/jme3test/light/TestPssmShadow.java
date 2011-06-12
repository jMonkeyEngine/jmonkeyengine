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
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.shadow.PssmShadowRenderer.CompareMode;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import java.util.Random;

public class TestPssmShadow extends SimpleApplication implements ActionListener {

    private Spatial teapot;
    private boolean renderShadows = true;
    private boolean hardwareShadows = false;
    private PssmShadowRenderer pssmRenderer;

    public static void main(String[] args){
        TestPssmShadow app = new TestPssmShadow();
        app.start();
    }

    public void loadScene(){
        Material mat = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        Material matSoil = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
        matSoil.setColor("Color", ColorRGBA.Cyan);

        teapot = new Geometry("sphere", new Sphere(30, 30, 2));
//        teapot = new Geometry("cube", new Box(1.0f, 1.0f, 1.0f));
//        teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalTranslation(0,0,10);

        teapot.setMaterial(mat);
        teapot.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(teapot);

        long seed = 1294719330150L; //System.currentTimeMillis();
        Random random = new Random(seed);
        System.out.println(seed);

        for (int i = 0; i < 30; i++) {
            Spatial t = teapot.clone(false);
            rootNode.attachChild(t);
            teapot.setLocalTranslation((float) random.nextFloat() * 3, (float) random.nextFloat() * 3, (i + 2));
        }

        Geometry soil = new Geometry("soil", new Box(new Vector3f(0, -13, 550), 800, 10, 700));
        soil.setMaterial(matSoil);
        soil.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(soil);

        for (int i = 0; i < 30; i++) {
            Spatial t = teapot.clone(false);
            t.setLocalScale(10.0f);
            rootNode.attachChild(t);
            teapot.setLocalTranslation((float) random.nextFloat() * 300, (float) random.nextFloat() * 30, 30 * (i + 2));
        }
    }

    @Override
    public void simpleInitApp() {
        // put the camera in a bad position
        cam.setLocation(new Vector3f(41.59757f, 34.38738f, 11.528807f));
        cam.setRotation(new Quaternion(0.2905285f, 0.3816416f, -0.12772122f, 0.86811876f));
        flyCam.setMoveSpeed(100);

        loadScene();
           
        pssmRenderer = new PssmShadowRenderer(assetManager, 1024, 3);
        pssmRenderer.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        pssmRenderer.setLambda(0.55f);
        pssmRenderer.setShadowIntensity(0.6f);
        pssmRenderer.setCompareMode(CompareMode.Software);
        pssmRenderer.setFilterMode(FilterMode.Bilinear);
        pssmRenderer.displayDebug();
        viewPort.addProcessor(pssmRenderer);
        initInputs();
    }

      private void initInputs() {
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("ShadowUp", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("ShadowDown", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("ThicknessUp", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("ThicknessDown", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("lambdaUp", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("lambdaDown", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("toggleHW", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addListener(this, "lambdaUp", "lambdaDown", "toggleHW", "toggle", "ShadowUp","ShadowDown","ThicknessUp","ThicknessDown");
    }

    public void onAction(String name, boolean keyPressed, float tpf) {
        if (name.equals("toggle") && keyPressed) {
            if (renderShadows) {
                renderShadows = false;
                viewPort.removeProcessor(pssmRenderer);
            } else {
                renderShadows = true;
                viewPort.addProcessor(pssmRenderer);
            }
        } else if (name.equals("toggleHW") && keyPressed) {
            hardwareShadows = !hardwareShadows;
            pssmRenderer.setCompareMode(hardwareShadows ? CompareMode.Hardware : CompareMode.Software);
            System.out.println("HW Shadows: " + hardwareShadows);
        }

        if (name.equals("lambdaUp") && keyPressed) {
            pssmRenderer.setLambda(pssmRenderer.getLambda() + 0.01f);
            System.out.println("Lambda : " + pssmRenderer.getLambda());
        } else if (name.equals("lambdaDown") && keyPressed) {
            pssmRenderer.setLambda(pssmRenderer.getLambda() - 0.01f);
            System.out.println("Lambda : " + pssmRenderer.getLambda());
        }

        if (name.equals("ShadowUp") && keyPressed) {
            pssmRenderer.setShadowIntensity(pssmRenderer.getShadowIntensity() + 0.1f);
            System.out.println("Shadow intensity : " + pssmRenderer.getShadowIntensity());
        }
        if (name.equals("ShadowDown") && keyPressed) {
            pssmRenderer.setShadowIntensity(pssmRenderer.getShadowIntensity() - 0.1f);
            System.out.println("Shadow intensity : " + pssmRenderer.getShadowIntensity());
        }
        if (name.equals("ThicknessUp") && keyPressed) {
            pssmRenderer.setEdgesThickness(pssmRenderer.getEdgesThickness() + 1);
            System.out.println("Shadow thickness : " + pssmRenderer.getEdgesThickness());
        }
        if (name.equals("ThicknessDown") && keyPressed) {
            pssmRenderer.setEdgesThickness(pssmRenderer.getEdgesThickness() - 1);
            System.out.println("Shadow thickness : " + pssmRenderer.getEdgesThickness());
        }
    }


}
