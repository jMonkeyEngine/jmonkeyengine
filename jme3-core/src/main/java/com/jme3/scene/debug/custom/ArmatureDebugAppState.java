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
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.scene.debug.custom;

import com.jme3.anim.Armature;
import com.jme3.anim.Joint;
import com.jme3.anim.SkinningControl;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A debug application state for visualizing and interacting with JME3 armatures (skeletons).
 * This state allows users to see the joints of an armature, select individual joints
 * by clicking on them, and view their local and model transforms.
 * It also provides a toggle to display non-deforming joints.
 * <p>
 * This debug state operates on its own `ViewPort` and `debugNode` to prevent
 * interference with the main scene's rendering.
 *
 * @author Nehon
 * @author capdevon
 */
public class ArmatureDebugAppState extends BaseAppState {

    private static final Logger logger = Logger.getLogger(ArmatureDebugAppState.class.getName());

    private static final String PICK_JOINT = "ArmatureDebugAppState_PickJoint";
    private static final String TOGGLE_JOINTS = "ArmatureDebugAppState_DisplayAllJoints";

    /**
     * The maximum delay for a mouse click to be registered as a single click.
     */
    public static final float CLICK_MAX_DELAY = 0.2f;

    private Node debugNode = new Node("debugNode");
    private final Map<Armature, ArmatureDebugger> armatures = new HashMap<>();
    private final List<Consumer<Joint>> selectionListeners = new ArrayList<>();
    private boolean displayAllJoints = false;
    private float clickDelay = -1;
    private ViewPort vp;
    private Camera cam;
    private InputManager inputManager;
    private boolean showOnTop = true;
    private boolean enableLogging = true;

    @Override
    protected void initialize(Application app) {

        this.inputManager = app.getInputManager();
        this.cam = app.getCamera();

        vp = app.getRenderManager().createMainView("ArmatureDebugView", cam);
        vp.attachScene(debugNode);
        vp.setClearDepth(showOnTop);

        for (ArmatureDebugger debugger : armatures.values()) {
            debugger.initialize(app.getAssetManager(), cam);
        }

        // Initially disable the viewport until the state is enabled
        vp.setEnabled(false);

        registerInput();
    }

    private void registerInput() {
        inputManager.addMapping(PICK_JOINT, new MouseButtonTrigger(MouseInput.BUTTON_LEFT), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping(TOGGLE_JOINTS, new KeyTrigger(KeyInput.KEY_F10));
        inputManager.addListener(actionListener, PICK_JOINT, TOGGLE_JOINTS);
    }

    private void unregisterInput() {
        inputManager.deleteMapping(PICK_JOINT);
        inputManager.deleteMapping(TOGGLE_JOINTS);
        inputManager.removeListener(actionListener);
    }

    @Override
    protected void cleanup(Application app) {
        unregisterInput();
        app.getRenderManager().removeMainView(vp);
        // Clear maps to release references
        armatures.clear();
        selectionListeners.clear();
        debugNode.detachAllChildren();
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
    }

    @Override
    public void render(RenderManager rm) {
        debugNode.updateGeometricState();
    }

    /**
     * Adds an ArmatureDebugger for the armature associated with a given SkinningControl.
     *
     * @param skControl The SkinningControl whose armature needs to be debugged.
     * @return The newly created or existing ArmatureDebugger for the given armature.
     */
    public ArmatureDebugger addArmatureFrom(SkinningControl skControl) {
        return addArmatureFrom(skControl.getArmature(), skControl.getSpatial());
    }

    /**
     * Adds an ArmatureDebugger for a specific Armature, associating it with a Spatial.
     * If an ArmatureDebugger for this armature already exists, it is returned.
     * Otherwise, a new ArmatureDebugger is created, initialized, and attached to the debug node.
     *
     * @param armature The Armature to debug.
     * @param sp The Spatial associated with this armature (used for determining world transform and deforming joints).
     * @return The newly created or existing ArmatureDebugger for the given armature.
     */
    public ArmatureDebugger addArmatureFrom(Armature armature, Spatial sp) {

        ArmatureDebugger debugger = armatures.get(armature);
        if (debugger != null) {
            return debugger;
        }

        // Use a visitor to find joints that actually deform the mesh
        JointInfoVisitor visitor = new JointInfoVisitor(armature);
        sp.depthFirstTraversal(visitor);

        // Create a new ArmatureDebugger
        debugger = new ArmatureDebugger(sp.getName() + "_ArmatureDebugger", armature, visitor.deformingJoints);
        debugger.setLocalTransform(sp.getWorldTransform());
        if (sp instanceof Node) {
            List<Geometry> geoms = new ArrayList<>();
            findGeoms((Node) sp, geoms);
            if (geoms.size() == 1) {
                debugger.setLocalTransform(geoms.get(0).getWorldTransform());
            }
        }

        // Store and attach the new debugger
        armatures.put(armature, debugger);
        debugNode.attachChild(debugger);

        // If the AppState is already initialized, initialize the new ArmatureDebugger immediately
        if (isInitialized()) {
            AssetManager assetManager = getApplication().getAssetManager();
            debugger.initialize(assetManager, cam);
        }
        return debugger;
    }

    /**
     * Recursively finds all Geometry instances within a given Node and its children.
     *
     * @param node The starting Node to search from.
     * @param geoms The list to which found Geometry instances will be added.
     */
    private void findGeoms(Node node, List<Geometry> geoms) {
        for (Spatial s : node.getChildren()) {
            if (s instanceof Geometry) {
                geoms.add((Geometry) s);
            } else if (s instanceof Node) {
                findGeoms((Node) s, geoms);
            }
        }
    }

    /**
     * The ActionListener implementation to handle input events.
     * Specifically, it processes mouse clicks for joint selection and
     * the F10 key press for toggling display of all joints.
     */
    private final ActionListener actionListener = new ActionListener() {

        private final CollisionResults results = new CollisionResults();

        @Override
        public void onAction(String name, boolean isPressed, float tpf) {
            if (name.equals(PICK_JOINT)) {
                if (isPressed) {
                    // Start counting click delay on mouse press
                    clickDelay = 0;

                } else if (clickDelay < CLICK_MAX_DELAY) {
                    // Process click only if it's a quick release (not a hold)
                    Ray ray = screenPointToRay(cam, inputManager.getCursorPosition());
                    results.clear();
                    debugNode.collideWith(ray, results);

                    if (results.size() == 0) {
                        // If no collision, deselect all joints in all armatures
                        for (ArmatureDebugger ad : armatures.values()) {
                            ad.select(null);
                        }
                    } else {
                        // Get the closest geometry hit by the ray
                        Geometry target = results.getClosestCollision().getGeometry();
                        logger.log(Level.INFO, "Pick: {0}", target);

                        for (ArmatureDebugger ad : armatures.values()) {
                            Joint selectedjoint = ad.select(target);

                            if (selectedjoint != null) {
                                // If a joint was selected, notify it and print its properties
                                notifySelectionListeners(selectedjoint);
                                printJointInfo(selectedjoint, ad);
                                break;
                            }
                        }
                    }
                }
            }
            else if (name.equals(TOGGLE_JOINTS) && isPressed) {
                displayAllJoints = !displayAllJoints;
                for (ArmatureDebugger ad : armatures.values()) {
                    ad.displayNonDeformingJoint(displayAllJoints);
                }
            }
        }

        private void printJointInfo(Joint selectedjoint, ArmatureDebugger ad) {
            if (enableLogging) {
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
            }
        }

        private Ray screenPointToRay(Camera cam, Vector2f click2d) {
            TempVars vars = TempVars.get();
            Vector3f nearPoint = vars.vect1;
            Vector3f farPoint = vars.vect2;

            // Get the world coordinates for the near and far points
            cam.getWorldCoordinates(click2d, 0, nearPoint);
            cam.getWorldCoordinates(click2d, 1, farPoint);

            // Calculate direction and normalize
            Vector3f direction = farPoint.subtractLocal(nearPoint).normalizeLocal();
            Ray ray = new Ray(nearPoint, direction);

            vars.release();
            return ray;
        }
    };

    /**
     * Notifies all registered {@code Consumer<Joint>} listeners about the selected joint.
     *
     * @param selectedJoint The joint that was selected.
     */
    private void notifySelectionListeners(Joint selectedJoint) {
        for (Consumer<Joint> listener : selectionListeners) {
            listener.accept(selectedJoint);
        }
    }

    /**
     * Adds a listener that will be notified when a joint is selected.
     *
     * @param listener The {@code Consumer<Joint>} listener to add.
     */
    public void addSelectionListener(Consumer<Joint> listener) {
        selectionListeners.add(listener);
    }

    /**
     * Removes a previously added selection listener.
     *
     * @param listener The {@code Consumer<Joint>} listener to remove.
     */
    public void removeSelectionListener(Consumer<Joint> listener) {
        selectionListeners.remove(listener);
    }

    /**
     * Clears all registered selection listeners.
     */
    public void clearSelectionListeners() {
        selectionListeners.clear();
    }

    /**
     * Returns the root node for debug visualizations.
     * This node contains all the `ArmatureDebugger` instances.
     *
     * @return The debug Node.
     */
    public Node getDebugNode() {
        return debugNode;
    }

    @Deprecated
    public void setDebugNode(Node debugNode) {
        this.debugNode = debugNode;
    }

    /**
     * Checks if the armature debug gizmos are set to always
     * render on top of other scene geometry.
     *
     * @return true if gizmos always render on top, false otherwise.
     */
    public boolean isShowOnTop() {
        return showOnTop;
    }

    /**
     * Sets whether armature debug gizmos should always
     * render on top of other scene geometry.
     *
     * @param showOnTop true to always show gizmos on top, false to respect depth.
     */
    public void setShowOnTop(boolean showOnTop) {
        this.showOnTop = showOnTop;
        if (vp != null) {
            vp.setClearDepth(showOnTop);
        }
    }

    /**
     * Returns whether logging of detailed joint information is currently enabled.
     *
     * @return true if logging is enabled, false otherwise.
     */
    public boolean isEnableLogging() {
        return enableLogging;
    }

    /**
     * Sets whether logging of detailed joint information should be enabled.
     *
     * @param enableLogging true to enable logging, false to disable.
     */
    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    /**
     * A utility visitor class to traverse the scene graph and identify
     * which joints in a given armature are actually deforming a mesh.
     */
    private static class JointInfoVisitor extends SceneGraphVisitorAdapter {

        private final List<Joint> deformingJoints = new ArrayList<>();
        private final Armature armature;

        /**
         * Constructs a JointInfoVisitor for a specific armature.
         *
         * @param armature The armature whose deforming joints are to be identified.
         */
        public JointInfoVisitor(Armature armature) {
            this.armature = armature;
        }

        /**
         * Visits a Geometry node in the scene graph.
         * For each Geometry, it checks all joints in the associated armature
         * to see if they influence this mesh.
         *
         * @param g The Geometry node being visited.
         */
        @Override
        public void visit(Geometry g) {
            for (Joint joint : armature.getJointList()) {
                int index = armature.getJointIndex(joint);
                if (g.getMesh().isAnimatedByJoint(index)) {
                    deformingJoints.add(joint);
                }
            }
        }

    }
}
