/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jme3test.batching;

/*
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

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.shadow.PssmShadowRenderer;
import com.jme3.shadow.PssmShadowRenderer.CompareMode;
import com.jme3.shadow.PssmShadowRenderer.FilterMode;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import jme3test.bullet.BombControl;

/**
 *
 * @author double1984 (tower mod by atom)
 */
public class TestBatchNodeTower extends SimpleApplication {

    int bricksPerLayer = 8;
    int brickLayers = 30;

    static float brickWidth = .75f, brickHeight = .25f, brickDepth = .25f;
    float radius = 3f;
    float angle = 0;


    Material mat;
    Material mat2;
    Material mat3;
    PssmShadowRenderer bsr;
    private Sphere bullet;
    private Box brick;
    private SphereCollisionShape bulletCollisionShape;

    private BulletAppState bulletAppState;
    BatchNode batchNode = new BatchNode("batch Node");
    
    public static void main(String args[]) {
        TestBatchNodeTower f = new TestBatchNodeTower();
        AppSettings s = new AppSettings(true);
        f.setSettings(s);
        f.start();
    }

    @Override
    public void simpleInitApp() {
        timer = new NanoTimer();
        bulletAppState = new BulletAppState();
        bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
     //   bulletAppState.setEnabled(false);
        stateManager.attach(bulletAppState);
        bullet = new Sphere(32, 32, 0.4f, true, false);
        bullet.setTextureMode(TextureMode.Projected);
        bulletCollisionShape = new SphereCollisionShape(0.4f);

        brick = new Box(Vector3f.ZERO, brickWidth, brickHeight, brickDepth);
        brick.scaleTextureCoordinates(new Vector2f(1f, .5f));
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);
        initMaterial();
        initTower();
        initFloor();
        initCrossHairs();
        this.cam.setLocation(new Vector3f(0, 25f, 8f));
        cam.lookAt(Vector3f.ZERO, new Vector3f(0, 1, 0));
        cam.setFrustumFar(80);
        inputManager.addMapping("shoot", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addListener(actionListener, "shoot");
        rootNode.setShadowMode(ShadowMode.Off);
        
        batchNode.batch();
        batchNode.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(batchNode);
        
        
        bsr = new PssmShadowRenderer(assetManager, 1024, 2);
        bsr.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        bsr.setLambda(0.55f);
        bsr.setShadowIntensity(0.6f);
        bsr.setCompareMode(CompareMode.Hardware);
        bsr.setFilterMode(FilterMode.PCF4);
        viewPort.addProcessor(bsr);   
    }

    private PhysicsSpace getPhysicsSpace() {
        return bulletAppState.getPhysicsSpace();
    }
    private ActionListener actionListener = new ActionListener() {

        public void onAction(String name, boolean keyPressed, float tpf) {
            if (name.equals("shoot") && !keyPressed) {
                Geometry bulletg = new Geometry("bullet", bullet);
                bulletg.setMaterial(mat2);
                bulletg.setShadowMode(ShadowMode.CastAndReceive);
                bulletg.setLocalTranslation(cam.getLocation());
                RigidBodyControl bulletNode = new BombControl(assetManager, bulletCollisionShape, 1);
//                RigidBodyControl bulletNode = new RigidBodyControl(bulletCollisionShape, 1);
                bulletNode.setLinearVelocity(cam.getDirection().mult(25));
                bulletg.addControl(bulletNode);
                rootNode.attachChild(bulletg);
                getPhysicsSpace().add(bulletNode);
            }
        }
    };

    public void initTower() {
        double tempX = 0;
        double tempY = 0;
        double tempZ = 0;
        angle = 0f;
        for (int i = 0; i < brickLayers; i++){
            // Increment rows
            if(i!=0)
                tempY+=brickHeight*2;
            else
                tempY=brickHeight;
            // Alternate brick seams
            angle = 360.0f / bricksPerLayer * i/2f;
            for (int j = 0; j < bricksPerLayer; j++){
              tempZ = Math.cos(Math.toRadians(angle))*radius;
              tempX = Math.sin(Math.toRadians(angle))*radius;
              System.out.println("x="+((float)(tempX))+" y="+((float)(tempY))+" z="+(float)(tempZ));
              Vector3f vt = new Vector3f((float)(tempX), (float)(tempY), (float)(tempZ));
              // Add crenelation
              if (i==brickLayers-1){
                if (j%2 == 0){
                    addBrick(vt);
                }
              }
              // Create main tower
              else {
                addBrick(vt);
              }
              angle += 360.0/bricksPerLayer;
            }
          }

    }

    public void initFloor() {
        Box floorBox = new Box(Vector3f.ZERO, 10f, 0.1f, 5f);
        floorBox.scaleTextureCoordinates(new Vector2f(3, 6));

        Geometry floor = new Geometry("floor", floorBox);
        floor.setMaterial(mat3);
        floor.setShadowMode(ShadowMode.Receive);
        floor.setLocalTranslation(0, 0, 0);
        floor.addControl(new RigidBodyControl(0));
        this.rootNode.attachChild(floor);
        this.getPhysicsSpace().add(floor);
    }

    public void initMaterial() {
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
        key.setGenerateMips(true);
        Texture tex = assetManager.loadTexture(key);
        mat.setTexture("ColorMap", tex);

        mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
        key2.setGenerateMips(true);
        Texture tex2 = assetManager.loadTexture(key2);
        mat2.setTexture("ColorMap", tex2);

        mat3 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
        key3.setGenerateMips(true);
        Texture tex3 = assetManager.loadTexture(key3);
        tex3.setWrap(WrapMode.Repeat);
        mat3.setTexture("ColorMap", tex3);
    }
int nbBrick =0;
    public void addBrick(Vector3f ori) {
        Geometry reBoxg = new Geometry("brick", brick);
        reBoxg.setMaterial(mat);
        reBoxg.setLocalTranslation(ori);
        reBoxg.rotate(0f, (float)Math.toRadians(angle) , 0f );
        reBoxg.addControl(new RigidBodyControl(1.5f));
        reBoxg.setShadowMode(ShadowMode.CastAndReceive);
        reBoxg.getControl(RigidBodyControl.class).setFriction(1.6f);
        this.batchNode.attachChild(reBoxg);
        this.getPhysicsSpace().add(reBoxg);
        nbBrick++;
    }

    protected void initCrossHairs() {
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+"); // crosshairs
        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    }
}