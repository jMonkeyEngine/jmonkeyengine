/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.light;

import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.control.LightControl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the world-space rotation of a {@link LightProbe} and its synchronization through {@link LightControl}.
 */
public class LightProbeRotationTest {

    private static final float DELTA = 1e-5f;

    @Test
    public void newProbeIsUnrotated() {
        LightProbe probe = new LightProbe();

        assertRotationsEqual(Quaternion.IDENTITY, probe.getRotation());
        Assertions.assertEquals(Matrix3f.IDENTITY, probe.getUniformRotationMatrix());
    }

    @Test
    public void setRotationUpdatesUniformMatrix() {
        LightProbe probe = new LightProbe();
        Quaternion rotation = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);

        probe.setRotation(rotation);

        Vector3f probeX = probe.getUniformRotationMatrix().mult(Vector3f.UNIT_X);
        assertVectorsEqual(rotation.mult(Vector3f.UNIT_X), probeX);
    }

    @Test
    public void setRotationDoesNotAffectSphericalArea() {
        LightProbe probe = new LightProbe();
        Matrix3f areaRotation = new Matrix3f();

        probe.setRotation(new Quaternion().fromAngleAxis(1f, Vector3f.UNIT_Y));

        probe.getUniformMatrix().toRotationMatrix(areaRotation);
        Assertions.assertEquals(Matrix3f.IDENTITY, areaRotation);
    }

    @Test
    public void setRotationTurnsOrientedBoxArea() {
        Quaternion rotation = new Quaternion().fromAngleAxis(1f, Vector3f.UNIT_Y);

        LightProbe rotatedThenBoxed = new LightProbe();
        rotatedThenBoxed.setRotation(rotation);
        rotatedThenBoxed.setAreaType(LightProbe.AreaType.OrientedBox);

        LightProbe boxedThenRotated = new LightProbe();
        boxedThenRotated.setAreaType(LightProbe.AreaType.OrientedBox);
        boxedThenRotated.setRotation(rotation);

        assertRotationsEqual(rotation, ((OrientedBoxProbeArea) rotatedThenBoxed.getArea()).getRotation());
        assertRotationsEqual(rotation, ((OrientedBoxProbeArea) boxedThenRotated.getArea()).getRotation());
    }

    @Test
    public void setRotationKeepsBoxRotationFromSetup() {
        LightProbe probe = new LightProbe();
        probe.setAreaType(LightProbe.AreaType.OrientedBox);
        OrientedBoxProbeArea box = (OrientedBoxProbeArea) probe.getArea();
        box.setRotation(yRotation(30));

        probe.setRotation(yRotation(10));
        assertRotationsEqual(yRotation(40), box.getRotation());

        probe.setRotation(yRotation(25));
        assertRotationsEqual(yRotation(55), box.getRotation());

        probe.setRotation(Quaternion.IDENTITY);
        assertRotationsEqual(yRotation(30), box.getRotation());
    }

    @Test
    public void cloneHasIndependentRotation() {
        LightProbe probe = new LightProbe();
        probe.setRotation(new Quaternion().fromAngleAxis(1f, Vector3f.UNIT_Y));

        LightProbe clone = probe.clone();
        clone.setRotation(new Quaternion().fromAngleAxis(2f, Vector3f.UNIT_Y));

        assertRotationsEqual(new Quaternion().fromAngleAxis(1f, Vector3f.UNIT_Y), probe.getRotation());
        assertVectorsEqual(probe.getRotation().mult(Vector3f.UNIT_X), probe.getUniformRotationMatrix().mult(Vector3f.UNIT_X));
    }

    @Test
    public void rotationSurvivesSaveAndLoad() {
        AssetManager assetManager = new DesktopAssetManager(true);
        LightProbe probe = new LightProbe();
        Quaternion rotation = new Quaternion().fromAngleAxis(0.7f, Vector3f.UNIT_Y);
        probe.setRotation(rotation);

        LightProbe loaded = BinaryExporter.saveAndLoad(assetManager, probe);

        assertRotationsEqual(rotation, loaded.getRotation());
        assertVectorsEqual(rotation.mult(Vector3f.UNIT_X), loaded.getUniformRotationMatrix().mult(Vector3f.UNIT_X));
    }

    @Test
    public void lightControlCopiesSpatialTransformToProbe() {
        Node parent = new Node("parent");
        parent.setLocalTranslation(5, 0, 0);
        parent.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
        Node child = new Node("child");
        child.setLocalTranslation(0, 0, 2);
        child.setLocalRotation(new Quaternion().fromAngleAxis(0.3f, Vector3f.UNIT_X));
        parent.attachChild(child);
        parent.updateGeometricState();

        LightProbe probe = new LightProbe();
        child.addControl(new LightControl(probe));
        child.getControl(LightControl.class).update(0);

        assertVectorsEqual(child.getWorldTranslation(), probe.getPosition());
        assertRotationsEqual(child.getWorldRotation(), probe.getRotation());
    }

    @Test
    public void lightControlCopiesProbeTransformToSpatial() {
        Node parent = new Node("parent");
        parent.setLocalTranslation(5, 0, 0);
        parent.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y));
        Node child = new Node("child");
        parent.attachChild(child);
        parent.updateGeometricState();

        LightProbe probe = new LightProbe();
        probe.setPosition(new Vector3f(1, 2, 3));
        Quaternion probeRotation = new Quaternion().fromAngleAxis(0.4f, Vector3f.UNIT_Z);
        probe.setRotation(probeRotation);
        child.addControl(new LightControl(probe, LightControl.ControlDirection.LightToSpatial));
        child.getControl(LightControl.class).update(0);
        parent.updateGeometricState();

        assertVectorsEqual(probe.getPosition(), child.getWorldTranslation());
        assertRotationsEqual(probeRotation, child.getWorldRotation());
    }

    private static Quaternion yRotation(float degrees) {
        return new Quaternion().fromAngleAxis(degrees * FastMath.DEG_TO_RAD, Vector3f.UNIT_Y);
    }

    private static void assertVectorsEqual(Vector3f expected, Vector3f actual) {
        Assertions.assertEquals(expected.x, actual.x, DELTA);
        Assertions.assertEquals(expected.y, actual.y, DELTA);
        Assertions.assertEquals(expected.z, actual.z, DELTA);
    }

    /**
     * Compares orientations by what they do to the axes, so a quaternion and its negation count as equal.
     */
    private static void assertRotationsEqual(Quaternion expected, Quaternion actual) {
        assertVectorsEqual(expected.mult(Vector3f.UNIT_X), actual.mult(Vector3f.UNIT_X));
        assertVectorsEqual(expected.mult(Vector3f.UNIT_Y), actual.mult(Vector3f.UNIT_Y));
    }
}
