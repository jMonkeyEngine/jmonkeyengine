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

package jme3test.light;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;
import com.jme3.util.TangentBinormalGenerator;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;


public class TestTangentGen extends SimpleApplication {

    float angle;
    PointLight pl;
    Geometry lightMdl;

    public static void main(String[] args){
        TestTangentGen app = new TestTangentGen();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20);
        Sphere sphereMesh = new Sphere(32, 32, 1);
        sphereMesh.setTextureMode(Sphere.TextureMode.Projected);
        sphereMesh.updateGeometry(32, 32, 1, false, false);
        addMesh("Sphere", sphereMesh, new Vector3f(-1, 0, 0));

        Quad quadMesh = new Quad(1, 1);
        quadMesh.updateGeometry(1, 1);
        addMesh("Quad", quadMesh, new Vector3f(1, 0, 0));

        Mesh strip = createTriangleStripMesh();
        addMesh("strip", strip, new Vector3f(0, -3, 0));
        
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(1, -1, -1).normalizeLocal());
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
    }

    private void addMesh(String name, Mesh mesh, Vector3f translation) {
        TangentBinormalGenerator.generate(mesh);

        Geometry testGeom = new Geometry(name, mesh);
        Material mat = assetManager.loadMaterial("Textures/BumpMapTest/Tangent.j3m");
        testGeom.setMaterial(mat);
        testGeom.getLocalTranslation().set(translation);
        rootNode.attachChild(testGeom);

        Geometry debug = new Geometry(
                "Debug " + name,
                TangentBinormalGenerator.genTbnLines(mesh, 0.08f)
        );
        Material debugMat = assetManager.loadMaterial("Common/Materials/VertexColor.j3m");
        debug.setMaterial(debugMat);
        debug.setCullHint(Spatial.CullHint.Never);
        debug.getLocalTranslation().set(translation);
        rootNode.attachChild(debug);
    }

    @Override
    public void simpleUpdate(float tpf){
    }

    private Mesh createTriangleStripMesh() {
        Mesh strip = new Mesh();
        strip.setMode(Mode.TriangleStrip);
        FloatBuffer vb = BufferUtils.createFloatBuffer(3*3*3); // 3 rows * 3 columns * 3 floats
        vb.rewind();
        vb.put(new float[]{0,2,0}); vb.put(new float[]{1,2,0}); vb.put(new float[]{2,2,0});
        vb.put(new float[]{0,1,0}); vb.put(new float[]{1,1,0}); vb.put(new float[]{2,1,0});
        vb.put(new float[]{0,0,0}); vb.put(new float[]{1,0,0}); vb.put(new float[]{2,0,0});
        FloatBuffer nb = BufferUtils.createFloatBuffer(3*3*3);
        nb.rewind();
        nb.put(new float[]{0,0,1}); nb.put(new float[]{0,0,1}); nb.put(new float[]{0,0,1});
        nb.put(new float[]{0,0,1}); nb.put(new float[]{0,0,1}); nb.put(new float[]{0,0,1});
        nb.put(new float[]{0,0,1}); nb.put(new float[]{0,0,1}); nb.put(new float[]{0,0,1});
        FloatBuffer tb = BufferUtils.createFloatBuffer(3*3*2);
        tb.rewind();
        tb.put(new float[]{0,0}); tb.put(new float[]{0.5f,0}); tb.put(new float[]{1,0});
        tb.put(new float[]{0,0.5f}); tb.put(new float[]{0.5f,0.5f}); tb.put(new float[]{1,0.5f});
        tb.put(new float[]{0,1}); tb.put(new float[]{0.5f,1}); tb.put(new float[]{1,1});
        int[] indexes = new int[]{0,3,1,4,2,5, 5,3, 3,6,4,7,5,8};
        IntBuffer ib = BufferUtils.createIntBuffer(indexes.length);
        ib.put(indexes);
        strip.setBuffer(Type.Position, 3, vb);
		strip.setBuffer(Type.Normal, 3, nb);
		strip.setBuffer(Type.TexCoord, 2, tb);
		strip.setBuffer(Type.Index, 3, ib);
        strip.updateBound();
        return strip;
    }

}
