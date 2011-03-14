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
package com.jme3.bullet.collision.shapes;

import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CapsuleShapeX;
import com.bulletphysics.collision.shapes.CapsuleShapeZ;
import com.jme3.bullet.util.Converter;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 * Basic capsule collision shape
 * @author normenhansen
 */
public class CapsuleCollisionShape extends CollisionShape{
    protected float radius,height;
    protected int axis;

    public CapsuleCollisionShape() {
    }

    /**
     * creates a new CapsuleCollisionShape with the given radius and height
     * @param radius the radius of the capsule
     * @param height the height of the capsule
     */
    public CapsuleCollisionShape(float radius, float height) {
        this.radius=radius;
        this.height=height;
        this.axis=1;
        CapsuleShape capShape=new CapsuleShape(radius,height);
        cShape=capShape;
    }

    /**
     * creates a capsule shape around the given axis (0=X,1=Y,2=Z)
     * @param radius
     * @param height
     * @param axis
     */
    public CapsuleCollisionShape(float radius, float height, int axis) {
        this.radius=radius;
        this.height=height;
        this.axis=axis;
        createShape();
    }

    public float getRadius() {
        return radius;
    }

    public float getHeight() {
        return height;
    }

    public int getAxis() {
        return axis;
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(radius, "radius", 0.5f);
        capsule.write(height, "height", 1);
        capsule.write(axis, "axis", 1);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        radius = capsule.readFloat("radius", 0.5f);
        height = capsule.readFloat("height", 0.5f);
        axis = capsule.readInt("axis", 1);
        createShape();
    }

    protected void createShape(){
        switch(axis){
            case 0:
                cShape=new CapsuleShapeX(radius,height);
            break;
            case 1:
                cShape=new CapsuleShape(radius,height);
            break;
            case 2:
                cShape=new CapsuleShapeZ(radius,height);
            break;
        }
        cShape.setLocalScaling(Converter.convert(getScale()));
        cShape.setMargin(margin);
    }

}
