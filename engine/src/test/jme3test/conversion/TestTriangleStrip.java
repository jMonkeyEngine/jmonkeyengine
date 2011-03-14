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

package jme3test.conversion;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import jme3tools.converters.model.ModelConverter;

public class TestTriangleStrip extends SimpleApplication {


    public static void main(String[] args){
        TestTriangleStrip app = new TestTriangleStrip();
        app.start();
    }

    public void simpleInitApp() {
        Geometry teaGeom = (Geometry) assetManager.loadModel("Models/Teapot/Teapot.obj");
        Mesh teaMesh = teaGeom.getMesh();
        ModelConverter.generateStrips(teaMesh, true, false, 24, 0);

        // show normals as material
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");

        for (int y = -10; y < 10; y++){
            for (int x = -10; x < 10; x++){
                Geometry teaClone = new Geometry("teapot", teaMesh);
                teaClone.setMaterial(mat);

                teaClone.setLocalTranslation(x * .5f, 0, y * .5f);
                teaClone.setLocalScale(.5f);

                rootNode.attachChild(teaClone);
            }
        }

        cam.setLocation(new Vector3f(8.378951f, 5.4324f, 8.795956f));
        cam.setRotation(new Quaternion(-0.083419204f, 0.90370524f, -0.20599906f, -0.36595422f));
    }

}
