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
import com.jme3.math.Plane;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A planar collision shape based on Bullet's btStaticPlaneShape.
 *
 * @author normenhansen
 */
public class PlaneCollisionShape extends CollisionShape{
    /**
     * description of the plane
     */
    private Plane plane;

    /**
     * No-argument constructor needed by SavableClassUtil. Do not invoke
     * directly!
     */
    protected PlaneCollisionShape() {
    }

    /**
     * Instantiate a plane shape defined by the specified plane.
     *
     * @param plane the desired plane (not null, alias created)
     */
    public PlaneCollisionShape(Plane plane) {
        this.plane = plane;
        createShape();
    }

    /**
     * Access the defining plane.
     *
     * @return the pre-existing instance (not null)
     */
    public final Plane getPlane() {
        return plane;
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
        capsule.write(plane, "collisionPlane", new Plane());
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
        plane = (Plane) capsule.readSavable("collisionPlane", new Plane());
        createShape();
    }

    /**
     * Instantiate the configured shape in Bullet.
     */
    protected void createShape() {
        objectId = createShape(plane.getNormal(), plane.getConstant());
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
//        objectId = new StaticPlaneShape(Converter.convert(plane.getNormal()),plane.getConstant());
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        setScale(scale);
        setMargin(margin);
    }
    
    private native long createShape(Vector3f normal, float constant);

}
