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

package jme3test.helloworld;

import com.jme3.anim.AnimComposer;
import com.jme3.anim.tween.Tween;
import com.jme3.anim.tween.Tweens;
import com.jme3.anim.tween.action.Action;
import com.jme3.anim.tween.action.BlendSpace;
import com.jme3.anim.tween.action.LinearBlendSpace;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Sample 7 - Load an OgreXML model and play some of its animations.
 */
public class HelloAnimation extends SimpleApplication {

  private Action advance;
  private AnimComposer control;

  public static void main(String[] args) {
    HelloAnimation app = new HelloAnimation();
    app.start();
  }

  @Override
  public void simpleInitApp() {
    viewPort.setBackgroundColor(ColorRGBA.LightGray);
    initKeys();

    /* Add a light source so we can see the model */
    DirectionalLight dl = new DirectionalLight();
    dl.setDirection(new Vector3f(-0.1f, -1f, -1).normalizeLocal());
    rootNode.addLight(dl);

    /* Load a model that contains animation */
    Node player = (Node) assetManager.loadModel("Models/Oto/Oto.mesh.xml");
    player.setLocalScale(0.5f);
    rootNode.attachChild(player);

    /* Use the model's AnimComposer to play its "stand" animation clip. */
    control = player.getControl(AnimComposer.class);
    control.setCurrentAction("stand");

    /* Compose an animation action named "halt"
       that transitions from "Walk" to "stand" in half a second. */
    BlendSpace quickBlend = new LinearBlendSpace(0f, 0.5f);
    Action halt = control.actionBlended("halt", quickBlend, "stand", "Walk");
    halt.setLength(0.5);

    /* Compose an animation action named "advance"
       that walks for one cycle, then halts, then invokes onAdvanceDone(). */
    Action walk = control.action("Walk");
    Tween doneTween = Tweens.callMethod(this, "onAdvanceDone");
    advance = control.actionSequence("advance", walk, halt, doneTween);
  }

  /**
   * Callback to indicate that the "advance" animation action has completed.
   */
  void onAdvanceDone() {
    /*
     * Play the "stand" animation action.
     */
    control.setCurrentAction("stand");
  }

  /**
   * Map the spacebar to the "Walk" input action, and add a listener to initiate
   * the "advance" animation action each time it's pressed.
   */
  private void initKeys() {
    inputManager.addMapping("Walk", new KeyTrigger(KeyInput.KEY_SPACE));

    ActionListener handler = new ActionListener() {
      @Override
      public void onAction(String name, boolean keyPressed, float tpf) {
        if (keyPressed && control.getCurrentAction() != advance) {
          control.setCurrentAction("advance");
        }
      }
    };
    inputManager.addListener(handler, "Walk");
  }

}
