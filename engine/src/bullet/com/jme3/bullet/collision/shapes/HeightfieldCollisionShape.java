/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.bullet.collision.shapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.util.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses Bullet Physics Heightfield terrain collision system. This is MUCH faster
 * than using a regular mesh.
 * There are a couple tricks though:
 *	-No rotation or translation is supported.
 *	-The collision bbox must be centered around 0,0,0 with the height above and below the y-axis being
 *	equal on either side. If not, the whole collision box is shifted vertically and things don't collide
 *	as they should.
 * 
 * @author Brent Owens
 */
public class HeightfieldCollisionShape extends CollisionShape {

    protected int heightStickWidth;
    protected int heightStickLength;
    protected float[] heightfieldData;
    protected float heightScale;
    protected float minHeight;
    protected float maxHeight;
    protected int upAxis;
    protected boolean flipQuadEdges;
    protected ByteBuffer bbuf;
//    protected FloatBuffer fbuf;

    public HeightfieldCollisionShape() {
    }

    public HeightfieldCollisionShape(float[] heightmap) {
        createCollisionHeightfield(heightmap, Vector3f.UNIT_XYZ);
    }

    public HeightfieldCollisionShape(float[] heightmap, Vector3f scale) {
        createCollisionHeightfield(heightmap, scale);
    }

    protected void createCollisionHeightfield(float[] heightmap, Vector3f worldScale) {
        this.scale = worldScale;
        this.heightScale = 1;//don't change away from 1, we use worldScale instead to scale

        this.heightfieldData = heightmap;

        float min = heightfieldData[0];
        float max = heightfieldData[0];
        // calculate min and max height
        for (int i = 0; i < heightfieldData.length; i++) {
            if (heightfieldData[i] < min) {
                min = heightfieldData[i];
            }
            if (heightfieldData[i] > max) {
                max = heightfieldData[i];
            }
        }
        // we need to center the terrain collision box at 0,0,0 for BulletPhysics. And to do that we need to set the
        // min and max height to be equal on either side of the y axis, otherwise it gets shifted and collision is incorrect.
        if (max < 0) {
            max = -min;
        } else {
            if (Math.abs(max) > Math.abs(min)) {
                min = -max;
            } else {
                max = -min;
            }
        }
        this.minHeight = min;
        this.maxHeight = max;

        this.upAxis = 1;
        this.flipQuadEdges = false;

        heightStickWidth = (int) FastMath.sqrt(heightfieldData.length);
        heightStickLength = heightStickWidth;


        createShape();
    }

    protected void createShape() {
        bbuf = BufferUtils.createByteBuffer(heightfieldData.length * 4); 
//        fbuf = bbuf.asFloatBuffer();//FloatBuffer.wrap(heightfieldData);
//        fbuf.rewind();
//        fbuf.put(heightfieldData);
        for (int i = 0; i < heightfieldData.length; i++) {
            float f = heightfieldData[i];
            bbuf.putFloat(f);
        }
//        fbuf.rewind();
        objectId = createShape(heightStickWidth, heightStickLength, bbuf, heightScale, minHeight, maxHeight, upAxis, flipQuadEdges);
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Created Shape {0}", Long.toHexString(objectId));
        setScale(scale);
        setMargin(margin);
    }

    private native long createShape(int heightStickWidth, int heightStickLength, ByteBuffer heightfieldData, float heightScale, float minHeight, float maxHeight, int upAxis, boolean flipQuadEdges);

    public Mesh createJmeMesh() {
        //TODO return Converter.convert(bulletMesh);
        return null;
    }

    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(heightStickWidth, "heightStickWidth", 0);
        capsule.write(heightStickLength, "heightStickLength", 0);
        capsule.write(heightScale, "heightScale", 0);
        capsule.write(minHeight, "minHeight", 0);
        capsule.write(maxHeight, "maxHeight", 0);
        capsule.write(upAxis, "upAxis", 1);
        capsule.write(heightfieldData, "heightfieldData", new float[0]);
        capsule.write(flipQuadEdges, "flipQuadEdges", false);
    }

    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule capsule = im.getCapsule(this);
        heightStickWidth = capsule.readInt("heightStickWidth", 0);
        heightStickLength = capsule.readInt("heightStickLength", 0);
        heightScale = capsule.readFloat("heightScale", 0);
        minHeight = capsule.readFloat("minHeight", 0);
        maxHeight = capsule.readFloat("maxHeight", 0);
        upAxis = capsule.readInt("upAxis", 1);
        heightfieldData = capsule.readFloatArray("heightfieldData", new float[0]);
        flipQuadEdges = capsule.readBoolean("flipQuadEdges", false);
        createShape();
    }
}
