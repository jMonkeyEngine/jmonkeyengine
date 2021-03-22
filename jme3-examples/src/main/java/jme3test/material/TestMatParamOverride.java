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
package jme3test.material;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector4f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;

/**
 * Test if {@link MatParamOverride}s are working correctly.
 *
 * @author Kirill Vainer
 */
public class TestMatParamOverride extends SimpleApplication {

    final private Box box = new Box(1, 1, 1);
    final MatParamOverride overrideYellow
            = new MatParamOverride(VarType.Vector4, "Color",
                    ColorRGBA.Yellow);
    final MatParamOverride overrideWhite
            = new MatParamOverride(VarType.Vector4, "Color",
                    Vector4f.UNIT_XYZW);
    final MatParamOverride overrideGray
            = new MatParamOverride(VarType.Vector4, "Color",
                    new Quaternion(0.5f, 0.5f, 0.5f, 1f));

    public static void main(String[] args) {
        TestMatParamOverride app = new TestMatParamOverride();
        app.start();
    }

    private void createBox(float location, ColorRGBA color) {
        Geometry geom = new Geometry("Box", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        geom.setMaterial(mat);
        geom.move(location, 0, 0);
        rootNode.attachChild(geom);
    }

    @Override
    public void simpleInitApp() {
        inputManager.setCursorVisible(true);

        createBox(-3, ColorRGBA.Red);
        createBox(0, ColorRGBA.Green);
        createBox(3, ColorRGBA.Blue);

        System.out.println("Press G, W, Y, or space bar ...");
        inputManager.addMapping("overrideClear", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("overrideGray", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("overrideWhite", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("overrideYellow", new KeyTrigger(KeyInput.KEY_Y));
        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (isPressed) {
                    if (name.equals("overrideClear")) {
                        rootNode.clearMatParamOverrides();
                    } else if (name.equals("overrideGray")) {
                        rootNode.clearMatParamOverrides();
                        rootNode.addMatParamOverride(overrideGray);
                    } else if (name.equals("overrideWhite")) {
                        rootNode.clearMatParamOverrides();
                        rootNode.addMatParamOverride(overrideWhite);
                    } else if (name.equals("overrideYellow")) {
                        rootNode.clearMatParamOverrides();
                        rootNode.addMatParamOverride(overrideYellow);
                    }
                    System.out.println(rootNode.getLocalMatParamOverrides());
                }
            }
        }, "overrideClear", "overrideGray", "overrideWhite", "overrideYellow");
    }
}
