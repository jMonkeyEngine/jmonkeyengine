/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
 * EXEMPLARY, OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3test.animation;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.SkinningControl;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingVolume;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.debug.WireBox;

/**
 * Demonstrates the toggleable per-frame bounding-volume update feature of
 * {@link SkinningControl}.
 *
 * <p>The Elephant model plays its "legUp" animation in a loop. A wireframe
 * box shows the world bounding volume of the model. Press {@code B} to
 * toggle {@link SkinningControl#setUpdateBounds(boolean) updateBounds}:
 *
 * <ul>
 *   <li><b>updateBounds OFF (default):</b> The bounding box stays fixed at
 *       the bind pose — it will not grow when the leg extends upward.</li>
 *   <li><b>updateBounds ON:</b> The bounding box tracks the animated pose
 *       correctly, expanding and contracting as the leg moves.</li>
 * </ul>
 *
 * @see SkinningControl#setUpdateBounds(boolean)
 */
public class TestAnimatedModelBound extends SimpleApplication {

    /** Root node of the loaded model. */
    private Node modelRoot;
    /** SkinningControl whose updateBounds flag we toggle. */
    private SkinningControl skinningControl;
    /** Wireframe box visualizing the world bounding volume each frame. */
    private Geometry boundGeom;
    /** Label shown in the top-left corner. */
    private BitmapText statusText;

    public static void main(String[] args) {
        TestAnimatedModelBound app = new TestAnimatedModelBound();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // ---------- lighting ----------
        rootNode.addLight(new AmbientLight(new ColorRGBA(0.3f, 0.3f, 0.3f, 1f)));
        DirectionalLight sun = new DirectionalLight(
                new Vector3f(-1f, -1f, -1f).normalizeLocal(),
                ColorRGBA.White);
        rootNode.addLight(sun);

        // ---------- camera ----------
        cam.setLocation(new Vector3f(0f, 2f, 8f));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(5f);

        // ---------- model ----------
        modelRoot = (Node) assetManager.loadModel("Models/Elephant/Elephant.mesh.xml");
        float scale = 0.04f;
        modelRoot.scale(scale);
        rootNode.attachChild(modelRoot);

        skinningControl = modelRoot.getControl(SkinningControl.class);
        // updateBounds is OFF by default — the bounding box will stay static.
        skinningControl.setHardwareSkinningPreferred(false); // easier to visualize with SW skinning

        AnimComposer composer = modelRoot.getControl(AnimComposer.class);
        composer.setCurrentAction("legUp");

        // ---------- bounding-box visualizer ----------
        // Create an unshaded wireframe geometry; we reposition it every frame.
        Material wireMat = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        wireMat.setColor("Color", ColorRGBA.Yellow);
        wireMat.getAdditionalRenderState().setWireframe(true);

        boundGeom = new Geometry("boundingBox", new WireBox(1f, 1f, 1f));
        boundGeom.setMaterial(wireMat);
        rootNode.attachChild(boundGeom);

        // ---------- HUD ----------
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        statusText = new BitmapText(guiFont);
        statusText.setSize(guiFont.getCharSet().getRenderedSize());
        statusText.setLocalTranslation(10f,
                settings.getHeight() - 10f, 0f);
        guiNode.attachChild(statusText);
        updateStatusText();

        // ---------- key binding ----------
        inputManager.addMapping("ToggleBounds",
                new KeyTrigger(KeyInput.KEY_B));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    boolean current = skinningControl.isUpdateBounds();
                    skinningControl.setUpdateBounds(!current);
                    updateStatusText();
                }
            }
        }, "ToggleBounds");
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Update the wireframe box to match the current world bounding volume.
        modelRoot.updateGeometricState();
        BoundingVolume wb = modelRoot.getWorldBound();
        if (wb instanceof BoundingBox) {
            BoundingBox bbox = (BoundingBox) wb;
            Vector3f center = bbox.getCenter();
            ((WireBox) boundGeom.getMesh()).updatePositions(
                    bbox.getXExtent(), bbox.getYExtent(), bbox.getZExtent());
            boundGeom.setLocalTranslation(center);
        }
    }

    /** Refreshes the HUD label that shows the current updateBounds state. */
    private void updateStatusText() {
        boolean on = skinningControl.isUpdateBounds();
        statusText.setText(
                "Press B to toggle updateBounds\n"
                + "updateBounds: " + (on ? "ON  — bounding box tracks the animated pose"
                                         : "OFF — bounding box stays at bind pose (default)"));
    }
}
