/*
 * Copyright (c) 2026 jMonkeyEngine
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
package jme3test.app;

import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListenerAdapter;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.debug.Grid;
import com.jme3.system.AppSettings;


public class TestDisplayScaling extends SimpleApplication implements ActionListener {

    private static final String NEXT_SCALE_MODE = "NextScaleMode";
    private static final float SUPERSAMPLING = 2f;

    private BitmapText infoText;
    private Geometry cube;
    private Geometry testPanel;
    private BitmapText testText;
    private Node qualityTarget;
    private float requestedMode = Float.NaN;

    public static void main(String[] args) {
        TestDisplayScaling app = new TestDisplayScaling();
        AppSettings settings = new AppSettings(true);
        settings.setWindowSize(1280, 720);
        settings.setDisplayScaleMode(AppSettings.DISPLAY_SCALE_DISABLED);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        inputManager.addMapping("ScaleDisabled", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("ScaleNativePixels", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("ScaleDpiAware", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("ScaleSS", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping(NEXT_SCALE_MODE, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(this, "ScaleDisabled", "ScaleNativePixels", "ScaleDpiAware", "ScaleSS",
                NEXT_SCALE_MODE);
        inputManager.addRawInputListener(new RawInputListenerAdapter() {
            @Override
            public void onTouchEvent(TouchEvent evt) {
                if (evt.getType() == TouchEvent.Type.DOWN) {
                    requestCycleMode();
                }
            }
        });

        viewPort.setBackgroundColor(new ColorRGBA(0.08f, 0.09f, 0.11f, 1f));
        cam.setLocation(new Vector3f(0f, 0f, 6f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        Geometry fineGrid = new Geometry("Fine Edge Quality Grid", new Grid(121, 121, 0.05f));
        fineGrid.setMaterial(createMaterial(new ColorRGBA(0.75f, 0.78f, 0.84f, 1f)));
        fineGrid.setLocalTranslation(-3f, -3f, -1.35f);
        fineGrid.rotate(FastMath.HALF_PI, 0f, FastMath.QUARTER_PI);
        rootNode.attachChild(fineGrid);

        cube = new Geometry("High DPI Cube", new Box(1f, 1f, 1f));
        cube.setMaterial(createMaterial(new ColorRGBA(0.1f, 0.65f, 1f, 1f)));
        rootNode.attachChild(cube);

        testPanel = new Geometry("Logical GUI Test Panel", new Quad(180f, 80f));
        testPanel.setMaterial(createMaterial(new ColorRGBA(1f, 0.72f, 0.18f, 1f)));
        testPanel.setLocalTranslation(40f, 40f, 0f);
        guiNode.attachChild(testPanel);

        testText = new BitmapText(guiFont);
        testText.setText("TEST");
        testText.setSize(36f);
        testText.setColor(ColorRGBA.Black);
        testText.setLocalTranslation(60f, 98f, 1f);
        guiNode.attachChild(testText);

        createQualityTarget();

        infoText = new BitmapText(guiFont);
        infoText.setSize(18f);
        infoText.setLocalTranslation(20f, cam.getHeight() - 20f, 0f);
        guiNode.attachChild(infoText);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!Float.isNaN(requestedMode)) {
            setMode(requestedMode);
            return;
        }

        cube.rotate(tpf * 0.45f, tpf * 0.7f, 0f);
        Vector2f cursor = inputManager.getCursorPosition();
        infoText.setText("Mode: " + getDisplayScaleModeName(settings.getDisplayScaleMode())
                + "\nLogical: " + cam.getWidth() + " x " + cam.getHeight()
                + "\nRender target: " + viewPort.getRenderTargetWidth() + " x " + viewPort.getRenderTargetHeight()
                + "\nDrawable: " + context.getFramebufferWidth() + " x " + context.getFramebufferHeight()
                + "\nMouse: " + Math.round(cursor.x) + ", " + Math.round(cursor.y)
                + "\nKeys: 1 disabled, 2 native pixels, 3 DPI aware, 4 supersampling 2x"
                + "\nClick/touch: cycle mode");
        infoText.setLocalTranslation(20f, cam.getHeight() - 20f, 0f);
        qualityTarget.setLocalTranslation(cam.getWidth() - 390f, 40f, 0f);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) {
            return;
        }
        if ("ScaleDisabled".equals(name)) {
            requestMode(AppSettings.DISPLAY_SCALE_DISABLED);
        } else if ("ScaleNativePixels".equals(name)) {
            requestMode(AppSettings.DISPLAY_SCALE_NATIVE_PIXELS);
        } else if ("ScaleDpiAware".equals(name)) {
            requestMode(AppSettings.DISPLAY_SCALE_DPI_AWARE);
        } else if ("ScaleSS".equals(name)) {
            requestMode(SUPERSAMPLING);
        } else if (NEXT_SCALE_MODE.equals(name)) {
            requestCycleMode();
        }
    }

    private void requestCycleMode() {
        float currentMode = settings.getDisplayScaleMode();
        if (currentMode == AppSettings.DISPLAY_SCALE_DISABLED) {
            requestMode(AppSettings.DISPLAY_SCALE_NATIVE_PIXELS);
        } else if (currentMode == AppSettings.DISPLAY_SCALE_NATIVE_PIXELS) {
            requestMode(AppSettings.DISPLAY_SCALE_DPI_AWARE);
        } else if (currentMode == AppSettings.DISPLAY_SCALE_DPI_AWARE) {
            requestMode(SUPERSAMPLING);
        } else {
            requestMode(AppSettings.DISPLAY_SCALE_DISABLED);
        }
    }

    private void requestMode(float mode) {
        requestedMode = mode;
    }

    private String getDisplayScaleModeName(float mode) {
        if (mode == AppSettings.DISPLAY_SCALE_DISABLED) {
            return "DISABLED";
        } else if (mode == AppSettings.DISPLAY_SCALE_NATIVE_PIXELS) {
            return "NATIVE_PIXELS";
        } else if (mode == AppSettings.DISPLAY_SCALE_DPI_AWARE) {
            return "DPI_AWARE";
        }
        return "SUPERSAMPLING " + mode + "x";
    }

    private void setMode(float mode) {
        requestedMode = Float.NaN;
        settings.setDisplayScaleMode(mode);
        restart();
    }

    private void createQualityTarget() {
        qualityTarget = new Node("High DPI Quality Target");
        guiNode.attachChild(qualityTarget);

        Geometry background = new Geometry("Quality Target Background", new Quad(350f, 150f));
        background.setMaterial(createMaterial(new ColorRGBA(0.02f, 0.025f, 0.03f, 1f)));
        qualityTarget.attachChild(background);

        Material white = createMaterial(ColorRGBA.White);
        Material gray = createMaterial(new ColorRGBA(0.45f, 0.48f, 0.52f, 1f));

        for (int x = 20; x < 180; x += 2) {
            Geometry stripe = new Geometry("One Pixel Stripe", new Quad(1f, 56f));
            stripe.setMaterial(white);
            stripe.setLocalTranslation(x, 72f, 1f);
            qualityTarget.attachChild(stripe);
        }

        for (int i = 0; i < 18; i++) {
            Geometry diagonal = new Geometry("Subtle Diagonal Edge", new Quad(155f, 1f));
            diagonal.setMaterial(i % 2 == 0 ? white : gray);
            diagonal.setLocalTranslation(190f, 20f + i * 6f, 1f);
            diagonal.rotate(0f, 0f, 0.08f * i);
            qualityTarget.attachChild(diagonal);
        }

        BitmapText label = new BitmapText(guiFont);
        label.setText("1px stripes and tiny text");
        label.setSize(10f);
        label.setLocalTranslation(20f, 142f, 1f);
        qualityTarget.attachChild(label);

        BitmapText tinyText = new BitmapText(guiFont);
        tinyText.setText("abcdef0123456789 ABCDEF");
        tinyText.setSize(8f);
        tinyText.setLocalTranslation(20f, 58f, 1f);
        qualityTarget.attachChild(tinyText);
    }

    private Material createMaterial(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }
}
