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
package jme3test.bullet;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.GImpactCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

/**
 *
 * @author normenhansen
 */
public class PhysicsTestHelper {
    /**
     * A private constructor to inhibit instantiation of this class.
     */
    private PhysicsTestHelper() {
    }

    /**
     * creates a simple physics test world with a floor, an obstacle and some test boxes
     *
     * @param rootNode where lights and geometries should be added
     * @param assetManager for loading assets
     * @param space where collision objects should be added
     */
    public static void createPhysicsTestWorld(Node rootNode, AssetManager assetManager, PhysicsSpace space) {
        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.LightGray);
        rootNode.addLight(light);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));

        Box floorBox = new Box(140, 0.25f, 140);
        Geometry floorGeometry = new Geometry("Floor", floorBox);
        floorGeometry.setMaterial(material);
        floorGeometry.setLocalTranslation(0, -5, 0);
//        Plane plane = new Plane();
//        plane.setOriginNormal(new Vector3f(0, 0.25f, 0), Vector3f.UNIT_Y);
//        floorGeometry.addControl(new RigidBodyControl(new PlaneCollisionShape(plane), 0));
        floorGeometry.addControl(new RigidBodyControl(0));
        rootNode.attachChild(floorGeometry);
        space.add(floorGeometry);

        //movable boxes
        for (int i = 0; i < 12; i++) {
            Box box = new Box(0.25f, 0.25f, 0.25f);
            Geometry boxGeometry = new Geometry("Box", box);
            boxGeometry.setMaterial(material);
            boxGeometry.setLocalTranslation(i, 5, -3);
            //RigidBodyControl automatically uses box collision shapes when attached to single geometry with box mesh
            boxGeometry.addControl(new RigidBodyControl(2));
            rootNode.attachChild(boxGeometry);
            space.add(boxGeometry);
        }

        //immovable sphere with mesh collision shape
        Sphere sphere = new Sphere(8, 8, 1);
        Geometry sphereGeometry = new Geometry("Sphere", sphere);
        sphereGeometry.setMaterial(material);
        sphereGeometry.setLocalTranslation(4, -4, 2);
        sphereGeometry.addControl(new RigidBodyControl(new MeshCollisionShape(sphere), 0));
        rootNode.attachChild(sphereGeometry);
        space.add(sphereGeometry);

    }

    public static void createPhysicsTestWorldSoccer(Node rootNode, AssetManager assetManager, PhysicsSpace space) {
        AmbientLight light = new AmbientLight();
        light.setColor(ColorRGBA.LightGray);
        rootNode.addLight(light);

        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));

        Box floorBox = new Box(20, 0.25f, 20);
        Geometry floorGeometry = new Geometry("Floor", floorBox);
        floorGeometry.setMaterial(material);
        floorGeometry.setLocalTranslation(0, -0.25f, 0);
//        Plane plane = new Plane();
//        plane.setOriginNormal(new Vector3f(0, 0.25f, 0), Vector3f.UNIT_Y);
//        floorGeometry.addControl(new RigidBodyControl(new PlaneCollisionShape(plane), 0));
        floorGeometry.addControl(new RigidBodyControl(0));
        rootNode.attachChild(floorGeometry);
        space.add(floorGeometry);

        //movable spheres
        for (int i = 0; i < 5; i++) {
            Sphere sphere = new Sphere(16, 16, .5f);
            Geometry ballGeometry = new Geometry("Soccer ball", sphere);
            ballGeometry.setMaterial(material);
            ballGeometry.setLocalTranslation(i, 2, -3);
            //RigidBodyControl automatically uses Sphere collision shapes when attached to single geometry with sphere mesh
            ballGeometry.addControl(new RigidBodyControl(.001f));
            ballGeometry.getControl(RigidBodyControl.class).setRestitution(1);
            rootNode.attachChild(ballGeometry);
            space.add(ballGeometry);
        }
        {
            //immovable Box with mesh collision shape
            Box box = new Box(1, 1, 1);
            Geometry boxGeometry = new Geometry("Box", box);
            boxGeometry.setMaterial(material);
            boxGeometry.setLocalTranslation(4, 1, 2);
            boxGeometry.addControl(new RigidBodyControl(new MeshCollisionShape(box), 0));
            rootNode.attachChild(boxGeometry);
            space.add(boxGeometry);
        }
        {
            //immovable Box with mesh collision shape
            Box box = new Box(1, 1, 1);
            Geometry boxGeometry = new Geometry("Box", box);
            boxGeometry.setMaterial(material);
            boxGeometry.setLocalTranslation(4, 3, 4);
            boxGeometry.addControl(new RigidBodyControl(new MeshCollisionShape(box), 0));
            rootNode.attachChild(boxGeometry);
            space.add(boxGeometry);
        }
    }

    /**
     * creates a box geometry with a RigidBodyControl
     *
     * @param assetManager for loading assets
     * @return a new Geometry
     */
    public static Geometry createPhysicsTestBox(AssetManager assetManager) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        Box box = new Box(0.25f, 0.25f, 0.25f);
        Geometry boxGeometry = new Geometry("Box", box);
        boxGeometry.setMaterial(material);
        //RigidBodyControl automatically uses box collision shapes when attached to single geometry with box mesh
        boxGeometry.addControl(new RigidBodyControl(2));
        return boxGeometry;
    }

    /**
     * creates a sphere geometry with a RigidBodyControl
     *
     * @param assetManager for loading assets
     * @return a new Geometry
     */
    public static Geometry createPhysicsTestSphere(AssetManager assetManager) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
        Sphere sphere = new Sphere(8, 8, 0.25f);
        Geometry boxGeometry = new Geometry("Sphere", sphere);
        boxGeometry.setMaterial(material);
        //RigidBodyControl automatically uses sphere collision shapes when attached to single geometry with sphere mesh
        boxGeometry.addControl(new RigidBodyControl(2));
        return boxGeometry;
    }

    /**
     * creates an empty node with a RigidBodyControl
     *
     * @param manager for loading assets
     * @param shape a shape for the collision object
     * @param mass a mass for rigid body
     * @return a new Node
     */
    public static Node createPhysicsTestNode(AssetManager manager, CollisionShape shape, float mass) {
        Node node = new Node("PhysicsNode");
        RigidBodyControl control = new RigidBodyControl(shape, mass);
        node.addControl(control);
        return node;
    }

    /**
     * creates the necessary input listener and action to shoot balls from the camera
     *
     * @param app the application that's running
     * @param rootNode where ball geometries should be added
     * @param space where collision objects should be added
     */
    public static void createBallShooter(final Application app, final Node rootNode, final PhysicsSpace space) {
        ActionListener actionListener = new ActionListener() {

            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                Sphere bullet = new Sphere(32, 32, 0.4f, true, false);
                bullet.setTextureMode(TextureMode.Projected);
                Material mat2 = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
                TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
                key2.setGenerateMips(true);
                Texture tex2 = app.getAssetManager().loadTexture(key2);
                mat2.setTexture("ColorMap", tex2);
                if (name.equals("shoot") && !keyPressed) {
                    Geometry bulletGeometry = new Geometry("bullet", bullet);
                    bulletGeometry.setMaterial(mat2);
                    bulletGeometry.setShadowMode(ShadowMode.CastAndReceive);
                    bulletGeometry.setLocalTranslation(app.getCamera().getLocation());
                    RigidBodyControl bulletControl = new RigidBodyControl(10);
                    bulletGeometry.addControl(bulletControl);
                    bulletControl.setLinearVelocity(app.getCamera().getDirection().mult(25));
                    bulletGeometry.addControl(bulletControl);
                    rootNode.attachChild(bulletGeometry);
                    space.add(bulletControl);
                }
            }
        };
        app.getInputManager().addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        app.getInputManager().addListener(actionListener, "shoot");
    }

    /**
     * Creates a curved "floor" with a GImpactCollisionShape provided as the RigidBodyControl's collision
     * shape. Surface has four slightly concave corners to allow for multiple tests and minimize falling off
     * the edge of the floor.
     *
     * @param assetManager for loading assets
     * @param floorDimensions width/depth of the "floor" (X/Z)
     * @param position sets the floor's local translation
     * @return a new Geometry
     */
    public static Geometry createGImpactTestFloor(AssetManager assetManager, float floorDimensions, Vector3f position) {
        Geometry floor = createTestFloor(assetManager, floorDimensions, position, ColorRGBA.Red);
        RigidBodyControl floorControl = new RigidBodyControl(new GImpactCollisionShape(floor.getMesh()), 0);
        floor.addControl(floorControl);
        return floor;
    }

    /**
     * Creates a curved "floor" with a MeshCollisionShape provided as the RigidBodyControl's collision shape.
     * Surface has four slightly concave corners to allow for multiple tests and minimize falling off the edge
     * of the floor.
     *
     * @param assetManager for loading assets
     * @param floorDimensions width/depth of the "floor" (X/Z)
     * @param position sets the floor's local translation
     * @return a new Geometry
     */
    public static Geometry createMeshTestFloor(AssetManager assetManager, float floorDimensions, Vector3f position) {
        Geometry floor = createTestFloor(assetManager, floorDimensions, position, new ColorRGBA(0.5f, 0.5f, 0.9f, 1));
        RigidBodyControl floorControl = new RigidBodyControl(new MeshCollisionShape(floor.getMesh()), 0);
        floor.addControl(floorControl);
        return floor;
    }

    private static Geometry createTestFloor(AssetManager assetManager, float floorDimensions, Vector3f position, ColorRGBA color) {
        Geometry floor = new Geometry("floor", createFloorMesh(20, floorDimensions));
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.getAdditionalRenderState().setWireframe(true);
        material.setColor("Color", color);
        floor.setMaterial(material);
        floor.setLocalTranslation(position);
        return floor;
    }

    private static Mesh createFloorMesh(int meshDetail, float floorDimensions) {
        if (meshDetail < 10) {
            meshDetail = 10;
        }
        int numVertices = meshDetail * meshDetail * 2 * 3;//width * depth * two tris * 3 verts per tri

        int[] indexBuf = new int[numVertices];
        int i = 0;
        for (int x = 0; x < meshDetail; x++) {
            for (int z = 0; z < meshDetail; z++) {
                indexBuf[i] = i++;
                indexBuf[i] = i++;
                indexBuf[i] = i++;
                indexBuf[i] = i++;
                indexBuf[i] = i++;
                indexBuf[i] = i++;
            }
        }

        float[] vertBuf = new float[numVertices * 3];
        float xIncrement = floorDimensions / meshDetail;
        float zIncrement = floorDimensions / meshDetail;
        int j = 0;
        for (int x = 0; x < meshDetail; x++) {
            float xPos = x * xIncrement;
            for (int z = 0; z < meshDetail; z++) {
                float zPos = z * zIncrement;
                //First tri
                vertBuf[j++] = xPos;
                vertBuf[j++] = getY(xPos, zPos, floorDimensions);
                vertBuf[j++] = zPos;
                vertBuf[j++] = xPos;
                vertBuf[j++] = getY(xPos, zPos + zIncrement, floorDimensions);
                vertBuf[j++] = zPos + zIncrement;
                vertBuf[j++] = xPos + xIncrement;
                vertBuf[j++] = getY(xPos + xIncrement, zPos, floorDimensions);
                vertBuf[j++] = zPos;
                //Second tri
                vertBuf[j++] = xPos;
                vertBuf[j++] = getY(xPos, zPos + zIncrement, floorDimensions);
                vertBuf[j++] = zPos + zIncrement;
                vertBuf[j++] = xPos + xIncrement;
                vertBuf[j++] = getY(xPos + xIncrement, zPos + zIncrement, floorDimensions);
                vertBuf[j++] = zPos + zIncrement;
                vertBuf[j++] = xPos + xIncrement;
                vertBuf[j++] = getY(xPos + xIncrement, zPos, floorDimensions);
                vertBuf[j++] = zPos;
            }
        }

        Mesh m = new Mesh();
        m.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indexBuf));
        m.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertBuf));
        m.updateBound();
        return m;
    }

    private static float getY(float x, float z, float max) {
        float yMaxHeight = 8;
        float xv = FastMath.unInterpolateLinear(FastMath.abs(x - (max / 2)), 0, max) * FastMath.TWO_PI;
        float zv = FastMath.unInterpolateLinear(FastMath.abs(z - (max / 2)), 0, max) * FastMath.TWO_PI;

        float xComp = (FastMath.sin(xv) + 1) * 0.5f;
        float zComp = (FastMath.sin(zv) + 1) * 0.5f;

        return -yMaxHeight * xComp * zComp;
    }
}
