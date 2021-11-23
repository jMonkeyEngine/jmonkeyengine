/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package jme3test.post;

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.ContrastAdjustmentFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;

/**
 * Tests {@link ContrastAdjustmentFilter} on different color channels with an
 * adjustable exponents, scales and input range during runtime.
 * @author pavl_g.
 */
public class TestContrastAdjustmentFilter extends SimpleApplication {

    private float counter = 0f;
    private ContrastAdjustmentFilter contrastAdjustmentFilter;

    public static void main(String[] args) {
        new TestContrastAdjustmentFilter().start();
    }

    @Override
    public void simpleInitApp() {
        //setup a spatial and a texture
        final Sphere globe = new Sphere(40, 40, 5f);
        final Geometry earth = new Geometry("Globe", globe);
        final Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        final Texture texture = assetManager.loadTexture("Textures/Sky/Earth/Earth.jpg");
        material.setTexture("ColorMap", texture);
        earth.setMaterial(material);

        rootNode.attachChild(earth);

        //add light
        final AmbientLight ambientLight = new AmbientLight(ColorRGBA.White);
        rootNode.addLight(ambientLight);

        //setup the filter
        final FilterPostProcessor postProcessor = new FilterPostProcessor(assetManager);
        contrastAdjustmentFilter = new ContrastAdjustmentFilter();
        //adjusting some parameters
        contrastAdjustmentFilter.setExponents(1.8f, 1.8f, 2.1f)
                                .setInputRange(0, 0.367f)
                                .setScales(0.25f, 0.25f, 1f);
        postProcessor.addFilter(contrastAdjustmentFilter);
        viewPort.addProcessor(postProcessor);

    }

    @Override
    public void simpleUpdate(float tpf) {
        counter += tpf;
        if (counter >= 4f) {
            counter = 0f;
        }
        if (contrastAdjustmentFilter != null) {
            //adjust the transfer function during runtime
            contrastAdjustmentFilter.setScales(counter * 0.5f, counter, counter * 0.5f)
                                    .setExponents(counter, counter * 2f, counter)
                                    .setInputRange(0, 1 / counter);
        }
    }
}
