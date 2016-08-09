/*
 * Copyright (c) 2009-2013 jMonkeyEngine
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
 * This class is the java implementation of
 * the enhanced version of Ogre engine Lod generator, by Péter Szücs, originally
 * based on Stan Melax "easy mesh simplification". The MIT licenced C++ source
 * code can be found here
 * https://github.com/worldforge/ember/tree/master/src/components/ogre/lod
 * The licencing for the original code is : 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
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
package jme3tools.optimize;

import com.jme3.bounding.BoundingSphere;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is an utility class that allows to generated the lod levels for an
 * arbitrary mesh. It computes a collapse cost for each vertex and each edges.
 * The higher the cost the most likely collapsing the edge or the vertex will
 * produce artifacts on the mesh. <p>This class is the java implementation of
 * the enhanced version of Ogre engine Lod generator, by Péter Szücs, originally
 * based on Stan Melax "easy mesh simplification". The MIT licenced C++ source
 * code can be found here
 * https://github.com/worldforge/ember/tree/master/src/components/ogre/lod more
 * informations can be found here http://www.melax.com/polychop
 * http://sajty.elementfx.com/progressivemesh/GSoC2012.pdf </p>
 *
 * <p>The algorithm sort vertices according to their collapse cost
 * ascending. It collapse from the "cheapest" vertex to the more expensive.<br>
 * <strong>Usage : </strong><br>
 * <pre>
 *      LodGenerator lODGenerator = new LodGenerator(geometry);
 *      lODGenerator.bakeLods(reductionMethod,reductionvalue);
 * </pre> redutionMethod type is VertexReductionMethod described here
 * {@link TriangleReductionMethod} reduction value depends on the
 * reductionMethod<p>
 *
 *
 * @author Nehon
 */
public class LodGenerator {
    
    private static final Logger logger = Logger.getLogger(LodGenerator.class.getName());
    private static final float NEVER_COLLAPSE_COST = Float.MAX_VALUE;
    private static final float UNINITIALIZED_COLLAPSE_COST = Float.POSITIVE_INFINITY;
    private Vector3f tmpV1 = new Vector3f();
    private Vector3f tmpV2 = new Vector3f();
    private boolean bestQuality = true;
    private int indexCount = 0;
    private List<Vertex> collapseCostSet = new ArrayList<Vertex>();
    private float collapseCostLimit;
    private List<Triangle> triangleList;
    private List<Vertex> vertexList = new ArrayList<Vertex>();
    private float meshBoundingSphereRadius;
    private Mesh mesh;

    /**
     * Describe the way triangles will be removed. <br> PROPORTIONAL :
     * Percentage of triangles to be removed from the mesh. Valid range is a
     * number between 0.0 and 1.0 <br> CONSTANT : Triangle count to be removed
     * from the mesh. Pass only integers or it will be rounded. <br>
     * COLLAPSE_COST : Reduces the vertices, until the cost is bigger then the
     * given value. Collapse cost is equal to the amount of artifact the
     * reduction causes. This generates the best Lod output, but the collapse
     * cost depends on implementation.
     */
    public enum TriangleReductionMethod {

        /**
         * Percentage of triangles to be removed from the mesh.
         *
         * Valid range is a number between 0.0 and 1.0
         */
        PROPORTIONAL,
        /**
         * Triangle count to be removed from the mesh.
         *
         * Pass only integers or it will be rounded.
         */
        CONSTANT,
        /**
         * Reduces the vertices, until the cost is bigger then the given value.
         *
         * Collapse cost is equal to the amount of artifact the reduction
         * causes. This generates the best Lod output, but the collapse cost
         * depends on implementation.
         */
        COLLAPSE_COST
    };
    
    private class Edge {
        
        Vertex destination;
        float collapseCost = UNINITIALIZED_COLLAPSE_COST;
        int refCount;
        
        public Edge(Vertex destination) {
            this.destination = destination;
        }
        
        public void set(Edge other) {
            destination = other.destination;
            collapseCost = other.collapseCost;
            refCount = other.refCount;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) {
                return false;
            }
            return destination == ((Edge) obj).destination;
        }
        
        @Override
        public int hashCode() {
            return destination.hashCode();
        }
        
        @Override
        public String toString() {
            return "Edge{" + "collapsTo " + destination.index + '}';
        }
    }
    
    private class Vertex {
        
        Vector3f position = new Vector3f();
        float collapseCost = UNINITIALIZED_COLLAPSE_COST;
        List<Edge> edges = new ArrayList<Edge>();
        Set<Triangle> triangles = new HashSet<Triangle>();
        Vertex collapseTo;
        boolean isSeam;
        int index;//index in the buffer for debugging

        @Override
        public String toString() {
            return index + " : " + position.toString();
        }
    }
    
    private class Triangle {
        
        Vertex[] vertex = new Vertex[3];
        Vector3f normal;
        boolean isRemoved;
        //indices of the vertices in the vertex buffer
        int[] vertexId = new int[3];
        
        void computeNormal() {
            // Cross-product 2 edges
            tmpV1.set(vertex[1].position).subtractLocal(vertex[0].position);
            tmpV2.set(vertex[2].position).subtractLocal(vertex[1].position);
            
            normal = tmpV1.cross(tmpV2);
            normal.normalizeLocal();
        }
        
        boolean hasVertex(Vertex v) {
            return (v == vertex[0] || v == vertex[1] || v == vertex[2]);
        }
        
        int getVertexIndex(Vertex v) {
            for (int i = 0; i < 3; i++) {
                if (vertex[i] == v) {
                    return vertexId[i];
                }
            }
            throw new IllegalArgumentException("Vertex " + v + "is not part of triangle" + this);
        }
        
        boolean isMalformed() {
            return vertex[0] == vertex[1] || vertex[0] == vertex[2] || vertex[1] == vertex[2];
        }
        
        @Override
        public String toString() {
            String out = "Triangle{\n";
            for (int i = 0; i < 3; i++) {
                out += vertexId[i] + " : " + vertex[i].toString() + "\n";
            }
            out += '}';
            return out;
        }
    }
    /**
     * Comparator used to sort vertices according to their collapse cost
     */
    private Comparator collapseComparator = new Comparator<Vertex>() {
        public int compare(Vertex o1, Vertex o2) {
            if (Float.compare(o1.collapseCost, o2.collapseCost) == 0) {
                return 0;
            }
            if (o1.collapseCost < o2.collapseCost) {
                return -1;
            }
            return 1;
        }
    };

    /**
     * Construct a LodGenerator for the given geometry
     *
     * @param geom the geometry to consider to generate de Lods.
     */
    public LodGenerator(Geometry geom) {
        mesh = geom.getMesh();
        build();
    }
    
    private void build() {
        BoundingSphere bs = new BoundingSphere();
        bs.computeFromPoints(mesh.getFloatBuffer(VertexBuffer.Type.Position));
        meshBoundingSphereRadius = bs.getRadius();
        List<Vertex> vertexLookup = new ArrayList<Vertex>();
        initialize();
        
        gatherVertexData(mesh, vertexLookup);
        gatherIndexData(mesh, vertexLookup);
        computeCosts();
       // assert (assertValidMesh());
        
    }
    
    private void gatherVertexData(Mesh mesh, List<Vertex> vertexLookup) {

        //in case the model is currently animating with software animation
        //attempting to retrieve the bind position instead of the position.
        VertexBuffer position = mesh.getBuffer(VertexBuffer.Type.BindPosePosition);
        if (position == null) {
            position = mesh.getBuffer(VertexBuffer.Type.Position);
        }
        FloatBuffer pos = (FloatBuffer) position.getDataReadOnly();
        pos.rewind();
        
        while (pos.remaining() != 0) {
            Vertex v = new Vertex();
            v.position.setX(pos.get());
            v.position.setY(pos.get());
            v.position.setZ(pos.get());
            v.isSeam = false;
            Vertex existingV = findSimilar(v);
            if (existingV != null) {
                //vertex position already exists
                existingV.isSeam = true;
                v.isSeam = true;
            } else {
                vertexList.add(v);
            }
            vertexLookup.add(v);
        }
        pos.rewind();
    }
    
    private Vertex findSimilar(Vertex v) {
        for (Vertex vertex : vertexList) {
            if (vertex.position.equals(v.position)) {
                return vertex;
            }
        }
        return null;
    }
    
    private void gatherIndexData(Mesh mesh, List<Vertex> vertexLookup) {
        VertexBuffer indexBuffer = mesh.getBuffer(VertexBuffer.Type.Index);
        indexCount = indexBuffer.getNumElements() * 3;
        Buffer b = indexBuffer.getDataReadOnly();
        b.rewind();
        
        while (b.remaining() != 0) {
            Triangle tri = new Triangle();
            tri.isRemoved = false;
            triangleList.add(tri);            
            for (int i = 0; i < 3; i++) {
                if (b instanceof IntBuffer) {
                    tri.vertexId[i] = ((IntBuffer) b).get();
                } else {
                    //bit shift to avoid negative values due to conversion form short to int.
                    //we need an unsigned int here.
                    tri.vertexId[i] = ((ShortBuffer) b).get()& 0xffff;
                }
               // assert (tri.vertexId[i] < vertexLookup.size());
                tri.vertex[i] = vertexLookup.get(tri.vertexId[i]);
                //debug only;
                tri.vertex[i].index = tri.vertexId[i];
            }
            if (tri.isMalformed()) {
                if (!tri.isRemoved) {
                    logger.log(Level.FINE, "malformed triangle found with ID:{0}\n{1} It will be excluded from Lod level calculations.", new Object[]{triangleList.indexOf(tri), tri.toString()});
                    tri.isRemoved = true;
                    indexCount -= 3;
                }
                
            } else {
                tri.computeNormal();
                addTriangleToEdges(tri);
            }
        }
        b.rewind();
    }
    
    private void computeCosts() {
        collapseCostSet.clear();
        
        for (Vertex vertex : vertexList) {
            
            if (!vertex.edges.isEmpty()) {
                computeVertexCollapseCost(vertex);
            } else {
                logger.log(Level.FINE, "Found isolated vertex {0} It will be excluded from Lod level calculations.", vertex);
            }
        }
//        assert (vertexList.size() == collapseCostSet.size());
//        assert (checkCosts());
    }

    //Debug only
    private boolean checkCosts() {
        for (Vertex vertex : vertexList) {
            boolean test = find(collapseCostSet, vertex);
            if (!test) {
                System.out.println("vertex " + vertex.index + " not present in collapse costs");
                return false;
            }
        }
        return true;
    }
    
    private void computeVertexCollapseCost(Vertex vertex) {
        
        vertex.collapseCost = UNINITIALIZED_COLLAPSE_COST;
      //  assert (!vertex.edges.isEmpty());
        for (Edge edge : vertex.edges) {
            edge.collapseCost = computeEdgeCollapseCost(vertex, edge);
         //   assert (edge.collapseCost != UNINITIALIZED_COLLAPSE_COST);
            if (vertex.collapseCost > edge.collapseCost) {
                vertex.collapseCost = edge.collapseCost;
                vertex.collapseTo = edge.destination;
            }
        }
       // assert (vertex.collapseCost != UNINITIALIZED_COLLAPSE_COST);
        collapseCostSet.add(vertex);
    }
    
    float computeEdgeCollapseCost(Vertex src, Edge dstEdge) {
        // This is based on Ogre's collapse cost calculation algorithm.

        Vertex dest = dstEdge.destination;

        // Check for singular triangle destruction
        // If src and dest both only have 1 triangle (and it must be a shared one)
        // then this would destroy the shape, so don't do this
        if (src.triangles.size() == 1 && dest.triangles.size() == 1) {
            return NEVER_COLLAPSE_COST;
        }

        // Degenerate case check
        // Are we going to invert a face normal of one of the neighbouring faces?
        // Can occur when we have a very small remaining edge and collapse crosses it
        // Look for a face normal changing by > 90 degrees
        for (Triangle triangle : src.triangles) {
            // Ignore the deleted faces (those including src & dest)
            if (!triangle.hasVertex(dest)) {
                // Test the new face normal
                Vertex pv0, pv1, pv2;

                // Replace src with dest wherever it is
                pv0 = (triangle.vertex[0] == src) ? dest : triangle.vertex[0];
                pv1 = (triangle.vertex[1] == src) ? dest : triangle.vertex[1];
                pv2 = (triangle.vertex[2] == src) ? dest : triangle.vertex[2];

                // Cross-product 2 edges
                tmpV1.set(pv1.position).subtractLocal(pv0.position);
                tmpV2.set(pv2.position).subtractLocal(pv1.position);

                //computing the normal
                Vector3f newNormal = tmpV1.crossLocal(tmpV2);
                newNormal.normalizeLocal();

                // Dot old and new face normal
                // If < 0 then more than 90 degree difference
                if (newNormal.dot(triangle.normal) < 0.0f) {
                    // Don't do it!
                    return NEVER_COLLAPSE_COST;
                }
            }
        }
        
        float cost;

        // Special cases
        // If we're looking at a border vertex
        if (isBorderVertex(src)) {
            if (dstEdge.refCount > 1) {
                // src is on a border, but the src-dest edge has more than one tri on it
                // So it must be collapsing inwards
                // Mark as very high-value cost
                // curvature = 1.0f;
                cost = 1.0f;
            } else {
                // Collapsing ALONG a border
                // We can't use curvature to measure the effect on the model
                // Instead, see what effect it has on 'pulling' the other border edges
                // The more colinear, the less effect it will have
                // So measure the 'kinkiness' (for want of a better term)

                // Find the only triangle using this edge.
                // PMTriangle* triangle = findSideTriangle(src, dst);

                cost = 0.0f;
                Vector3f collapseEdge = tmpV1.set(src.position).subtractLocal(dest.position);
                collapseEdge.normalizeLocal();
                
                for (Edge edge : src.edges) {
                    
                    Vertex neighbor = edge.destination;
                    //reference check intended
                    if (neighbor != dest && edge.refCount == 1) {
                        Vector3f otherBorderEdge = tmpV2.set(src.position).subtractLocal(neighbor.position);
                        otherBorderEdge.normalizeLocal();
                        // This time, the nearer the dot is to -1, the better, because that means
                        // the edges are opposite each other, therefore less kinkiness
                        // Scale into [0..1]
                        float kinkiness = (otherBorderEdge.dot(collapseEdge) + 1.002f) * 0.5f;
                        cost = Math.max(cost, kinkiness);
                    }
                }
            }
        } else { // not a border

            // Standard inner vertex
            // Calculate curvature
            // use the triangle facing most away from the sides
            // to determine our curvature term
            // Iterate over src's faces again
            cost = 0.001f;
            
            for (Triangle triangle : src.triangles) {
                float mincurv = 1.0f; // curve for face i and closer side to it

                for (Triangle triangle2 : src.triangles) {
                    if (triangle2.hasVertex(dest)) {

                        // Dot product of face normal gives a good delta angle
                        float dotprod = triangle.normal.dot(triangle2.normal);
                        // NB we do (1-..) to invert curvature where 1 is high curvature [0..1]
                        // Whilst dot product is high when angle difference is low
                        mincurv = Math.min(mincurv, (1.002f - dotprod) * 0.5f);
                    }
                }
                cost = Math.max(cost, mincurv);
            }
        }

        // check for texture seam ripping
        if (src.isSeam) {
            if (!dest.isSeam) {
                cost += meshBoundingSphereRadius;
            } else {
                cost += meshBoundingSphereRadius * 0.5;
            }
        }
        
     //   assert (cost >= 0);
        
        return cost * src.position.distanceSquared(dest.position);
    }
    int nbCollapsedTri = 0;

    /**
     * Computes the lod and return a list of VertexBuffers that can then be used
     * for lod (use Mesh.setLodLevels(VertexBuffer[]))<br>
     *
     * This method must be fed with the reduction method
     * {@link TriangleReductionMethod} and a list of reduction values.<br> for
     * each value a lod will be generated. <br> The resulting array will always
     * contain at index 0 the original index buffer of the mesh. <p>
     * <strong>Important note :</strong> some meshes cannot be decimated, so the
     * result of this method can vary depending of the given mesh. Also the
     * reduction values are indicative and the produces mesh will not always
     * meet the required reduction.
     *
     * @param reductionMethod the reduction method to use
     * @param reductionValues the reduction value to use for each lod level.
     * @return an array of VertexBuffers containing the different index buffers
     * representing the lod levels.
     */
    public VertexBuffer[] computeLods(TriangleReductionMethod reductionMethod, float... reductionValues) {
        int tricount = triangleList.size();
        int lastBakeVertexCount = tricount;
        int lodCount = reductionValues.length;
        VertexBuffer[] lods = new VertexBuffer[lodCount + 1];
        int numBakedLods = 1;
        lods[0] = mesh.getBuffer(VertexBuffer.Type.Index);
        for (int curLod = 0; curLod < lodCount; curLod++) {
            int neededTriCount = calcLodTriCount(reductionMethod, reductionValues[curLod]);
            while (neededTriCount < tricount) {
                Collections.sort(collapseCostSet, collapseComparator);
                Iterator<Vertex> it = collapseCostSet.iterator();
                
                if (it.hasNext()) {
                    Vertex v = it.next();
                    if (v.collapseCost < collapseCostLimit) {
                        if (!collapse(v)) {
                            logger.log(Level.FINE, "Couldn''t collapse vertex{0}", v.index);
                        }
                        Iterator<Vertex> it2 = collapseCostSet.iterator();
                        if (it2.hasNext()) {
                            it2.next();
                            it2.remove();// Remove src from collapse costs.
                        }
                        
                    } else {
                        break;
                    }
                } else {
                    break;
                }
                tricount = triangleList.size() - nbCollapsedTri;
            }
            logger.log(Level.FINE, "collapsed {0} tris", nbCollapsedTri);
            boolean outSkipped = (lastBakeVertexCount == tricount);
            if (!outSkipped) {
                lastBakeVertexCount = tricount;
                lods[curLod + 1] = makeLod(mesh);
                numBakedLods++;
            }
        }
        if (numBakedLods <= lodCount) {
            VertexBuffer[] bakedLods = new VertexBuffer[numBakedLods];
            System.arraycopy(lods, 0, bakedLods, 0, numBakedLods);
            return bakedLods;
        } else {
            return lods;
        }
    }

    /**
     * Computes the lods and bake them into the mesh<br>
     *
     * This method must be fed with the reduction method
     * {@link TriangleReductionMethod} and a list of reduction values.<br> for
     * each value a lod will be generated. <p> <strong>Important note :</strong>
     * some meshes cannot be decimated, so the result of this method can vary
     * depending of the given mesh. Also the reduction values are indicative and
     * the produces mesh will not always meet the required reduction.
     *
     * @param reductionMethod the reduction method to use
     * @param reductionValues the reduction value to use for each lod level.
     */
    public void bakeLods(TriangleReductionMethod reductionMethod, float... reductionValues) {
        mesh.setLodLevels(computeLods(reductionMethod, reductionValues));
    }
    
    private VertexBuffer makeLod(Mesh mesh) {
        VertexBuffer indexBuffer = mesh.getBuffer(VertexBuffer.Type.Index);
        
        boolean isShortBuffer = indexBuffer.getFormat() == VertexBuffer.Format.UnsignedShort;
        // Create buffers.	
        VertexBuffer lodBuffer = new VertexBuffer(VertexBuffer.Type.Index);
        int bufsize = indexCount == 0 ? 3 : indexCount;
        
        if (isShortBuffer) {
            lodBuffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedShort, BufferUtils.createShortBuffer(bufsize));
        } else {
            lodBuffer.setupData(VertexBuffer.Usage.Static, 3, VertexBuffer.Format.UnsignedInt, BufferUtils.createIntBuffer(bufsize));
        }
        
        
        
        lodBuffer.getData().rewind();
        //Check if we should fill it with a "dummy" triangle.
        if (indexCount == 0) {
            if (isShortBuffer) {
                for (int m = 0; m < 3; m++) {
                    ((ShortBuffer) lodBuffer.getData()).put((short) 0);
                }
            } else {
                for (int m = 0; m < 3; m++) {
                    ((IntBuffer) lodBuffer.getData()).put(0);
                }
            }
        }

        // Fill buffers.       
        Buffer buf = lodBuffer.getData();
        buf.rewind();
        for (Triangle triangle : triangleList) {
            if (!triangle.isRemoved) {
            //    assert (indexCount != 0);
                if (isShortBuffer) {
                    for (int m = 0; m < 3; m++) {
                        ((ShortBuffer) buf).put((short) triangle.vertexId[m]);
                        
                    }
                } else {
                    for (int m = 0; m < 3; m++) {
                        ((IntBuffer) buf).put(triangle.vertexId[m]);
                    }
                    
                }
            }
        }
        buf.clear();
        lodBuffer.updateData(buf);
        return lodBuffer;
    }
    
    private int calcLodTriCount(TriangleReductionMethod reductionMethod, float reductionValue) {
        int nbTris = mesh.getTriangleCount();
        switch (reductionMethod) {
            case PROPORTIONAL:
                collapseCostLimit = NEVER_COLLAPSE_COST;
                return (int) (nbTris - (nbTris * (reductionValue)));
            
            case CONSTANT:
                collapseCostLimit = NEVER_COLLAPSE_COST;
                if (reductionValue < nbTris) {
                    return nbTris - (int) reductionValue;
                }
                return 0;
            
            case COLLAPSE_COST:
                collapseCostLimit = reductionValue;
                return 0;
            
            default:
                return nbTris;
        }
    }
    
    private int findDstID(int srcId, List<CollapsedEdge> tmpCollapsedEdges) {
        int i = 0;
        for (CollapsedEdge collapsedEdge : tmpCollapsedEdges) {
            if (collapsedEdge.srcID == srcId) {
                return i;
            }
            i++;
        }
        return Integer.MAX_VALUE;
    }
    
    private class CollapsedEdge {
        
        int srcID;
        int dstID;
    };
    
    private void removeTriangleFromEdges(Triangle triangle, Vertex skip) {
        // skip is needed if we are iterating on the vertex's edges or triangles.
        for (int i = 0; i < 3; i++) {
            if (triangle.vertex[i] != skip) {
                triangle.vertex[i].triangles.remove(triangle);
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int n = 0; n < 3; n++) {
                if (i != n) {
                    removeEdge(triangle.vertex[i], new Edge(triangle.vertex[n]));
                }
            }
        }
    }
    
    private void removeEdge(Vertex v, Edge edge) {
        Edge ed = null;
        for (Edge edge1 : v.edges) {
            if (edge1.equals(edge)) {
                ed = edge1;
                break;
            }
        }
        
        if (ed.refCount == 1) {
            v.edges.remove(ed);
        } else {
            ed.refCount--;
        }
        
    }
    
    boolean isBorderVertex(Vertex vertex) {
        for (Edge edge : vertex.edges) {
            if (edge.refCount == 1) {
                return true;
            }
        }
        return false;
    }
    
    private void addTriangleToEdges(Triangle tri) {
        if (bestQuality) {
            Triangle duplicate = getDuplicate(tri);
            if (duplicate != null) {
                if (!tri.isRemoved) {
                    tri.isRemoved = true;
                    indexCount -= 3;
                    logger.log(Level.FINE, "duplicate triangle found{0}{1} It will be excluded from Lod level calculations.", new Object[]{tri, duplicate});
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            tri.vertex[i].triangles.add(tri);
        }
        for (int i = 0; i < 3; i++) {
            for (int n = 0; n < 3; n++) {
                if (i != n) {
                    addEdge(tri.vertex[i], new Edge(tri.vertex[n]));
                }
            }
        }
    }
    
    private void addEdge(Vertex v, Edge edge) {
      //  assert (edge.destination != v);
        
        for (Edge ed : v.edges) {
            if (ed.equals(edge)) {
                ed.refCount++;
                return;
            }
        }
        
        v.edges.add(edge);
        edge.refCount = 1;
        
    }
    
    private void initialize() {
        triangleList = new ArrayList<LodGenerator.Triangle>();
    }
    
    private Triangle getDuplicate(Triangle triangle) {
        // duplicate triangle detection (where all vertices has the same position)
        for (Triangle tri : triangle.vertex[0].triangles) {
            if (isDuplicateTriangle(triangle, tri)) {
                return tri;
            }
        }
        return null;
    }
    
    private boolean isDuplicateTriangle(Triangle triangle, Triangle triangle2) {
        for (int i = 0; i < 3; i++) {
            if (triangle.vertex[i] != triangle2.vertex[0]
                    || triangle.vertex[i] != triangle2.vertex[1]
                    || triangle.vertex[i] != triangle2.vertex[2]) {
                return false;
            }
        }
        return true;
    }
    
    private void replaceVertexID(Triangle triangle, int oldID, int newID, Vertex dst) {
        dst.triangles.add(triangle);
        // NOTE: triangle is not removed from src. This is implementation specific optimization.

        // Its up to the compiler to unroll everything.
        for (int i = 0; i < 3; i++) {
            if (triangle.vertexId[i] == oldID) {
                for (int n = 0; n < 3; n++) {
                    if (i != n) {
                        // This is implementation specific optimization to remove following line.
                        //removeEdge(triangle.vertex[i], new Edge(triangle.vertex[n]));

                        removeEdge(triangle.vertex[n], new Edge(triangle.vertex[i]));
                        addEdge(triangle.vertex[n], new Edge(dst));
                        addEdge(dst, new Edge(triangle.vertex[n]));
                    }
                }
                triangle.vertex[i] = dst;
                triangle.vertexId[i] = newID;
                return;
            }
        }
     //   assert (false);
    }
    
    private void updateVertexCollapseCost(Vertex vertex) {
        float collapseCost = UNINITIALIZED_COLLAPSE_COST;
        Vertex collapseTo = null;
        
        for (Edge edge : vertex.edges) {
            edge.collapseCost = computeEdgeCollapseCost(vertex, edge);
          //  assert (edge.collapseCost != UNINITIALIZED_COLLAPSE_COST);
            if (collapseCost > edge.collapseCost) {
                collapseCost = edge.collapseCost;
                collapseTo = edge.destination;
            }
        }
        if (collapseCost != vertex.collapseCost || vertex.collapseTo != collapseTo) {
//            assert (vertex.collapseTo != null);
//            assert (find(collapseCostSet, vertex));
            collapseCostSet.remove(vertex);
            if (collapseCost != UNINITIALIZED_COLLAPSE_COST) {
                vertex.collapseCost = collapseCost;
                vertex.collapseTo = collapseTo;
                collapseCostSet.add(vertex);
            }
        }
      //  assert (vertex.collapseCost != UNINITIALIZED_COLLAPSE_COST);
    }
    
    private boolean hasSrcID(int srcID, List<CollapsedEdge> cEdges) {
        // This will only return exact matches.
        for (CollapsedEdge collapsedEdge : cEdges) {
            if (collapsedEdge.srcID == srcID) {
                return true;
            }
        }
        
        return false; // Not found
    }
    
    private boolean collapse(Vertex src) {
        Vertex dest = src.collapseTo;
        if (src.edges.isEmpty()) {
            return false;
        }
//        assert (assertValidVertex(dest));
//        assert (assertValidVertex(src));
        
//        assert (src.collapseCost != NEVER_COLLAPSE_COST);
//        assert (src.collapseCost != UNINITIALIZED_COLLAPSE_COST);
//        assert (!src.edges.isEmpty());
//        assert (!src.triangles.isEmpty());
//        assert (src.edges.contains(new Edge(dest)));

        // It may have vertexIDs and triangles from different submeshes(different vertex buffers),
        // so we need to connect them correctly based on deleted triangle's edge.
        // mCollapsedEdgeIDs will be used, when looking up the connections for replacement.
        List<CollapsedEdge> tmpCollapsedEdges = new ArrayList<CollapsedEdge>();
        for (Iterator<Triangle> it = src.triangles.iterator(); it.hasNext();) {
            Triangle triangle = it.next();
            if (triangle.hasVertex(dest)) {
                // Remove a triangle
                // Tasks:
                // 1. Add it to the collapsed edges list.
                // 2. Reduce index count for the Lods, which will not have this triangle.
                // 3. Mark as removed, so it will not be added in upcoming Lod levels.
                // 4. Remove references/pointers to this triangle.

                // 1. task
                int srcID = triangle.getVertexIndex(src);
                if (!hasSrcID(srcID, tmpCollapsedEdges)) {
                    CollapsedEdge cEdge = new CollapsedEdge();
                    cEdge.srcID = srcID;
                    cEdge.dstID = triangle.getVertexIndex(dest);
                    tmpCollapsedEdges.add(cEdge);
                }

                // 2. task
                indexCount -= 3;

                // 3. task
                triangle.isRemoved = true;
                nbCollapsedTri++;

                // 4. task
                removeTriangleFromEdges(triangle, src);
                it.remove();
                
            }
        }
//        assert (!tmpCollapsedEdges.isEmpty());
//        assert (!dest.edges.contains(new Edge(src)));
        
        
        for (Iterator<Triangle> it = src.triangles.iterator(); it.hasNext();) {
            Triangle triangle = it.next();
            if (!triangle.hasVertex(dest)) {
                // Replace a triangle
                // Tasks:
                // 1. Determine the edge which we will move along. (we need to modify single vertex only)
                // 2. Move along the selected edge.

                // 1. task
                int srcID = triangle.getVertexIndex(src);
                int id = findDstID(srcID, tmpCollapsedEdges);
                if (id == Integer.MAX_VALUE) {
                    // Not found any edge to move along.
                    // Destroy the triangle.
                    //     if (!triangle.isRemoved) {
                    triangle.isRemoved = true;
                    indexCount -= 3;
                    removeTriangleFromEdges(triangle, src);
                    it.remove();
                    nbCollapsedTri++;
                    continue;
                }
                int dstID = tmpCollapsedEdges.get(id).dstID;

                // 2. task
                replaceVertexID(triangle, srcID, dstID, dest);
                
                
                if (bestQuality) {
                    triangle.computeNormal();
                }
                
            }
        }
        
        if (bestQuality) {
            for (Edge edge : src.edges) {
                updateVertexCollapseCost(edge.destination);
            }
            updateVertexCollapseCost(dest);
            for (Edge edge : dest.edges) {
                updateVertexCollapseCost(edge.destination);
            }
            
        } else {
            // TODO: Find out why is this needed. assertOutdatedCollapseCost() fails on some
            // rare situations without this. For example goblin.mesh fails.	
            //Treeset to have an ordered list with unique values
            SortedSet<Vertex> updatable = new TreeSet<Vertex>(collapseComparator);
            
            for (Edge edge : src.edges) {
                updatable.add(edge.destination);
                for (Edge edge1 : edge.destination.edges) {
                    updatable.add(edge1.destination);
                }
            }
            
            
            for (Vertex vertex : updatable) {
                updateVertexCollapseCost(vertex);
            }
            
        }
        return true;
    }
    
    private boolean assertValidMesh() {
        // Allows to find bugs in collapsing.
        for (Vertex vertex : collapseCostSet) {
            assertValidVertex(vertex);
        }
        return true;
        
    }
    
    private boolean assertValidVertex(Vertex v) {
        // Allows to find bugs in collapsing.
        //       System.out.println("Asserting " + v.index);
        for (Triangle t : v.triangles) {
            for (int i = 0; i < 3; i++) {
                //             System.out.println("check " + t.vertex[i].index);

                //assert (collapseCostSet.contains(t.vertex[i]));
                assert (find(collapseCostSet, t.vertex[i]));
                
                assert (t.vertex[i].edges.contains(new Edge(t.vertex[i].collapseTo)));
                for (int n = 0; n < 3; n++) {
                    if (i != n) {
                        
                        int id = t.vertex[i].edges.indexOf(new Edge(t.vertex[n]));
                        Edge ed = t.vertex[i].edges.get(id);
                        //assert (ed.collapseCost != UNINITIALIZED_COLLAPSE_COST);
                    } else {
                        assert (!t.vertex[i].edges.contains(new Edge(t.vertex[n])));
                    }
                }
            }
        }
        return true;
    }
    
    private boolean find(List<Vertex> set, Vertex v) {
        for (Vertex vertex : set) {
            if (v == vertex) {
                return true;
            }
        }
        return false;
    }
}
