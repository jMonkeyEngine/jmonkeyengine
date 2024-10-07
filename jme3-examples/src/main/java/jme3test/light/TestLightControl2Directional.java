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

import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Dome;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.TempVars;

/**
 * Similar to {@link TestLightControlDirectional}, except that the spatial is controlled by the light this
 * time.
 *
 * @author Markil 3
 */
public class TestLightControl2Directional extends SimpleApplication {
    private final Vector3f rotAxis = new Vector3f(Vector3f.UNIT_X);
    private final float[] angles = new float[3];

    private Node lightNode;
    private DirectionalLight direction;

    public static void main(String[] args) {
        TestLightControl2Directional app = new TestLightControl2Directional();
        app.start();
    }

    public void setupLighting() {
        Geometry lightMdl;
        AmbientLight al = new AmbientLight();
        al.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(al);

        direction = new DirectionalLight();
        direction.setColor(ColorRGBA.White.mult(10));
        rootNode.addLight(direction);

        lightMdl = new Geometry("Light", new Dome(Vector3f.ZERO, 2, 32, 5, false));
        lightMdl.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightMdl.setLocalTranslation(new Vector3f(0, 0, 0));
        lightMdl.setLocalRotation(new Quaternion().fromAngles(FastMath.PI / 2F, 0, 0));
        rootNode.attachChild(lightMdl);

        /*
         * We need this Dome doesn't have a "floor."
         */
        Geometry lightFloor = new Geometry("LightFloor", new Cylinder(2, 32, 5, .1F, true));
        lightFloor.setMaterial(assetManager.loadMaterial("Common/Materials/RedColor.j3m"));
        lightFloor.getMaterial().setColor("Color", ColorRGBA.White);

        lightNode = new Node();
        lightNode.addControl(new LightControl(direction, LightControl.ControlDirection.LightToSpatial));
        lightNode.attachChild(lightMdl);
        lightNode.attachChild(lightFloor);

        rootNode.attachChild(lightNode);
    }

    public void setupDome() {
        Geometry dome = new Geometry("Dome", new Sphere(16, 32, 30, false, true));
        dome.setMaterial(new Material(this.assetManager, "Common/MatDefs/Light/PBRLighting.j3md"));
        dome.setLocalTranslation(new Vector3f(0, 0, 0));
        rootNode.attachChild(dome);
    }

    @Override
    public void simpleInitApp() {
        this.cam.setLocation(new Vector3f(-50, 20, 50));
        this.cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        flyCam.setMoveSpeed(30);

        setupLighting();
        setupDome();
    }

    @Override
    public void simpleUpdate(float tpf) {
        final Vector3f INIT_DIR = Vector3f.UNIT_Z.negate();
        /*
         * In Radians per second
         */
        final float ROT_SPEED = FastMath.PI / 2;
        /*
         * 360 degree rotation
         */
        final float FULL_ROT = FastMath.PI * 2;

        TempVars vars = TempVars.get();
        Vector3f lightDirection = vars.vect2, nodeDirection = vars.vect3;
        float length;

        angles[0] += rotAxis.x * ROT_SPEED * tpf;
        angles[1] += rotAxis.y * ROT_SPEED * tpf;
        angles[2] += rotAxis.z * ROT_SPEED * tpf;
        direction.setDirection(new Quaternion().fromAngles(angles).mult(INIT_DIR));
        super.simpleUpdate(tpf);

        /*
         * Make sure they are equal.
         */
        lightDirection.set(direction.getDirection());
        lightDirection.normalizeLocal();
        lightNode.getWorldRotation().mult(Vector3f.UNIT_Z, nodeDirection);
        nodeDirection.negateLocal().normalizeLocal();
        length = lightDirection.subtract(nodeDirection, vars.vect4).lengthSquared();
        length = FastMath.abs(length);
        if (length > .1F) {
            System.err.printf("Rotation not equal: is %s, needs to be %s (%f)\n", nodeDirection, lightDirection, length);
        }

        if (angles[0] >= FULL_ROT || angles[1] >= FULL_ROT || angles[2] >= FULL_ROT) {
            direction.setDirection(INIT_DIR);
            angles[0] = 0;
            angles[1] = 0;
            angles[2] = 0;
            if (rotAxis.x > 0 && rotAxis.y == 0 && rotAxis.z == 0) {
                rotAxis.set(0, 1, 0);
            } else if (rotAxis.y > 0 && rotAxis.x == 0 && rotAxis.z == 0) {
                rotAxis.set(0, 0, 1);
            } else if (rotAxis.z > 0 && rotAxis.x == 0 && rotAxis.y == 0) {
                rotAxis.set(FastMath.nextRandomFloat() % 1, FastMath.nextRandomFloat() % 1, FastMath.nextRandomFloat() % 1);
            } else {
                rotAxis.set(1, 0, 0);
            }
        }

        vars.release();
    }
}
