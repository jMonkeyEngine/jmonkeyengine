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
package com.jme3.effect.shapes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This emiter shape emits the particles from the given shape's vertices
 * @author Marcin Roguski (Kaelthas)
 */
public class EmitterMeshVertexShape implements EmitterShape {

    protected List<List<Vector3f>> vertices;
    protected List<List<Vector3f>> normals;

    /**
     * Empty constructor. Sets nothing.
     */
    public EmitterMeshVertexShape() {
    }

    /**
     * Constructor. It stores a copy of vertex list of all meshes.
     * @param meshes
     *        a list of meshes that will form the emitter's shape
     */
    public EmitterMeshVertexShape(List<Mesh> meshes) {
        this.setMeshes(meshes);
    }

    /**
     * This method sets the meshes that will form the emiter's shape.
     * @param meshes
     *        a list of meshes that will form the emitter's shape
     */
    public void setMeshes(List<Mesh> meshes) {
        Map<Vector3f, Vector3f> vertToNormalMap = new HashMap<Vector3f, Vector3f>();

        this.vertices = new ArrayList<List<Vector3f>>(meshes.size());
        this.normals = new ArrayList<List<Vector3f>>(meshes.size());
        for (Mesh mesh : meshes) {
            // fetching the data
            float[] vertexTable = BufferUtils.getFloatArray(mesh.getFloatBuffer(Type.Position));
            float[] normalTable = BufferUtils.getFloatArray(mesh.getFloatBuffer(Type.Normal));

            // unifying normals
            for (int i = 0; i < vertexTable.length; i += 3) {// the tables should have the same size and be dividable by 3
                Vector3f vert = new Vector3f(vertexTable[i], vertexTable[i + 1], vertexTable[i + 2]);
                Vector3f norm = vertToNormalMap.get(vert);
                if (norm == null) {
                    norm = new Vector3f(normalTable[i], normalTable[i + 1], normalTable[i + 2]);
                    vertToNormalMap.put(vert, norm);
                } else {
                    norm.addLocal(normalTable[i], normalTable[i + 1], normalTable[i + 2]);
                }
            }

            // adding data to vertices and normals
            List<Vector3f> vertices = new ArrayList<Vector3f>(vertToNormalMap.size());
            List<Vector3f> normals = new ArrayList<Vector3f>(vertToNormalMap.size());
            for (Entry<Vector3f, Vector3f> entry : vertToNormalMap.entrySet()) {
                vertices.add(entry.getKey());
                normals.add(entry.getValue().normalizeLocal());
            }
            this.vertices.add(vertices);
            this.normals.add(normals);
        }
    }

    /**
     * This method fills the point with coordinates of randomly selected mesh vertex.
     * @param store
     *        the variable to store with coordinates of randomly selected mesh vertex
     */
    @Override
    public void getRandomPoint(Vector3f store) {
        int meshIndex = FastMath.nextRandomInt(0, vertices.size() - 1);
        int vertIndex = FastMath.nextRandomInt(0, vertices.get(meshIndex).size() - 1);
        store.set(vertices.get(meshIndex).get(vertIndex));
    }

    /**
     * This method fills the point with coordinates of randomly selected mesh vertex.
     * The normal param is filled with selected vertex's normal.
     * @param store
     *        the variable to store with coordinates of randomly selected mesh vertex
     * @param normal
     *        filled with selected vertex's normal
     */
    @Override
    public void getRandomPointAndNormal(Vector3f store, Vector3f normal) {
        int meshIndex = FastMath.nextRandomInt(0, vertices.size() - 1);
        int vertIndex = FastMath.nextRandomInt(0, vertices.get(meshIndex).size() - 1);
        store.set(vertices.get(meshIndex).get(vertIndex));
        normal.set(normals.get(meshIndex).get(vertIndex));
    }

    @Override
    public EmitterShape deepClone() {
        try {
            EmitterMeshVertexShape clone = (EmitterMeshVertexShape) super.clone();
            if (this.vertices != null) {
                clone.vertices = new ArrayList<List<Vector3f>>(vertices.size());
                for (List<Vector3f> list : vertices) {
                    List<Vector3f> vectorList = new ArrayList<Vector3f>(list.size());
                    for (Vector3f vector : list) {
                        vectorList.add(vector.clone());
                    }
                    clone.vertices.add(vectorList);
                }
            }
            if (this.normals != null) {
                clone.normals = new ArrayList<List<Vector3f>>(normals.size());
                for (List<Vector3f> list : normals) {
                    List<Vector3f> vectorList = new ArrayList<Vector3f>(list.size());
                    for (Vector3f vector : list) {
                        vectorList.add(vector.clone());
                    }
                    clone.normals.add(vectorList);
                }
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public Object jmeClone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        this.vertices = cloner.clone(vertices);
        this.normals = cloner.clone(normals);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.writeSavableArrayList((ArrayList<List<Vector3f>>) vertices, "vertices", null);
        oc.writeSavableArrayList((ArrayList<List<Vector3f>>) normals, "normals", null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        this.vertices = ic.readSavableArrayList("vertices", null);

        List<List<Vector3f>> tmpNormals = ic.readSavableArrayList("normals", null);
        if (tmpNormals != null){
            this.normals = tmpNormals;
        }
    }
}
