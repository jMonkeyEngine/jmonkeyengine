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
package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.HttpZipLocator;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.TempVars;

/**
 * Tests the setting of the FOV and aspect ratios.
 *
 * @author Markil 3
 */
public class TestAspectFov extends SimpleApplication implements ActionListener, AnalogListener {
    final String FOV_IN = "fovIn", FOV_OUT = "fovOut", ASPECT_IN = "aspectIn", ASPECT_OUT = "aspectOut";
    final float[] ratios = new float[]{2F / 1F, 3F / 1F, 4F / 3F, 5F / 4F, 16F / 9F, 1F / 2F, 1F / 3F, 3F / 4F, 4F / 5F, 9F / 16F};
    private int ratioIndex = 0;
    private BitmapText header, fov, aspect;

    public static void main(String[] args) {
        new TestAspectFov().start();
    }

    @Override
    public void simpleInitApp() {
        header = new BitmapText(this.guiFont);
        header.setText("Adjust FOV with R/F, adjust Aspect Ratio with T/G");
        guiNode.attachChild(header);
        fov = new BitmapText(this.guiFont);
        guiNode.attachChild(fov);
        aspect = new BitmapText(this.guiFont);
        guiNode.attachChild(aspect);

        viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
        flyCam.setMoveSpeed(100);

        // We add light so we see the scene
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(1.3f));
        rootNode.addLight(al);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White);
        dl.setDirection(new Vector3f(2.8f, -2.8f, -2.8f).normalizeLocal());
        rootNode.addLight(dl);

        assetManager.registerLocator("https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/jmonkeyengine/town.zip",
                HttpZipLocator.class);
        Spatial sceneModel = assetManager.loadModel("main.scene");
        sceneModel.setLocalScale(2f);

        rootNode.attachChild(sceneModel);

        inputManager.addMapping(FOV_IN, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping(FOV_OUT, new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping(ASPECT_IN, new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping(ASPECT_OUT, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener(this, FOV_IN, FOV_OUT, ASPECT_IN, ASPECT_OUT);
    }

    @Override
    public void update() {
        TempVars vars = TempVars.get();
        super.update();
        header.setLocalTranslation(0, cam.getHeight(), 0);
        vars.vect1.set(header.getLocalTranslation());
        vars.vect1.subtractLocal(0, header.getLineHeight(), 0);
        fov.setLocalTranslation(vars.vect1);
        fov.setText("FOV: " + cam.getFov());
        vars.vect1.subtractLocal(0, fov.getLineHeight(), 0);
        aspect.setLocalTranslation(vars.vect1);
        aspect.setText("Aspect Ratio: " + cam.getAspect());
        vars.release();
    }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        if (pressed) {
            switch (name) {
                case ASPECT_IN:
                    ratioIndex--;
                    if (ratioIndex < 0) {
                        ratioIndex = ratios.length - 1;
                    }
                    cam.setAspect(ratios[ratioIndex]);
                    break;
                case ASPECT_OUT:
                    ratioIndex = (ratioIndex + 1) % ratios.length;
                    cam.setAspect(ratios[ratioIndex]);
                    break;
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        switch (name) {
            case FOV_IN:
                cam.setFov(cam.getFov() - tpf * 10);
                break;
            case FOV_OUT:
                cam.setFov(cam.getFov() + tpf * 10);
                break;
        }
    }
}
