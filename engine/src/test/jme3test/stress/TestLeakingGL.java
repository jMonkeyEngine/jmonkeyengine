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

package jme3test.stress;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.NativeObjectManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates 400 new meshes every frame then leaks them.
 * Notice how memory usage stays constant and OpenGL objects
 * are properly destroyed.
 */
public class TestLeakingGL extends SimpleApplication {

    private Material solidColor;
    private Sphere original;

    public static void main(String[] args){
        TestLeakingGL app = new TestLeakingGL();
        app.start();
    }

    public void simpleInitApp() {
        original = new Sphere(4, 4, 1);
        original.setStatic();
        original.setInterleaved();

        // this will make sure all spheres are rendered always
        rootNode.setCullHint(CullHint.Never);
        solidColor = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
        cam.setLocation(new Vector3f(0, 5, 0));
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);

        Logger.getLogger(Node.class.getName()).setLevel(Level.WARNING);
        Logger.getLogger(NativeObjectManager.class.getName()).setLevel(Level.WARNING);
    }

    @Override
    public void simpleUpdate(float tpf){
        rootNode.detachAllChildren();
        for (int y = -15; y < 15; y++){
            for (int x = -15; x < 15; x++){
                Mesh sphMesh = original.deepClone();
                Geometry sphere = new Geometry("sphere", sphMesh);

                sphere.setMaterial(solidColor);
                sphere.setLocalTranslation(x * 1.5f, 0, y * 1.5f);
                rootNode.attachChild(sphere);
            }
        }
    }
}
