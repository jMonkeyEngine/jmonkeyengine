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
package jme3test.games;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.debug.DebugTools;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WorldOfInception - Find the galaxy center ;)
 *
 * @author normenhansen
 */
public class WorldOfInception extends SimpleApplication implements AnalogListener {

    //Assumptions: POI radius in world == 1, only one player, vector3f hash describes enough worlds
    private static final Logger logger = Logger.getLogger(WorldOfInception.class.getName());
    private static final Random random = new Random(System.currentTimeMillis());
    private static final float scaleDist = 10;
    private static final float poiRadius = 100;
    private static final int poiCount = 30;
    private static Material poiMaterial;
    private static Mesh poiMesh;
    private static Material ballMaterial;
    private static Mesh ballMesh;
    private static CollisionShape poiHorizonCollisionShape;
    private static CollisionShape poiCollisionShape;
    private static CollisionShape ballCollisionShape;
    private InceptionLevel currentLevel;
    private final Vector3f walkDirection = new Vector3f();
    private static DebugTools debugTools;

    public WorldOfInception() {
        //base level vector position hash == seed
        super(new InceptionLevel(null, Vector3f.ZERO));
        currentLevel = super.getStateManager().getState(InceptionLevel.class);
        currentLevel.takeOverParent();
        currentLevel.getRootNode().setLocalScale(Vector3f.UNIT_XYZ);
        currentLevel.getRootNode().setLocalTranslation(Vector3f.ZERO);
    }

    public static void main(String[] args) {
        WorldOfInception app = new WorldOfInception();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        //set far frustum only so we see the outer world longer
        cam.setFrustumFar(10000);
        cam.setLocation(Vector3f.ZERO);
        debugTools = new DebugTools(assetManager);
        rootNode.attachChild(debugTools.debugNode);
        poiMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        poiMaterial.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        poiMesh = new Sphere(16, 16, 1f);

        ballMaterial = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        ballMaterial.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        ballMaterial.setColor("Color", ColorRGBA.Red);
        ballMesh = new Sphere(16, 16, 1.0f);

        poiHorizonCollisionShape = new MeshCollisionShape(new Sphere(128, 128, poiRadius));
        poiCollisionShape = new SphereCollisionShape(1f);
        ballCollisionShape = new SphereCollisionShape(1f);
        setupKeys();
        setupDisplay();
        setupFog();
    }

    private void setupKeys() {
        inputManager.addMapping("StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("StrafeRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("StrafeUp", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("StrafeDown", new KeyTrigger(KeyInput.KEY_Z), new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Return", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("Esc", new KeyTrigger(KeyInput.KEY_ESCAPE));
        inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
        inputManager.addListener(this, "StrafeLeft", "StrafeRight", "Forward", "Back", "StrafeUp", "StrafeDown", "Space", "Reset", "Esc", "Up", "Down", "Left", "Right");
    }

    private void setupDisplay() {
        if (fpsText == null) {
            fpsText = new BitmapText(guiFont, false);
        }
        fpsText.setLocalScale(0.7f, 0.7f, 0.7f);
        fpsText.setLocalTranslation(0, fpsText.getLineHeight(), 0);
        fpsText.setText("");
        fpsText.setCullHint(Spatial.CullHint.Never);
        guiNode.attachChild(fpsText);
    }

    private void setupFog() {
        // use fog to give more sense of depth
        FilterPostProcessor fpp;
        FogFilter fog;
        fpp=new FilterPostProcessor(assetManager);
        fog=new FogFilter();
        fog.setFogColor(new ColorRGBA(0.0f, 0.0f, 0.0f, 1.0f));
        fog.setFogDistance(poiRadius);
        fog.setFogDensity(2.0f);
        fpp.addFilter(fog);
        viewPort.addProcessor(fpp);
    }

    public void onAnalog(String name, float value, float tpf) {
        Vector3f left = rootNode.getLocalRotation().mult(Vector3f.UNIT_X.negate());
        Vector3f forward = rootNode.getLocalRotation().mult(Vector3f.UNIT_Z.negate());
        Vector3f up = rootNode.getLocalRotation().mult(Vector3f.UNIT_Y);
        //TODO: properly scale input based on current scaling level
        tpf = tpf * (10 - (9.0f * currentLevel.getCurrentScaleAmount()));
        if (name.equals("StrafeLeft") && value > 0) {
            walkDirection.addLocal(left.mult(tpf));
        } else if (name.equals("StrafeRight") && value > 0) {
            walkDirection.addLocal(left.negate().multLocal(tpf));
        } else if (name.equals("Forward") && value > 0) {
            walkDirection.addLocal(forward.mult(tpf));
        } else if (name.equals("Back") && value > 0) {
            walkDirection.addLocal(forward.negate().multLocal(tpf));
        } else if (name.equals("StrafeUp") && value > 0) {
            walkDirection.addLocal(up.mult(tpf));
        } else if (name.equals("StrafeDown") && value > 0) {
            walkDirection.addLocal(up.negate().multLocal(tpf));
        } else if (name.equals("Up") && value > 0) {
            //TODO: rotate rootNode, needs to be global
        } else if (name.equals("Down") && value > 0) {
        } else if (name.equals("Left") && value > 0) {
        } else if (name.equals("Right") && value > 0) {
        } else if (name.equals("Esc")) {
            stop();
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        currentLevel = currentLevel.getCurrentLevel();
        currentLevel.move(walkDirection);
        fpsText.setText("Location: " + currentLevel.getCoordinates());
        walkDirection.set(Vector3f.ZERO);
    }

    public static class InceptionLevel extends AbstractAppState {

        private final InceptionLevel parent;
        private final Vector3f inParentPosition;
        private SimpleApplication application;
        private BulletAppState physicsState;
        private Node rootNode;
        private Vector3f playerPos;
        private InceptionLevel currentActiveChild;
        private InceptionLevel currentReturnLevel;
        private float curScaleAmount = 0;

        public InceptionLevel(InceptionLevel parent, Vector3f inParentPosition) {
            this.parent = parent;
            this.inParentPosition = inParentPosition;
        }

        @Override
        public void update(float tpf) {
            super.update(tpf);
            if (currentReturnLevel != this) {
                return;
            }
            debugTools.setYellowArrow(new Vector3f(0, 0, -2), playerPos.divide(poiRadius));
            float curLocalDist = getPlayerPosition().length();
            // If we are outside the range of one point of interest, move out to
            // the next upper level
            if (curLocalDist > poiRadius + FastMath.ZERO_TOLERANCE) { //DAFUQ normalize?
                if (parent == null) {
                    //TODO: could add new nodes coming in instead for literally endless space
                    logger.log(Level.INFO, "Hit event horizon");
                    currentReturnLevel = this;
                    return;
                }
                //give to parent
                logger.log(Level.INFO, "give to parent");;
                parent.takeOverChild(inParentPosition.add(playerPos.normalize()));
                application.getStateManager().attach(parent);
                currentReturnLevel = parent;
                return;
            }

            AppStateManager stateManager = application.getStateManager();
            // We create child positions based on the parent position hash so we
            // should in practice get the same galaxy w/o too many doubles
            // with each run with the same root vector.
            Vector3f[] vectors = getPositions(poiCount, inParentPosition.hashCode());
            for (int i = 0; i < vectors.length; i++) {
                Vector3f vector3f = vectors[i];
                //negative rootNode location is our actual player position
                Vector3f distVect = vector3f.subtract(playerPos);
                float distance = distVect.length();
                if (distance <= 1) {
                    checkActiveChild(vector3f);
                    float percent = 0;
                    curScaleAmount = 0;
                    this.scaleAsParent(percent, playerPos, distVect);
                    currentActiveChild.scaleAsChild(percent, distVect);
                    logger.log(Level.INFO, "Give over to child {0}", currentActiveChild);
                    currentActiveChild.takeOverParent();
                    stateManager.detach(this);
                    currentReturnLevel = currentActiveChild;
                    return;
                } else if (distance <= 1 + scaleDist) {
                    debugTools.setRedArrow(Vector3f.ZERO, distVect);
                    checkActiveChild(vector3f);
                    //TODO: scale percent nicer for less of an "explosion" effect
                    float percent = 1 - mapValue(distance - 1, 0, scaleDist, 0, 1);
                    curScaleAmount = percent;
                    rootNode.getChild(i).setCullHint(Spatial.CullHint.Always);
                    this.scaleAsParent(percent, playerPos, distVect);
                    currentActiveChild.scaleAsChild(percent, distVect);
                    currentReturnLevel = this;
                    return;
                } else if (currentActiveChild != null && currentActiveChild.getPositionInParent().equals(vector3f)) {
                    //TODO: doing this here causes problems when close to multiple pois
                    rootNode.getChild(i).setCullHint(Spatial.CullHint.Inherit);
                }
            }
            checkActiveChild(null);
            curScaleAmount = 0;
            rootNode.setLocalScale(1);
            rootNode.setLocalTranslation(playerPos.negate());
            debugTools.setRedArrow(Vector3f.ZERO, Vector3f.ZERO);
            debugTools.setBlueArrow(Vector3f.ZERO, Vector3f.ZERO);
            debugTools.setGreenArrow(Vector3f.ZERO, Vector3f.ZERO);
        }

        private void checkActiveChild(Vector3f vector3f) {
            AppStateManager stateManager = application.getStateManager();
            if(vector3f == null){
                if(currentActiveChild != null){
                    logger.log(Level.INFO, "Detach child {0}", currentActiveChild);
                    stateManager.detach(currentActiveChild);
                    currentActiveChild = null;
                }
                return;
            }
            if (currentActiveChild == null) {
                currentActiveChild = new InceptionLevel(this, vector3f);
                stateManager.attach(currentActiveChild);
                logger.log(Level.INFO, "Attach child {0}", currentActiveChild);
            } else if (!currentActiveChild.getPositionInParent().equals(vector3f)) {
                logger.log(Level.INFO, "Switching from child {0}", currentActiveChild);
                stateManager.detach(currentActiveChild);
                currentActiveChild = new InceptionLevel(this, vector3f);
                stateManager.attach(currentActiveChild);
                logger.log(Level.INFO, "Attach child {0}", currentActiveChild);
            }
        }

        private void scaleAsChild(float percent, Vector3f dist) {
            float childScale = mapValue(percent, 1.0f / poiRadius, 1);
            Vector3f distToHorizon = dist.normalize();
            Vector3f scaledDistToHorizon = distToHorizon.mult(childScale * poiRadius);
            Vector3f rootOff = dist.add(scaledDistToHorizon);
            debugTools.setBlueArrow(Vector3f.ZERO, rootOff);
            getRootNode().setLocalScale(childScale);
            getRootNode().setLocalTranslation(rootOff);
            //prepare player position already
            Vector3f playerPosition = dist.normalize().mult(-poiRadius);
            setPlayerPosition(playerPosition);
        }

        private void scaleAsParent(float percent, Vector3f playerPos, Vector3f dist) {
            float scale = mapValue(percent, 1.0f, poiRadius);
            Vector3f distToHorizon = dist.subtract(dist.normalize());
            Vector3f offLocation = playerPos.add(distToHorizon);
            Vector3f rootOff = offLocation.mult(scale).negate();
            rootOff.addLocal(dist);
            debugTools.setGreenArrow(Vector3f.ZERO, offLocation);
            getRootNode().setLocalScale(scale);
            getRootNode().setLocalTranslation(rootOff);
        }

        public void takeOverParent() {
            //got playerPos from scaleAsChild before
            getPlayerPosition().normalizeLocal().multLocal(poiRadius);
            currentReturnLevel = this;
        }

        public void takeOverChild(Vector3f playerPos) {
            this.playerPos.set(playerPos);
            currentReturnLevel = this;
        }

        public InceptionLevel getLastLevel(Ray pickRay) {
            // TODO: get a level based on positions getting ever more accurate,
            // from any given position
            return null;
        }

        public InceptionLevel getLevel(Vector3f... location) {
            // TODO: get a level based on positions getting ever more accurate,
            // from any given position
            return null;
        }

        private void initData() {
            getRootNode();
            physicsState = new BulletAppState();
            physicsState.startPhysics();
            physicsState.getPhysicsSpace().setGravity(Vector3f.ZERO);
            //horizon
            physicsState.getPhysicsSpace().add(new RigidBodyControl(poiHorizonCollisionShape, 0));
            int hashCode = inParentPosition.hashCode();
            Vector3f[] positions = getPositions(poiCount, hashCode);
            for (int i = 0; i < positions.length; i++) {
                Vector3f vector3f = positions[i];
                Geometry poiGeom = new Geometry("poi", poiMesh);
                poiGeom.setLocalTranslation(vector3f);
                poiGeom.setMaterial(poiMaterial);
                RigidBodyControl control = new RigidBodyControl(poiCollisionShape, 0);
                //!!! Important
                control.setApplyPhysicsLocal(true);
                poiGeom.addControl(control);
                physicsState.getPhysicsSpace().add(poiGeom);
                rootNode.attachChild(poiGeom);

            }
            //add balls after so first 10 geoms == locations
            for (int i = 0; i < positions.length; i++) {
                Vector3f vector3f = positions[i];
                Geometry ball = getRandomBall(vector3f);
                physicsState.getPhysicsSpace().add(ball);
                rootNode.attachChild(ball);
            }

        }

        private Geometry getRandomBall(Vector3f location) {
            Vector3f localLocation = new Vector3f();
            localLocation.set(location);
            localLocation.addLocal(new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize().mult(3));
            Geometry poiGeom = new Geometry("ball", ballMesh);
            poiGeom.setLocalTranslation(localLocation);
            poiGeom.setMaterial(ballMaterial);
            RigidBodyControl control = new RigidBodyControl(ballCollisionShape, 1);
            //!!! Important
            control.setApplyPhysicsLocal(true);
            poiGeom.addControl(control);
            float x = (random.nextFloat() - 0.5f) * 100;
            float y = (random.nextFloat() - 0.5f) * 100;
            float z = (random.nextFloat() - 0.5f) * 100;
            control.setLinearVelocity(new Vector3f(x, y, z));
            return poiGeom;
        }

        private void cleanupData() {
            physicsState.stopPhysics();
            //TODO: remove all objects?
            physicsState = null;
            rootNode = null;
        }

        @Override
        public void initialize(AppStateManager stateManager, Application app) {
            super.initialize(stateManager, app);
            //only generate data and attach node when we are actually attached (or picking)
            initData();
            application = (SimpleApplication) app;
            application.getRootNode().attachChild(getRootNode());
            application.getStateManager().attach(physicsState);
        }

        @Override
        public void cleanup() {
            super.cleanup();
            //detach everything when we are detached
            application.getRootNode().detachChild(rootNode);
            application.getStateManager().detach(physicsState);
            cleanupData();
        }

        public Node getRootNode() {
            if (rootNode == null) {
                rootNode = new Node("ZoomLevel");
                if (parent != null) {
                    rootNode.setLocalScale(1.0f / poiRadius);
                }
            }
            return rootNode;
        }

        public Vector3f getPositionInParent() {
            return inParentPosition;
        }

        public Vector3f getPlayerPosition() {
            if (playerPos == null) {
                playerPos = new Vector3f();
            }
            return playerPos;
        }

        public void setPlayerPosition(Vector3f vec) {
            if (playerPos == null) {
                playerPos = new Vector3f();
            }
            playerPos.set(vec);
        }

        public void move(Vector3f dir) {
            if (playerPos == null) {
                playerPos = new Vector3f();
            }
            playerPos.addLocal(dir);
        }

        public float getCurrentScaleAmount() {
            return curScaleAmount;
        }

        public InceptionLevel getParent() {
            return parent;
        }

        public InceptionLevel getCurrentLevel() {
            return currentReturnLevel;
        }

        public String getCoordinates() {
            InceptionLevel cur = this;
            StringBuilder strb = new StringBuilder();
            strb.insert(0, this.getPlayerPosition());
            strb.insert(0, this.getPositionInParent() + " / ");
            cur = cur.getParent();
            while (cur != null) {
                strb.insert(0, cur.getPositionInParent() + " / ");
                cur = cur.getParent();
            }
            return strb.toString();
        }
    }

    public static Vector3f[] getPositions(int count, long seed) {
        Random rnd = new Random(seed);
        Vector3f[] vectors = new Vector3f[count];
        for (int i = 0; i < count; i++) {
            vectors[i] = new Vector3f((rnd.nextFloat() - 0.5f) * poiRadius,
                    (rnd.nextFloat() - 0.5f) * poiRadius,
                    (rnd.nextFloat() - 0.5f) * poiRadius);
        }
        return vectors;
    }

    /**
     * Maps a value from 0-1 to a range from min to max.
     *
     * @param x
     * @param min
     * @param max
     * @return
     */
    public static float mapValue(float x, float min, float max) {
        return mapValue(x, 0, 1, min, max);
    }

    /**
     * Maps a value from inputMin to inputMax to a range from min to max.
     *
     * @param x
     * @param inputMin
     * @param inputMax
     * @param min
     * @param max
     * @return
     */
    public static float mapValue(float x, float inputMin, float inputMax, float min, float max) {
        return (x - inputMin) * (max - min) / (inputMax - inputMin) + min;
    }
}
