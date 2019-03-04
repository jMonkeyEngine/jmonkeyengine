/*
 * Copyright (c) 2009-2017 jMonkeyEngine
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

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.DirectionalLightShadowFilter;
import java.util.concurrent.Callable;

/**
 * Physics based marble game.
 * 
 * @author SkidRunner (Mark E. Picknell)
 */
public class RollingTheMonkey extends SimpleApplication implements ActionListener, PhysicsCollisionListener {
    
    private static final String TITLE           = "Rolling The Monkey";
    private static final String MESSAGE         = "Thanks for Playing!";
    private static final String INFO_MESSAGE    = "Collect all the spinning cubes!\nPress the 'R' key any time to reset!";
    
    private static final float PLAYER_DENSITY   = 1200;  // OLK(Java LOL) = 1200, STEEL = 8000, RUBBER = 1000
    private static final float PLAYER_REST      = 0.1f;     // OLK = 0.1f, STEEL = 0.0f, RUBBER = 1.0f I made these up.
    
    private static final float PLAYER_RADIUS    = 2.0f;
    private static final float PLAYER_ACCEL     = 1.0f;
    
    private static final float PICKUP_SIZE      = 0.5f;
    private static final float PICKUP_RADIUS    = 15.0f;
    private static final int   PICKUP_COUNT     = 16;
    private static final float PICKUP_SPEED     = 5.0f;
    
    private static final float PLAYER_VOLUME    = (FastMath.pow(PLAYER_RADIUS, 3) * FastMath.PI) / 3;   // V = 4/3 * PI * R pow 3
    private static final float PLAYER_MASS      = PLAYER_DENSITY * PLAYER_VOLUME;
    private static final float PLAYER_FORCE     = 80000 * PLAYER_ACCEL;  // F = M(4m diameter steel ball) * A
    private static final Vector3f PLAYER_START  = new Vector3f(0.0f, PLAYER_RADIUS * 2, 0.0f);
    
    private static final String INPUT_MAPPING_FORWARD   = "INPUT_MAPPING_FORWARD";
    private static final String INPUT_MAPPING_BACKWARD  = "INPUT_MAPPING_BACKWARD";
    private static final String INPUT_MAPPING_LEFT      = "INPUT_MAPPING_LEFT";
    private static final String INPUT_MAPPING_RIGHT     = "INPUT_MAPPING_RIGHT";
    private static final String INPUT_MAPPING_RESET     = "INPUT_MAPPING_RESET";
    
    public static void main(String[] args) {
        RollingTheMonkey app = new RollingTheMonkey();
        app.start();
    }
    
    private boolean keyForward;
    private boolean keyBackward;
    private boolean keyLeft;
    private boolean keyRight;
    
    private PhysicsSpace space;
    
    private RigidBodyControl player;
    private int score;
    
    private Node pickUps;
    
    BitmapText infoText;
    BitmapText scoreText;
    BitmapText messageText;
    
    @Override
    public void simpleInitApp() {
        flyCam.setEnabled(false);
        cam.setLocation(new Vector3f(0.0f, 12.0f, 21.0f));
        viewPort.setBackgroundColor(new ColorRGBA(0.2118f, 0.0824f, 0.6549f, 1.0f));
        
        // init physics
        BulletAppState bulletState = new BulletAppState();
        stateManager.attach(bulletState);
        space = bulletState.getPhysicsSpace();
        space.addCollisionListener(this);
        
        // create light
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(-0.7f, -0.3f, -0.5f)).normalizeLocal());
        System.out.println("Here We Go: " + sun.getDirection());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun); 
        
        // create materials
        Material materialRed = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        materialRed.setBoolean("UseMaterialColors",true);
        materialRed.setBoolean("HardwareShadows", true);
        materialRed.setColor("Diffuse", new ColorRGBA(0.9451f, 0.0078f, 0.0314f, 1.0f));
        materialRed.setColor("Specular", ColorRGBA.White);
        materialRed.setFloat("Shininess", 64.0f);
        
        Material materialGreen = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        materialGreen.setBoolean("UseMaterialColors",true);
        materialGreen.setBoolean("HardwareShadows", true);
        materialGreen.setColor("Diffuse", new ColorRGBA(0.0431f, 0.7725f, 0.0078f, 1.0f));
        materialGreen.setColor("Specular", ColorRGBA.White);
        materialGreen.setFloat("Shininess", 64.0f);
        
        Material logoMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        logoMaterial.setBoolean("UseMaterialColors",true);
        logoMaterial.setBoolean("HardwareShadows", true);
        logoMaterial.setTexture("DiffuseMap", assetManager.loadTexture("com/jme3/app/Monkey.png"));
        logoMaterial.setColor("Diffuse", ColorRGBA.White);
        logoMaterial.setColor("Specular", ColorRGBA.White);
        logoMaterial.setFloat("Shininess", 32.0f);
        
        Material materialYellow = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        materialYellow.setBoolean("UseMaterialColors",true);
        materialYellow.setBoolean("HardwareShadows", true);
        materialYellow.setColor("Diffuse", new ColorRGBA(0.9529f, 0.7843f, 0.0078f, 1.0f));
        materialYellow.setColor("Specular", ColorRGBA.White);
        materialYellow.setFloat("Shininess", 64.0f);
        
        // create level spatial
        // TODO: create your own level mesh
        Node level = new Node("level");
        level.setShadowMode(ShadowMode.CastAndReceive);
        
        Geometry floor = new Geometry("floor", new Box(22.0f, 0.5f, 22.0f));
        floor.setShadowMode(ShadowMode.Receive);
        floor.setLocalTranslation(0.0f, -0.5f, 0.0f);
        floor.setMaterial(materialGreen);
        
        Geometry wallNorth = new Geometry("wallNorth", new Box(22.0f, 2.0f, 0.5f));
        wallNorth.setLocalTranslation(0.0f, 2.0f, 21.5f);
        wallNorth.setMaterial(materialRed);
        
        Geometry wallSouth = new Geometry("wallSouth", new Box(22.0f, 2.0f, 0.5f));
        wallSouth.setLocalTranslation(0.0f, 2.0f, -21.5f);
        wallSouth.setMaterial(materialRed);
        
        Geometry wallEast = new Geometry("wallEast", new Box(0.5f, 2.0f, 21.0f));
        wallEast.setLocalTranslation(-21.5f, 2.0f, 0.0f);
        wallEast.setMaterial(materialRed);
        
        Geometry wallWest = new Geometry("wallWest", new Box(0.5f, 2.0f, 21.0f));
        wallWest.setLocalTranslation(21.5f, 2.0f, 0.0f);
        wallWest.setMaterial(materialRed);
        
        level.attachChild(floor);
        level.attachChild(wallNorth);
        level.attachChild(wallSouth);
        level.attachChild(wallEast);
        level.attachChild(wallWest);
        
        // The easy way: level.addControl(new RigidBodyControl(0));
        
        // create level Shape
        CompoundCollisionShape levelShape = new CompoundCollisionShape();
        BoxCollisionShape floorShape = new BoxCollisionShape(new Vector3f(22.0f, 0.5f, 22.0f));
        BoxCollisionShape wallNorthShape = new BoxCollisionShape(new Vector3f(22.0f, 2.0f, 0.5f));
        BoxCollisionShape wallSouthShape = new BoxCollisionShape(new Vector3f(22.0f, 2.0f, 0.5f));
        BoxCollisionShape wallEastShape = new BoxCollisionShape(new Vector3f(0.5f, 2.0f, 21.0f));
        BoxCollisionShape wallWestShape = new BoxCollisionShape(new Vector3f(0.5f, 2.0f, 21.0f));
        
        levelShape.addChildShape(floorShape, new Vector3f(0.0f, -0.5f, 0.0f));
        levelShape.addChildShape(wallNorthShape, new Vector3f(0.0f, 2.0f, -21.5f));
        levelShape.addChildShape(wallSouthShape, new Vector3f(0.0f, 2.0f, 21.5f));
        levelShape.addChildShape(wallEastShape, new Vector3f(-21.5f, 2.0f, 0.0f));
        levelShape.addChildShape(wallEastShape, new Vector3f(21.5f, 2.0f, 0.0f));
        
        level.addControl(new RigidBodyControl(levelShape, 0));
        
        rootNode.attachChild(level);
        space.addAll(level);
        
        // create Pickups
        // TODO: create your own pickUp mesh
        //       create single mesh for all pickups
        // HINT: think particles.
        pickUps = new Node("pickups");
        
        Quaternion rotation = new Quaternion();
        Vector3f translation = new Vector3f(0.0f, PICKUP_SIZE * 1.5f, -PICKUP_RADIUS);
        int index = 0;
        float ammount = FastMath.TWO_PI / PICKUP_COUNT;
        for(float angle = 0; angle < FastMath.TWO_PI; angle += ammount) {
            Geometry pickUp = new Geometry("pickUp" + (index++), new Box(PICKUP_SIZE,PICKUP_SIZE, PICKUP_SIZE));
            pickUp.setShadowMode(ShadowMode.CastAndReceive);
            pickUp.setMaterial(materialYellow);
            pickUp.setLocalRotation(rotation.fromAngles(
                    FastMath.rand.nextFloat() * FastMath.TWO_PI,
                    FastMath.rand.nextFloat() * FastMath.TWO_PI,
                    FastMath.rand.nextFloat() * FastMath.TWO_PI));
            
            rotation.fromAngles(0.0f, angle, 0.0f);
            rotation.mult(translation, pickUp.getLocalTranslation());
            pickUps.attachChild(pickUp);
            
            pickUp.addControl(new GhostControl(new SphereCollisionShape(PICKUP_SIZE)));
            
            
            space.addAll(pickUp);
            //space.addCollisionListener(pickUpControl);
        }
        rootNode.attachChild(pickUps);
        
        // Create player
        // TODO: create your own player mesh
        Geometry playerGeometry = new Geometry("player", new Sphere(16, 32, PLAYER_RADIUS));
        playerGeometry.setShadowMode(ShadowMode.CastAndReceive);
        playerGeometry.setLocalTranslation(PLAYER_START.clone());
        playerGeometry.setMaterial(logoMaterial);
        
        // Store control for applying forces
        // TODO: create your own player control
        player = new RigidBodyControl(new SphereCollisionShape(PLAYER_RADIUS), PLAYER_MASS);
        player.setRestitution(PLAYER_REST);
        
        playerGeometry.addControl(player);
        
        rootNode.attachChild(playerGeometry);
        space.addAll(playerGeometry);
        
        inputManager.addMapping(INPUT_MAPPING_FORWARD, new KeyTrigger(KeyInput.KEY_UP)
                , new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(INPUT_MAPPING_BACKWARD, new KeyTrigger(KeyInput.KEY_DOWN)
                , new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(INPUT_MAPPING_LEFT, new KeyTrigger(KeyInput.KEY_LEFT)
                , new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(INPUT_MAPPING_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT)
                , new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(INPUT_MAPPING_RESET, new KeyTrigger(KeyInput.KEY_R));
        inputManager.addListener(this, INPUT_MAPPING_FORWARD, INPUT_MAPPING_BACKWARD
                , INPUT_MAPPING_LEFT, INPUT_MAPPING_RIGHT, INPUT_MAPPING_RESET);
        
        // init UI
        infoText = new BitmapText(guiFont, false);
        infoText.setText(INFO_MESSAGE);
        guiNode.attachChild(infoText);
        
        scoreText = new BitmapText(guiFont, false);
        scoreText.setText("Score: 0");
        guiNode.attachChild(scoreText);
        
        messageText = new BitmapText(guiFont, false);
        messageText.setText(MESSAGE);
        messageText.setLocalScale(0.0f);
        guiNode.attachChild(messageText);
        
        infoText.setLocalTranslation(0.0f, cam.getHeight(), 0.0f);
        scoreText.setLocalTranslation((cam.getWidth() - scoreText.getLineWidth()) / 2.0f,
                scoreText.getLineHeight(), 0.0f);
        messageText.setLocalTranslation((cam.getWidth() - messageText.getLineWidth()) / 2.0f,
                (cam.getHeight() - messageText.getLineHeight()) / 2, 0.0f);
        
        // init shadows
        FilterPostProcessor processor = new FilterPostProcessor(assetManager);
        DirectionalLightShadowFilter filter = new DirectionalLightShadowFilter(assetManager, 2048, 1);
        filter.setLight(sun);
        processor.addFilter(filter);
        viewPort.addProcessor(processor);
        
    }
    
    @Override
    public void simpleUpdate(float tpf) {
        // Update and position the score
        scoreText.setText("Score: " + score);
        scoreText.setLocalTranslation((cam.getWidth() - scoreText.getLineWidth()) / 2.0f,
                scoreText.getLineHeight(), 0.0f);
        
        // Rotate all the pickups
        float pickUpSpeed = PICKUP_SPEED * tpf;
        for(Spatial pickUp : pickUps.getChildren()) {
            pickUp.rotate(pickUpSpeed, pickUpSpeed, pickUpSpeed);
        }
        
        Vector3f centralForce = new Vector3f();
        
        if(keyForward) centralForce.addLocal(cam.getDirection());
        if(keyBackward) centralForce.addLocal(cam.getDirection().negate());
        if(keyLeft) centralForce.addLocal(cam.getLeft());
        if(keyRight) centralForce.addLocal(cam.getLeft().negate());
        
        if(!Vector3f.ZERO.equals(centralForce)) {
            centralForce.setY(0);                   // stop ball from pusing down or flying up
            centralForce.normalizeLocal();          // normalize force
            centralForce.multLocal(PLAYER_FORCE);   // scale vector to force

            player.applyCentralForce(centralForce); // apply force to player
        }
        
        cam.lookAt(player.getPhysicsLocation(), Vector3f.UNIT_Y);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch(name) {
            case INPUT_MAPPING_FORWARD:
                keyForward = isPressed;
                break;
            case INPUT_MAPPING_BACKWARD:
                keyBackward = isPressed;
                break;
            case INPUT_MAPPING_LEFT:
                keyLeft = isPressed;
                break;
            case INPUT_MAPPING_RIGHT:
                keyRight = isPressed;
                break;
            case INPUT_MAPPING_RESET:
                enqueue(new Callable<Void>() {
                    @Override
                    public Void call() {
                        reset();
                        return null;
                    }
                });
                break;
        }
    }
    @Override
    public void collision(PhysicsCollisionEvent event) {
        Spatial nodeA = event.getNodeA();
        Spatial nodeB = event.getNodeB();
        
        String nameA = nodeA == null ? "" : nodeA.getName();
        String nameB = nodeB == null ? "" : nodeB.getName();
        
        if(nameA.equals("player") && nameB.startsWith("pickUp")) {
            GhostControl pickUpControl = nodeB.getControl(GhostControl.class);
            if(pickUpControl != null && pickUpControl.isEnabled()) {
                pickUpControl.setEnabled(false);
                nodeB.removeFromParent();
                nodeB.setLocalScale(0.0f);
                score += 1;
                if(score >= PICKUP_COUNT) {
                    messageText.setLocalScale(1.0f);
                }
            }
        } else if(nameA.startsWith("pickUp") && nameB.equals("player")) {
            GhostControl pickUpControl = nodeA.getControl(GhostControl.class);
            if(pickUpControl != null && pickUpControl.isEnabled()) {
                pickUpControl.setEnabled(false);
                nodeA.setLocalScale(0.0f);
                score += 1;
                if(score >= PICKUP_COUNT) {
                    messageText.setLocalScale(1.0f);
                }
            }
        }
    }
    
    private void reset() {
        // Reset the pickups
        for(Spatial pickUp : pickUps.getChildren()) {
            GhostControl pickUpControl = pickUp.getControl(GhostControl.class);
            if(pickUpControl != null) {
                pickUpControl.setEnabled(true);
            }
            pickUp.setLocalScale(1.0f);
        }
        // Reset the player
        player.setPhysicsLocation(PLAYER_START.clone());
        player.setAngularVelocity(Vector3f.ZERO.clone());
        player.setLinearVelocity(Vector3f.ZERO.clone());
        // Reset the score
        score = 0;
        // Reset the message
        messageText.setLocalScale(0.0f);
    }
    
}
