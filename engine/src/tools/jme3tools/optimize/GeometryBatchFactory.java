package jme3tools.optimize;

import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.VirtualIndexBuffer;
import com.jme3.scene.mesh.WrappedIndexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.util.IntMap.Entry;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeometryBatchFactory {

    private static void doTransformVerts(FloatBuffer inBuf, int offset, FloatBuffer outBuf, Matrix4f transform){
        Vector3f pos = new Vector3f();

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = 0; i < inBuf.capacity()/3; i++){
            pos.x = inBuf.get(i*3+0);
            pos.y = inBuf.get(i*3+1);
            pos.z = inBuf.get(i*3+2);

            transform.mult(pos, pos);

            outBuf.put(offset+i*3+0, pos.x);
            outBuf.put(offset+i*3+1, pos.y);
            outBuf.put(offset+i*3+2, pos.z);
        }
    }

    private static void doTransformNorms(FloatBuffer inBuf, int offset, FloatBuffer outBuf, Matrix4f transform){
        Vector3f norm = new Vector3f();

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = 0; i < inBuf.capacity()/3; i++){
            norm.x = inBuf.get(i*3+0);
            norm.y = inBuf.get(i*3+1);
            norm.z = inBuf.get(i*3+2);

            transform.multNormal(norm, norm);

            outBuf.put(offset+i*3+0, norm.x);
            outBuf.put(offset+i*3+1, norm.y);
            outBuf.put(offset+i*3+2, norm.z);
        }
    }

    /**
     * Merges all geometries in the collection into
     * the output mesh. Does not take into account materials.
     * 
     * @param geometries
     * @param outMesh
     */
    public static void mergeGeometries(Collection<Geometry> geometries, Mesh outMesh){
        int[] compsForBuf = new int[VertexBuffer.Type.values().length];
        Format[] formatForBuf = new Format[compsForBuf.length];

        int totalVerts = 0;
        int totalTris  = 0;

        Mode mode = null;
        for (Geometry geom : geometries){
            totalVerts += geom.getVertexCount();
            totalTris  += geom.getTriangleCount();

            Mode listMode;
            int components;
            switch (geom.getMesh().getMode()){
                case Points:
                    listMode = Mode.Points;
                    components = 1;
                    break;
                case LineLoop:
                case LineStrip:
                case Lines:
                    listMode = Mode.Lines;
                    components = 2;
                    break;
                case TriangleFan:
                case TriangleStrip:
                case Triangles:
                    listMode = Mode.Triangles;
                    components = 3;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            
            for (Entry<VertexBuffer> entry : geom.getMesh().getBuffers()){
                compsForBuf[entry.getKey()] = entry.getValue().getNumComponents();
                formatForBuf[entry.getKey()] = entry.getValue().getFormat();
            }

            if (mode != null && mode != listMode){
                throw new UnsupportedOperationException("Cannot combine different"
                                                      + " primitive types: " + mode + " != " + listMode);
            }
            mode = listMode;
            compsForBuf[Type.Index.ordinal()] = components;
        }

        outMesh.setMode(mode);
        if (totalVerts >= 65536){
            // make sure we create an UnsignedInt buffer so
            // we can fit all of the meshes
            formatForBuf[Type.Index.ordinal()] = Format.UnsignedInt;
        }else{
            formatForBuf[Type.Index.ordinal()] = Format.UnsignedShort;
        }

        // generate output buffers based on retrieved info
        for (int i = 0; i < compsForBuf.length; i++){
            if (compsForBuf[i] == 0)
                continue;

            Buffer data;
            if (i == Type.Index.ordinal()){
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalTris);
            }else{
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalVerts);
            }

            VertexBuffer vb = new VertexBuffer(Type.values()[i]);
            vb.setupData(Usage.Static, compsForBuf[i], formatForBuf[i], data);
            outMesh.setBuffer(vb);
        }

        int globalVertIndex = 0;
        int globalTriIndex  = 0;

        for (Geometry geom : geometries){
            Mesh inMesh = geom.getMesh();
            geom.computeWorldMatrix();
            Matrix4f worldMatrix = geom.getWorldMatrix();

            int geomVertCount = inMesh.getVertexCount();
            int geomTriCount  = inMesh.getTriangleCount();

            for (int bufType = 0; bufType < compsForBuf.length; bufType++){
                VertexBuffer inBuf = inMesh.getBuffer(Type.values()[bufType]);
                VertexBuffer outBuf = outMesh.getBuffer(Type.values()[bufType]);

                if (outBuf == null)
                    continue;

                if (Type.Index.ordinal() == bufType){
                    int components = compsForBuf[bufType];

                    IndexBuffer inIdx = inMesh.getIndexBuffer();
                    if (inIdx == null){
                        inIdx = new VirtualIndexBuffer(geomVertCount, inMesh.getMode());
                    }else if (inMesh.getMode() != mode){
                        inIdx = new WrappedIndexBuffer(inMesh);
                    }
                    IndexBuffer outIdx = outMesh.getIndexBuffer();

                    for (int tri = 0; tri < geomTriCount; tri++){
                        for (int comp = 0; comp < components; comp++){
                            int idx = inIdx.get(tri*components+comp) + globalVertIndex;
                            outIdx.put((globalTriIndex + tri) * components + comp, idx);
                        }
                    }
                }else if (Type.Position.ordinal() == bufType){
                    FloatBuffer inPos = (FloatBuffer) inBuf.getData();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    doTransformVerts(inPos, globalVertIndex, outPos, worldMatrix);
                }else if (Type.Normal.ordinal() == bufType || Type.Tangent.ordinal() == bufType){
                    FloatBuffer inPos = (FloatBuffer) inBuf.getData();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    doTransformNorms(inPos, globalVertIndex, outPos, worldMatrix);
                }else{
                    for (int vert = 0; vert < geomVertCount; vert++){
                        int curGlobalVertIndex = globalVertIndex + vert;
                        inBuf.copyElement(vert, outBuf, curGlobalVertIndex);
                    }
                }
            }

            globalVertIndex += geomVertCount;
            globalTriIndex  += geomTriCount;
        }
    }


    public static List<Geometry> makeBatches(Collection<Geometry> geometries){
        ArrayList<Geometry> retVal = new ArrayList<Geometry>();
        HashMap<Material, List<Geometry>> matToGeom = new HashMap<Material, List<Geometry>>();

        for (Geometry geom : geometries){
            List<Geometry> outList = matToGeom.get(geom.getMaterial());
            if (outList == null){
                outList = new ArrayList<Geometry>();
                matToGeom.put(geom.getMaterial(), outList);
            }
            outList.add(geom);
        }

        int batchNum = 0;
        for (Map.Entry<Material, List<Geometry>> entry : matToGeom.entrySet()){
            Material mat = entry.getKey();
            List<Geometry> geomsForMat = entry.getValue();
            Mesh mesh = new Mesh();
            mergeGeometries(geomsForMat, mesh);
            mesh.updateCounts();
            mesh.updateBound();

            Geometry out = new Geometry("batch[" + (batchNum++) + "]", mesh);
            out.setMaterial(mat);
            retVal.add(out);
        }

        return retVal;
    }

    private static void gatherGeoms(Spatial scene, List<Geometry> geoms){
        if (scene instanceof Node){
            Node node = (Node) scene;
            for (Spatial child : node.getChildren()){
                gatherGeoms(child, geoms);
            }
        }else if (scene instanceof Geometry){
            geoms.add((Geometry)scene);
        }
    }

    public static Spatial optimize(Spatial scene){
        ArrayList<Geometry> geoms = new ArrayList<Geometry>();
        gatherGeoms(scene, geoms);

        List<Geometry> batchedGeoms = makeBatches(geoms);

        Node node = new Node(scene.getName());
        for (Geometry geom : batchedGeoms){
            node.attachChild(geom);
        }

        return node;
    }

    public static void printMesh(Mesh mesh){
        for (int bufType = 0; bufType < Type.values().length; bufType++){
            VertexBuffer outBuf = mesh.getBuffer(Type.values()[bufType]);
            if (outBuf == null)
                continue;

            System.out.println(outBuf.getBufferType() + ": ");
            for (int vert = 0; vert < outBuf.getNumElements(); vert++){
                String str = "[";
                for (int comp = 0; comp < outBuf.getNumComponents(); comp++){
                    Object val = outBuf.getElementComponent(vert, comp);
                    outBuf.setElementComponent(vert, comp, val);
                    val = outBuf.getElementComponent(vert, comp);
                    str += val;
                    if (comp != outBuf.getNumComponents()-1)
                        str += ", ";
                }
                str += "]";
                System.out.println(str);
            }
            System.out.println("------");
        }
    }

    public static void main(String[] args){
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, new float[]{
            0, 0, 0,
            1, 0, 0,
            1, 1, 0,
            0, 1, 0
        });
        mesh.setBuffer(Type.Index, 2, new short[]{
           0, 1,
           1, 2,
           2, 3,
           3, 0
        });

        Geometry g1 = new Geometry("g1", mesh);

        ArrayList<Geometry> geoms = new ArrayList<Geometry>();
        geoms.add(g1);

        Mesh outMesh = new Mesh();
        mergeGeometries(geoms, outMesh);
        printMesh(outMesh);
    }
}
