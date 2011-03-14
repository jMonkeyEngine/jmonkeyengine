/**
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package jme3game.golem;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.AnimEventListener;
import com.jme3.animation.LoopMode;
import com.jme3.bullet.BulletAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.nodes.PhysicsCharacterNode;
import com.jme3.bullet.nodes.PhysicsNode;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.effect.EmitterSphereShape;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.system.AppSettings;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import java.util.ArrayList;
import java.util.List;
import jme3tools.converters.ImageToAwt;

/**
 *
 * @author normenhansen
 */
public class Golem extends SimpleApplication
        implements ActionListener, PhysicsCollisionListener, AnimEventListener {

    // constants
    static final Quaternion ROTATE_LEFT = new Quaternion();

    static {
        ROTATE_LEFT.fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y);
    }
    //
    float myTimer;
    private BulletAppState bulletAppState;
    //character
    PhysicsCharacterNode characterNode;
    Node character;
    //temp vectors
    Vector3f walkDirection = new Vector3f();
    Quaternion modelRotation = new Quaternion();
    Vector3f modelDirection = new Vector3f();
    Vector3f modelRight = new Vector3f();
    //terrain
    TerrainQuad terrain;
    Node terrainNode;
    //Materials
    Material matTerrain;
    Material matWire;
    Material matBullet;
    Material matBomb;
    Material matRock;
    //animation
    AnimChannel animationChannel;
    AnimChannel shootingChannel;
    AnimControl animationControl;
    float airTime = 0;
    //camera
    boolean left = false, right = false, up = false, down = false;
    ChaseCamera chaseCam;
    //player's bullet
    Sphere bullet;
    SphereCollisionShape bulletCollisionShape;
    //enemy's bomb
    Sphere bomb;
    SphereCollisionShape bombCollisionShape;
    Vector3f wallLoc = new Vector3f(0, 11, -105);
    Vector3f enemyLoc = new Vector3f(wallLoc.x + 15f, wallLoc.y + 15, wallLoc.z);
    Vector3f trajectory = new Vector3f(0, 10, 10);
    //explosion
    ParticleEmitter effect;
    //brick wall
    private Node wallNode;
    Box brick;
    float bLength = 1.6f;
    float bWidth = 1f;
    float bHeight = 1f;

    public static void main(String[] args) {
        Golem app = new Golem();
        AppSettings settings = new AppSettings(true);
        settings.setSamples(4);
        app.setSettings(settings);
        app.setShowSettings(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bulletAppState);
        setupKeys();
        prepareBullet();
        prepareBomb();
        prepareEffect();
        createLight();
        createSky();
        createTerrain();
        createWall(wallLoc);
        createCharacter();
        setupChaseCamera();
        setupAnimationController();

    }

    private void createWall(Vector3f off) {
        wallNode = new Node("the wall");

        matRock = new Material(assetManager, "Common/MatDefs/Misc/SimpleTextured.j3md");
        Texture tex_ml = assetManager.loadTexture("Textures/Terrain/BrickWall/BrickWall.jpg");
        matRock.setTexture("ColorMap", tex_ml);

        brick = new Box(Vector3f.ZERO, bLength, bHeight, bWidth);
        brick.scaleTextureCoordinates(new Vector2f(1f, 1f));
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 10; col++) {
                Vector3f vt = new Vector3f(
                        off.x + (col + 1f) * bLength * 2.01f,
                        off.y + (row + 1f) * bHeight * 2.01f,
                        off.z);
                addBrick(vt);
            }
        }
        this.rootNode.attachChild(wallNode);
    }

    private void addBrick(Vector3f ori) {
        Geometry reBoxg = new Geometry("brick", brick);
        reBoxg.setMaterial(matRock);
        PhysicsNode brickNode = new PhysicsNode(
                reBoxg,
                new BoxCollisionShape(new Vector3f(bLength, bHeight, bWidth)), 1.5f);
        brickNode.setLocalTranslation(ori);
        brickNode.setShadowMode(ShadowMode.CastAndReceive);
        wallNode.attachChild(brickNode);
        this.getPhysicsSpace().add(brickNode);
    }

    private void createCharacter() {
        CapsuleCollisionShape capsule = new CapsuleCollisionShape(3f, 4f);
        characterNode = new PhysicsCharacterNode(capsule, 0.01f);
        character = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
        characterNode.attachChild(character);
        characterNode.setName("the Golem");
        characterNode.setLocalTranslation(new Vector3f(0, 10, 50));
        characterNode.setMaxSlope(FastMath.PI / 2f * 3f);
        rootNode.attachChild(characterNode);
        getPhysicsSpace().add(characterNode);
    }

    private void prepareBullet() {
        bullet = new Sphere(32, 32, 0.4f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(0.4f);
        matBullet = new Material(getAssetManager(), "Common/MatDefs/Misc/WireColor.j3md");
        matBullet.setColor("Color", ColorRGBA.Green);
        getPhysicsSpace().addCollisionListener(this);
    }

    private void prepareBomb() {
        bomb = new Sphere(32, 32, 2f, true, false);
        bomb.setTextureMode(TextureMode.Projected);
        bombCollisionShape = new SphereCollisionShape(2);
        matBomb = new Material(getAssetManager(), "Common/MatDefs/Misc/WireColor.j3md");
        matBomb.setColor("Color", ColorRGBA.Red);
        getPhysicsSpace().addCollisionListener(this);
    }

    private void prepareEffect() {
        int COUNT_FACTOR = 1;
        float COUNT_FACTOR_F = 1f;
        effect = new ParticleEmitter("Flame", Type.Triangle, 32 * COUNT_FACTOR);
        effect.setSelectRandomImage(true);
        effect.setStartColor(new ColorRGBA(1f, 0.4f, 0.05f, (float) (1f / COUNT_FACTOR_F)));
        effect.setEndColor(new ColorRGBA(.4f, .22f, .12f, 0f));
        effect.setStartSize(1.3f);
        effect.setEndSize(2f);
        effect.setShape(new EmitterSphereShape(Vector3f.ZERO, 1f));
        effect.setParticlesPerSec(0);
        effect.setGravity(-5f);
        effect.setLowLife(.4f);
        effect.setHighLife(.5f);
        effect.setInitialVelocity(new Vector3f(0, 7, 0));
        effect.setVelocityVariation(1f);
        effect.setImagesX(2);
        effect.setImagesY(2);
        Material mat = new Material(
                assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture",
                assetManager.loadTexture("Effects/Explosion/flame.png"));
        effect.setMaterial(mat);
        effect.setLocalScale(100);
        effect.setCullHint(CullHint.Never);
        rootNode.attachChild(effect);
    }

    private void createLight() {
        Vector3f direction = new Vector3f(0.1f, -0.7f, 1).normalizeLocal();
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(direction);
        dl.setColor(new ColorRGBA(1f, 1f, 1f, 1.0f));
        rootNode.addLight(dl);

//    PointLight spotlight = new PointLight();
//     spotlight.setPosition(Vector3f.ZERO);
//     spotlight.setRadius(50);
//    spotlight.setColor(ColorRGBA.White);
//    rootNode.addLight(spotlight);
    }

    private void createSky() {
        rootNode.attachChild(
                SkyFactory.createSky(assetManager, "Textures/Sky/Bright/BrightSky.dds", false));
    }

    private void createTerrain() {
        matTerrain = new Material(assetManager, "Common/MatDefs/Terrain/Terrain.j3md");
        matTerrain.setTexture("Alpha", assetManager.loadTexture("Textures/Terrain/splat/alphamap2.png"));
        Texture heightMapImage = assetManager.loadTexture("Textures/Terrain/splat/fortress512.png");
        Texture grass = assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("Tex1", grass);
        matTerrain.setFloat("Tex1Scale", 64f);
        Texture dirt = assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("Tex2", dirt);
        matTerrain.setFloat("Tex2Scale", 32f);
        Texture rock = assetManager.loadTexture("Textures/Terrain/splat/road.jpg");
        rock.setWrap(WrapMode.Repeat);
        matTerrain.setTexture("Tex3", rock);
        matTerrain.setFloat("Tex3Scale", 128f);
        matWire = new Material(assetManager, "Common/MatDefs/Misc/WireColor.j3md");
        matWire.setColor("Color", ColorRGBA.Green);

        AbstractHeightMap heightmap = null;
        try {
            heightmap = new ImageBasedHeightMap(
                    ImageToAwt.convert(heightMapImage.getImage(), false, true, 0), 0.25f);
            heightmap.load();

        } catch (Exception e) {
            e.printStackTrace();
        }

        terrain = new TerrainQuad("terrain", 65, 513, heightmap.getHeightMap());
        List<Camera> cameras = new ArrayList<Camera>();
        cameras.add(getCamera());
        TerrainLodControl control = new TerrainLodControl(terrain, cameras);
        terrain.addControl(control);
        terrain.setMaterial(matTerrain);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
        terrain.setLocalScale(new Vector3f(1, 1, 1));

        terrainNode = new PhysicsNode(CollisionShapeFactory.createMeshShape(terrain),0);
        terrainNode.attachChild(terrain);
        rootNode.attachChild(terrainNode);
        getPhysicsSpace().add(terrainNode);
    }

    private void setupChaseCamera() {
        flyCam.setEnabled(false);
        chaseCam = new ChaseCamera(cam, characterNode, inputManager);
    }

    private void setupAnimationController() {
        animationControl = character.getControl(AnimControl.class);
        animationControl.addListener(this);
        animationChannel = animationControl.createChannel();
        shootingChannel = animationControl.createChannel();
//        System.out.println(animationControl.getSkeleton());
        shootingChannel.addBone(animationControl.getSkeleton().getBone("uparm.right"));
        shootingChannel.addBone(animationControl.getSkeleton().getBone("arm.right"));
        shootingChannel.addBone(animationControl.getSkeleton().getBone("hand.right"));
    }

    @Override
    public void simpleUpdate(float tpf) {
        rootNode.updateLogicalState(tpf);
        rootNode.updateGeometricState();
        Vector3f camDir = cam.getDirection().clone().multLocal(0.2f);
        Vector3f camLeft = cam.getLeft().clone().multLocal(0.2f);
        camDir.y = 0;
        camLeft.y = 0;
        walkDirection.set(0, 0, 0);
        modelDirection.set(0, 0, 2);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        if (!characterNode.onGround()) {
            airTime = airTime + tpf;
        } else {
            airTime = 0;
        }
        if (walkDirection.length() == 0) {
            if (!"stand".equals(animationChannel.getAnimationName())) {
                animationChannel.setAnim("stand", 1f);
            }
        } else {
            modelRotation.lookAt(walkDirection, Vector3f.UNIT_Y);
            if (airTime > .3f) {
                if (!"stand".equals(animationChannel.getAnimationName())) {
                    animationChannel.setAnim("stand");
                }
            } else if (!"Walk".equals(animationChannel.getAnimationName())) {
                animationChannel.setAnim("Walk", 0.7f);
            }
        }
        character.setLocalRotation(modelRotation);
        modelRotation.multLocal(modelDirection);
        modelRight.set(modelDirection);
        ROTATE_LEFT.multLocal(modelRight);
        characterNode.setWalkDirection(walkDirection);

        // attack
        myTimer += tpf;
        if (myTimer > 10) {
            shootBomb();
            myTimer = 0;
        }


    }

    public void onAction(String binding, boolean value, float tpf) {
        if (binding.equals("CharLeft")) {
            if (value) {
                left = true;
            } else {
                left = false;
            }
        } else if (binding.equals("CharRight")) {
            if (value) {
                right = true;
            } else {
                right = false;
            }
        } else if (binding.equals("CharForw")) {
            if (value) {
                up = true;
            } else {
                up = false;
            }
        } else if (binding.equals("CharBack")) {
            if (value) {
                down = true;
            } else {
                down = false;
            }
        } else if (binding.equals("CharJump")) {
            characterNode.jump();
        } else if (binding.equals("CharShoot") && !value) {
            shootBullet();
        }
    }

    private void shootBullet() {
        shootingChannel.setAnim("Dodge", 0.1f);
        shootingChannel.setLoopMode(LoopMode.DontLoop);
        Geometry bulletGeo = new Geometry("bullet", bullet);
        bulletGeo.setMaterial(matBullet);
        PhysicsNode bulletNode = new PhysicsNode(bulletGeo, bulletCollisionShape, 1);
        bulletNode.setCcdMotionThreshold(0.1f);
        bulletNode.setName("bullet");
        bulletNode.setLocalTranslation(characterNode.getLocalTranslation().add(modelDirection.mult(1.8f).addLocal(modelRight.mult(0.9f))));
        bulletNode.setShadowMode(ShadowMode.CastAndReceive);
        bulletNode.setLinearVelocity(modelDirection.mult(40));
        rootNode.attachChild(bulletNode);
        getPhysicsSpace().add(bulletNode);
    }

    private void shootBomb() {
        Geometry bombGeo = new Geometry("bomb", bomb);
        bombGeo.setMaterial(matBomb);
        PhysicsNode bombNode = new PhysicsNode(bombGeo, bombCollisionShape, 1);
        bombNode.setCcdMotionThreshold(0.1f);
        bombNode.setName("bomb");
        bombNode.setLocalTranslation(enemyLoc);
        bombNode.setShadowMode(ShadowMode.Cast);

        Vector3f w = wallNode.getWorldTranslation();
        Vector3f c = characterNode.getWorldTranslation();
        bombNode.setLinearVelocity(w.subtract(c).normalize().mult(30)); // TODO
        rootNode.attachChild(bombNode);
        getPhysicsSpace().add(bombNode);
    }

    public void collision(PhysicsCollisionEvent event) {
        if ("bullet".equals(event.getNodeA().getName())) {
            Spatial node = event.getNodeA();
            bulletHitSomething(node);
        } else if ("bullet".equals(event.getNodeB().getName())) {
            Spatial node = event.getNodeB();
            bulletHitSomething(node);
        }
        if ("bomb".equals(event.getNodeA().getName())) {
            Spatial agent = event.getNodeA();
            Spatial target = event.getNodeB();
            bombHitSomething(agent, target);
        } else if ("bomb".equals(event.getNodeB().getName())) {
            Spatial agent = event.getNodeB();
            Spatial target = event.getNodeA();
            bombHitSomething(agent, target);
        }
    }

    private void bombHitSomething(Spatial a, Spatial t) {
        getPhysicsSpace().remove(a);
        a.removeFromParent();
        effect.killAllParticles();
        effect.setLocalTranslation(t.getLocalTranslation());
        effect.emitAllParticles();
        if ("The Golem".equals(t.getName())) {
            System.out.println("*** A bomb hit " + t.getName() + "! ***");
        }
    }

    private void bulletHitSomething(Spatial node) {
        getPhysicsSpace().remove(node);
        if (!node.getName().equals("terrain")) {
            node.removeFromParent();
        }
        effect.killAllParticles();
        effect.setLocalTranslation(node.getLocalTranslation());
        effect.emitAllParticles();
    }

    public void onAnimCycleDone(AnimControl control, AnimChannel channel, String animName) {
        if (channel == shootingChannel) {
            channel.setAnim("stand");
        }
    }

    public void onAnimChange(AnimControl control, AnimChannel channel, String animName) {
    }

    private void AxisRods() {
        Material mat = new Material(assetManager,
                "Common/MatDefs/Misc/SolidColor.j3md");
        mat.setColor("Color", ColorRGBA.White);
        Box x = new Box(Vector3f.ZERO, 100f, 0.5f, 0.5f);
        Geometry gx = new Geometry("Box", x);
        gx.setMaterial(mat);
        rootNode.attachChild(gx);
        Box y = new Box(Vector3f.ZERO, 0.5f, 100f, 0.5f);
        Geometry gy = new Geometry("Box", y);
        gy.setMaterial(mat);
        rootNode.attachChild(gy);
        Box z = new Box(Vector3f.ZERO, 0.1f, 0.1f, 100f);
        Geometry gz = new Geometry("Box", z);
        gz.setMaterial(mat);
        rootNode.attachChild(gz);
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }

    /** Mapping WASD keys for walking, return for jumping, space for shooting */
    private void setupKeys() {
        inputManager.addMapping("wireframe", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("CharLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("CharRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("CharForw", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("CharBack", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("CharJump", new KeyTrigger(KeyInput.KEY_RETURN));
        inputManager.addMapping("CharShoot", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(this, "wireframe");
        inputManager.addListener(this, "CharLeft");
        inputManager.addListener(this, "CharRight");
        inputManager.addListener(this, "CharForw");
        inputManager.addListener(this, "CharBack");
        inputManager.addListener(this, "CharJump");
        inputManager.addListener(this, "CharShoot");
    }
}
