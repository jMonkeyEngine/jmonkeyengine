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

package jme3test.scene;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class TestLineWidthRenderState extends SimpleApplication {

    private Material mat;

    public static void main(String[] args){
        TestLineWidthRenderState app = new TestLineWidthRenderState();
        app.start();
    }



    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);
        cam.setLocation(new Vector3f(5.5826545f, 3.6192513f, 8.016988f));
        cam.setRotation(new Quaternion(-0.04787097f, 0.9463123f, -0.16569641f, -0.27339742f));

        Box b = new Box(1, 1, 1);
        Geometry geom = new Geometry("Box", b);
        mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Blue);
        mat.getAdditionalRenderState().setWireframe(true);
        mat.getAdditionalRenderState().setLineWidth(2);
        geom.setMaterial(mat);
        rootNode.attachChild(geom);

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if(name.equals("up") && isPressed){
                    mat.getAdditionalRenderState().setLineWidth(mat.getAdditionalRenderState().getLineWidth() + 1);
                }
                if(name.equals("down") && isPressed){
                    mat.getAdditionalRenderState().setLineWidth(Math.max(mat.getAdditionalRenderState().getLineWidth() - 1, 1));
                }
            }
        }, "up", "down");
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_U));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_J));
    }
}