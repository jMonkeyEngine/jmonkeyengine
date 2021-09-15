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
package com.jme3.scene.debug.custom;

import com.jme3.anim.*;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.DirectionalLight;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.*;

import java.util.*;

/**
 * @author Nehon
 */
public class ArmatureDebugAppState extends BaseAppState {

    public static final float CLICK_MAX_DELAY = 0.2f;
    private Node debugNode = new Node("debugNode");
    private Map<Armature, ArmatureDebugger> armatures = new HashMap<>();
    private Map<Armature, Joint> selectedBones = new HashMap<>();
    private Application app;
    private boolean displayAllJoints = false;
    private float clickDelay = -1;
    Vector3f tmp = new Vector3f();
    Vector3f tmp2 = new Vector3f();
    ViewPort vp;

    @Override
    protected void initialize(Application app) {
        vp = app.getRenderManager().createMainView("debug", app.getCamera());
        vp.attachScene(debugNode);
        vp.setClearDepth(true);
        this.app = app;
        for (ArmatureDebugger armatureDebugger : armatures.values()) {
            armatureDebugger.initialize(app.getAssetManager(), app.getCamera());
        }
        app.getInputManager().addListener(actionListener, "shoot", "toggleJoints");
        app.getInputManager().addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        app.getInputManager().addMapping("toggleJoints", new KeyTrigger(KeyInput.KEY_F10));

        debugNode.addLight(new DirectionalLight(new Vector3f(-1f, -1f, -1f).normalizeLocal()));

        debugNode.addLight(new DirectionalLight(new Vector3f(1f, 1f, 1f).normalizeLocal(), new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f)));
        vp.setEnabled(false);
    }

    @Override
    protected void cleanup(Application app) {

    }

    @Override
    protected void onEnable() {
        vp.setEnabled(true);
    }

    @Override
    protected void onDisable() {
        vp.setEnabled(false);
    }

    @Override
    public void update(float tpf) {
        if (clickDelay > -1) {
            clickDelay += tpf;
        }
        debugNode.updateLogicalState(tpf);
        debugNode.updateGeometricState();

    }

    public ArmatureDebugger addArmatureFrom(SkinningControl skinningControl) {
        Armature armature = skinningControl.getArmature();
        Spatial forSpatial = skinningControl.getSpatial();
        return addArmatureFrom(armature, forSpatial);
    }

    public ArmatureDebugger addArmatureFrom(Armature armature, Spatial forSpatial) {

        ArmatureDebugger ad = armatures.get(armature);
        if(ad != null){
            return ad;
        }

        JointInfoVisitor visitor = new JointInfoVisitor(armature);
        forSpatial.depthFirstTraversal(visitor);

        ad = new ArmatureDebugger(forSpatial.getName() + "_Armature", armature, visitor.deformingJoints);
        ad.setLocalTransform(forSpatial.getWorldTransform());
        if (forSpatial instanceof Node) {
            List<Geometry> geoms = new ArrayList<>();
            findGeoms((Node) forSpatial, geoms);
            if (geoms.size() == 1) {
                ad.setLocalTransform(geoms.get(0).getWorldTransform());
            }
        }
        armatures.put(armature, ad);
        debugNode.attachChild(ad);
        if (isInitialized()) {
            ad.initialize(app.getAssetManager(), app.getCamera());
        }
        return ad;
    }

    private void findGeoms(Node node, List<Geometry> geoms) {
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                geoms.add((Geometry) spatial);
            } else if (spatial instanceof Node) {
                findGeoms((Node) spatial, geoms);
            }
        }
    }

    final private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals("shoot") && isPressed) {
                clickDelay = 0;
            }
            if (name.equals("shoot") && !isPressed && clickDelay < CLICK_MAX_DELAY) {
                Vector2f click2d = app.getInputManager().getCursorPosition();
                CollisionResults results = new CollisionResults();

                Camera camera = app.getCamera();
                Vector3f click3d = camera.getWorldCoordinates(click2d, 0f, tmp);
                Vector3f dir = camera.getWorldCoordinates(click2d, 1f, tmp2)
                        .subtractLocal(click3d)
                        .normalizeLocal();
                Ray ray = new Ray(click3d, dir);
                debugNode.collideWith(ray, results);

                if (results.size() == 0) {
                    for (ArmatureDebugger ad : armatures.values()) {
                        ad.select(null);
                    }
                    return;
                }
                
                // The closest result is the target that the player picked:
                Geometry target = results.getClosestCollision().getGeometry();
                for (ArmatureDebugger ad : armatures.values()) {
                    Joint selectedjoint = ad.select(target);
                    if (selectedjoint != null) {
                        selectedBones.put(ad.getArmature(), selectedjoint);
                        System.err.println("-----------------------");
                        System.err.println("Selected Joint : " + selectedjoint.getName() + " in armature " + ad.getName());
                        System.err.println("Root Bone : " + (selectedjoint.getParent() == null));
                        System.err.println("-----------------------");
                        System.err.println("Local translation: " + selectedjoint.getLocalTranslation());
                        System.err.println("Local rotation: " + selectedjoint.getLocalRotation());
                        System.err.println("Local scale: " + selectedjoint.getLocalScale());
                        System.err.println("---");
                        System.err.println("Model translation: " + selectedjoint.getModelTransform().getTranslation());
                        System.err.println("Model rotation: " + selectedjoint.getModelTransform().getRotation());
                        System.err.println("Model scale: " + selectedjoint.getModelTransform().getScale());
                        System.err.println("---");
                        System.err.println("Bind inverse Transform: ");
                        System.err.println(selectedjoint.getInverseModelBindMatrix());
                        return;
                    }
                }
            }
            if (name.equals("toggleJoints") && isPressed) {
                displayAllJoints = !displayAllJoints;
                for (ArmatureDebugger ad : armatures.values()) {
                    ad.displayNonDeformingJoint(displayAllJoints);
                }
            }
        }
    };

//    public Map<Skeleton, Bone> getSelectedBones() {
//        return selectedBones;
//    }

    public Node getDebugNode() {
        return debugNode;
    }

    public void setDebugNode(Node debugNode) {
        this.debugNode = debugNode;
    }

    private class JointInfoVisitor extends SceneGraphVisitorAdapter {

        List<Joint> deformingJoints = new ArrayList<>();
        Armature armature;

        public JointInfoVisitor(Armature armature) {
            this.armature = armature;
        }

        @Override
        public void visit(Geometry g) {
            for (Joint joint : armature.getJointList()) {
                if (g.getMesh().isAnimatedByJoint(armature.getJointIndex(joint))) {
                    deformingJoints.add(joint);
                }
            }
        }
    }
}
