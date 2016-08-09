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

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

/**
 * BatchNode holds geometries that are a batched version of all the geometries that are in its sub scenegraph.
 * There is one geometry per different material in the sub tree.
 * The geometries are directly attached to the node in the scene graph.
 * Usage is like any other node except you have to call the {@link #batch()} method once all the geometries have been attached to the sub scene graph and their material set
 * (see todo more automagic for further enhancements)
 * All the geometries that have been batched are set to not be rendered - {@link CullHint} is left intact.
 * The sub geometries can be transformed as usual, their transforms are used to update the mesh of the geometryBatch.
 * Sub geoms can be removed but it may be slower than the normal spatial removing
 * Sub geoms can be added after the batch() method has been called but won't be batched and will just be rendered as normal geometries.
 * To integrate them in the batch you have to call the batch() method again on the batchNode.
 *
 * TODO normal or tangents or both looks a bit weird
 * TODO more automagic (batch when needed in the updateLogicalState)
 * @author Nehon
 */
public class BatchNode extends GeometryGroupNode {

    private static final Logger logger = Logger.getLogger(BatchNode.class.getName());
    /**
     * the list of geometry holding the batched meshes
     */
    protected SafeArrayList<Batch> batches = new SafeArrayList<Batch>(Batch.class);
    /**
     * a map for storing the batches by geometry to quickly access the batch when updating
     */
    protected Map<Geometry, Batch> batchesByGeom = new HashMap<Geometry, Batch>();
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
    public void onTransformChange(Geometry geom) {
        updateSubBatch(geom);
    }

    @Override
    public void onMaterialChange(Geometry geom) {
        throw new UnsupportedOperationException(
                "Cannot set the material of a batched geometry, "
                + "change the material of the parent BatchNode.");
    }

    @Override
    public void onMeshChange(Geometry geom) {
        throw new UnsupportedOperationException(
                "Cannot set the mesh of a batched geometry");
    }

    @Override
    public void onGeometryUnassociated(Geometry geom) {
        setNeedsFullRebatch(true);
    }

    protected Matrix4f getTransformMatrix(Geometry g){
        return g.cachedWorldMat;
    }

    protected void updateSubBatch(Geometry bg) {
        Batch batch = batchesByGeom.get(bg);
        if (batch != null) {
            Mesh mesh = batch.geometry.getMesh();
            Mesh origMesh = bg.getMesh();

            VertexBuffer pvb = mesh.getBuffer(VertexBuffer.Type.Position);
            FloatBuffer posBuf = (FloatBuffer) pvb.getData();
            VertexBuffer nvb = mesh.getBuffer(VertexBuffer.Type.Normal);
            FloatBuffer normBuf = (FloatBuffer) nvb.getData();

            VertexBuffer opvb = origMesh.getBuffer(VertexBuffer.Type.Position);
            FloatBuffer oposBuf = (FloatBuffer) opvb.getData();
            VertexBuffer onvb = origMesh.getBuffer(VertexBuffer.Type.Normal);
            FloatBuffer onormBuf = (FloatBuffer) onvb.getData();
            Matrix4f transformMat = getTransformMatrix(bg);

            if (mesh.getBuffer(VertexBuffer.Type.Tangent) != null) {

                VertexBuffer tvb = mesh.getBuffer(VertexBuffer.Type.Tangent);
                FloatBuffer tanBuf = (FloatBuffer) tvb.getData();
                VertexBuffer otvb = origMesh.getBuffer(VertexBuffer.Type.Tangent);
                FloatBuffer otanBuf = (FloatBuffer) otvb.getData();
                doTransformsTangents(oposBuf, onormBuf, otanBuf, posBuf, normBuf, tanBuf, bg.startIndex, bg.startIndex + bg.getVertexCount(), transformMat);
                tvb.updateData(tanBuf);
            } else {
                doTransforms(oposBuf, onormBuf, posBuf, normBuf, bg.startIndex, bg.startIndex + bg.getVertexCount(), transformMat);
            }
            pvb.updateData(posBuf);
            nvb.updateData(normBuf);


            batch.geometry.updateModelBound();
        }
    }

    /**
     * Batch this batchNode
     * every geometry of the sub scene graph of this node will be batched into a single mesh that will be rendered in one call
     */
    public void batch() {
        doBatch();
        //we set the batch geometries to ignore transforms to avoid transforms of parent nodes to be applied twice
        for (Batch batch : batches.getArray()) {
            batch.geometry.setIgnoreTransform(true);
            batch.geometry.setUserData(UserData.JME_PHYSICSIGNORE, true);
        }
    }

    protected void doBatch() {
        Map<Material, List<Geometry>> matMap = new HashMap<Material, List<Geometry>>();
        int nbGeoms = 0;

        gatherGeometries(matMap, this, needsFullRebatch);
        if (needsFullRebatch) {
            for (Batch batch : batches.getArray()) {
                batch.geometry.removeFromParent();
            }
            batches.clear();
            batchesByGeom.clear();
        }
        //only reset maxVertCount if there is something new to batch
        if (matMap.size() > 0) {
            maxVertCount = 0;
        }

        for (Map.Entry<Material, List<Geometry>> entry : matMap.entrySet()) {
            Mesh m = new Mesh();
            Material material = entry.getKey();
            List<Geometry> list = entry.getValue();
            nbGeoms += list.size();
            String batchName = name + "-batch" + batches.size();
            Batch batch;
            if (!needsFullRebatch) {
                batch = findBatchByMaterial(material);
                if (batch != null) {
                    list.add(0, batch.geometry);
                    batchName = batch.geometry.getName();
                    batch.geometry.removeFromParent();
                } else {
                    batch = new Batch();
                }
            } else {
                batch = new Batch();
            }
            mergeGeometries(m, list);
            m.setDynamic();

            batch.updateGeomList(list);

            batch.geometry = new Geometry(batchName);
            batch.geometry.setMaterial(material);
            this.attachChild(batch.geometry);


            batch.geometry.setMesh(m);
            batch.geometry.getMesh().updateCounts();
            batch.geometry.updateModelBound();
            batches.add(batch);
        }
        if (batches.size() > 0) {
            needsFullRebatch = false;
        }


        logger.log(Level.FINE, "Batched {0} geometries in {1} batches.", new Object[]{nbGeoms, batches.size()});

        //init the temp arrays if something has been batched only.
        if(matMap.size()>0){
            //TODO these arrays should be allocated by chunk instead to avoid recreating them each time the batch is changed.
            //init temp float arrays
            tmpFloat = new float[maxVertCount * 3];
            tmpFloatN = new float[maxVertCount * 3];
            if (useTangents) {
                tmpFloatT = new float[maxVertCount * 4];
            }
        }
    }

    //in case the detached spatial is a node, we unbatch all geometries in its subegraph
    @Override
    public Spatial detachChildAt(int index) {
        Spatial s = super.detachChildAt(index);
        if (s instanceof Node) {
            unbatchSubGraph(s);
        }
        return s;
    }

    /**
     * recursively visit the subgraph and unbatch geometries
     * @param s
     */
    private void unbatchSubGraph(Spatial s) {
        if (s instanceof Node) {
            for (Spatial sp : ((Node) s).getChildren()) {
                unbatchSubGraph(sp);
            }
        } else if (s instanceof Geometry) {
            Geometry g = (Geometry) s;
            if (g.isGrouped()) {
                g.unassociateFromGroupNode();
            }
        }
    }


    private void gatherGeometries(Map<Material, List<Geometry>> map, Spatial n, boolean rebatch) {

        if (n instanceof Geometry) {

            if (!isBatch(n) && n.getBatchHint() != BatchHint.Never) {
                Geometry g = (Geometry) n;
                if (!g.isGrouped() || rebatch) {
                    if (g.getMaterial() == null) {
                        throw new IllegalStateException("No material is set for Geometry: " + g.getName() + " please set a material before batching");
                    }
                    List<Geometry> list = map.get(g.getMaterial());
                    if (list == null) {
                        //trying to compare materials with the isEqual method
                        for (Map.Entry<Material, List<Geometry>> mat : map.entrySet()) {
                            if (g.getMaterial().contentEquals(mat.getKey())) {
                                list = mat.getValue();
                            }
                        }
                    }
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
                gatherGeometries(map, child, rebatch);
            }
        }

    }

    private Batch findBatchByMaterial(Material m) {
        for (Batch batch : batches.getArray()) {
            if (batch.geometry.getMaterial().contentEquals(m)) {
                return batch;
            }
        }
        return null;
    }

    public final boolean isBatch(Spatial s) {
        for (Batch batch : batches.getArray()) {
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
            Batch b = batches.iterator().next();
            return b.geometry.getMaterial();
        }
        return null;
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
        boolean[] normForBuf = new boolean[VertexBuffer.Type.values().length];

        int totalVerts = 0;
        int totalTris = 0;
        int totalLodLevels = 0;
        int maxWeights = -1;

        Mesh.Mode mode = null;
        float lineWidth = 1f;
        for (Geometry geom : geometries) {
            totalVerts += geom.getVertexCount();
            totalTris += geom.getTriangleCount();
            totalLodLevels = Math.min(totalLodLevels, geom.getMesh().getNumLodLevels());
            if (maxVertCount < geom.getVertexCount()) {
                maxVertCount = geom.getVertexCount();
            }
            Mesh.Mode listMode;
            //float listLineWidth = 1f;
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
                    //listLineWidth = geom.getMesh().getLineWidth();
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
                int currentCompsForBuf = compsForBuf[vb.getBufferType().ordinal()];
                if (vb.getBufferType() != VertexBuffer.Type.Index && currentCompsForBuf != 0 && currentCompsForBuf != vb.getNumComponents()) {
                    throw new UnsupportedOperationException("The geometry " + geom + " buffer " + vb.getBufferType()
                            + " has different number of components than the rest of the meshes "
                            + "(this: " + vb.getNumComponents() + ", expected: " + currentCompsForBuf + ")");
                }
                compsForBuf[vb.getBufferType().ordinal()] = vb.getNumComponents();
                formatForBuf[vb.getBufferType().ordinal()] = vb.getFormat();
                normForBuf[vb.getBufferType().ordinal()] = vb.isNormalized();
            }

            maxWeights = Math.max(maxWeights, geom.getMesh().getMaxNumWeights());

            if (mode != null && mode != listMode) {
                throw new UnsupportedOperationException("Cannot combine different"
                        + " primitive types: " + mode + " != " + listMode);
            }
            mode = listMode;
            //Not needed anymore as lineWidth is now in RenderState and will be taken into account when merging according to the material
//            if (mode == Mesh.Mode.Lines) {
//                if (lineWidth != 1f && listLineWidth != lineWidth) {
//                    throw new UnsupportedOperationException("When using Mesh Line mode, cannot combine meshes with different line width "
//                            + lineWidth + " != " + listLineWidth);
//                }
//                lineWidth = listLineWidth;
//            }
            compsForBuf[VertexBuffer.Type.Index.ordinal()] = components;
        }

        outMesh.setMaxNumWeights(maxWeights);
        outMesh.setMode(mode);
        //outMesh.setLineWidth(lineWidth);
        if (totalVerts >= 65536) {
            // make sure we create an UnsignedInt buffer so we can fit all of the meshes
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
            vb.setNormalized(normForBuf[i]);
            outMesh.setBuffer(vb);
        }

        int globalVertIndex = 0;
        int globalTriIndex = 0;

        for (Geometry geom : geometries) {
            Mesh inMesh = geom.getMesh();
            if (!isBatch(geom)) {
                geom.associateWithGroupNode(this, globalVertIndex);
            }

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
                    if (inBuf == null) {
                        throw new IllegalArgumentException("Geometry " + geom.getName() + " has no " + outBuf.getBufferType() + " buffer whereas other geoms have. all geometries should have the same types of buffers.\n Try to use GeometryBatchFactory.alignBuffer() on the BatchNode before batching");
                    } else if (outBuf == null) {
                        throw new IllegalArgumentException("Geometry " + geom.getName() + " has a " + outBuf.getBufferType() + " buffer whereas other geoms don't. all geometries should have the same types of buffers.\n Try to use GeometryBatchFactory.alignBuffer() on the BatchNode before batching");
                    } else {
                        inBuf.copyElements(0, outBuf, globalVertIndex, geomVertCount);
                    }
                }
            }

            globalVertIndex += geomVertCount;
            globalTriIndex += geomTriCount;
        }
    }

    private void doTransforms(FloatBuffer bindBufPos, FloatBuffer bindBufNorm, FloatBuffer bufPos, FloatBuffer bufNorm, int start, int end, Matrix4f transform) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;
        Vector3f norm = vars.vect2;

        int length = (end - start) * 3;

        // offset is given in element units
        // convert to be in component units
        int offset = start * 3;
        bindBufPos.rewind();
        bindBufNorm.rewind();
        //bufPos.position(offset);
        //bufNorm.position(offset);
        bindBufPos.get(tmpFloat, 0, length);
        bindBufNorm.get(tmpFloatN, 0, length);
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

    private void doTransformsTangents(FloatBuffer bindBufPos, FloatBuffer bindBufNorm, FloatBuffer bindBufTangents,FloatBuffer bufPos, FloatBuffer bufNorm, FloatBuffer bufTangents, int start, int end, Matrix4f transform) {
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


        bindBufPos.rewind();
        bindBufNorm.rewind();
        bindBufTangents.rewind();
        bindBufPos.get(tmpFloat, 0, length);
        bindBufNorm.get(tmpFloatN, 0, length);
        bindBufTangents.get(tmpFloatT, 0, tanLength);

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

        for (int i = 0; i < inBuf.limit() / componentSize; i++) {
            pos.x = inBuf.get(i * componentSize + 0);
            pos.y = inBuf.get(i * componentSize + 1);
            pos.z = inBuf.get(i * componentSize + 2);

            outBuf.put(offset + i * componentSize + 0, pos.x);
            outBuf.put(offset + i * componentSize + 1, pos.y);
            outBuf.put(offset + i * componentSize + 2, pos.z);
        }
        vars.release();
    }

    protected class Batch implements JmeCloneable {
        /**
         * update the batchesByGeom map for this batch with the given List of geometries
         * @param list
         */
        void updateGeomList(List<Geometry> list) {
            for (Geometry geom : list) {
                if (!isBatch(geom)) {
                    batchesByGeom.put(geom, this);
                }
            }
        }
        Geometry geometry;

        public final Geometry getGeometry() {
            return geometry;
        }

        @Override
        public Batch jmeClone() {
            try {
                return (Batch)super.clone();
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError();
            }
        }

        @Override
        public void cloneFields( Cloner cloner, Object original ) {
            this.geometry = cloner.clone(geometry);
        }

    }

    protected void setNeedsFullRebatch(boolean needsFullRebatch) {
        this.needsFullRebatch = needsFullRebatch;
    }

    @Override
    public Node clone(boolean cloneMaterials) {
        BatchNode clone = (BatchNode)super.clone(cloneMaterials);
        if ( batches.size() > 0) {
            for ( Batch b : batches ) {
                for ( int i =0; i < clone.children.size(); i++ ) {
                    if ( clone.children.get(i).getName().equals(b.geometry.getName())) {
                        clone.children.remove(i);
                        break;
                    }
                }
            }
            clone.needsFullRebatch = true;
            clone.batches = new SafeArrayList<Batch>(Batch.class);
            clone.batchesByGeom = new HashMap<Geometry, Batch>();
            clone.batch();
        }
        return clone;
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        super.cloneFields(cloner, original);

        this.batches = cloner.clone(batches);
        this.tmpFloat = cloner.clone(tmpFloat);
        this.tmpFloatN = cloner.clone(tmpFloatN);
        this.tmpFloatT = cloner.clone(tmpFloatT);


        HashMap<Geometry, Batch> newBatchesByGeom = new HashMap<Geometry, Batch>();
        for( Map.Entry<Geometry, Batch> e : batchesByGeom.entrySet() ) {
            newBatchesByGeom.put(cloner.clone(e.getKey()), cloner.clone(e.getValue()));
        }
        this.batchesByGeom = newBatchesByGeom;
    }

    @Override
    public int collideWith(Collidable other, CollisionResults results) {
        int total = 0;
        for (Spatial child : children.getArray()){
            if (!isBatch(child)) {
                total += child.collideWith(other, results);
            }
        }
        return total;
    }
}
