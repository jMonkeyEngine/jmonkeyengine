/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.util.mikktspace;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.FloatBuffer;

/**
 *
 * @author Nehon
 */
public class MikkTSpaceImpl implements MikkTSpaceContext {

    Mesh mesh;

    public MikkTSpaceImpl(Mesh mesh) {
        this.mesh = mesh;
        //replacing any existing tangent buffer, if you came here you want them new.
        mesh.clearBuffer(VertexBuffer.Type.Tangent);
        FloatBuffer fb = BufferUtils.createFloatBuffer(mesh.getVertexCount() * 4);
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4, fb);
    }

    @Override
    public int getNumFaces() {
        return mesh.getTriangleCount();        
    }

    @Override
    public int getNumVerticesOfFace(int face) {
        return 3;
    }

    @Override
    public void getPosition(float[] posOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer position = mesh.getBuffer(VertexBuffer.Type.Position);
        FloatBuffer pos = (FloatBuffer) position.getData();
        pos.position(vertIndex * 3);
        posOut[0] = pos.get();
        posOut[1] = pos.get();
        posOut[2] = pos.get();
    }

    @Override
    public void getNormal(float[] normOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer normal = mesh.getBuffer(VertexBuffer.Type.Normal);
        FloatBuffer norm = (FloatBuffer) normal.getData();
        norm.position(vertIndex * 3);
        normOut[0] = norm.get();
        normOut[1] = norm.get();
        normOut[2] = norm.get();
    }

    @Override
    public void getTexCoord(float[] texOut, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer texCoord = mesh.getBuffer(VertexBuffer.Type.TexCoord);
        FloatBuffer tex = (FloatBuffer) texCoord.getData();
        tex.position(vertIndex * 2);
        texOut[0] = tex.get();
        texOut[1] = tex.get();        
    }

    @Override
    public void setTSpaceBasic(float[] tangent, float sign, int face, int vert) {
        int vertIndex = getIndex(face, vert);
        VertexBuffer tangentBuffer = mesh.getBuffer(VertexBuffer.Type.Tangent);
        FloatBuffer tan = (FloatBuffer) tangentBuffer.getData();
        
        tan.position(vertIndex * 4);
        tan.put(tangent);
        tan.put(sign);
        
        tan.rewind();
        tangentBuffer.setUpdateNeeded();
    }

    @Override
    public void setTSpace(float[] tangent, float[] biTangent, float magS, float magT, boolean isOrientationPreserving, int face, int vert) {
        //Do nothing
    }

    private int getIndex(int face, int vert) {
        IndexBuffer index = mesh.getIndexBuffer();
        int vertIndex = index.get(face * 3 + vert);
        return vertIndex;
    }

}
