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
package jme3test.input;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

/** A 3rd-person chase camera orbits a target (teapot). 
 * Follow the teapot with WASD keys, rotate by dragging the mouse. */
public class TestChaseCamera extends SimpleApplication implements AnalogListener, ActionListener {

  private Geometry teaGeom;
  private ChaseCamera chaseCam;

  public static void main(String[] args) {
    TestChaseCamera app = new TestChaseCamera();
    app.start();
  }

  public void simpleInitApp() {
    // Load a teapot model
    teaGeom = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
    Material mat_tea = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
    teaGeom.setMaterial(mat_tea);
    rootNode.attachChild(teaGeom);

    // Load a floor model
    Material mat_ground = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat_ground.setTexture("ColorMap", assetManager.loadTexture("Interface/Logo/Monkey.jpg"));
    Geometry ground = new Geometry("ground", new Quad(50, 50));
    ground.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
    ground.setLocalTranslation(-25, -1, 25);
    ground.setMaterial(mat_ground);
    rootNode.attachChild(ground);
    
    // Disable the default first-person cam!
    flyCam.setEnabled(false);

    // Enable a chase cam
    chaseCam = new ChaseCamera(cam, teaGeom, inputManager);

    //Uncomment this to invert the camera's vertical rotation Axis 
    //chaseCam.setInvertVerticalAxis(true);

    //Uncomment this to invert the camera's horizontal rotation Axis
    //chaseCam.setInvertHorizontalAxis(true);

    //Comment this to disable smooth camera motion
    chaseCam.setSmoothMotion(true);

    //Uncomment this to disable trailing of the camera 
    //WARNING, trailing only works with smooth motion enabled. It is true by default.
    //chaseCam.setTrailingEnabled(false);

    //Uncomment this to look 3 world units above the target
    //chaseCam.setLookAtOffset(Vector3f.UNIT_Y.mult(3));

    //Uncomment this to enable rotation when the middle mouse button is pressed (like Blender)
    //WARNING : setting this trigger disable the rotation on right and left mouse button click
    //chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));

    //Uncomment this to set mutiple triggers to enable rotation of the cam
    //Here spade bar and middle mouse button
    //chaseCam.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE),new KeyTrigger(KeyInput.KEY_SPACE));

    //registering inputs for target's movement
    registerInput();

  }

  public void registerInput() {
    inputManager.addMapping("moveForward", new KeyTrigger(keyInput.KEY_UP), new KeyTrigger(keyInput.KEY_W));
    inputManager.addMapping("moveBackward", new KeyTrigger(keyInput.KEY_DOWN), new KeyTrigger(keyInput.KEY_S));
    inputManager.addMapping("moveRight", new KeyTrigger(keyInput.KEY_RIGHT), new KeyTrigger(keyInput.KEY_D));
    inputManager.addMapping("moveLeft", new KeyTrigger(keyInput.KEY_LEFT), new KeyTrigger(keyInput.KEY_A));
    inputManager.addMapping("displayPosition", new KeyTrigger(keyInput.KEY_P));
    inputManager.addListener(this, "moveForward", "moveBackward", "moveRight", "moveLeft");
    inputManager.addListener(this, "displayPosition");
  }

  public void onAnalog(String name, float value, float tpf) {
    if (name.equals("moveForward")) {
      teaGeom.move(0, 0, -5 * tpf);
    }
    if (name.equals("moveBackward")) {
      teaGeom.move(0, 0, 5 * tpf);
    }
    if (name.equals("moveRight")) {
      teaGeom.move(5 * tpf, 0, 0);
    }
    if (name.equals("moveLeft")) {
      teaGeom.move(-5 * tpf, 0, 0);

    }

  }

  public void onAction(String name, boolean keyPressed, float tpf) {
    if (name.equals("displayPosition") && keyPressed) {
      teaGeom.move(10, 10, 10);

    }
  }

  @Override
  public void simpleUpdate(float tpf) {
    super.simpleUpdate(tpf);

    //  teaGeom.move(new Vector3f(0.001f, 0, 0));
    // pivot.rotate(0, 0.00001f, 0);
    //   rootNode.updateGeometricState();
  }
//    public void update() {
//        super.update();
//// render the viewports
//        float tpf = timer.getTimePerFrame();
//        state.getRootNode().rotate(0, 0.000001f, 0);
//        stateManager.update(tpf);
//        stateManager.render(renderManager);
//        renderManager.render(tpf);
//    }
}
