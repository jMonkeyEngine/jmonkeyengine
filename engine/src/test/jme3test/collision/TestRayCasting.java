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

package jme3test.collision;

import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingSphere;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;

public class TestRayCasting extends SimpleApplication {

    private RayTrace tracer;
    private Spatial teapot;

    public static void main(String[] args){
        TestRayCasting app = new TestRayCasting();
        app.setPauseOnLostFocus(false);
        app.start();
    }

    @Override
    public void simpleInitApp() {
//        flyCam.setEnabled(false);

        // load material
        Material mat = (Material) assetManager.loadMaterial("Interface/Logo/Logo.j3m");

        Mesh q = new Mesh();
        q.setBuffer(Type.Position, 3, new float[]
        {
            1, 0, 0,
            0, 1.5f, 0,
            -1, 0, 0
        }
        );
        q.setBuffer(Type.Index, 3, new int[]{ 0, 1, 2 });
        q.setBound(new BoundingSphere());
        q.updateBound();
//        Geometry teapot = new Geometry("MyGeom", q);

        teapot = assetManager.loadModel("Models/Teapot/Teapot.mesh.xml");
//        teapot.scale(2f, 2f, 2f);
//        teapot.move(2f, 2f, -.5f);
        teapot.rotate(FastMath.HALF_PI, FastMath.HALF_PI, FastMath.HALF_PI);
        teapot.setMaterial(mat);
        rootNode.attachChild(teapot);

//        cam.setLocation(cam.getLocation().add(0,1,0));
//        cam.lookAt(teapot.getWorldBound().getCenter(), Vector3f.UNIT_Y);

        tracer = new RayTrace(rootNode, cam, 160, 128);
        tracer.show();
        tracer.update();
    }

    @Override
    public void simpleUpdate(float tpf){
        teapot.rotate(0,tpf,0);
        tracer.update();
    }

}