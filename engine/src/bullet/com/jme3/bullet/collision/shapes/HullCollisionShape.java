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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HullCollisionShape extends CollisionShape {

    private float[] points;
//    protected FloatBuffer fbuf;

    public HullCollisionShape() {
    }

    public HullCollisionShape(Mesh mesh) {
        this.points = getPoints(mesh);
        createShape();
    }

    public HullCollisionShape(float[] points) {
        this.points = points;
        createShape();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);

        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(points, "points", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);

        // for backwards compatability
        Mesh mesh = (Mesh) capsule.readSavable("hullMesh", null);
        if (mesh != null) {
            this.points = getPoints(mesh);
        } else {
            this.points = capsule.readFloatArray("points", null);

        }
//        fbuf = ByteBuffer.allocateDirect(points.length * 4).asFloatBuffer();
//        fbuf.put(points);
//        fbuf = FloatBuffer.wrap(points).order(ByteOrder.nativeOrder()).asFloatBuffer();
        createShape();
    }

    protected void createShape() {
//        ObjectArrayList<Vector3f> pointList = new ObjectArrayList<Vector3f>();
//        for (int i = 0; i < points.length; i += 3) {
//            pointList.add(new Vector3f(points[i], points[i + 1], points[i + 2]));
//        }
//        objectId = new ConvexHullShape(pointList);
//        objectId.setLocalScaling(Converter.convert(getScale()));
//        objectId.setMargin(margin);
        ByteBuffer bbuf=BufferUtils.createByteBuffer(points.length * 4); 
//        fbuf = bbuf.asFloatBuffer();
//        fbuf.rewind();
//        fbuf.put(points);
        for (int i = 0; i < points.length; i++) {
            float f = points[i];
            bbuf.putFloat(f);
        }
        bbuf.rewind();
        objectId = createShape(bbuf);
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(ByteBuffer points);

    protected float[] getPoints(Mesh mesh) {
        FloatBuffer vertices = mesh.getFloatBuffer(Type.Position);
        vertices.rewind();
        int components = mesh.getVertexCount() * 3;
        float[] pointsArray = new float[components];
        for (int i = 0; i < components; i += 3) {
            pointsArray[i] = vertices.get();
            pointsArray[i + 1] = vertices.get();
            pointsArray[i + 2] = vertices.get();
        }
        return pointsArray;
    }
}
