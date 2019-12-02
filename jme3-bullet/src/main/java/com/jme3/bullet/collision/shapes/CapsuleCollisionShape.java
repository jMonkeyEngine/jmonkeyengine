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
 * A capsule collision shape based on Bullet's btCapsuleShapeX, btCapsuleShape,
 * or btCapsuleShapeZ. These shapes have no margin and cannot be scaled.
 *
 * @author normenhansen
 */
public class CapsuleCollisionShape extends CollisionShape{
    /**
     * copy of height of the cylindrical portion (&ge;0)
     */
    private float height;
    /**
     * copy of radius (&ge;0)
     */
    private float radius;
    /**
     * copy of main (height) axis (0&rarr;X, 1&rarr;Y, 2&rarr;Z)
     */
    private int axis;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected CapsuleCollisionShape() {
    }

    /**
     * Instantiate a Y-axis capsule shape with the specified radius and height.
     *
     * @param radius the desired radius (&ge;0)
     * @param height the desired height (of the cylindrical portion) (&ge;0)
     */
    public CapsuleCollisionShape(float radius, float height) {
        this.radius=radius;
        this.height=height;
        this.axis=1;
        createShape();
    }

    /**
     * Instantiate a capsule shape around the specified main (height) axis.
     *
     * @param radius the desired radius (&ge;0)
     * @param height the desired height (of the cylindrical portion) (&ge;0)
     * @param axis which local axis: 0&rarr;X, 1&rarr;Y, 2&rarr;Z
     */
    public CapsuleCollisionShape(float radius, float height, int axis) {
        this.radius=radius;
        this.height=height;
        this.axis=axis;
        createShape();
    }

    /**
     * Read the radius of the capsule.
     *
     * @return the radius (&ge;0)
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Read the height (of the cylindrical portion) of the capsule.
     *
     * @return height (&ge;0)
     */
    public float getHeight() {
        return height;
    }

    /**
     * Determine the main (height) axis of the capsule.
     *
     * @return 0 for local X, 1 for local Y, or 2 for local Z
     */
    public int getAxis() {
        return axis;
    }

    /**
     * Alter the scaling factors of this shape. Scaling is disabled
     * for capsule shapes.
     *
     * @param scale the desired scaling factor for each local axis (not null, no
     * negative component, unaffected, default=1,1,1)
     */
    @Override
    public void setScale(Vector3f scale) {
        if (!scale.equals(Vector3f.UNIT_XYZ)) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "CapsuleCollisionShape cannot be scaled");
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
        capsule.write(radius, "radius", 0.5f);
        capsule.write(height, "height", 1);
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
        radius = capsule.readFloat("radius", 0.5f);
        height = capsule.readFloat("height", 0.5f);
        axis = capsule.readInt("axis", 1);
        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    protected void createShape(){
        objectId = createShape(axis, radius, height);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
//        switch(axis){
//            case 0:
//                objectId=new CapsuleShapeX(radius,height);
//            break;
//            case 1:
//                objectId=new CapsuleShape(radius,height);
//            break;
//            case 2:
//                objectId=new CapsuleShapeZ(radius,height);
//            break;
//        }
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
    }
    
    private native long createShape(int axis, float radius, float height);

}
