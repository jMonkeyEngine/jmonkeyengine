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
package jme3test.light;

import com.jme3.app.ChaseCameraAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.*;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.*;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.*;
import com.jme3.shadow.ShadowUtil;
import com.jme3.util.TempVars;


public class TestObbVsBounds extends SimpleApplication {

    private Node ln;
    final private BoundingBox aabb = new BoundingBox();
    final private BoundingSphere sphere = new BoundingSphere(10, new Vector3f(-30, 0, -60));

    private final static float MOVE_SPEED = 60;
    final private Vector3f tmp = new Vector3f();
    final private Quaternion tmpQuat = new Quaternion();
    private boolean moving, shift;
    private boolean panning;

    final private OrientedBoxProbeArea area = new OrientedBoxProbeArea();
    private Camera frustumCam;

    private Geometry areaGeom;
    private Geometry frustumGeom;
    private Geometry aabbGeom;
    private Geometry sphereGeom;

    public static void main(String[] args) {
        TestObbVsBounds app = new TestObbVsBounds();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        frustumCam = cam.clone();
        frustumCam.setFrustumFar(25);
        makeCamFrustum();
        aabb.setCenter(20, 10, -60);
        aabb.setXExtent(10);
        aabb.setYExtent(5);
        aabb.setZExtent(3);
        makeBoxWire(aabb);
        makeSphereWire(sphere);

        rootNode.addLight(new DirectionalLight());
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(0.2f));
        rootNode.addLight(al);

        Grid grid = new Grid(50, 50, 5);
        Geometry gridGeom = new Geometry("grid", grid);
        gridGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        gridGeom.getMaterial().setColor("Color", ColorRGBA.Gray);
        rootNode.attachChild(gridGeom);
        gridGeom.setLocalTranslation(-125, -25, -125);

        area.setCenter(Vector3f.ZERO);
        area.setExtent(new Vector3f(4, 8, 5));
        makeAreaGeom();

        ln = new Node("lb");
        ln.setLocalRotation(new Quaternion(-0.18826798f, -0.38304946f, -0.12780227f, 0.895261f));
        ln.attachChild(areaGeom);
        ln.setLocalScale(4,8,5);
        rootNode.attachChild(ln);

        inputManager.addMapping("click", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        inputManager.addMapping("shift", new KeyTrigger(KeyInput.KEY_LSHIFT), new KeyTrigger(KeyInput.KEY_RSHIFT));
        inputManager.addMapping("middleClick", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));
        inputManager.addMapping("up", new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        inputManager.addMapping("down", new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        inputManager.addMapping("left", new MouseAxisTrigger(MouseInput.AXIS_X, true));
        inputManager.addMapping("right", new MouseAxisTrigger(MouseInput.AXIS_X, false));


        final Node camTarget = new Node("CamTarget");
        rootNode.attachChild(camTarget);

        ChaseCameraAppState chaser = new ChaseCameraAppState();
        chaser.setTarget(camTarget);
        chaser.setMaxDistance(150);
        chaser.setDefaultDistance(70);
        chaser.setDefaultHorizontalRotation(FastMath.HALF_PI);
        chaser.setMinVerticalRotation(-FastMath.PI);
        chaser.setMaxVerticalRotation(FastMath.PI * 2);
        chaser.setToggleRotationTrigger(new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        stateManager.attach(chaser);
        flyCam.setEnabled(false);

        inputManager.addListener(new AnalogListener() {
            @Override
            public void onAnalog(String name, float value, float tpf) {
                Spatial s = null;
                float mult = 1;
                if (moving) {
                    s = ln;
                }
                if (panning) {
                    s = camTarget;
                    mult = -1;
                }
                if ((moving || panning) && s != null) {
                    if (shift) {
                        if (name.equals("left")) {
                            tmp.set(cam.getDirection());
                            s.rotate(tmpQuat.fromAngleAxis(value, tmp));
                        }
                        if (name.equals("right")) {
                            tmp.set(cam.getDirection());
                            s.rotate(tmpQuat.fromAngleAxis(-value, tmp));
                        }
                    } else {
                        value *= MOVE_SPEED * mult;
                        if (name.equals("up")) {
                            tmp.set(cam.getUp()).multLocal(value);
                            s.move(tmp);
                        }
                        if (name.equals("down")) {
                            tmp.set(cam.getUp()).multLocal(-value);
                            s.move(tmp);
                        }
                        if (name.equals("left")) {
                            tmp.set(cam.getLeft()).multLocal(value);
                            s.move(tmp);
                        }
                        if (name.equals("right")) {
                            tmp.set(cam.getLeft()).multLocal(-value);
                            s.move(tmp);
                        }
                    }
                }
            }
        }, "up", "down", "left", "right");

        inputManager.addListener(new ActionListener() {
            @Override
            public void onAction(String name, boolean isPressed, float tpf) {
                if (name.equals("click")) {
                    if (isPressed) {
                        moving = true;
                    } else {
                        moving = false;
                    }
                }
                if (name.equals("middleClick")) {
                    if (isPressed) {
                        panning = true;
                    } else {
                        panning = false;
                    }
                }
                if (name.equals("shift")) {
                    if (isPressed) {
                        shift = true;
                    } else {
                        shift = false;
                    }
                }
            }
        }, "click", "middleClick", "shift");

    }

    public void makeAreaGeom() {

        Vector3f[] points = new Vector3f[8];

        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

        points[0].set(-1, -1, 1);
        points[1].set(-1, 1, 1);
        points[2].set(1, 1, 1);
        points[3].set(1, -1, 1);

        points[4].set(-1, -1, -1);
        points[5].set(-1, 1, -1);
        points[6].set(1, 1, -1);
        points[7].set(1, -1, -1);

        Mesh box = WireFrustum.makeFrustum(points);
        areaGeom = new Geometry("light", box);
        areaGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        areaGeom.getMaterial().setColor("Color", ColorRGBA.White);
    }

    public void makeCamFrustum() {
        Vector3f[] points = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            points[i] = new Vector3f();
        }
        ShadowUtil.updateFrustumPoints2(frustumCam, points);
        WireFrustum frustumShape = new WireFrustum(points);
        frustumGeom = new Geometry("frustum", frustumShape);
        frustumGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        rootNode.attachChild(frustumGeom);
    }

    public void makeBoxWire(BoundingBox box) {
        Vector3f[] points = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            points[i] = new Vector3f();
        }
        points[0].set(-1, -1, 1);
        points[1].set(-1, 1, 1);
        points[2].set(1, 1, 1);
        points[3].set(1, -1, 1);

        points[4].set(-1, -1, -1);
        points[5].set(-1, 1, -1);
        points[6].set(1, 1, -1);
        points[7].set(1, -1, -1);

        WireFrustum frustumShape = new WireFrustum(points);
        aabbGeom = new Geometry("box", frustumShape);
        aabbGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        aabbGeom.getMaterial().getAdditionalRenderState().setWireframe(true);
        aabbGeom.setLocalTranslation(box.getCenter());
        aabbGeom.setLocalScale(box.getXExtent(), box.getYExtent(), box.getZExtent());
        rootNode.attachChild(aabbGeom);
    }

    public void makeSphereWire(BoundingSphere sphere) {

        sphereGeom = new Geometry("box", new Sphere(16, 16, 10));
        sphereGeom.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        sphereGeom.getMaterial().getAdditionalRenderState().setWireframe(true);
        sphereGeom.setLocalTranslation(sphere.getCenter());
        rootNode.attachChild(sphereGeom);
    }


    @Override
    public void simpleUpdate(float tpf) {

        area.setCenter(ln.getLocalTranslation());
        area.setRotation(ln.getLocalRotation());

        TempVars vars = TempVars.get();
        boolean intersectBox = area.intersectsBox(aabb, vars);
        boolean intersectFrustum = area.intersectsFrustum(frustumCam, vars);
        boolean intersectSphere = area.intersectsSphere(sphere, vars);
        vars.release();

        boolean intersect = intersectBox || intersectFrustum || intersectSphere;

        areaGeom.getMaterial().setColor("Color", intersect ? ColorRGBA.Green : ColorRGBA.White);
        sphereGeom.getMaterial().setColor("Color", intersectSphere ? ColorRGBA.Cyan : ColorRGBA.White);
        frustumGeom.getMaterial().setColor("Color", intersectFrustum ? ColorRGBA.Cyan : ColorRGBA.White);
        aabbGeom.getMaterial().setColor("Color", intersectBox ? ColorRGBA.Cyan : ColorRGBA.White);

    }
}
