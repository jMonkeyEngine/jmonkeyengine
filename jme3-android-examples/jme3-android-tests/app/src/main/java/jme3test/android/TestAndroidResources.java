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
package jme3test.android;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

/**
 * Test case to look for images stored in the Android drawable and mipmap directories.  Image files are
 * stored in the main->res->drawable-xxxx directories and main->res->mipmap-xxxx directories.  The Android OS
 * will choose the best matching image based on the device capabilities.
 *
 * @author iwgeric
 */
public class TestAndroidResources extends SimpleApplication {

    @Override
    public void simpleInitApp() {
        // Create boxes with textures that are stored in the Android Resources Folders
        // Images are stored in multiple Drawable and Mipmap directories.  Android picks the ones that
        // match the device size and density.
        Box box1Mesh = new Box(1, 1, 1);
        Geometry box1 = new Geometry("Monkey Box 1", box1Mesh);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setTexture("ColorMap", assetManager.loadTexture("drawable_monkey.png"));
        box1.setMaterial(mat1);
        box1.setLocalTranslation(-2, 0, 0);
        rootNode.attachChild(box1);

        Box box2Mesh = new Box(1, 1, 1);
        Geometry box2 = new Geometry("Monkey Box 2", box2Mesh);
        Material mat2 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setTexture("ColorMap", assetManager.loadTexture("mipmap_monkey.png"));
        box2.setMaterial(mat2);
        box2.setLocalTranslation(2, 0, 0);
        rootNode.attachChild(box2);

    }
}
