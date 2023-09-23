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
package jme3test.batching;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Box;
import com.jme3.system.NanoTimer;
import com.jme3.util.TangentBinormalGenerator;

/**
 * A test to demonstrate the usage and functionality of the {@link BatchNode}
 * @author Nehon
 */
public class TestBatchNode extends SimpleApplication {
    private BatchNode batch;
    private WireFrustum frustum;
    private final Vector3f[] points;
    private Geometry cube2;
    private float time = 0;
    private DirectionalLight dl;
    private boolean done = false;

    public static void main(String[] args) {
        TestBatchNode app = new TestBatchNode();
        app.start();
    }

    public TestBatchNode() {
        points = new Vector3f[8];
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    @Override
    public void simpleInitApp() {
        timer = new NanoTimer();
        batch = new BatchNode("theBatchNode");

        /*
         * A cube with a color "bleeding" through transparent texture. Uses
         * Texture from jme3-testdata library!
         */
        Box boxShape4 = new Box(1f, 1f, 1f);
        Geometry cube = new Geometry("cube1", boxShape4);
        Material mat = assetManager.loadMaterial("Textures/Terrain/Pond/Pond.j3m");
        cube.setMaterial(mat);
        //Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        //mat.setColor("Diffuse", ColorRGBA.Blue);
        //mat.setBoolean("UseMaterialColors", true);
        /*
         * A cube with a color "bleeding" through transparent texture. Uses
         * Texture from jme3-testdata library!
         */
        Box box = new Box(1f, 1f, 1f);
        cube2 = new Geometry("cube2", box);
        cube2.setMaterial(mat);

        TangentBinormalGenerator.generate(cube);
        TangentBinormalGenerator.generate(cube2);

        batch.attachChild(cube);
        //  batch.attachChild(cube2);
        //  batch.setMaterial(mat);
        batch.batch();
        rootNode.attachChild(batch);
        cube.setLocalTranslation(3, 0, 0);
        cube2.setLocalTranslation(0, 20, 0);

        updateBoundingPoints(points);
        frustum = new WireFrustum(points);
        Geometry frustumMdl = new Geometry("f", frustum);
        frustumMdl.setCullHint(Spatial.CullHint.Never);
        frustumMdl.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        frustumMdl.getMaterial().getAdditionalRenderState().setWireframe(true);
        frustumMdl.getMaterial().setColor("Color", ColorRGBA.Red);
        rootNode.attachChild(frustumMdl);
        dl = new DirectionalLight();
        dl.setColor(ColorRGBA.White.mult(2));
        dl.setDirection(new Vector3f(1, -1, -1));
        rootNode.addLight(dl);
        flyCam.setMoveSpeed(10);
    }

    @Override
    public void simpleUpdate(float tpf) {
        if (!done) {
            done = true;
            batch.attachChild(cube2);
            batch.batch();
        }
        updateBoundingPoints(points);
        frustum.update(points);
        time += tpf;
        dl.setDirection(cam.getDirection());
        cube2.setLocalTranslation(FastMath.sin(-time) * 3, FastMath.cos(time) * 3, 0);
        cube2.setLocalRotation(new Quaternion().fromAngleAxis(time, Vector3f.UNIT_Z));
        cube2.setLocalScale(Math.max(FastMath.sin(time), 0.5f));

        // batch.setLocalRotation(new Quaternion().fromAngleAxis(time, Vector3f.UNIT_Z));
    }

    public void updateBoundingPoints(Vector3f[] points) {
        BoundingBox bb = (BoundingBox) batch.getWorldBound();
        float xe = bb.getXExtent();
        float ye = bb.getYExtent();
        float ze = bb.getZExtent();
        float x = bb.getCenter().x;
        float y = bb.getCenter().y;
        float z = bb.getCenter().z;

        points[0].set(new Vector3f(x - xe, y - ye, z - ze));
        points[1].set(new Vector3f(x - xe, y + ye, z - ze));
        points[2].set(new Vector3f(x + xe, y + ye, z - ze));
        points[3].set(new Vector3f(x + xe, y - ye, z - ze));

        points[4].set(new Vector3f(x + xe, y - ye, z + ze));
        points[5].set(new Vector3f(x - xe, y - ye, z + ze));
        points[6].set(new Vector3f(x - xe, y + ye, z + ze));
        points[7].set(new Vector3f(x + xe, y + ye, z + ze));
    }
}
