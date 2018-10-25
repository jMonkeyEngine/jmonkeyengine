/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.LightNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.debug.WireFrustum;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.shadow.ShadowUtil;
import com.jme3.texture.Texture;
import com.jme3.util.TempVars;

public class TestConeVSFrustum extends SimpleApplication {

    public static void main(String[] args) {
        TestConeVSFrustum app = new TestConeVSFrustum();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        viewPort.setBackgroundColor(ColorRGBA.DarkGray);
        frustumCam = cam.clone();
        frustumCam.setFrustumFar(25);
        Vector3f[] points = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            points[i] = new Vector3f();
        }
        ShadowUtil.updateFrustumPoints2(frustumCam, points);
        WireFrustum frustumShape = new WireFrustum(points);
        Geometry frustum = new Geometry("frustum", frustumShape);
        frustum.setMaterial(new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"));
        rootNode.attachChild(frustum);

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

//        flyCam.setMoveSpeed(30);
//        flyCam.setDragToRotate(true);
//        cam.setLocation(new Vector3f(56.182674f, 19.037334f, 7.093905f));
//        cam.setRotation(new Quaternion(0.0816657f, -0.82228005f, 0.12213967f, 0.5497892f));
        spotLight = new SpotLight();
        spotLight.setSpotRange(25);
        spotLight.setSpotOuterAngle(10 * FastMath.DEG_TO_RAD);

        float radius = FastMath.tan(spotLight.getSpotOuterAngle()) * spotLight.getSpotRange();

        Cylinder cylinder = new Cylinder(5, 16, 0.01f, radius, spotLight.getSpotRange(), true, false);
        geom = new Geometry("light", cylinder);
        geom.setMaterial(new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"));
        geom.getMaterial().setColor("Diffuse", ColorRGBA.White);
        geom.getMaterial().setColor("Ambient", ColorRGBA.DarkGray);
        geom.getMaterial().setBoolean("UseMaterialColors", true);
        final LightNode ln = new LightNode("lb", spotLight);
        ln.attachChild(geom);
        geom.setLocalTranslation(0, -spotLight.getSpotRange() / 2f, 0);
        geom.rotate(-FastMath.HALF_PI, 0, 0);
        rootNode.attachChild(ln);
//        ln.rotate(FastMath.QUARTER_PI, 0, 0);
        //      ln.setLocalTranslation(0, 0, -16);

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
                if ((moving || panning) &&  s!=null) {
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
        /**
         * An unshaded textured cube. // * Uses texture from jme3-test-data
         * library!
         */
        Box boxMesh = new Box(1f, 1f, 1f);
        boxGeo = new Geometry("A Textured Box", boxMesh);
        Material boxMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture monkeyTex = assetManager.loadTexture("Interface/Logo/Monkey.jpg");
        boxMat.setTexture("ColorMap", monkeyTex);
        boxGeo.setMaterial(boxMat);
//        rootNode.attachChild(boxGeo);
//
//boxGeo2 = boxGeo.clone();
//rootNode.attachChild(boxGeo2); 
        System.err.println("light " + spotLight.getPosition());

    }
    Geometry boxGeo, boxGeo2;
    private final static float MOVE_SPEED = 60;
    Vector3f tmp = new Vector3f();
    Quaternion tmpQuat = new Quaternion();
    boolean moving, shift;
    boolean panning;
    Geometry geom;
    SpotLight spotLight;
    Camera frustumCam;

    @Override
    public void simpleUpdate(float tpf) {
        TempVars vars = TempVars.get();
        boolean intersect = spotLight.intersectsFrustum(frustumCam, vars);


        if (intersect) {
            geom.getMaterial().setColor("Diffuse", ColorRGBA.Green);
        } else {
            geom.getMaterial().setColor("Diffuse", ColorRGBA.White);
        }
        Vector3f farPoint = vars.vect1.set(spotLight.getPosition()).addLocal(vars.vect2.set(spotLight.getDirection()).multLocal(spotLight.getSpotRange()));

        //computing the radius of the base disc
        float farRadius = (spotLight.getSpotRange() / FastMath.cos(spotLight.getSpotOuterAngle())) * FastMath.sin(spotLight.getSpotOuterAngle());
        //computing the projection direction : perpendicular to the light direction and coplanar with the direction vector and the normal vector
        Vector3f perpDirection = vars.vect2.set(spotLight.getDirection()).crossLocal(frustumCam.getWorldPlane(3).getNormal()).normalizeLocal().crossLocal(spotLight.getDirection());
        //projecting the far point on the base disc perimeter
        Vector3f projectedPoint = vars.vect3.set(farPoint).addLocal(perpDirection.multLocal(farRadius));


        vars.release();
//        boxGeo.setLocalTranslation(spotLight.getPosition());
        //  boxGeo.setLocalTranslation(projectedPoint);
    }
}
