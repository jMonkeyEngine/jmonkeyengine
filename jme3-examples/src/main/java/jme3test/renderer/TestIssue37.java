/*
 * Copyright (c) 2021 jMonkeyEngine
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

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.material.MatParamOverride;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;

/**
 * Test a Material with increasing numbers of texture parameters, to see what
 * happens when the renderer's dynamic limit is exceeded.
 *
 * If successful, this test throws an IllegalStateException with a helpful
 * diagnostic message.
 */
public class TestIssue37 extends SimpleApplication {

    /**
     * Edit this field to change how parameters are assigned (which determines
     * where the exception is caught): true to use mat param overrides, false to
     * use ordinary mat params.
     */
    final private boolean useOverrides = true;

    private int numTextures;
    private Material manyTexturesMaterial;
    private Texture testTexture;

    public static void main(String[] args) {
        Application application = new TestIssue37();
        application.start();
    }

    @Override
    public void simpleInitApp() {
        /*
         * Attach a test geometry to the scene.
         */
        Mesh cubeMesh = new Box(1f, 1f, 1f);
        Geometry cubeGeometry = new Geometry("Box", cubeMesh);
        rootNode.attachChild(cubeGeometry);
        /*
         * Apply a test material (with no textures assigned) to the geometry.
         */
        manyTexturesMaterial = new Material(assetManager,
                "jme3test/materials/TestIssue37.j3md");
        manyTexturesMaterial.setName("manyTexturesMaterial");
        cubeGeometry.setMaterial(manyTexturesMaterial);
        numTextures = 0;
        /*
         * Load the test texture.
         */
        String texturePath = "Interface/Logo/Monkey.jpg";
        testTexture = assetManager.loadTexture(texturePath);
    }

    /**
     * During each update, define another texture parameter until the dynamic
     * limit is reached.
     *
     * @param tpf ignored
     */
    @Override
    public void simpleUpdate(float tpf) {
        String parameterName = "ColorMap" + numTextures;
        if (useOverrides) {
            MatParamOverride override = new MatParamOverride(VarType.Texture2D,
                    parameterName, testTexture);
            rootNode.addMatParamOverride(override);
        } else {
            manyTexturesMaterial.setTexture(parameterName, testTexture);
        }
        ++numTextures;
    }
}
