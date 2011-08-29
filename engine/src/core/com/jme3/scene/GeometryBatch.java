/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package com.jme3.scene;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.IntMap.Entry;
import com.jme3.util.SafeArrayList;
import com.jme3.util.TempVars;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nehon
 */
public class GeometryBatch extends Geometry {

    private SafeArrayList<BatchedGeometry> children = new SafeArrayList<BatchedGeometry>(BatchedGeometry.class);
    private List<Geometry> tmpList = new ArrayList<Geometry>();
    private boolean needMeshUpdate = false;

    public GeometryBatch() {
        this("GeometryBatch");
    }

    public GeometryBatch(String name) {
        this.name = name;
        addControl(new ControlUpdate());
    }

    public BatchedGeometry batch(Geometry geom) {

        tmpList.clear();
        Mesh m = new Mesh();
        if (mesh != null) {

            tmpList.add(this);
        }
        tmpList.add(geom);
        List<BatchedGeometry> l = mergeGeometries(m, tmpList);
        mesh = m;
        mesh.updateCounts();
        mesh.updateBound();
        return l.get(0);

    }

    public List<BatchedGeometry> batch(List<Geometry> geom) {
        if (mesh != null) {
            geom.add(0, this);
        }

        Mesh m = new Mesh();
        List<BatchedGeometry> l = mergeGeometries(m, geom);
        mesh = m;
        mesh.updateCounts();
        mesh.updateBound();
        return l;
    }

    public List<BatchedGeometry> batch(Geometry... geoms) {
        tmpList.clear();
        Mesh m = new Mesh();
        if (mesh != null) {
            tmpList.add(this);

        }
        for (Geometry geometry : geoms) {
            tmpList.add(geometry);
        }

        List<BatchedGeometry> l = mergeGeometries(m, tmpList);
        mesh = m;
        mesh.updateCounts();
        mesh.updateBound();
        return l;
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
                updateModelBound();
                needMeshUpdate = false;
            }
        }

        if ((refreshFlags & RF_BOUND) != 0) {
            updateWorldBound();
        }

        assert refreshFlags == 0;
    }

    protected void updateSubBatch(BatchedGeometry bg) {
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

    /**
     * <code>getChild</code> returns the first child found with exactly the
     * given name (case sensitive.)
     * 
     * @param name
     *            the name of the child to retrieve. If null, we'll return null.
     * @return the child if found, or null.
     */
    public Spatial getChild(String name) {
        if (name == null) {
            return null;
        }

        for (Spatial child : children.getArray()) {
            if (name.equals(child.getName())) {
                return child;
            } else if (child instanceof Node) {
                Spatial out = ((Node) child).getChild(name);
                if (out != null) {
                    return out;
                }
            }
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
    private List<BatchedGeometry> mergeGeometries(Mesh outMesh, List<Geometry> geometries) {
        int[] compsForBuf = new int[VertexBuffer.Type.values().length];
        VertexBuffer.Format[] formatForBuf = new VertexBuffer.Format[compsForBuf.length];
        List<BatchedGeometry> batchedGeoms = new ArrayList<BatchedGeometry>();

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
            if (geom != this) {
                BatchedGeometry bg = new BatchedGeometry(this, geom);
                bg.startIndex = globalVertIndex;
                bg.setLocalTransform(geom.getLocalTransform());
                children.add(bg);
                batchedGeoms.add(bg);
            }
            Mesh inMesh = geom.getMesh();            

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
        return batchedGeoms;
    }

    private void doTransformVerts(FloatBuffer inBuf, int offset, int start, int end, FloatBuffer outBuf, Matrix4f transform) {
        TempVars vars = TempVars.get();
        Vector3f pos = vars.vect1;

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = start; i < end; i++) {
            pos.x = inBuf.get(i * 3 + 0);
            pos.y = inBuf.get(i * 3 + 1);
            pos.z = inBuf.get(i * 3 + 2);

            transform.mult(pos, pos);

            outBuf.put(offset + i * 3 + 0, pos.x);
            outBuf.put(offset + i * 3 + 1, pos.y);
            outBuf.put(offset + i * 3 + 2, pos.z);
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
            pos.x = inBuf.get(i * 3 + 0);
            pos.y = inBuf.get(i * 3 + 1);
            pos.z = inBuf.get(i * 3 + 2);

            transform.multNormal(pos, pos);

            outBuf.put(offset + i * 3 + 0, pos.x);
            outBuf.put(offset + i * 3 + 1, pos.y);
            outBuf.put(offset + i * 3 + 2, pos.z);
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

    public class ControlUpdate extends AbstractControl {

        @Override
        protected void controlUpdate(float tpf) {
            for (BatchedGeometry batchedGeometry : children) {
                for (int i = 0; i < batchedGeometry.getNumControls(); i++) {
                    batchedGeometry.getControl(i).update(tpf);
                }
            }
        }

        @Override
        protected void controlRender(RenderManager rm, ViewPort vp) {
            for (BatchedGeometry batchedGeometry : children) {
                for (int i = 0; i < batchedGeometry.getNumControls(); i++) {
                    batchedGeometry.getControl(i).render(rm, vp);
                }
            }
        }

        public com.jme3.scene.control.Control cloneForSpatial(Spatial spatial) {
            return null;
        }
    }
}
