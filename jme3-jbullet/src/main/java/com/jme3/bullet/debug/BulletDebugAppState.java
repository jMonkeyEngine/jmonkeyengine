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
package com.jme3.bullet.debug;

import com.jme3.app.Application;
import com.jme3.app.VRAppState;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.joints.PhysicsJoint;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.bullet.objects.PhysicsGhostObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.objects.PhysicsVehicle;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An app state to manage a debug visualization of a physics space.
 * <p>
 * This class is shared between JBullet and Native Bullet.
 *
 * @author normenhansen
 */
public class BulletDebugAppState extends AbstractAppState {

    /**
     * message logger for this class
     */
    protected static final Logger logger = Logger.getLogger(BulletDebugAppState.class.getName());

    /**
     * caches the virtual reality state (null means not yet determined)
     */
    private Boolean isVr = null;

    /**
     * limit which objects are visualized, or null to visualize all objects
     */
    protected DebugAppStateFilter filter;
    protected Application app;
    protected AssetManager assetManager;
    /**
     * physics space to visualize (not null)
     */
    protected final PhysicsSpace space;
    /**
     * scene-graph node to parent the geometries
     */
    protected final Node physicsDebugRootNode = new Node("Physics Debug Root Node");
    /**
     * view port in which to render (not null)
     */
    protected ViewPort viewPort;
    protected RenderManager rm;
    /**
     * material for inactive rigid bodies
     */
    public Material DEBUG_BLUE;
    public Material DEBUG_RED;
    /**
     * material for joints
     */
    public Material DEBUG_GREEN;
    /**
     * material for ghosts
     */
    public Material DEBUG_YELLOW;
    /**
     * material for vehicles and active rigid bodies
     */
    public Material DEBUG_MAGENTA;
    /**
     * material for physics characters
     */
    public Material DEBUG_PINK;
    /**
     * map rigid bodies to visualizations
     */
    protected HashMap<PhysicsRigidBody, Spatial> bodies = new HashMap<>();
    /**
     * map joints to visualizations
     */
    protected HashMap<PhysicsJoint, Spatial> joints = new HashMap<>();
    /**
     * map ghosts to visualizations
     */
    protected HashMap<PhysicsGhostObject, Spatial> ghosts = new HashMap<>();
    /**
     * map physics characters to visualizations
     */
    protected HashMap<PhysicsCharacter, Spatial> characters = new HashMap<>();
    /**
     * map vehicles to visualizations
     */
    protected HashMap<PhysicsVehicle, Spatial> vehicles = new HashMap<>();
    /**
     * Instantiate an app state to visualize the specified space. This constructor should be invoked only by
     * BulletAppState.
     *
     * @param space physics space to visualize (not null, alias created)
     */
    public BulletDebugAppState(PhysicsSpace space) {
        this.space = space;
    }

    public DebugTools getNewDebugTools() {
        return new DebugTools(assetManager);
    }

    /**
     * Alter which objects are visualized.
     *
     * @param filter the desired filter, or null to visualize all objects
     */
    public void setFilter(DebugAppStateFilter filter) {
        this.filter = filter;
    }
    
    /**
     * Initialize this state prior to its 1st update. Should be invoked only by
     * a subclass or by the AppStateManager.
     *
     * @param stateManager the manager for this state (not null)
     * @param app the application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.app = app;
        this.rm = app.getRenderManager();
        this.assetManager = app.getAssetManager();
        setupMaterials(app);
        physicsDebugRootNode.setCullHint(Spatial.CullHint.Never);

        if (isVr()) {
            /* This is a less good solution than the non-vr version (as the debug shapes can be obscured by the regular
            * geometry), however it is the best possible as VR does not currently support multiple viewports per eye */
            VRAppState vrAppState = stateManager.getState(VRAppState.ID, VRAppState.class);
            vrAppState.getLeftViewPort().attachScene(physicsDebugRootNode);
            vrAppState.getRightViewPort().attachScene(physicsDebugRootNode);
        } else {
            viewPort = rm.createMainView("Physics Debug Overlay", app.getCamera());
            viewPort.setClearFlags(false, true, false);
            viewPort.attachScene(physicsDebugRootNode);
        }
    }

    /**
     * Transition this state from terminating to detached. Should be invoked
     * only by a subclass or by the AppStateManager. Invoked once for each time
     * {@link #initialize(com.jme3.app.state.AppStateManager, com.jme3.app.Application)}
     * is invoked.
     */
    @Override
    public void cleanup() {
        if (isVr()) {
            VRAppState vrAppState = app.getStateManager().getState(VRAppState.ID, VRAppState.class);
            vrAppState.getLeftViewPort().detachScene(physicsDebugRootNode);
            vrAppState.getRightViewPort().detachScene(physicsDebugRootNode);
        } else {
            rm.removeMainView(viewPort);
        }

        super.cleanup();
    }

    /**
     * Update this state prior to rendering. Should be invoked only by a
     * subclass or by the AppStateManager. Invoked once per frame, provided the
     * state is attached and enabled.
     *
     * @param tpf the time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);
        //update all object links
        updateRigidBodies();
        updateGhosts();
        updateCharacters();
        updateJoints();
        updateVehicles();
        //update our debug root node
        physicsDebugRootNode.updateLogicalState(tpf);
        physicsDebugRootNode.updateGeometricState();
    }

    /**
     * Render this state. Should be invoked only by a subclass or by the
     * AppStateManager. Invoked once per frame, provided the state is attached
     * and enabled.
     *
     * @param rm the render manager (not null)
     */
    @Override
    public void render(RenderManager rm) {
        super.render(rm);
        if (viewPort != null) {
            rm.renderScene(physicsDebugRootNode, viewPort);
        }
    }

    /**
     * Initialize the materials.
     *
     * @param app the application which owns this state (not null)
     */
    private void setupMaterials(Application app) {
        AssetManager manager = app.getAssetManager();
        DEBUG_BLUE = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_BLUE.getAdditionalRenderState().setWireframe(true);
        DEBUG_BLUE.setColor("Color", ColorRGBA.Blue);
        DEBUG_GREEN = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_GREEN.getAdditionalRenderState().setWireframe(true);
        DEBUG_GREEN.setColor("Color", ColorRGBA.Green);
        DEBUG_RED = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_RED.getAdditionalRenderState().setWireframe(true);
        DEBUG_RED.setColor("Color", ColorRGBA.Red);
        DEBUG_YELLOW = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_YELLOW.getAdditionalRenderState().setWireframe(true);
        DEBUG_YELLOW.setColor("Color", ColorRGBA.Yellow);
        DEBUG_MAGENTA = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_MAGENTA.getAdditionalRenderState().setWireframe(true);
        DEBUG_MAGENTA.setColor("Color", ColorRGBA.Magenta);
        DEBUG_PINK = new Material(manager, "Common/MatDefs/Misc/Unshaded.j3md");
        DEBUG_PINK.getAdditionalRenderState().setWireframe(true);
        DEBUG_PINK.setColor("Color", ColorRGBA.Pink);
    }

    private void updateRigidBodies() {
        HashMap<PhysicsRigidBody, Spatial> oldObjects = bodies;
        bodies = new HashMap<PhysicsRigidBody, Spatial>();
        Collection<PhysicsRigidBody> current = space.getRigidBodyList();
        //create new map
        for (Iterator<PhysicsRigidBody> it = current.iterator(); it.hasNext();) {
            PhysicsRigidBody physicsObject = it.next();
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                bodies.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else {
                if (filter == null || filter.displayObject(physicsObject)) {
                    logger.log(Level.FINE, "Create new debug RigidBody");
                    //create new spatial
                    Node node = new Node(physicsObject.toString());
                    node.addControl(new BulletRigidBodyDebugControl(this, physicsObject));
                    bodies.put(physicsObject, node);
                    physicsDebugRootNode.attachChild(node);
                }
            }
        }
        //remove leftover spatials
        for (Map.Entry<PhysicsRigidBody, Spatial> entry : oldObjects.entrySet()) {
            PhysicsRigidBody object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }

    private void updateJoints() {
        HashMap<PhysicsJoint, Spatial> oldObjects = joints;
        joints = new HashMap<PhysicsJoint, Spatial>();
        Collection<PhysicsJoint> current = space.getJointList();
        //create new map
        for (Iterator<PhysicsJoint> it = current.iterator(); it.hasNext();) {
            PhysicsJoint physicsObject = it.next();
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                joints.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else {
                if (filter == null || filter.displayObject(physicsObject)) {
                    logger.log(Level.FINE, "Create new debug Joint");
                    //create new spatial
                    Node node = new Node(physicsObject.toString());
                    node.addControl(new BulletJointDebugControl(this, physicsObject));
                    joints.put(physicsObject, node);
                    physicsDebugRootNode.attachChild(node);
                }
            }
        }
        //remove leftover spatials
        for (Map.Entry<PhysicsJoint, Spatial> entry : oldObjects.entrySet()) {
            PhysicsJoint object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }

    private void updateGhosts() {
        HashMap<PhysicsGhostObject, Spatial> oldObjects = ghosts;
        ghosts = new HashMap<PhysicsGhostObject, Spatial>();
        Collection<PhysicsGhostObject> current = space.getGhostObjectList();
        //create new map
        for (Iterator<PhysicsGhostObject> it = current.iterator(); it.hasNext();) {
            PhysicsGhostObject physicsObject = it.next();
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                ghosts.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else {
                if (filter == null || filter.displayObject(physicsObject)) {
                    logger.log(Level.FINE, "Create new debug GhostObject");
                    //create new spatial
                    Node node = new Node(physicsObject.toString());
                    node.addControl(new BulletGhostObjectDebugControl(this, physicsObject));
                    ghosts.put(physicsObject, node);
                    physicsDebugRootNode.attachChild(node);
                }
            }
        }
        //remove leftover spatials
        for (Map.Entry<PhysicsGhostObject, Spatial> entry : oldObjects.entrySet()) {
            PhysicsGhostObject object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }

    private void updateCharacters() {
        HashMap<PhysicsCharacter, Spatial> oldObjects = characters;
        characters = new HashMap<PhysicsCharacter, Spatial>();
        Collection<PhysicsCharacter> current = space.getCharacterList();
        //create new map
        for (Iterator<PhysicsCharacter> it = current.iterator(); it.hasNext();) {
            PhysicsCharacter physicsObject = it.next();
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                characters.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else {
                if (filter == null || filter.displayObject(physicsObject)) {
                    logger.log(Level.FINE, "Create new debug Character");
                    //create new spatial
                    Node node = new Node(physicsObject.toString());
                    node.addControl(new BulletCharacterDebugControl(this, physicsObject));
                    characters.put(physicsObject, node);
                    physicsDebugRootNode.attachChild(node);
                }
            }
        }
        //remove leftover spatials
        for (Map.Entry<PhysicsCharacter, Spatial> entry : oldObjects.entrySet()) {
            PhysicsCharacter object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }

    private void updateVehicles() {
        HashMap<PhysicsVehicle, Spatial> oldObjects = vehicles;
        vehicles = new HashMap<PhysicsVehicle, Spatial>();
        Collection<PhysicsVehicle> current = space.getVehicleList();
        //create new map
        for (Iterator<PhysicsVehicle> it = current.iterator(); it.hasNext();) {
            PhysicsVehicle physicsObject = it.next();
            //copy existing spatials
            if (oldObjects.containsKey(physicsObject)) {
                Spatial spat = oldObjects.get(physicsObject);
                vehicles.put(physicsObject, spat);
                oldObjects.remove(physicsObject);
            } else {
                if (filter == null || filter.displayObject(physicsObject)) {
                    logger.log(Level.FINE, "Create new debug Vehicle");
                    //create new spatial
                    Node node = new Node(physicsObject.toString());
                    node.addControl(new BulletVehicleDebugControl(this, physicsObject));
                    vehicles.put(physicsObject, node);
                    physicsDebugRootNode.attachChild(node);
                }
            }
        }
        //remove leftover spatials
        for (Map.Entry<PhysicsVehicle, Spatial> entry : oldObjects.entrySet()) {
            PhysicsVehicle object = entry.getKey();
            Spatial spatial = entry.getValue();
            spatial.removeFromParent();
        }
    }

    /**
     * Interface to restrict which physics objects are visualized.
     */
    public static interface DebugAppStateFilter {

        /**
         * Test whether the specified physics object should be displayed.
         *
         * @param obj the joint or collision object to test (unaffected)
         * @return return true if the object should be displayed, false if not
         */
        public boolean displayObject(Object obj);
    }

    private boolean isVr() {
        if (isVr == null) {
            try {
                VRAppState vrAppState = app.getStateManager().getState(VRAppState.ID, VRAppState.class);
                isVr = vrAppState != null && !vrAppState.DISABLE_VR;
            } catch (NoClassDefFoundError e) {
                //Vr isn't even on the classpath
                isVr = false;
            }
        }
        return isVr;
    }
}