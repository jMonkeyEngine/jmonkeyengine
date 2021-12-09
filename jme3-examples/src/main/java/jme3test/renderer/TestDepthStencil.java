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

package jme3test.renderer;

import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture2D;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;
import com.jme3.ui.Picture;

public class TestDepthStencil extends SimpleApplication {

    private boolean enableStencil = false;
    
    final private Node fbNode = new Node("Framebuffer Node");
    private FrameBuffer fb;

    public static void main(String[] args){
        TestDepthStencil app = new TestDepthStencil();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        int w = settings.getWidth();
        int h = settings.getHeight();

        //setup framebuffer
        fb = new FrameBuffer(w, h, 1);

        Texture2D fbTex = new Texture2D(w, h, Format.RGB8);
        fb.setDepthTarget(FrameBufferTarget.newTarget(Format.Depth24Stencil8));
        fb.addColorTarget(FrameBufferTarget.newTarget(fbTex));

        // setup framebuffer's scene
        Sphere sphMesh = new Sphere(20, 20, 1);
        Material solidColor = assetManager.loadMaterial("Common/Materials/RedColor.j3m");

        final Geometry sphere = new Geometry("sphere", sphMesh);
        sphere.setMaterial(solidColor);
        fbNode.attachChild(sphere);
        
        sphere.addControl(new AbstractControl() {
            @Override
            protected void controlUpdate(float tpf) {
                Material mat = sphere.getMaterial();
                mat.getAdditionalRenderState().setStencil(enableStencil,
                    RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep,
                    RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep, RenderState.StencilOperation.Keep,
                    RenderState.TestFunction.Never, RenderState.TestFunction.Never
                    //TestFunction.Always, TestFunction.Always
                );
            }

            @Override
            protected void controlRender(RenderManager rm, ViewPort vp) {
            }
        });

        //setup main scene
        Picture p = new Picture("Picture");
        p.setPosition(0, 0);
        p.setWidth(w);
        p.setHeight(h);
        p.setTexture(assetManager, fbTex, false);

        rootNode.attachChild(p);
        
        inputManager.addMapping("toggle", new KeyTrigger(KeyInput.KEY_SPACE));
        ActionListener acl = new ActionListener() {
            @Override
            public void onAction(String name, boolean keyPressed, float tpf) {
                if (name.equals("toggle") && keyPressed) {
                    if (enableStencil) {
                        enableStencil = false;
                        System.out.println("Stencil Disabled (model should be visible)");
                    } else {
                        enableStencil = true;
                        System.out.println("Stencil Enabled (model should be hidden)");
                    }
                }
            }
        };
        inputManager.addListener(acl, "toggle");
        
        System.out.println("Press space to toggle stencil");
    }

    @Override
    public void simpleUpdate(float tpf){
        fbNode.updateLogicalState(tpf);
        fbNode.updateGeometricState();
    }

    @Override
    public void simpleRender(RenderManager rm){
        Renderer r = rm.getRenderer();

        //do FBO rendering
        r.setFrameBuffer(fb);

        rm.setCamera(cam, false); // FBO uses current camera
        r.clearBuffers(true, true, true);
        rm.renderScene(fbNode, viewPort);
        rm.flushQueue(viewPort);

        //go back to default rendering and let
        //SimpleApplication render the default scene
        r.setFrameBuffer(null);
    }

}
