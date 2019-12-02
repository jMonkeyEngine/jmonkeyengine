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
 * A cylindrical collision shape based on Bullet's btCylinderShapeX, new
 * btCylinderShape, or btCylinderShapeZ.
 *
 * @author normenhansen
 */
public class CylinderCollisionShape extends CollisionShape {

    /**
     * copy of half-extents of the cylinder on each local axis (not null, no
     * negative component)
     */
    protected Vector3f halfExtents;
    /**
     * copy of main (height) axis (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     */
    protected int axis;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected CylinderCollisionShape() {
    }

    /**
     * Instantiate a Z-axis cylinder shape with the specified half extents.
     *
     * @param halfExtents the desired unscaled half extents (not null, no
     * negative component, alias created)
     */
    public CylinderCollisionShape(Vector3f halfExtents) {
        this.halfExtents = halfExtents;
        this.axis = 2;
        createShape();
    }

    /**
     * Instantiate a cylinder shape around the specified axis.
     *
     * @param halfExtents the desired unscaled half extents (not null, no 
     * negative component, alias created)
     * @param axis which local axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public CylinderCollisionShape(Vector3f halfExtents, int axis) {
        this.halfExtents = halfExtents;
        this.axis = axis;
        createShape();
    }

    /**
     * Access the half extents of the cylinder.
     *
     * @return the pre-existing vector (not null, no negative component)
     */
    public final Vector3f getHalfExtents() {
        return halfExtents;
    }

    /**
     * Determine the main axis of the cylinder.
     *
     * @return 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public int getAxis() {
        return axis;
    }

    /**
     * Alter the scaling factors of this shape. Scaling is disabled
     * for cylinder shapes.
     *
     * @param scale the desired scaling factor for each local axis (not null, no
     * negative component, unaffected, default=1,1,1)
     */
    @Override
    public void setScale(Vector3f scale) {
    	 if (!scale.equals(Vector3f.UNIT_XYZ)) {
    		 Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "CylinderCollisionShape cannot be scaled");
    	 }
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
        capsule.write(halfExtents, "halfExtents", new Vector3f(0.5f, 0.5f, 0.5f));
        capsule.write(axis, "axis", 1);
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
        halfExtents = (Vector3f) capsule.readSavable("halfExtents", new Vector3f(0.5f, 0.5f, 0.5f));
        axis = capsule.readInt("axis", 1);
        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    protected void createShape() {
        objectId = createShape(axis, halfExtents);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
//        switch (axis) {
//            case 0:
//                objectId = new CylinderShapeX(Converter.convert(halfExtents));
//                break;
//            case 1:
//                objectId = new CylinderShape(Converter.convert(halfExtents));
//                break;
//            case 2:
//                objectId = new CylinderShapeZ(Converter.convert(halfExtents));
//                break;
//        }
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        setScale(scale);
        setMargin(margin);
    }
    
    private native long createShape(int axis, Vector3f halfExtents);

}
