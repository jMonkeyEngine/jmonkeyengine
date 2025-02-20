/*
 * Copyright (c) 2025 jMonkeyEngine
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
package jme3test.model.anim;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.AnimLayer;
import com.jme3.anim.Armature;
import com.jme3.anim.ArmatureMask;
import com.jme3.anim.SingleLayerInfluenceMask;
import com.jme3.anim.SkinningControl;
import com.jme3.anim.tween.action.BlendableAction;
import com.jme3.anim.util.AnimMigrationUtils;
import com.jme3.app.SimpleApplication;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

/**
 * Tests {@link SingleLayerInfluenceMask}.
 *
 * The test runs two simultaneous looping actions on separate layers.
 * <p>
 * The test <strong>fails</strong> if the visible dancing action does <em>not</em>
 * loop seamlessly when using SingleLayerInfluenceMasks. Note that the action is
 * not expected to loop seamlessly when <em>not</em> using
 * SingleLayerArmatureMasks.
 * <p>
 * Press the spacebar to switch between using SingleLayerInfluenceMasks and
 * masks provided by {@link ArmatureMask}.
 *
 * @author codex
 */
public class TestSingleLayerInfluenceMask extends SimpleApplication implements ActionListener {

    private Spatial model;
    private AnimComposer animComposer;
    private SkinningControl skinningControl;
    private final String idleLayer = "idleLayer";
    private final String danceLayer = "danceLayer";
    private boolean useSingleLayerInfMask = true;
    private BitmapText display;

    public static void main(String[] args) {
        TestSingleLayerInfluenceMask app = new TestSingleLayerInfluenceMask();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        flyCam.setMoveSpeed(30f);
        
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-0.1f, -0.7f, -1).normalizeLocal());
        rootNode.addLight(dl);

        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        display = new BitmapText(font);
        display.setSize(font.getCharSet().getRenderedSize());
        display.setText("");
        display.setLocalTranslation(5, context.getSettings().getHeight() - 5, 0);
        guiNode.attachChild(display);

        inputManager.addMapping("SWITCH_MASKS", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "SWITCH_MASKS");

        setupModel();
        createAnimMasks();
        testSerialization();
        playAnimations();
        updateUI();
    }

    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(model.getWorldTranslation(), Vector3f.UNIT_Y);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("SWITCH_MASKS") && isPressed) {
            useSingleLayerInfMask = !useSingleLayerInfMask;
            animComposer.removeLayer(idleLayer);
            animComposer.removeLayer(danceLayer);
            
            createAnimMasks();
            playAnimations();
            updateUI();
        }
    }

    /**
     * Sets up the model by loading it, migrating animations, and attaching it to
     * the root node.
     */
    private void setupModel() {
        model = assetManager.loadModel("Models/Sinbad/SinbadOldAnim.j3o");
        // Migrate the model's animations to the new system
        AnimMigrationUtils.migrate(model);
        rootNode.attachChild(model);

        animComposer = model.getControl(AnimComposer.class);
        skinningControl = model.getControl(SkinningControl.class);

        ((BlendableAction) animComposer.action("Dance")).setMaxTransitionWeight(.9f);
        ((BlendableAction) animComposer.action("IdleTop")).setMaxTransitionWeight(.8f);
    }

    /**
     * Creates animation masks for the idle and dance layers.
     */
    private void createAnimMasks() {
        Armature armature = skinningControl.getArmature();
        ArmatureMask idleMask;
        ArmatureMask danceMask;

        if (useSingleLayerInfMask) {
            // Create single layer influence masks for idle and dance layers
            idleMask = new SingleLayerInfluenceMask(idleLayer, animComposer, armature);
            danceMask = new SingleLayerInfluenceMask(danceLayer, animComposer, armature);

        } else {
            // Create default armature masks for idle and dance layers
            idleMask = ArmatureMask.createMask(armature, "Root");
            danceMask = ArmatureMask.createMask(armature, "Root");
        }

        // Assign the masks to the respective animation layers
        animComposer.makeLayer(idleLayer, idleMask);
        animComposer.makeLayer(danceLayer, danceMask);
    }

    /**
     * Plays the "Dance" and "IdleTop" animations on their respective layers.
     */
    private void playAnimations() {
        animComposer.setCurrentAction("Dance", danceLayer);
        animComposer.setCurrentAction("IdleTop", idleLayer);
    }

    /**
     * Tests the serialization of animation masks.
     */
    private void testSerialization() {
        AnimComposer aComposer = model.getControl(AnimComposer.class);
        for (String layerName : aComposer.getLayerNames()) {

            System.out.println("layerName: " + layerName);
            AnimLayer layer = aComposer.getLayer(layerName);

            if (layer.getMask() instanceof SingleLayerInfluenceMask) {
                SingleLayerInfluenceMask mask = (SingleLayerInfluenceMask) layer.getMask();
                // Serialize and deserialize the mask
                mask = BinaryExporter.saveAndLoad(assetManager, mask);
                // Reassign the AnimComposer to the mask and remake the layer
                mask.setAnimComposer(aComposer);
                aComposer.makeLayer(layerName, mask);
            }
        }
    }

    private void updateUI() {
        display.setText("Using SingleLayerInfluenceMasks: " + useSingleLayerInfMask + "\nPress Spacebar to switch masks");
    }

}
