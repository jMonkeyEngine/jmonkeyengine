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
package com.jme3.bullet.collision.shapes;

import com.bulletphysics.collision.shapes.BU_Simplex1to4;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;

/**
 * A simple point, line, triangle or quad collisionShape based on one to four points-
 * @author normenhansen
 */
public class SimplexCollisionShape extends CollisionShape {

    private Vector3f vector1, vector2, vector3, vector4;

    public SimplexCollisionShape() {
    }

    public SimplexCollisionShape(Vector3f point1, Vector3f point2, Vector3f point3, Vector3f point4) {
        vector1 = point1;
        vector2 = point2;
        vector3 = point3;
        vector4 = point4;
        createShape();
    }

    public SimplexCollisionShape(Vector3f point1, Vector3f point2, Vector3f point3) {
        vector1 = point1;
        vector2 = point2;
        vector3 = point3;
        createShape();
    }

    public SimplexCollisionShape(Vector3f point1, Vector3f point2) {
        vector1 = point1;
        vector2 = point2;
        createShape();
    }

    public SimplexCollisionShape(Vector3f point1) {
        vector1 = point1;
        createShape();
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(vector1, "simplexPoint1", null);
        capsule.write(vector2, "simplexPoint2", null);
        capsule.write(vector3, "simplexPoint3", null);
        capsule.write(vector4, "simplexPoint4", null);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        vector1 = (Vector3f) capsule.readSavable("simplexPoint1", null);
        vector2 = (Vector3f) capsule.readSavable("simplexPoint2", null);
        vector3 = (Vector3f) capsule.readSavable("simplexPoint3", null);
        vector4 = (Vector3f) capsule.readSavable("simplexPoint4", null);
        createShape();
    }

    protected void createShape() {
        if (vector4 != null) {
            cShape = new BU_Simplex1to4(Converter.convert(vector1), Converter.convert(vector2), Converter.convert(vector3), Converter.convert(vector4));
        } else if (vector3 != null) {
            cShape = new BU_Simplex1to4(Converter.convert(vector1), Converter.convert(vector2), Converter.convert(vector3));
        } else if (vector2 != null) {
            cShape = new BU_Simplex1to4(Converter.convert(vector1), Converter.convert(vector2));
        } else {
            cShape = new BU_Simplex1to4(Converter.convert(vector1));
        }
        cShape.setLocalScaling(Converter.convert(getScale()));
        cShape.setMargin(margin);
    }
}
