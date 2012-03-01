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
package com.jme3.scene;

import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BatchNode holds geometrie that are batched version of all geometries that are in its sub scenegraph.
 * There is one geometry per different material in the sub tree.
 * this geometries are directly attached to the node in the scene graph.
 * usage is like any other node except you have to call the {@link #batch()} method once all geoms have been attached to the sub scene graph and theire material set
 * (see todo more automagic for further enhancements)
 * all the geometry that have been batched are set to {@link CullHint#Always} to not render them.
 * the sub geometries can be transformed as usual their transforms are used to update the mesh of the geometryBatch.
 * sub geoms can be removed but it may be slower than the normal spatial removing
 * Sub geoms can be added after the batch() method has been called but won't be batched and will be rendered as normal geometries.
 * To integrate them in the batch you have to call the batch() method again on the batchNode.
 * 
 * TODO normal or tangents or both looks a bit weird
 * TODO more automagic (batch when needed in the updateLigicalState)
 * @author Nehon
 */
public class BatchNode extends Node implements Savable {

    private static final Logger logger = Logger.getLogger(BatchNode.class.getName());
    /**
     * the map of geometry holding the batched meshes
     */
    protected Map<Material, Batch> batches = new HashMap<Material, Batch>();
    /**
     * used to store transformed vectors before proceeding to a bulk put into the FloatBuffer 
     */
    private float[] tmpFloat;
    private float[] tmpFloatN;
    private float[] tmpFloatT;
    int maxVertCount = 0;
    boolean useTangents = false;
    boolean needsFullRebatch = true;

    /**
     * Construct a batchNode
     */
    public BatchNode() {
        super();
    }

    public BatchNode(String name) {
        super(name);
    }

    @Override
    public void updateGeometricState() {
        if ((refreshFlags & RF_LIGHTLIST) != 0) {
            updateWorldLightList();
        }

        if ((refreshFlags & RF_TRANSFORM) != 0) {
            // combine with parent transforms- same for all spatial
            // subclasses.
            updateWorldTransforms();
        }

        if (!children.isEmpty()) {
            // the important part- make sure child geometric state is refreshed
            // first before updating own world bound. This saves
            // a round-trip later on.
            // NOTE 9/19/09
            // Although it does save a round trip,

            for (Spatial child : children.getArray()) {
                child.updateGeometricState();
            }

            for (Batch batch : batches.values()) {
                if (batch.needMeshUpdate) {
                    batch.geometry.getMesh().updateBound();
                    batch.geometry.updateWorldBound();
                    batch.needMeshUpdate = false;

                }
            }


        }

        if ((refreshFlags & RF_BOUND) != 0) {
            updateWorldBound();
        }

        assert refreshFlags == 0;
    }

    protected Transform getTransforms(Geometry geom) {
        return geom.getWorldTransform();
    }

    protected void updateSubBatch(Geometry bg) {
        Batch batch = batches.get(bg.getMaterial());
        if (batch != null) {
            Mesh mesh = batch.geometry.getMesh();

            VertexBuffer pvb = mesh.getBuffer(VertexBuffer.Type.Position);
            FloatBuffer posBuf = (FloatBuffer) pvb.getData();
            VertexBuffer nvb = mesh.getBuffer(VertexBuffer.Type.Normal);
            FloatBuffer normBuf = (FloatBuffer) nvb.getData();

            if (mesh.getBuffer(VertexBuffer.Type.Tangent) != null) {

                VertexBuffer tvb = mesh.getBuffer(VertexBuffer.Type.Tangent);
                FloatBuffer tanBuf = (FloatBuffer) tvb.getData();
                doTransformsTangents(posBuf, normBuf, tanBuf, bg.startIndex, bg.startIndex + bg.getVertexCount(), bg.cachedOffsetMat);
                tvb.updateData(tanBuf);
            } else {
                doTransforms(posBuf, normBuf, bg.startIndex, bg.startIndex + bg.getVertexCount(), bg.cachedOffsetMat);
            }
            pvb.updateData(posBuf);
            nvb.updateData(normBuf);


            batch.needMeshUpdate = true;
        }
    }

    /**
     * Batch this batchNode
     * every geometry of the sub scene graph of this node will be batched into a single mesh that will be rendered in one call
     */
    public void batch() {
        doBatch();
        //we set the batch geometries to ignore transforms to avoid transforms of parent nodes to be applied twice        
        for (Batch batch : batches.values()) {
            batch.geometry.setIgnoreTransform(true);
        }
    }

    protected void doBatch() {
        Map<Material, List<Geometry>> matMap = new HashMap<Material, List<Geometry>>();
        maxVertCount = 0;
        int nbGeoms = 0;

        gatherGeomerties(matMap, this, needsFullRebatch);
        if (needsFullRebatch) {
            for (Batch batch : batches.values()) {
                batch.geometry.removeFromParent();
            }
            batches.clear();
        }
        for (Map.Entry<Material, List<Geometry>> entry : matMap.entrySet()) {
            Mesh m = new Mesh();
            Material material = entry.getKey();
            List<Geometry> list = entry.getValue();
            nbGeoms += list.size();
            if (!needsFullRebatch) {
                list.add(batches.get(material).geometry);
            }
            mergeGeometries(m, list);
            m.setDynamic();
            Batch batch = new Batch();

            batch.geometry = new Geometry(name + "-batch" + batches.size());
            batch.geometry.setMaterial(material);
            this.attachChild(batch.geometry);


            batch.geometry.setMesh(m);
            batch.geometry.getMesh().updateCounts();
            batch.geometry.getMesh().updateBound();
            batches.put(material, batch);
        }

        logger.log(Level.INFO, "Batched {0} geometries in {1} batches.", new Object[]{nbGeoms, batches.size()});


        //init temp float arrays
        tmpFloat = new float[maxVertCount * 3];
        tmpFloatN = new float[maxVertCount * 3];
        if (useTangents) {
            tmpFloatT = new float[maxVertCount * 4];
        }
    }

    private void gatherGeomerties(Map<Material, List<Geometry>> map, Spatial n, boolean rebatch) {

        if (n.getClass() == Geometry.class) {

            if (!isBatch(n) && n.getBatchHint() != BatchHint.Never) {
                Geometry g = (Geometry) n;
                if (!g.isBatched() || rebatch) {
                    if (g.getMaterial() == null) {
                        throw new IllegalStateException("No material is set for Geometry: " + g.getName() + " please set a material before batching");
                    }
                    List<Geometry> list = map.get(g.getMaterial());
                    if (list == null) {
                        list = new ArrayList<Geometry>();
                        map.put(g.getMaterial(), list);
                    }
                    g.setTransformRefresh();
                    list.add(g);
                }
            }

        } else if (n instanceof Node) {
            for (Spatial child : ((Node) n).getChildren()) {
                if (child instanceof BatchNode) {
                    continue;
                }
                gatherGeomerties(map, child, rebatch);
            }
        }

    }

    private boolean isBatch(Spatial s) {
        for (Batch batch : batches.values()) {
            if (batch.geometry == s) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the material to the all the batches of this BatchNode
     * use setMaterial(Material material,int batchIndex) to set a material to a specific batch
     * 
     * @param material the material to use for this geometry
     */
    @Override
    public void setMaterial(Material material) {
//        for (Batch batch : batches.values()) {
//            batch.geometry.setMaterial(material);
//        }
        throw new UnsupportedOperationException("Unsupported for now, please set the material on the geoms before batching");
    }

    /**
     * Returns the material that is used for the first batch of this BatchNode
     * 
     * use getMaterial(Material material,int batchIndex) to get a material from a specific batch
     * 
     * @return the material that is used for the first batch of this BatchNode
     * 
     * @see #setMaterial(com.jme3.material.Material) 
     */
    public Material getMaterial() {
        if (!batches.isEmpty()) {
            Batch b = batches.get(batches.keySet().iterator().next());
            return b.geometry.getMaterial();
        }
        return null;//material;
    }

//    /**
//     * Sets the material to the a specific batch of this BatchNode
//     * 
//     * 
//     * @param material the material to use for this geometry
//     */   
//    public void setMaterial(Material material,int batchIndex) {
//        if (!batches.isEmpty()) {
//            
//        }
//        
//    }
//
//    /**
//     * Returns the material that is used for the first batch of this BatchNode
//     * 
//     * use getMaterial(Material material,int batchIndex) to get a material from a specific batch
//     * 
//     * @return the material that is used for the first batch of this BatchNode
//     * 
//     * @see #setMaterial(com.jme3.material.Material) 
//     */
//    public Material getMaterial(int batchIndex) {
//        if (!batches.isEmpty()) {
//            Batch b = batches.get(batches.keySet().iterator().next());
//            return b.geometry.getMaterial();
//        }
//        return null;//material;
//    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
//
//        if (material != null) {
//            oc.write(material.getAssetName(), "materialName", null);
//        }
//        oc.write(material, "material", null);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);


//        material = null;
//        String matName = ic.readString("materialName", null);
//        if (matName != null) {
//            // Material name is set,
//            // Attempt to load material via J3M
//            try {
//                material = im.getAssetManager().loadMaterial(matName);
//            } catch (AssetNotFoundException ex) {
//                // Cannot find J3M file.
//                logger.log(Level.FINE, "Could not load J3M file {0} for Geometry.",
//                        matName);
//            }
//        }
//        // If material is NULL, try to load it from the geometry
//        if (material == null) {
//            material = (Material) ic.readSavable("material", null);
//        }

    }

    /**
     * Merges all geometries in the collection into
     * the output mesh. Does not take into account materials.
     * 
     * @param geometries
     * @param outMesh
     */
    private void mergeGeometries(Mesh outMesh, List<Geometry> geometries) {
        int[] compsForBuf = new int[VertexBuffer.Type.values().length];
        VertexBuffer.Format[] formatForBuf = new VertexBuffer.Format[compsForBuf.length];

        int totalVerts = 0;
        int totalTris = 0;
        int totalLodLevels = 0;

        Mesh.Mode mode = null;
        for (Geometry geom : geometries) {
            totalVerts += geom.getVertexCount();
            totalTris += geom.getTriangleCount();
            totalLodLevels = Math.min(totalLodLevels, geom.getMesh().getNumLodLevels());
            if (maxVertCount < geom.getVertexCount()) {
                maxVertCount = geom.getVertexCount();
            }
            Mesh.Mode listMode;
            int components;
            switch (geom.getMesh().getMode()) {
                case Points:
                    listMode = Mesh.Mode.Points;
                    components = 1;
                    break;
                case LineLoop:
                case LineStrip:
                case Lines:
                    listMode = Mesh.Mode.Lines;
                    components = 2;
                    break;
                case TriangleFan:
                case TriangleStrip:
                case Triangles:
                    listMode = Mesh.Mode.Triangles;
                    components = 3;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            for (VertexBuffer vb : geom.getMesh().getBufferList().getArray()) {
                compsForBuf[vb.getBufferType().ordinal()] = vb.getNumComponents();
                formatForBuf[vb.getBufferType().ordinal()] = vb.getFormat();
            }

            if (mode != null && mode != listMode) {
                throw new UnsupportedOperationException("Cannot combine different"
                        + " primitive types: " + mode + " != " + listMode);
            }
            mode = listMode;
            compsForBuf[VertexBuffer.Type.Index.ordinal()] = components;
        }

        outMesh.setMode(mode);
        if (totalVerts >= 65536) {
            // make sure we create an UnsignedInt buffer so
            // we can fit all of the meshes
            formatForBuf[VertexBuffer.Type.Index.ordinal()] = VertexBuffer.Format.UnsignedInt;
        } else {
            formatForBuf[VertexBuffer.Type.Index.ordinal()] = VertexBuffer.Format.UnsignedShort;
        }

        // generate output buffers based on retrieved info
        for (int i = 0; i < compsForBuf.length; i++) {
            if (compsForBuf[i] == 0) {
                continue;
            }

            Buffer data;
            if (i == VertexBuffer.Type.Index.ordinal()) {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalTris);
            } else {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalVerts);
            }

            VertexBuffer vb = new VertexBuffer(VertexBuffer.Type.values()[i]);
            vb.setupData(VertexBuffer.Usage.Dynamic, compsForBuf[i], formatForBuf[i], data);
            outMesh.setBuffer(vb);
        }

        int globalVertIndex = 0;
        int globalTriIndex = 0;

        for (Geometry geom : geometries) {
            Mesh inMesh = geom.getMesh();
            geom.batch(this, globalVertIndex);

            int geomVertCount = inMesh.getVertexCount();
            int geomTriCount = inMesh.getTriangleCount();

            for (int bufType = 0; bufType < compsForBuf.length; bufType++) {
                VertexBuffer inBuf = inMesh.getBuffer(VertexBuffer.Type.values()[bufType]);

                VertexBuffer outBuf = outMesh.getBuffer(VertexBuffer.Type.values()[bufType]);

                if (outBuf == null) {
                    continue;
                }

                if (VertexBuffer.Type.Index.ordinal() == bufType) {
                    int components = compsForBuf[bufType];

                    IndexBuffer inIdx = inMesh.getIndicesAsList();
                    IndexBuffer outIdx = outMesh.getIndexBuffer();

                    for (int tri = 0; tri < geomTriCount; tri++) {
                        for (int comp = 0; comp < components; comp++) {
                            int idx = inIdx.get(tri * components + comp) + globalVertIndex;
                            outIdx.put((globalTriIndex + tri) * components + comp, idx);
                        }
                    }
                } else if (VertexBuffer.Type.Position.ordinal() == bufType) {
                    FloatBuffer inPos = (FloatBuffer) inBuf.getData();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();                    
                    doCopyBuffer(inPos, globalVertIndex, outPos, 3);
                } else if (VertexBuffer.Type.Normal.ordinal() == bufType || VertexBuffer.Type.Tangent.ordinal() == bufType) {
                    FloatBuffer inPos = (FloatBuffer) inBuf.getData();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    doCopyBuffer(inPos, globalVertIndex, outPos, compsForBuf[bufType]);
                    if (VertexBuffer.Type.Tangent.ordinal() == bufType) {
                        useTangents = true;
                    }
                } else {
                    inBuf.copyElements(0, outBuf, globalVertIndex, geomVertCount);
//                    for (int vert = 0; vert < geomVertCount; vert++) {
//                        int curGlobalVertIndex = globalVertIndex + vert;
//                        inBuf.copyElement(vert, outBuf, curGlobalVertIndex);
//                    }
                }
            }

            globalVertIndex += geomVertCount;
            globalTriIndex += geomTriCount;
        }
    }

    private void doTransforms(FloatBuffer bufPos, FloatBuffer bufNorm, int start, int end, Matrix4f transform) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;
        Vector3f norm = vars.vect2;

        int length = (end - start) * 3;

        // offset is given in element units
        // convert to be in component units
        int offset = start * 3;
        bufPos.position(offset);
        bufNorm.position(offset);
        bufPos.get(tmpFloat, 0, length);
        bufNorm.get(tmpFloatN, 0, length);
        int index = 0;
        while (index < length) {
            pos.x = tmpFloat[index];
            norm.x = tmpFloatN[index++];
            pos.y = tmpFloat[index];
            norm.y = tmpFloatN[index++];
            pos.z = tmpFloat[index];
            norm.z = tmpFloatN[index];

            transform.mult(pos, pos);
            transform.multNormal(norm, norm);

            index -= 2;
            tmpFloat[index] = pos.x;
            tmpFloatN[index++] = norm.x;
            tmpFloat[index] = pos.y;
            tmpFloatN[index++] = norm.y;
            tmpFloat[index] = pos.z;
            tmpFloatN[index++] = norm.z;

        }
        vars.release();
        bufPos.position(offset);
        //using bulk put as it's faster
        bufPos.put(tmpFloat, 0, length);
        bufNorm.position(offset);
        //using bulk put as it's faster
        bufNorm.put(tmpFloatN, 0, length);
    }

    private void doTransformsTangents(FloatBuffer bufPos, FloatBuffer bufNorm, FloatBuffer bufTangents, int start, int end, Matrix4f transform) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;
        Vector3f norm = vars.vect2;
        Vector3f tan = vars.vect3;

        int length = (end - start) * 3;
        int tanLength = (end - start) * 4;

        // offset is given in element units
        // convert to be in component units
        int offset = start * 3;
        int tanOffset = start * 4;

        bufPos.position(offset);
        bufNorm.position(offset);
        bufTangents.position(tanOffset);
        bufPos.get(tmpFloat, 0, length);
        bufNorm.get(tmpFloatN, 0, length);
        bufTangents.get(tmpFloatT, 0, tanLength);

        int index = 0;
        int tanIndex = 0;
        while (index < length) {
            pos.x = tmpFloat[index];
            norm.x = tmpFloatN[index++];
            pos.y = tmpFloat[index];
            norm.y = tmpFloatN[index++];
            pos.z = tmpFloat[index];
            norm.z = tmpFloatN[index];

            tan.x = tmpFloatT[tanIndex++];
            tan.y = tmpFloatT[tanIndex++];
            tan.z = tmpFloatT[tanIndex++];

            transform.mult(pos, pos);
            transform.multNormal(norm, norm);
            transform.multNormal(tan, tan);

            index -= 2;
            tanIndex -= 3;

            tmpFloat[index] = pos.x;
            tmpFloatN[index++] = norm.x;
            tmpFloat[index] = pos.y;
            tmpFloatN[index++] = norm.y;
            tmpFloat[index] = pos.z;
            tmpFloatN[index++] = norm.z;

            tmpFloatT[tanIndex++] = tan.x;
            tmpFloatT[tanIndex++] = tan.y;
            tmpFloatT[tanIndex++] = tan.z;

            //Skipping 4th element of tangent buffer (handedness)
            tanIndex++;

        }
        vars.release();
        bufPos.position(offset);
        //using bulk put as it's faster
        bufPos.put(tmpFloat, 0, length);
        bufNorm.position(offset);
        //using bulk put as it's faster
        bufNorm.put(tmpFloatN, 0, length);
        bufTangents.position(tanOffset);
        //using bulk put as it's faster
        bufTangents.put(tmpFloatT, 0, tanLength);
    }

    private void doCopyBuffer(FloatBuffer inBuf, int offset, FloatBuffer outBuf, int componentSize) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;

        // offset is given in element units
        // convert to be in component units
        offset *= componentSize;

        for (int i = 0; i < inBuf.capacity() / componentSize; i++) {
            pos.x = inBuf.get(i * componentSize + 0);
            pos.y = inBuf.get(i * componentSize + 1);
            pos.z = inBuf.get(i * componentSize + 2);

            outBuf.put(offset + i * componentSize + 0, pos.x);
            outBuf.put(offset + i * componentSize + 1, pos.y);
            outBuf.put(offset + i * componentSize + 2, pos.z);
        }
        vars.release();
    }

    protected class Batch {

        Geometry geometry;
        boolean needMeshUpdate = false;
    }

    protected void setNeedsFullRebatch(boolean needsFullRebatch) {
        this.needsFullRebatch = needsFullRebatch;
    }
    
    public int getOffsetIndex(Geometry batchedGeometry){
        return batchedGeometry.startIndex;
    }
}
