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

package jme3tools.converters.model;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.*;

public class FloatToFixed {

    private static final float shortSize = Short.MAX_VALUE - Short.MIN_VALUE;
    private static final float shortOff  = (Short.MAX_VALUE + Short.MIN_VALUE) * 0.5f;

    private static final float byteSize = Byte.MAX_VALUE - Byte.MIN_VALUE;
    private static final float byteOff  = (Byte.MAX_VALUE + Byte.MIN_VALUE) * 0.5f;

    public static void convertToFixed(Geometry geom, Format posFmt, Format nmFmt, Format tcFmt){
        geom.updateModelBound();
        BoundingBox bbox = (BoundingBox) geom.getModelBound();
        Mesh mesh = geom.getMesh();

        VertexBuffer positions = mesh.getBuffer(Type.Position);
        VertexBuffer normals   = mesh.getBuffer(Type.Normal);
        VertexBuffer texcoords = mesh.getBuffer(Type.TexCoord);
        VertexBuffer indices   = mesh.getBuffer(Type.Index);

        // positions
        FloatBuffer fb = (FloatBuffer) positions.getData();
        if (posFmt != Format.Float){
            Buffer newBuf = VertexBuffer.createBuffer(posFmt, positions.getNumComponents(),
                                                      mesh.getVertexCount());
            Transform t = convertPositions(fb, bbox, newBuf);
            t.combineWithParent(geom.getLocalTransform());
            geom.setLocalTransform(t);

            VertexBuffer newPosVb = new VertexBuffer(Type.Position);
            newPosVb.setupData(positions.getUsage(),
                               positions.getNumComponents(),
                               posFmt,
                               newBuf);
            mesh.clearBuffer(Type.Position);
            mesh.setBuffer(newPosVb);
        }

        // normals, automatically convert to signed byte
        fb = (FloatBuffer) normals.getData();

        ByteBuffer bb = BufferUtils.createByteBuffer(fb.capacity());
        convertNormals(fb, bb);

        normals = new VertexBuffer(Type.Normal);
        normals.setupData(Usage.Static, 3, Format.Byte, bb);
        normals.setNormalized(true);
        mesh.clearBuffer(Type.Normal);
        mesh.setBuffer(normals);

        // texcoords
        fb = (FloatBuffer) texcoords.getData();
        if (tcFmt != Format.Float){
            Buffer newBuf = VertexBuffer.createBuffer(tcFmt,
                                                      texcoords.getNumComponents(),
                                                      mesh.getVertexCount());
            convertTexCoords2D(fb, newBuf);

            VertexBuffer newTcVb = new VertexBuffer(Type.TexCoord);
            newTcVb.setupData(texcoords.getUsage(),
                              texcoords.getNumComponents(),
                              tcFmt,
                              newBuf);
            mesh.clearBuffer(Type.TexCoord);
            mesh.setBuffer(newTcVb);
        }
    }

    public static void compressIndexBuffer(Mesh mesh){
        int vertCount = mesh.getVertexCount();
        VertexBuffer vb = mesh.getBuffer(Type.Index);
        Format targetFmt;
        if (vb.getFormat() == Format.UnsignedInt && vertCount <= 0xffff){
            if (vertCount <= 256)
                targetFmt = Format.UnsignedByte;
            else
                targetFmt = Format.UnsignedShort;
        }else if (vb.getFormat() == Format.UnsignedShort && vertCount <= 0xff){
            targetFmt = Format.UnsignedByte;
        }else{
            return;
        }

        IndexBuffer src = mesh.getIndexBuffer();
        Buffer newBuf = VertexBuffer.createBuffer(targetFmt, vb.getNumComponents(), src.size());

        VertexBuffer newVb = new VertexBuffer(Type.Index);
        newVb.setupData(vb.getUsage(), vb.getNumComponents(), targetFmt, newBuf);
        mesh.clearBuffer(Type.Index);
        mesh.setBuffer(newVb);

        IndexBuffer dst = mesh.getIndexBuffer();
        for (int i = 0; i < src.size(); i++){
            dst.put(i, src.get(i));
        }
    }

    private static void convertToFixed(FloatBuffer input, IntBuffer output){
        if (output.capacity() < input.capacity())
            throw new RuntimeException("Output must be at least as large as input!");

        input.clear();
        output.clear();
        for (int i = 0; i < input.capacity(); i++){
            output.put( (int) (input.get() * (float)(1<<16)) );
        }
        output.flip();
    }

    private static void convertToFloat(IntBuffer input, FloatBuffer output){
        if (output.capacity() < input.capacity())
            throw new RuntimeException("Output must be at least as large as input!");

        input.clear();
        output.clear();
        for (int i = 0; i < input.capacity(); i++){
            output.put( ((float)input.get() / (float)(1<<16)) );
        }
        output.flip();
    }

    private static void convertToUByte(FloatBuffer input, ByteBuffer output){
        if (output.capacity() < input.capacity())
            throw new RuntimeException("Output must be at least as large as input!");

        input.clear();
        output.clear();
        for (int i = 0; i < input.capacity(); i++){
            output.put( (byte) (input.get() * 255f) );
        }
        output.flip();
    }


    public static VertexBuffer convertToUByte(VertexBuffer vb){
        FloatBuffer fb = (FloatBuffer) vb.getData();
        ByteBuffer bb = BufferUtils.createByteBuffer(fb.capacity());
        convertToUByte(fb, bb);

        VertexBuffer newVb = new VertexBuffer(vb.getBufferType());
        newVb.setupData(vb.getUsage(),
                        vb.getNumComponents(),
                        Format.UnsignedByte,
                        bb);
        newVb.setNormalized(true);
        return newVb;
    }

    public static VertexBuffer convertToFixed(VertexBuffer vb){
        if (vb.getFormat() == Format.Int)
            return vb;

        FloatBuffer fb = (FloatBuffer) vb.getData();
        IntBuffer ib = BufferUtils.createIntBuffer(fb.capacity());
        convertToFixed(fb, ib);

        VertexBuffer newVb = new VertexBuffer(vb.getBufferType());
        newVb.setupData(vb.getUsage(),
                        vb.getNumComponents(),
                        Format.Int,
                        ib);
        return newVb;
    }

    public static VertexBuffer convertToFloat(VertexBuffer vb){
        if (vb.getFormat() == Format.Float)
            return vb;

        IntBuffer ib = (IntBuffer) vb.getData();
        FloatBuffer fb = BufferUtils.createFloatBuffer(ib.capacity());
        convertToFloat(ib, fb);

        VertexBuffer newVb = new VertexBuffer(vb.getBufferType());
        newVb.setupData(vb.getUsage(),
                        vb.getNumComponents(),
                        Format.Float,
                        fb);
        return newVb;
    }

    private static void convertNormals(FloatBuffer input, ByteBuffer output){
        if (output.capacity() < input.capacity())
            throw new RuntimeException("Output must be at least as large as input!");

        input.clear();
        output.clear();
        Vector3f temp = new Vector3f();
        int vertexCount = input.capacity() / 3;
        for (int i = 0; i < vertexCount; i++){
            BufferUtils.populateFromBuffer(temp, input, i);

            // offset and scale vector into -128 ... 127
            temp.multLocal(127).addLocal(0.5f, 0.5f, 0.5f);

            // quantize
            byte v1 = (byte) temp.getX();
            byte v2 = (byte) temp.getY();
            byte v3 = (byte) temp.getZ();

            // store
            output.put(v1).put(v2).put(v3);
        }
    }

    private static void convertTexCoords2D(FloatBuffer input, Buffer output){
        if (output.capacity() < input.capacity())
            throw new RuntimeException("Output must be at least as large as input!");

        input.clear();
        output.clear();
        Vector2f temp = new Vector2f();
        int vertexCount = input.capacity() / 2;

        ShortBuffer sb = null;
        IntBuffer ib = null;

        if (output instanceof ShortBuffer)
            sb = (ShortBuffer) output;
        else if (output instanceof IntBuffer)
            ib = (IntBuffer) output;
        else
            throw new UnsupportedOperationException();

        for (int i = 0; i < vertexCount; i++){
            BufferUtils.populateFromBuffer(temp, input, i);

            if (sb != null){
                sb.put( (short) (temp.getX()*Short.MAX_VALUE) );
                sb.put( (short) (temp.getY()*Short.MAX_VALUE) );
            }else{
                int v1 = (int) (temp.getX() * ((float)(1 << 16)));
                int v2 = (int) (temp.getY() * ((float)(1 << 16)));
                ib.put(v1).put(v2);
            }
        }
    }

    private static Transform convertPositions(FloatBuffer input, BoundingBox bbox, Buffer output){
        if (output.capacity() < input.capacity())
            throw new RuntimeException("Output must be at least as large as input!");

        Vector3f offset = bbox.getCenter().negate();
        Vector3f size = new Vector3f(bbox.getXExtent(), bbox.getYExtent(), bbox.getZExtent());
        size.multLocal(2);

        ShortBuffer sb = null;
        ByteBuffer bb = null;
        float dataTypeSize;
        float dataTypeOffset;
        if (output instanceof ShortBuffer){
            sb = (ShortBuffer) output;
            dataTypeOffset = shortOff;
            dataTypeSize = shortSize;
        }else{
            bb = (ByteBuffer) output;
            dataTypeOffset = byteOff;
            dataTypeSize = byteSize;
        }
        Vector3f scale = new Vector3f();
        scale.set(dataTypeSize, dataTypeSize, dataTypeSize).divideLocal(size);

        Vector3f invScale = new Vector3f();
        invScale.set(size).divideLocal(dataTypeSize);

        offset.multLocal(scale);
        offset.addLocal(dataTypeOffset, dataTypeOffset, dataTypeOffset);

        // offset = (-modelOffset * shortSize)/modelSize + shortOff
        // scale = shortSize / modelSize

        input.clear();
        output.clear();
        Vector3f temp = new Vector3f();
        int vertexCount = input.capacity() / 3;
        for (int i = 0; i < vertexCount; i++){
            BufferUtils.populateFromBuffer(temp, input, i);

            // offset and scale vector into -32768 ... 32767
            // or into -128 ... 127 if using bytes
            temp.multLocal(scale);
            temp.addLocal(offset);

            // quantize and store
            if (sb != null){
                short v1 = (short) temp.getX();
                short v2 = (short) temp.getY();
                short v3 = (short) temp.getZ();
                sb.put(v1).put(v2).put(v3);
            }else{
                byte v1 = (byte) temp.getX();
                byte v2 = (byte) temp.getY();
                byte v3 = (byte) temp.getZ();
                bb.put(v1).put(v2).put(v3);
            }
        }

        Transform transform = new Transform();
        transform.setTranslation(offset.negate().multLocal(invScale));
        transform.setScale(invScale);
        return transform;
    }

}
