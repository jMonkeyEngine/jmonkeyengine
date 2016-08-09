/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;

public class TestBlendEquations extends SimpleApplication {

    public static void main(String[] args) {
        TestBlendEquations app = new TestBlendEquations();
        app.start();
    }

    public void simpleInitApp() {
        Geometry teaGeom = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        teaGeom.scale(6);
        teaGeom.getMaterial().getAdditionalRenderState().setBlendEquation(RenderState.BlendEquation.Add);
        teaGeom.move(0, -2f, 0);

        DirectionalLight dl = new DirectionalLight();
        dl.setColor(ColorRGBA.Red);
        dl.setDirection(Vector3f.UNIT_XYZ.negate());

        rootNode.addLight(dl);
        rootNode.attachChild(teaGeom);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(0.5f, 0f, 1f, 0.3f));
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Color);
        mat.getAdditionalRenderState().setBlendEquation(RenderState.BlendEquation.Subtract);

        Geometry geo = new Geometry("BottomLeft", new Quad(guiViewPort.getCamera().getWidth() / 2, guiViewPort.getCamera().getHeight() / 2));
        geo.setMaterial(mat);
        geo.setQueueBucket(RenderQueue.Bucket.Gui);
        geo.setLocalTranslation(0, 0, 1);

        guiNode.attachChild(geo);

        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.getAdditionalRenderState().setBlendEquation(RenderState.BlendEquation.ReverseSubtract);
        m.setColor("Color", new ColorRGBA(0.0f, 1f, 1.f, 1f));
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.AlphaAdditive);

        geo = new Geometry("BottomRight", new Quad(guiViewPort.getCamera().getWidth() / 2, guiViewPort.getCamera().getHeight() / 2));
        geo.setMaterial(m);
        geo.setQueueBucket(RenderQueue.Bucket.Gui);
        geo.setLocalTranslation(guiViewPort.getCamera().getWidth() / 2, 0, 1);
        
        guiNode.attachChild(geo);
        
        m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        m.getAdditionalRenderState().setBlendEquation(RenderState.BlendEquation.Min);
        m.setColor("Color", new ColorRGBA(0.3f, 0f, 0.1f, 0.3f));
        m.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Additive);

        geo = new Geometry("TopRight", new Quad(guiViewPort.getCamera().getWidth() / 2, guiViewPort.getCamera().getHeight() / 2));
        geo.setMaterial(m);
        geo.setQueueBucket(RenderQueue.Bucket.Gui);
        geo.setLocalTranslation(guiViewPort.getCamera().getWidth() / 2, guiViewPort.getCamera().getHeight() / 2, 1);

        guiNode.attachChild(geo);

        geo = new Geometry("OverTeaPot", new Quad(guiViewPort.getCamera().getWidth() / 2, guiViewPort.getCamera().getHeight() / 2));
        geo.setMaterial(mat);
        geo.setQueueBucket(RenderQueue.Bucket.Transparent);
        geo.setLocalTranslation(0, -100, 5);

        rootNode.attachChild(geo);

    }


}
