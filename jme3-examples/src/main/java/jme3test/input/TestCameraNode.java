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
package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.CameraControl.ControlDirection;
import com.jme3.scene.shape.RectangleMesh;
import com.jme3.system.AppSettings;

/**
 * A 3rd-person camera node follows a target (teapot).  Follow the teapot with
 * WASD keys, rotate by dragging the mouse.
 */
public class TestCameraNode extends SimpleApplication implements AnalogListener, ActionListener {

  private Node teaNode;
  private boolean rotate = false;
  final private Vector3f direction = new Vector3f();

  public static void main(String[] args) {
    TestCameraNode app = new TestCameraNode();
    AppSettings s = new AppSettings(true);
    s.setFrameRate(100);
    app.setSettings(s);
    app.start();
  }

  @Override
  public void simpleInitApp() {
    // load a teapot model 
    Geometry teaGeom 
            = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
    teaGeom.setMaterial(mat);
    //create a node to attach the geometry and the camera node
    teaNode = new Node("teaNode");
    teaNode.attachChild(teaGeom);
    rootNode.attachChild(teaNode);
    // create a floor
    mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
    Geometry ground = new Geometry("ground", new RectangleMesh(
            new Vector3f(-25, -1, 25),
            new Vector3f(25, -1, 25),
            new Vector3f(-25, -1, -25)));
    ground.setMaterial(mat);
    rootNode.attachChild(ground);

    //creating the camera Node
    CameraNode camNode = new CameraNode("CamNode", cam);
    // Set the direction to SpatialToCamera, which means the camera will copy the movements of the Node.
    camNode.setControlDir(ControlDirection.SpatialToCamera);
    //attaching the camNode to the teaNode
    teaNode.attachChild(camNode);
    //setting the local translation of the cam node to move it away from the tea Node a bit
    camNode.setLocalTranslation(new Vector3f(-10, 0, 0));
    //setting the camNode to look at the teaNode
    camNode.lookAt(teaNode.getLocalTranslation(), Vector3f.UNIT_Y);

    //disable the default 1st-person flyCam (don't forget this!!)
    flyCam.setEnabled(false);

    registerInput();
  }

  public void registerInput() {
    inputManager.addMapping("moveForward", new KeyTrigger(KeyInput.KEY_UP), new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping("moveBackward", new KeyTrigger(KeyInput.KEY_DOWN), new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("moveRight", new KeyTrigger(KeyInput.KEY_RIGHT), new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping("moveLeft", new KeyTrigger(KeyInput.KEY_LEFT), new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping("toggleRotate", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addMapping("rotateRight", new MouseAxisTrigger(MouseInput.AXIS_X, true));
    inputManager.addMapping("rotateLeft", new MouseAxisTrigger(MouseInput.AXIS_X, false));
    inputManager.addListener(this, "moveForward", "moveBackward", "moveRight", "moveLeft");
    inputManager.addListener(this, "rotateRight", "rotateLeft", "toggleRotate");
  }

  @Override
  public void onAnalog(String name, float value, float tpf) {
    //computing the normalized direction of the cam to move the teaNode
    direction.set(cam.getDirection()).normalizeLocal();
    if (name.equals("moveForward")) {
      direction.multLocal(5 * tpf);
      teaNode.move(direction);
    }
    if (name.equals("moveBackward")) {
      direction.multLocal(-5 * tpf);
      teaNode.move(direction);
    }
    if (name.equals("moveRight")) {
      direction.crossLocal(Vector3f.UNIT_Y).multLocal(5 * tpf);
      teaNode.move(direction);
    }
    if (name.equals("moveLeft")) {
      direction.crossLocal(Vector3f.UNIT_Y).multLocal(-5 * tpf);
      teaNode.move(direction);
    }
    if (name.equals("rotateRight") && rotate) {
      teaNode.rotate(0, 5 * tpf, 0);
    }
    if (name.equals("rotateLeft") && rotate) {
      teaNode.rotate(0, -5 * tpf, 0);
    }

  }

  @Override
  public void onAction(String name, boolean keyPressed, float tpf) {
    //toggling rotation on or off
    if (name.equals("toggleRotate") && keyPressed) {
      rotate = true;
      inputManager.setCursorVisible(false);
    }
    if (name.equals("toggleRotate") && !keyPressed) {
      rotate = false;
      inputManager.setCursorVisible(true);
    }

  }
}
