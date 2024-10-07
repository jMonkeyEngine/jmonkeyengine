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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;

/**
 * This test demonstrates the usage of customized blend equations and factors on a material.<br>
 * Customized blend equations and factors always requires {@link com.jme3.material.RenderState.BlendMode#Custom}.
 *
 * @author the_Minka
 */
public class TestBlendEquations extends SimpleApplication {

    private Geometry leftQuad;
    private Geometry rightQuad;

    private float timer;

    public static void main(String[] args) {
        TestBlendEquations app = new TestBlendEquations();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        cam.setLocation(new Vector3f(0f, 0.5f, 3f));
        viewPort.setBackgroundColor(ColorRGBA.LightGray);

        // Add a light source to the scene.
        DirectionalLight directionalLight = new DirectionalLight();
        directionalLight.setColor(ColorRGBA.Magenta);
        directionalLight.setDirection(Vector3f.UNIT_XYZ.negate());
        rootNode.addLight(directionalLight);


        // Create and add a teapot to the scene graph.
        Spatial teapotModel = assetManager.loadModel("Models/Teapot/Teapot.obj");
        rootNode.attachChild(teapotModel);

        // Create the two moving quads with custom blend modes.
        createLeftQuad();
        createRightQuad();
    }

    /**
     * Adds a "transparent" quad to the scene, that shows an inverse blue value sight of the scene behind.
     */
    private void createLeftQuad() {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");

        // This color creates a blue value image. The effect will have a strength of 80% (set by the alpha value).
        material.setColor("Color", new ColorRGBA(0f, 0f, 1f, 0.8f));

        // Result.RGB = Source.A * Source.RGB - Source.A * Destination.RGB
        // Result.A   = Destination.A
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Custom);
        material.getAdditionalRenderState().setBlendEquation(RenderState.BlendEquation.Subtract);
        material.getAdditionalRenderState().setBlendEquationAlpha(RenderState.BlendEquationAlpha.Add);
        material.getAdditionalRenderState().setCustomBlendFactors(
                RenderState.BlendFunc.Src_Alpha, RenderState.BlendFunc.Src_Alpha,
                RenderState.BlendFunc.Zero, RenderState.BlendFunc.One);

        leftQuad = new Geometry("LeftQuad", new Quad(1f, 1f));
        leftQuad.setMaterial(material);
        leftQuad.setQueueBucket(RenderQueue.Bucket.Transparent);
        rootNode.attachChild(leftQuad);
    }

    /**
     * Adds a "transparent" quad to the scene, that limits the color values of the scene behind the object.<br>
     * This effect can be good seen on bright areas of the scene (e.g. areas with specular lighting effects).
     */
    private void createRightQuad() {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.4f, 0.4f, 0.4f, 1f));

        // Min( Source , Destination)
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Custom);
        material.getAdditionalRenderState().setBlendEquation(RenderState.BlendEquation.Min);
        material.getAdditionalRenderState().setBlendEquationAlpha(RenderState.BlendEquationAlpha.Min);

        // In OpenGL no blend factors are used, when using the blend equations Min or Max!
        //material.getAdditionalRenderState().setCustomBlendFactors(
        //        RenderState.BlendFunc.One, RenderState.BlendFunc.One,
        //        RenderState.BlendFunc.One, RenderState.BlendFunc.One);

        rightQuad = new Geometry("RightQuad", new Quad(1f, 1f));
        rightQuad.setMaterial(material);
        rightQuad.setQueueBucket(RenderQueue.Bucket.Transparent);
        rootNode.attachChild(rightQuad);
    }

    @Override
    public void simpleUpdate(float tpf) {
        timer += tpf;

        float xOffset = FastMath.sin(timer * 0.5f) * 2f;
        leftQuad.setLocalTranslation(xOffset - 2f, 0f, 0.5f);
        rightQuad.setLocalTranslation(xOffset + 1f, 0f, 0.5f);
    }

}
