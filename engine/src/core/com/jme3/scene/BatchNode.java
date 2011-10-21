/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.scene;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.bounding.BoundingVolume;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BatchNode holds a geometry that is a batched version off all geometries that are in its sub scenegraph.
 * this geometry is directly attached to the node in the scene graph.
 * usage is like any other node except you have to call the batch() method once all geoms have been attached to the sub scene graph (see todo more automagic for further enhancements)
 * all the geometry that have been batched are set to cullHint.always to not render them.
 * the sub geometries can be transformed as usual their transforms are used to update the mesh of the geometryBatch.
 * sub geoms can be removed but it may be slower than the normal spatial removing
 * Sub geoms can be added after the batch() method has been called but won't be batched and will be rendered as normal geometries.
 * To integrated them in the batch you have to call the batch() method again on the batchNode.
 * 
 * TODO account for sub-BatchNodes
 * TODO account for geometries that have different materials
 * TODO normal or tangents or both looks a bit weird
 * TODO more automagic (batch when needed in the updateLigicalState)
 * @author Nehon
 */
public class BatchNode extends Node implements Savable {

    private static final Logger logger = Logger.getLogger(BatchNode.class.getName());
    /**
     * the geometry holding the batched mesh
     */
    protected Geometry batch;
    protected Material material;
    private boolean needMeshUpdate = false;

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
            if (needMeshUpdate) {
                batch.getMesh().updateBound();
                batch.updateWorldBound();
                needMeshUpdate = false;
            }

        }

        if ((refreshFlags & RF_BOUND) != 0) {
            updateWorldBound();
        }

        assert refreshFlags == 0;
    }

    protected void updateSubBatch(Geometry bg) {
        if (batch != null) {
            Mesh mesh = batch.getMesh();

            FloatBuffer buf = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Position).getData();
            doTransformVerts(buf, 0, bg.startIndex, bg.startIndex + bg.getVertexCount(), buf, bg.cachedOffsetMat);
            mesh.getBuffer(VertexBuffer.Type.Position).updateData(buf);

            buf = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Normal).getData();
            doTransformNorm(buf, 0, bg.startIndex, bg.startIndex + bg.getVertexCount(), buf, bg.cachedOffsetMat);
            mesh.getBuffer(VertexBuffer.Type.Normal).updateData(buf);


            if (mesh.getBuffer(VertexBuffer.Type.Tangent) != null) {

                buf = (FloatBuffer) mesh.getBuffer(VertexBuffer.Type.Tangent).getData();
                doTransformNorm(buf, 0, bg.startIndex, bg.startIndex + bg.getVertexCount(), buf, bg.cachedOffsetMat);
                mesh.getBuffer(VertexBuffer.Type.Tangent).updateData(buf);
            }

            needMeshUpdate = true;
        }
    }

    @Override
    protected void setTransformRefresh() {
        refreshFlags |= RF_TRANSFORM;
        setBoundRefresh();

        for (Spatial child : children.getArray()) {
            if ((child.refreshFlags & RF_TRANSFORM) != 0) {
                continue;
            }

           innerTransformRefresh(child);
            //child.setTransformRefresh();

        }
    }
//
    private void innerTransformRefresh(Spatial s) {
        s.refreshFlags |= RF_TRANSFORM;
        s.setBoundRefresh();
        if (s instanceof Node) {            
            Node n = (Node) s;
           
            for (Spatial child :((SafeArrayList<Spatial>) n.getChildren()).getArray()) {
                if ((child.refreshFlags & RF_TRANSFORM) != 0) {
                    continue;
                }
                innerTransformRefresh(child);
            }
        }

    }

    /**
     * Batch this batchNode
     * every geometry of the sub scene graph of this node will be batched into a single mesh that will be render in one call
     */
    public void batch() {

        List<Geometry> tmpList = new ArrayList<Geometry>();
        Mesh m = new Mesh();
        populateList(tmpList, this);
        mergeGeometries(m, tmpList);

        if (batch == null) {
            batch = new Geometry(name + "-batch");
            batch.setMaterial(material);
            this.attachChild(batch);
        }
        batch.setMesh(m);
        batch.getMesh().updateCounts();
        batch.getMesh().updateBound();
    }

    private void populateList(List<Geometry> list, Spatial n) {

        if (n instanceof Geometry) {
            if (n != batch) {
                list.add((Geometry) n);
            }

        } else if (n instanceof Node) {
            for (Spatial child : ((Node) n).getChildren()) {
                populateList(list, child);
            }
        }

    }

    /**
     * Sets the material to use for this geometry.
     * 
     * @param material the material to use for this geometry
     */
    @Override
    public void setMaterial(Material material) {
        super.setMaterial(material);
        if (batch != null) {
            batch.setMaterial(material);
        }
        this.material = material;

    }

    /**
     * Returns the material that is used for this geometry.
     * 
     * @return the material that is used for this geometry
     * 
     * @see #setMaterial(com.jme3.material.Material) 
     */
    public Material getMaterial() {
        return material;
    }

    /**
     * @return The bounding volume of the mesh, in model space.
     */
    public BoundingVolume getModelBound() {
        if (batch != null) {
            return batch.getMesh().getBound();
        }
        return super.getWorldBound();

    }

    /**
     * This version of clone is a shallow clone, in other words, the
     * same mesh is referenced as the original geometry.
     * Exception: if the mesh is marked as being a software
     * animated mesh, (bind pose is set) then the positions
     * and normals are deep copied.
     */
    @Override
    public BatchNode clone(boolean cloneMaterial) {
        BatchNode clone = (BatchNode) super.clone(cloneMaterial);
        clone.batch = batch.clone(cloneMaterial);
        return clone;
    }

    /**
     * This version of clone is a shallow clone, in other words, the
     * same mesh is referenced as the original geometry.
     * Exception: if the mesh is marked as being a software
     * animated mesh, (bind pose is set) then the positions
     * and normals are deep copied.
     */
    @Override
    public BatchNode clone() {
        return clone(true);
    }

    /**
     * Creates a deep clone of the geometry,
     * this creates an identical copy of the mesh
     * with the vertexbuffer data duplicated.
     */
    @Override
    public Spatial deepClone() {
        BatchNode clone = clone(true);
        clone.batch = (Geometry) batch.deepClone();
        return clone;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);

        if (material != null) {
            oc.write(material.getAssetName(), "materialName", null);
        }
        oc.write(material, "material", null);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);


        material = null;
        String matName = ic.readString("materialName", null);
        if (matName != null) {
            // Material name is set,
            // Attempt to load material via J3M
            try {
                material = im.getAssetManager().loadMaterial(matName);
            } catch (AssetNotFoundException ex) {
                // Cannot find J3M file.
                logger.log(Level.FINE, "Could not load J3M file {0} for Geometry.",
                        matName);
            }
        }
        // If material is NULL, try to load it from the geometry
        if (material == null) {
            material = (Material) ic.readSavable("material", null);
        }

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

            for (Entry<VertexBuffer> entry : geom.getMesh().getBuffers()) {
                compsForBuf[entry.getKey()] = entry.getValue().getNumComponents();
                formatForBuf[entry.getKey()] = entry.getValue().getFormat();
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
            vb.setupData(VertexBuffer.Usage.Static, compsForBuf[i], formatForBuf[i], data);
            outMesh.setBuffer(vb);
        }

        int globalVertIndex = 0;
        int globalTriIndex = 0;

        for (Geometry geom : geometries) {
            Mesh inMesh = geom.getMesh();
            if (geom.getMaterial() != null && material == null) {
                material = geom.getMaterial();
            }
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
                    doCopyBuffer(inPos, globalVertIndex, outPos);
                } else if (VertexBuffer.Type.Normal.ordinal() == bufType || VertexBuffer.Type.Tangent.ordinal() == bufType) {
                    FloatBuffer inPos = (FloatBuffer) inBuf.getData();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    doCopyBuffer(inPos, globalVertIndex, outPos);
                } else {
                    for (int vert = 0; vert < geomVertCount; vert++) {
                        int curGlobalVertIndex = globalVertIndex + vert;
                        inBuf.copyElement(vert, outBuf, curGlobalVertIndex);
                    }
                }
            }

            globalVertIndex += geomVertCount;
            globalTriIndex += geomTriCount;
        }
    }

    private void doTransformVerts(FloatBuffer inBuf, int offset, int start, int end, FloatBuffer outBuf, Matrix4f transform) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = start; i < end; i++) {
            int index = i * 3;
            pos.x = inBuf.get(index);
            pos.y = inBuf.get(index + 1);
            pos.z = inBuf.get(index + 2);

            transform.mult(pos, pos);
            index += offset;
            outBuf.put(index, pos.x);
            outBuf.put(index + 1, pos.y);
            outBuf.put(index + 2, pos.z);
        }
        vars.release();
    }

    private void doTransformNorm(FloatBuffer inBuf, int offset, int start, int end, FloatBuffer outBuf, Matrix4f transform) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = start; i < end; i++) {
            int index = i * 3;
            pos.x = inBuf.get(index);
            pos.y = inBuf.get(index + 1);
            pos.z = inBuf.get(index + 2);

            transform.multNormal(pos, pos);
            index += offset;
            outBuf.put(index, pos.x);
            outBuf.put(index + 1, pos.y);
            outBuf.put(index + 2, pos.z);
        }
        vars.release();
    }

    private void doCopyBuffer(FloatBuffer inBuf, int offset, FloatBuffer outBuf) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = 0; i < inBuf.capacity() / 3; i++) {
            pos.x = inBuf.get(i * 3 + 0);
            pos.y = inBuf.get(i * 3 + 1);
            pos.z = inBuf.get(i * 3 + 2);

            outBuf.put(offset + i * 3 + 0, pos.x);
            outBuf.put(offset + i * 3 + 1, pos.y);
            outBuf.put(offset + i * 3 + 2, pos.z);
        }
        vars.release();
    }
}
