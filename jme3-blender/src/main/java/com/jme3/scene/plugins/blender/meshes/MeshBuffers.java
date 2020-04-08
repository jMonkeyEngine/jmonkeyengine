package com.jme3.scene.plugins.blender.meshes;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;

/**
 * A class that aggregates the mesh data to prepare proper buffers. The buffers refer only to ONE material.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class MeshBuffers {
    private static final int              MAXIMUM_WEIGHTS_PER_VERTEX = 4;

    /** The material index. */
    private final int                     materialIndex;
    /** The vertices. */
    private List<Vector3f>                verts                      = new ArrayList<Vector3f>();
    /** The normals. */
    private List<Vector3f>                normals                    = new ArrayList<Vector3f>();
    /** The UV coordinate sets. */
    private Map<String, List<Vector2f>>   uvCoords                   = new HashMap<String, List<Vector2f>>();
    /** The vertex colors. */
    private List<byte[]>                  vertColors                 = new ArrayList<byte[]>();
    /** The indexes. */
    private List<Integer>                 indexes                    = new ArrayList<Integer>();
    /** The maximum weights count assigned to a single vertex. Used during weights normalization. */
    private int                           maximumWeightsPerVertex;
    /** A list of mapping between weights and indexes. Each entry for the proper vertex. */
    private List<TreeMap<Float, Integer>> boneWeightAndIndexes       = new ArrayList<TreeMap<Float, Integer>>();

    /**
     * Constructor stores only the material index value.
     * @param materialIndex
     *            the material index
     */
    public MeshBuffers(int materialIndex) {
        this.materialIndex = materialIndex;
    }

    /**
     * @return the material index
     */
    public int getMaterialIndex() {
        return materialIndex;
    }

    /**
     * @return indexes buffer
     */
    public Buffer getIndexBuffer() {
        if (indexes.size() <= Short.MAX_VALUE) {
            short[] indices = new short[indexes.size()];
            for (int i = 0; i < indexes.size(); ++i) {
                indices[i] = indexes.get(i).shortValue();
            }
            return BufferUtils.createShortBuffer(indices);
        } else {
            int[] indices = new int[indexes.size()];
            for (int i = 0; i < indexes.size(); ++i) {
                indices[i] = indexes.get(i).intValue();
            }
            return BufferUtils.createIntBuffer(indices);
        }
    }

    /**
     * @return positions buffer
     */
    public VertexBuffer getPositionsBuffer() {
        VertexBuffer positionBuffer = new VertexBuffer(Type.Position);
        Vector3f[] data = verts.toArray(new Vector3f[verts.size()]);
        positionBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(data));
        return positionBuffer;
    }

    /**
     * @return normals buffer
     */
    public VertexBuffer getNormalsBuffer() {
        VertexBuffer positionBuffer = new VertexBuffer(Type.Normal);
        Vector3f[] data = normals.toArray(new Vector3f[normals.size()]);
        positionBuffer.setupData(Usage.Static, 3, Format.Float, BufferUtils.createFloatBuffer(data));
        return positionBuffer;
    }

    /**
     * @return bone buffers
     */
    public BoneBuffersData getBoneBuffers() {
        BoneBuffersData result = null;
        if (maximumWeightsPerVertex > 0) {
            this.normalizeBoneBuffers(MAXIMUM_WEIGHTS_PER_VERTEX);
            maximumWeightsPerVertex = MAXIMUM_WEIGHTS_PER_VERTEX;
            
            FloatBuffer weightsFloatData = BufferUtils.createFloatBuffer(boneWeightAndIndexes.size() * MAXIMUM_WEIGHTS_PER_VERTEX);
            ByteBuffer indicesData = BufferUtils.createByteBuffer(boneWeightAndIndexes.size() * MAXIMUM_WEIGHTS_PER_VERTEX);
            int index = 0;
            for (Map<Float, Integer> boneBuffersData : boneWeightAndIndexes) {
                if (boneBuffersData.size() > 0) {
                    int count = 0;
                    for (Entry<Float, Integer> entry : boneBuffersData.entrySet()) {
                        weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + count, entry.getKey());
                        indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX + count, entry.getValue().byteValue());
                        ++count;
                    }
                } else {
                    // if no bone is assigned to this vertex then attach it to the 0-indexed root bone
                    weightsFloatData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, 1.0f);
                    indicesData.put(index * MAXIMUM_WEIGHTS_PER_VERTEX, (byte) 0);
                }
                ++index;
            }
            VertexBuffer verticesWeights = new VertexBuffer(Type.BoneWeight);
            verticesWeights.setupData(Usage.CpuOnly, maximumWeightsPerVertex, Format.Float, weightsFloatData);

            VertexBuffer verticesWeightsIndices = new VertexBuffer(Type.BoneIndex);
            verticesWeightsIndices.setupData(Usage.CpuOnly, maximumWeightsPerVertex, Format.UnsignedByte, indicesData);

            result = new BoneBuffersData(maximumWeightsPerVertex, verticesWeights, verticesWeightsIndices);
        }

        return result;
    }

    /**
     * @return UV coordinates sets
     */
    public Map<String, List<Vector2f>> getUvCoords() {
        return uvCoords;
    }

    /**
     * @return <b>true</b> if vertex colors are used and <b>false</b> otherwise
     */
    public boolean areVertexColorsUsed() {
        return vertColors.size() > 0;
    }

    /**
     * @return vertex colors buffer
     */
    public ByteBuffer getVertexColorsBuffer() {
        ByteBuffer result = null;
        if (vertColors.size() > 0) {
            result = BufferUtils.createByteBuffer(4 * vertColors.size());
            for (byte[] v : vertColors) {
                if (v != null) {
                    result.put(v[0]).put(v[1]).put(v[2]).put(v[3]);
                } else {
                    result.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
                }
            }
            result.flip();
        }
        return result;
    }

    /**
     * @return <b>true</b> if indexes can be shorts' and <b>false</b> if they need to be ints'
     */
    public boolean isShortIndexBuffer() {
        return indexes.size() <= Short.MAX_VALUE;
    }

    /**
     * Appends a vertex and normal to the buffers.
     * @param vert
     *            vertex
     * @param normal
     *            normal vector
     */
    public void append(Vector3f vert, Vector3f normal) {
        int index = this.indexOf(vert, normal, null);
        if (index >= 0) {
            indexes.add(index);
        } else {
            indexes.add(verts.size());
            verts.add(vert);
            normals.add(normal);
        }
    }

    /**
     * Appends the face data to the buffers.
     * @param smooth
     *            tells if the face is smooth or flat
     * @param verts
     *            the vertices
     * @param normals
     *            the normals
     * @param uvCoords
     *            the UV coordinates
     * @param vertColors
     *            the vertex colors
     * @param vertexGroups
     *            the vertex groups
     */
    public void append(boolean smooth, Vector3f[] verts, Vector3f[] normals, Map<String, List<Vector2f>> uvCoords, byte[][] vertColors, List<Map<Float, Integer>> vertexGroups) {
        if (verts.length != normals.length) {
            throw new IllegalArgumentException("The amount of verts and normals MUST be equal!");
        }
        if (vertColors != null && vertColors.length != verts.length) {
            throw new IllegalArgumentException("The amount of vertex colors and vertices MUST be equal!");
        }
        if (vertexGroups.size() != 0 && vertexGroups.size() != verts.length) {
            throw new IllegalArgumentException("The amount of (if given) vertex groups and vertices MUST be equal!");
        }

        if (!smooth) {
            // make the normals perpendicular to the face
            normals[0] = normals[1] = normals[2] = FastMath.computeNormal(verts[0], verts[1], verts[2]);
        }

        for (int i = 0; i < verts.length; ++i) {
            int index = -1;
            Map<String, Vector2f> uvCoordsForVertex = this.getUVsForVertex(i, uvCoords);
            if (smooth && (index = this.indexOf(verts[i], normals[i], uvCoordsForVertex)) >= 0) {
                indexes.add(index);
            } else {
                indexes.add(this.verts.size());
                this.verts.add(verts[i]);
                this.normals.add(normals[i]);
                this.vertColors.add(vertColors[i]);

                if (uvCoords != null && uvCoords.size() > 0) {
                    for (Entry<String, List<Vector2f>> entry : uvCoords.entrySet()) {
                        if (this.uvCoords.containsKey(entry.getKey())) {
                            this.uvCoords.get(entry.getKey()).add(entry.getValue().get(i));
                        } else {
                            List<Vector2f> uvs = new ArrayList<Vector2f>();
                            uvs.add(entry.getValue().get(i));
                            this.uvCoords.put(entry.getKey(), uvs);
                        }
                    }
                }

                if (vertexGroups.size() > 0) {
                    Map<Float, Integer> group = vertexGroups.get(i);
                    maximumWeightsPerVertex = Math.max(maximumWeightsPerVertex, group.size());
                    boneWeightAndIndexes.add(new TreeMap<Float, Integer>(group));
                }
            }
        }
    }

    /**
     * Returns UV coordinates assigned for the vertex with the proper index.
     * @param vertexIndex
     *            the index of the vertex
     * @param uvs
     *            all UV coordinates we search in
     * @return a set of UV coordinates assigned to the given vertex
     */
    private Map<String, Vector2f> getUVsForVertex(int vertexIndex, Map<String, List<Vector2f>> uvs) {
        if (uvs == null || uvs.size() == 0) {
            return null;
        }
        Map<String, Vector2f> result = new HashMap<String, Vector2f>(uvs.size());
        for (Entry<String, List<Vector2f>> entry : uvs.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(vertexIndex));
        }
        return result;
    }

    /**
     * The method returns an index of a vertex described by the given data.
     * The method tries to find a vertex that mathes the given data. If it does it means
     * that such vertex is already used.
     * @param vert
     *            the vertex position coordinates
     * @param normal
     *            the vertex's normal vector
     * @param uvCoords
     *            the UV coords of the vertex
     * @return index of the found vertex of -1
     */
    private int indexOf(Vector3f vert, Vector3f normal, Map<String, Vector2f> uvCoords) {
        for (int i = 0; i < verts.size(); ++i) {
            if (verts.get(i).equals(vert) && normals.get(i).equals(normal)) {
                if (uvCoords != null && uvCoords.size() > 0) {
                    for (Entry<String, Vector2f> entry : uvCoords.entrySet()) {
                        List<Vector2f> uvs = this.uvCoords.get(entry.getKey());
                        if (uvs == null) {
                            return -1;
                        }
                        if (!uvs.get(i).equals(entry.getValue())) {
                            return -1;
                        }
                    }
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * The method normalizes the weights and bone indexes data.
     * First it truncates the amount to MAXIMUM_WEIGHTS_PER_VERTEX because this is how many weights JME can handle.
     * Next it normalizes the weights so that the sum of all verts is 1.
     * @param maximumSize
     *            the maximum size that the data will be truncated to (usually: MAXIMUM_WEIGHTS_PER_VERTEX)
     */
    private void normalizeBoneBuffers(int maximumSize) {
        for (TreeMap<Float, Integer> group : boneWeightAndIndexes) {
            if (group.size() > maximumSize) {
                NavigableMap<Float, Integer> descendingWeights = group.descendingMap();
                while (descendingWeights.size() > maximumSize) {
                    descendingWeights.pollLastEntry();
                }
            }

            // normalizing the weights so that the sum of the values is equal to '1'
            TreeMap<Float, Integer> normalizedGroup = new TreeMap<Float, Integer>();
            float sum = 0;
            for (Entry<Float, Integer> entry : group.entrySet()) {
                sum += entry.getKey();
            }

            if (sum != 0 && sum != 1) {
                for (Entry<Float, Integer> entry : group.entrySet()) {
                    normalizedGroup.put(entry.getKey() / sum, entry.getValue());
                }
                group.clear();
                group.putAll(normalizedGroup);
            }
        }
    }

    /**
     * A class that gathers the data for mesh bone buffers.
     * Added to increase code readability.
     * 
     * @author Marcin Roguski (Kaelthas)
     */
    public static class BoneBuffersData {
        public final int          maximumWeightsPerVertex;
        public final VertexBuffer verticesWeights;
        public final VertexBuffer verticesWeightsIndices;

        public BoneBuffersData(int maximumWeightsPerVertex, VertexBuffer verticesWeights, VertexBuffer verticesWeightsIndices) {
            this.maximumWeightsPerVertex = maximumWeightsPerVertex;
            this.verticesWeights = verticesWeights;
            this.verticesWeightsIndices = verticesWeightsIndices;
        }
    }
}
