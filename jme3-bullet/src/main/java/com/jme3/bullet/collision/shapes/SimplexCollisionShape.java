/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple point, line-segment, triangle, or tetrahedron collision shape based
 * on Bullet's btBU_Simplex1to4.
 *
 * @author normenhansen
 */
public class SimplexCollisionShape extends CollisionShape {

    /**
     * vertex positions
     */
    private Vector3f vector1, vector2, vector3, vector4;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected SimplexCollisionShape() {
    }

    /**
     * Instantiate a tetrahedral collision shape based on the specified points.
     *
     * @param point1 the coordinates of 1st point (not null, alias created)
     * @param point2 the coordinates of 2nd point (not null, alias created)
     * @param point3 the coordinates of 3rd point (not null, alias created)
     * @param point4 the coordinates of 4th point (not null, alias created)
     */
    public SimplexCollisionShape(Vector3f point1, Vector3f point2, Vector3f point3, Vector3f point4) {
        vector1 = point1;
        vector2 = point2;
        vector3 = point3;
        vector4 = point4;
        createShape();
    }

    /**
     * Instantiate a triangular collision shape based on the specified points.
     *
     * @param point1 the coordinates of 1st point (not null, alias created)
     * @param point2 the coordinates of 2nd point (not null, alias created)
     * @param point3 the coordinates of 3rd point (not null, alias created)
     */
    public SimplexCollisionShape(Vector3f point1, Vector3f point2, Vector3f point3) {
        vector1 = point1;
        vector2 = point2;
        vector3 = point3;
        createShape();
    }

    /**
     * Instantiate a line-segment collision shape based on the specified points.
     *
     * @param point1 the coordinates of 1st point (not null, alias created)
     * @param point2 the coordinates of 2nd point (not null, alias created)
     */
    public SimplexCollisionShape(Vector3f point1, Vector3f point2) {
        vector1 = point1;
        vector2 = point2;
        createShape();
    }

    /**
     * Instantiate a point collision shape based on the specified points.
     *
     * @param point1 the coordinates of point (not null, alias created)
     */
    public SimplexCollisionShape(Vector3f point1) {
        vector1 = point1;
        createShape();
    }

    /**
     * Serialize this shape, for example when saving to a J3O file.
     *
     * @param ex exporter (not null)
     * @throws IOException from exporter
     */
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(vector1, "simplexPoint1", null);
        capsule.write(vector2, "simplexPoint2", null);
        capsule.write(vector3, "simplexPoint3", null);
        capsule.write(vector4, "simplexPoint4", null);
    }

    /**
     * De-serialize this shape, for example when loading from a J3O file.
     *
     * @param im importer (not null)
     * @throws IOException from importer
     */
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        vector1 = (Vector3f) capsule.readSavable("simplexPoint1", null);
        vector2 = (Vector3f) capsule.readSavable("simplexPoint2", null);
        vector3 = (Vector3f) capsule.readSavable("simplexPoint3", null);
        vector4 = (Vector3f) capsule.readSavable("simplexPoint4", null);
        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    protected void createShape() {
        if (vector4 != null) {
            objectId = createShape(vector1, vector2, vector3, vector4);
//            objectId = new BU_Simplex1to4(Converter.convert(vector1), Converter.convert(vector2), Converter.convert(vector3), Converter.convert(vector4));
        } else if (vector3 != null) {
            objectId = createShape(vector1, vector2, vector3);
//            objectId = new BU_Simplex1to4(Converter.convert(vector1), Converter.convert(vector2), Converter.convert(vector3));
        } else if (vector2 != null) {
            objectId = createShape(vector1, vector2);
//            objectId = new BU_Simplex1to4(Converter.convert(vector1), Converter.convert(vector2));
        } else {
            objectId = createShape(vector1);
//            objectId = new BU_Simplex1to4(Converter.convert(vector1));
        }
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        setScale(scale);
        setMargin(margin);
    }
    
    private native long createShape(Vector3f vector1);
    
    private native long createShape(Vector3f vector1, Vector3f vector2);
    
    private native long createShape(Vector3f vector1, Vector3f vector2, Vector3f vector3);

    private native long createShape(Vector3f vector1, Vector3f vector2, Vector3f vector3, Vector3f vector4);
}
