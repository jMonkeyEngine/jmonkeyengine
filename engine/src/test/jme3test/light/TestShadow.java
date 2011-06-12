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
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Box;
import com.jme3.shadow.BasicShadowRenderer;
import com.jme3.shadow.ShadowUtil;

public class TestShadow extends SimpleApplication {

    float angle;
    Spatial lightMdl;
    Spatial teapot;
    Geometry frustumMdl;
    WireFrustum frustum;

    private BasicShadowRenderer bsr;
    private Vector3f[] points;

    {
        points = new Vector3f[8];
        for (int i = 0; i < points.length; i++) points[i] = new Vector3f();
    }

    public static void main(String[] args){
        TestShadow app = new TestShadow();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // put the camera in a bad position
        cam.setLocation(new Vector3f(0.7804813f, 1.7502685f, -2.1556435f));
        cam.setRotation(new Quaternion(0.1961598f, -0.7213164f, 0.2266092f, 0.6243975f));
        cam.setFrustumFar(10);

        Material mat = assetManager.loadMaterial("Common/Materials/WhiteColor.j3m");
        rootNode.setShadowMode(ShadowMode.Off);
        Box floor = new Box(Vector3f.ZERO, 3, 0.1f, 3);
        Geometry floorGeom = new Geometry("Floor", floor);
        floorGeom.setMaterial(mat);
        floorGeom.setLocalTranslation(0,-0.2f,0);
        floorGeom.setShadowMode(ShadowMode.Receive);
        rootNode.attachChild(floorGeom);

        teapot = assetManager.loadModel("Models/Teapot/Teapot.obj");
        teapot.setLocalScale(2f);
        teapot.setMaterial(mat);
        teapot.setShadowMode(ShadowMode.CastAndReceive);
        rootNode.attachChild(teapot);
//        lightMdl = new Geometry("Light", new Sphere(10, 10, 0.1f));
//        lightMdl.setMaterial(mat);
//        // disable shadowing for light representation
//        lightMdl.setShadowMode(ShadowMode.Off);
//        rootNode.attachChild(lightMdl);

        bsr = new BasicShadowRenderer(assetManager, 512);
        bsr.setDirection(new Vector3f(-1, -1, -1).normalizeLocal());
        viewPort.addProcessor(bsr);

        frustum = new WireFrustum(bsr.getPoints());
        frustumMdl = new Geometry("f", frustum);
        frustumMdl.setCullHint(Spatial.CullHint.Never);
        frustumMdl.setShadowMode(ShadowMode.Off);
        frustumMdl.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        frustumMdl.getMaterial().getAdditionalRenderState().setWireframe(true);
        frustumMdl.getMaterial().setColor("Color", ColorRGBA.Red);
        rootNode.attachChild(frustumMdl);
    }

    @Override
    public void simpleUpdate(float tpf){
        Camera shadowCam = bsr.getShadowCamera();
        ShadowUtil.updateFrustumPoints2(shadowCam, points);

        frustum.update(points);
        // rotate teapot around Y axis
        teapot.rotate(0, tpf * 0.25f, 0);
    }

}
