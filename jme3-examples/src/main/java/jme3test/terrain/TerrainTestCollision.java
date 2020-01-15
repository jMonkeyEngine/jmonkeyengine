/*
 * Copyright (c) 2009-2018 jMonkeyEngine
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
package jme3test.terrain;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.geomipmap.lodcalc.DistanceLodCalculator;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a terrain object and a collision node to go with it. Then
 * drops several balls from the sky that collide with the terrain
 * and roll around.
 * Left click to place a sphere on the ground where the crosshairs intersect the terrain.
 * Hit keys 1 or 2 to raise/lower the terrain at that spot.
 *
 * @author Brent Owens
 */
public class TerrainTestCollision extends SimpleApplication {

    TerrainQuad terrain;
    Node terrainPhysicsNode;
    Material matRock;
    Material matWire;
    boolean wireframe = false;
    protected BitmapText hintText;
    PointLight pl;
    Geometry lightMdl;
    List<Geometry> collisionMarkers;
    private BulletAppState bulletAppState;
    Geometry collisionSphere;
    Geometry collisionBox;
    Geometry selectedCollisionObject;

    public static void main(String[] args) {
        TerrainTestCollision app = new TerrainTestCollision();
        app.start();
    }

    @Override
    public void initialize() {
        super.initialize();
        loadHintText();
        initCrossHairs();
    }

    @Override
    public void simpleInitApp() {
        collisionMarkers = new ArrayList<>();
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        setupKeys();
        matRock = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
        matRock.setTexture("Alpha", assetManager.loadTexture("Textures/Terrain/splat/alphamap.png"));
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/mountains512.png");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matRock.setTexture("Tex1", grass);
        matRock.setFloat("Tex1Scale", 64f);
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matRock.setTexture("Tex2", dirt);
        matRock.setFloat("Tex2Scale", 32f);
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matRock.setTexture("Tex3", rock);
        matRock.setFloat("Tex3Scale", 128f);
        matWire = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matWire.getAdditionalRenderState().setWireframe(true);
        matWire.setColor("Color", ColorRGBA.Green);
        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(heightMapImage.getImage(), 0.25f);
            heightmap.load();

        } catch (Exception e) {
        }

        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        TerrainLodControl control = new TerrainLodControl(terrain, getCamera());
        control.setLodCalculator( new DistanceLodCalculator(65, 2.7f) ); // patch size, and a multiplier
        terrain.addControl(control);
        terrain.setMaterial(matRock);
        terrain.setLocalScale(new Vector3f(2, 2, 2));
        terrain.setLocked(false); // unlock it so we can edit the height
        rootNode.attachChild(terrain);

        // if set to false, only the first collision is returned and collision is slightly faster.
        terrain.setSupportMultipleCollisions(true);

        /**
         * Create PhysicsRigidBodyControl for collision
         */
        terrain.addControl(new RigidBodyControl(0));
        bulletAppState.getPhysicsSpace().addAll(terrain);


        // Add 5 physics spheres to the world, with random sizes and positions
        // let them drop from the sky
        for (int i = 0; i < 5; i++) {
            float r = (float) (8 * Math.random());
            Geometry sphere = new Geometry("cannonball", new Sphere(10, 10, r));
            sphere.setMaterial(matWire);
            float x = (float) (20 * Math.random()) - 40; // random position
            float y = (float) (20 * Math.random()) - 40; // random position
            float z = (float) (20 * Math.random()) - 40; // random position
            sphere.setLocalTranslation(new Vector3f(x, 100 + y, z));
            sphere.addControl(new RigidBodyControl(new SphereCollisionShape(r), 2));
            rootNode.attachChild(sphere);
            bulletAppState.getPhysicsSpace().add(sphere);
        }

        collisionBox = new Geometry("collisionBox", new Box(2, 2, 2));
        collisionBox.setModelBound(new BoundingBox());
        collisionBox.setLocalTranslation(new Vector3f(20, 95, 30));
        collisionBox.setMaterial(matWire);
        rootNode.attachChild(collisionBox);
        selectedCollisionObject = collisionBox;

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -0.5f, -0.1f).normalizeLocal());
        dl.setColor(new ColorRGBA(0.50f, 0.40f, 0.50f, 1.0f));
        rootNode.addLight(dl);

        cam.setLocation(new Vector3f(43f, 121f, 10f));
        cam.setRotation(new Quaternion(0.15824f, -0.79309f, 0.23223f, 0.5404f));
    }

    public void loadHintText() {
        hintText = new BitmapText(guiFont, false);
        hintText.setSize(guiFont.getCharSet().getRenderedSize());
        hintText.setLocalTranslation(0, getCamera().getHeight(), 0);
        hintText.setText("Press T to toggle wireframe");
        guiNode.attachChild(hintText);
    }

    protected void initCrossHairs() {
        //guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }

    private void setupKeys() {
        flyCam.setMoveSpeed(50);
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addListener(actionListener, "wireframe");
        inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
        inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
        inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
        inputManager.addMapping("Forwards", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addMapping("Backs", new KeyTrigger(KeyInput.KEY_I));
        inputManager.addListener(actionListener, "Lefts");
        inputManager.addListener(actionListener, "Rights");
        inputManager.addListener(actionListener, "Ups");
        inputManager.addListener(actionListener, "Downs");
        inputManager.addListener(actionListener, "Forwards");
        inputManager.addListener(actionListener, "Backs");
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");
        inputManager.addMapping("cameraDown", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addListener(actionListener, "cameraDown");
    }

    @Override
    public void update() {
        super.update();
    }

    private void createCollisionMarkers(int num) {
        for (int i = 0; i < num; i++) {
            Sphere s = new Sphere(6, 6, 1);
            Geometry collisionMarker = new Geometry("collisionMarker");
            collisionMarker.setMesh(s);
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", i == 0 ? ColorRGBA.Orange : ColorRGBA.Blue);
            collisionMarker.setMaterial(mat);
            rootNode.attachChild(collisionMarker);
            collisionMarkers.add(collisionMarker);
        }
    }

    private ActionListener actionListener = new ActionListener() {

        public void onAction(String binding, boolean keyPressed, float tpf) {
            if (binding.equals("wireframe") && !keyPressed) {
                wireframe = !wireframe;
                if (wireframe) {
                    terrain.setMaterial(matWire);
                } else {
                    terrain.setMaterial(matRock);
                }
            } else if (binding.equals("shoot") && !keyPressed) {
                Vector3f origin = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2, settings.getHeight() / 2), 0.0f);
                Vector3f direction = cam.getWorldCoordinates(new Vector2f(settings.getWidth() / 2, settings.getHeight() / 2), 0.3f);
                direction.subtractLocal(origin).normalizeLocal();

                Ray ray = new Ray(origin, direction);
                CollisionResults results = new CollisionResults();

                if (terrain.collideWith(ray, results) > 0) {
                    CollisionResult hit = results.getClosestCollision(); // sorts the collection before printing
                    printCollisions(results);

                    // Remove old markers.
                    for (Geometry g: collisionMarkers) {
                        g.removeFromParent();
                    }
                    collisionMarkers.clear();

                    createCollisionMarkers(results.size());

                    // Position Closest Collision
                    Vector2f loc = new Vector2f(hit.getContactPoint().x, hit.getContactPoint().z);
                    float height = terrain.getHeight(loc);
                    System.out.println("Closest Collision: " + hit.getContactPoint() + ", height: " + height + ", distance: " + hit.getDistance());
                    collisionMarkers.get(0).setLocalTranslation(new Vector3f(hit.getContactPoint().x, height, hit.getContactPoint().z));

                    // Position Rest: When getClosestCollision has been called, the results are sorted, and thus 0 is closest.
                    for (int i = 1; i < results.size(); i++) {
                        collisionMarkers.get(i).setLocalTranslation(results.getCollision(i).getContactPoint());
                    }
                }
            } else if (binding.equals("cameraDown") && !keyPressed) {
                getCamera().lookAtDirection(new Vector3f(0, -1, 0), Vector3f.UNIT_Y);
            } else if (binding.equals("Lefts") && !keyPressed) {
                Vector3f oldLoc = selectedCollisionObject.getLocalTranslation().clone();
                selectedCollisionObject.move(-0.5f, 0, 0);
                testCollision(oldLoc);
            } else if (binding.equals("Rights") && !keyPressed) {
                Vector3f oldLoc = selectedCollisionObject.getLocalTranslation().clone();
                selectedCollisionObject.move(0.5f, 0, 0);
                testCollision(oldLoc);
            } else if (binding.equals("Forwards") && !keyPressed) {
                Vector3f oldLoc = selectedCollisionObject.getLocalTranslation().clone();
                selectedCollisionObject.move(0, 0, 0.5f);
                testCollision(oldLoc);
            } else if (binding.equals("Backs") && !keyPressed) {
                Vector3f oldLoc = selectedCollisionObject.getLocalTranslation().clone();
                selectedCollisionObject.move(0, 0, -0.5f);
                testCollision(oldLoc);
            } else if (binding.equals("Ups") && !keyPressed) {
                Vector3f oldLoc = selectedCollisionObject.getLocalTranslation().clone();
                selectedCollisionObject.move(0, 0.5f, 0);
                testCollision(oldLoc);
            } else if (binding.equals("Downs") && !keyPressed) {
                Vector3f oldLoc = selectedCollisionObject.getLocalTranslation().clone();
                selectedCollisionObject.move(0, -0.5f, 0);
                testCollision(oldLoc);
            }

        }
    };

    private void testCollision(Vector3f oldLoc) {
        if (terrain.collideWith(selectedCollisionObject.getWorldBound(), new CollisionResults()) > 0) {
            selectedCollisionObject.setLocalTranslation(oldLoc);
        }
    }

    private void printCollisions(CollisionResults cr) {
        System.out.println("================ Collision Results ================");
        for (int i = 0; i < cr.size(); i++) {
            CollisionResult res = cr.getCollision(i);
            System.out.println("Result " + i);
            System.out.println("\t\t" + res.toString());
        }
        System.out.println("================ END Collision Results ================");
    }
}
