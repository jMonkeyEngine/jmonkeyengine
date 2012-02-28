package jme3tools.optimize;

import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.*;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.IntMap.Entry;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.logging.Logger;

public class GeometryBatchFactory {

    private static final Logger logger = Logger.getLogger(GeometryBatchFactory.class.getName());

    private static void doTransformVerts(FloatBuffer inBuf, int offset, FloatBuffer outBuf, Matrix4f transform) {
        Vector3f pos = new Vector3f();

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = 0; i < inBuf.capacity() / 3; i++) {
            pos.x = inBuf.get(i * 3 + 0);
            pos.y = inBuf.get(i * 3 + 1);
            pos.z = inBuf.get(i * 3 + 2);

            transform.mult(pos, pos);

            outBuf.put(offset + i * 3 + 0, pos.x);
            outBuf.put(offset + i * 3 + 1, pos.y);
            outBuf.put(offset + i * 3 + 2, pos.z);
        }
    }

    private static void doTransformNorms(FloatBuffer inBuf, int offset, FloatBuffer outBuf, Matrix4f transform) {
        Vector3f norm = new Vector3f();

        // offset is given in element units
        // convert to be in component units
        offset *= 3;

        for (int i = 0; i < inBuf.capacity() / 3; i++) {
            norm.x = inBuf.get(i * 3 + 0);
            norm.y = inBuf.get(i * 3 + 1);
            norm.z = inBuf.get(i * 3 + 2);

            transform.multNormal(norm, norm);

            outBuf.put(offset + i * 3 + 0, norm.x);
            outBuf.put(offset + i * 3 + 1, norm.y);
            outBuf.put(offset + i * 3 + 2, norm.z);
        }
    }
    
    private static void doTransformTangents(FloatBuffer inBuf, int offset, int components, FloatBuffer outBuf, Matrix4f transform) {
        Vector3f tan = new Vector3f();
        
        // offset is given in element units
        // convert to be in component units
        offset *= components;

        for (int i = 0; i < inBuf.capacity() / components; i++) {
            tan.x = inBuf.get(i * components + 0);
            tan.y = inBuf.get(i * components + 1);
            tan.z = inBuf.get(i * components + 2);

            transform.multNormal(tan, tan);

            outBuf.put(offset + i * components + 0, tan.x);
            outBuf.put(offset + i * components + 1, tan.y);
            outBuf.put(offset + i * components + 2, tan.z);
            
            if (components == 4){
                outBuf.put(offset + i * components + 3, inBuf.get(i * components + 3));
            }
        }
    }

    /**
     * Merges all geometries in the collection into
     * the output mesh. Creates a new material using the TextureAtlas.
     * 
     * @param geometries
     * @param outMesh
     */
    public static void mergeGeometries(Collection<Geometry> geometries, Mesh outMesh) {
        int[] compsForBuf = new int[VertexBuffer.Type.values().length];
        Format[] formatForBuf = new Format[compsForBuf.length];

        int totalVerts = 0;
        int totalTris = 0;
        int totalLodLevels = 0;

        Mode mode = null;
        for (Geometry geom : geometries) {
            totalVerts += geom.getVertexCount();
            totalTris += geom.getTriangleCount();
            totalLodLevels = Math.min(totalLodLevels, geom.getMesh().getNumLodLevels());

            Mode listMode;
            int components;
            switch (geom.getMesh().getMode()) {
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

            for (VertexBuffer vb : geom.getMesh().getBufferList().getArray()){
                compsForBuf[vb.getBufferType().ordinal()] = vb.getNumComponents();
                formatForBuf[vb.getBufferType().ordinal()] = vb.getFormat();
            }

            if (mode != null && mode != listMode) {
                throw new UnsupportedOperationException("Cannot combine different"
                        + " primitive types: " + mode + " != " + listMode);
            }
            mode = listMode;
            compsForBuf[Type.Index.ordinal()] = components;
        }

        outMesh.setMode(mode);
        if (totalVerts >= 65536) {
            // make sure we create an UnsignedInt buffer so
            // we can fit all of the meshes
            formatForBuf[Type.Index.ordinal()] = Format.UnsignedInt;
        } else {
            formatForBuf[Type.Index.ordinal()] = Format.UnsignedShort;
        }

        // generate output buffers based on retrieved info
        for (int i = 0; i < compsForBuf.length; i++) {
            if (compsForBuf[i] == 0) {
                continue;
            }

            Buffer data;
            if (i == Type.Index.ordinal()) {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalTris);
            } else {
                data = VertexBuffer.createBuffer(formatForBuf[i], compsForBuf[i], totalVerts);
            }

            VertexBuffer vb = new VertexBuffer(Type.values()[i]);
            vb.setupData(Usage.Static, compsForBuf[i], formatForBuf[i], data);
            outMesh.setBuffer(vb);
        }

        int globalVertIndex = 0;
        int globalTriIndex = 0;

        for (Geometry geom : geometries) {
            Mesh inMesh = geom.getMesh();
            geom.computeWorldMatrix();
            Matrix4f worldMatrix = geom.getWorldMatrix();

            int geomVertCount = inMesh.getVertexCount();
            int geomTriCount = inMesh.getTriangleCount();

            for (int bufType = 0; bufType < compsForBuf.length; bufType++) {
                VertexBuffer inBuf = inMesh.getBuffer(Type.values()[bufType]);
                VertexBuffer outBuf = outMesh.getBuffer(Type.values()[bufType]);

                if (inBuf == null || outBuf == null) {
                    continue;
                }

                if (Type.Index.ordinal() == bufType) {
                    int components = compsForBuf[bufType];

                    IndexBuffer inIdx = inMesh.getIndicesAsList();
                    IndexBuffer outIdx = outMesh.getIndexBuffer();

                    for (int tri = 0; tri < geomTriCount; tri++) {
                        for (int comp = 0; comp < components; comp++) {
                            int idx = inIdx.get(tri * components + comp) + globalVertIndex;
                            outIdx.put((globalTriIndex + tri) * components + comp, idx);
                        }
                    }
                } else if (Type.Position.ordinal() == bufType) {
                    FloatBuffer inPos = (FloatBuffer) inBuf.getDataReadOnly();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    doTransformVerts(inPos, globalVertIndex, outPos, worldMatrix);
                } else if (Type.Normal.ordinal() == bufType) {
                    FloatBuffer inPos = (FloatBuffer) inBuf.getDataReadOnly();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    doTransformNorms(inPos, globalVertIndex, outPos, worldMatrix);
                }else if(Type.Tangent.ordinal() == bufType){                
                    FloatBuffer inPos = (FloatBuffer) inBuf.getDataReadOnly();
                    FloatBuffer outPos = (FloatBuffer) outBuf.getData();
                    int components = inBuf.getNumComponents();
                    doTransformTangents(inPos, globalVertIndex, components, outPos, worldMatrix);
                } else {
                    inBuf.copyElements(0, outBuf, globalVertIndex, geomVertCount);
                }
            }

            globalVertIndex += geomVertCount;
            globalTriIndex += geomTriCount;
        }
    }

    public static void makeLods(Collection<Geometry> geometries, Mesh outMesh) {
        int lodLevels = 0;
        int[] lodSize = null;
        int index = 0;
        for (Geometry g : geometries) {
            if (lodLevels == 0) {
                lodLevels = g.getMesh().getNumLodLevels();
            }
            if (lodSize == null) {
                lodSize = new int[lodLevels];
            }
            for (int i = 0; i < lodLevels; i++) {
                lodSize[i] += g.getMesh().getLodLevel(i).getData().capacity();
                //if( i == 0) System.out.println(index + " " +lodSize[i]);
            }
            index++;
        }
        int[][] lodData = new int[lodLevels][];
        for (int i = 0; i < lodLevels; i++) {
            lodData[i] = new int[lodSize[i]];
        }
        VertexBuffer[] lods = new VertexBuffer[lodLevels];
        int bufferPos[] = new int[lodLevels];
        //int index = 0;
        int numOfVertices = 0;
        int curGeom = 0;
        for (Geometry g : geometries) {
            if (numOfVertices == 0) {
                numOfVertices = g.getVertexCount();
            }
            for (int i = 0; i < lodLevels; i++) {
                ShortBuffer buffer = (ShortBuffer) g.getMesh().getLodLevel(i).getDataReadOnly();
                //System.out.println("buffer: " + buffer.capacity() + " limit: " + lodSize[i] + " " + index);
                for (int j = 0; j < buffer.capacity(); j++) {
                    lodData[i][bufferPos[i] + j] = buffer.get() + numOfVertices * curGeom;
                    //bufferPos[i]++;
                }
                bufferPos[i] += buffer.capacity();
            }
            curGeom++;
        }
        for (int i = 0; i < lodLevels; i++) {
            lods[i] = new VertexBuffer(Type.Index);
            lods[i].setupData(Usage.Dynamic, 1, Format.UnsignedInt, BufferUtils.createIntBuffer(lodData[i]));
        }
        System.out.println(lods.length);
        outMesh.setLodLevels(lods);
    }

    public static List<Geometry> makeBatches(Collection<Geometry> geometries) {
        return makeBatches(geometries, false);
    }

    /**
     * Batches a collection of Geometries so that all with the same material get combined.
     * @param geometries The Geometries to combine
     * @return A List of newly created Geometries, each with a  distinct material
     */
    public static List<Geometry> makeBatches(Collection<Geometry> geometries, boolean useLods) {
        ArrayList<Geometry> retVal = new ArrayList<Geometry>();
        HashMap<Material, List<Geometry>> matToGeom = new HashMap<Material, List<Geometry>>();

        for (Geometry geom : geometries) {
            List<Geometry> outList = matToGeom.get(geom.getMaterial());
            if (outList == null) {
                outList = new ArrayList<Geometry>();
                matToGeom.put(geom.getMaterial(), outList);
            }
            outList.add(geom);
        }

        int batchNum = 0;
        for (Map.Entry<Material, List<Geometry>> entry : matToGeom.entrySet()) {
            Material mat = entry.getKey();
            List<Geometry> geomsForMat = entry.getValue();
            Mesh mesh = new Mesh();
            mergeGeometries(geomsForMat, mesh);
            // lods
            if (useLods) {
                makeLods(geomsForMat, mesh);
            }
            mesh.updateCounts();
            mesh.updateBound();

            Geometry out = new Geometry("batch[" + (batchNum++) + "]", mesh);
            out.setMaterial(mat);
            retVal.add(out);
        }

        return retVal;
    }

    public static void gatherGeoms(Spatial scene, List<Geometry> geoms) {
        if (scene instanceof Node) {
            Node node = (Node) scene;
            for (Spatial child : node.getChildren()) {
                gatherGeoms(child, geoms);
            }
        } else if (scene instanceof Geometry) {
            geoms.add((Geometry) scene);
        }
    }

    /**
     * Optimizes a scene by combining Geometry with the same material.
     * All Geometries found in the scene are detached from their parent and
     * a new Node containing the optimized Geometries is attached.
     * @param scene The scene to optimize
     * @return The newly created optimized geometries attached to a node
     */
    public static Spatial optimize(Node scene) {
        return optimize(scene, false);
    }

    /**
     * Optimizes a scene by combining Geometry with the same material.
     * All Geometries found in the scene are detached from their parent and
     * a new Node containing the optimized Geometries is attached.
     * @param scene The scene to optimize
     * @param useLods true if you want the resulting geometry to keep lod information
     * @return The newly created optimized geometries attached to a node
     */
    public static Node optimize(Node scene, boolean useLods) {
        ArrayList<Geometry> geoms = new ArrayList<Geometry>();

        gatherGeoms(scene, geoms);

        List<Geometry> batchedGeoms = makeBatches(geoms, useLods);
        for (Geometry geom : batchedGeoms) {
            scene.attachChild(geom);
        }

        for (Iterator<Geometry> it = geoms.iterator(); it.hasNext();) {
            Geometry geometry = it.next();
            geometry.removeFromParent();
        }

        // Since the scene is returned unaltered the transform must be reset
        scene.setLocalTransform(Transform.IDENTITY);

        return scene;
    }

    public static void printMesh(Mesh mesh) {
        for (int bufType = 0; bufType < Type.values().length; bufType++) {
            VertexBuffer outBuf = mesh.getBuffer(Type.values()[bufType]);
            if (outBuf == null) {
                continue;
            }

            System.out.println(outBuf.getBufferType() + ": ");
            for (int vert = 0; vert < outBuf.getNumElements(); vert++) {
                String str = "[";
                for (int comp = 0; comp < outBuf.getNumComponents(); comp++) {
                    Object val = outBuf.getElementComponent(vert, comp);
                    outBuf.setElementComponent(vert, comp, val);
                    val = outBuf.getElementComponent(vert, comp);
                    str += val;
                    if (comp != outBuf.getNumComponents() - 1) {
                        str += ", ";
                    }
                }
                str += "]";
                System.out.println(str);
            }
            System.out.println("------");
        }
    }

    public static void main(String[] args) {
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
