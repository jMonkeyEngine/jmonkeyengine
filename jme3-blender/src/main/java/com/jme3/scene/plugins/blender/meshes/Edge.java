package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.math.FastMath;
import com.jme3.math.Line;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.IndexesLoop.IndexPredicate;

/**
 * A class that represents a single edge between two vertices.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class Edge extends Line {
    private static final long   serialVersionUID = 7172714692126675311L;

    private static final Logger LOGGER           = Logger.getLogger(Edge.class.getName());

    /** The vertices indexes. */
    private int                 index1, index2;

    public Edge() {
    }

    /**
     * This constructor only stores the indexes of the vertices. The position vertices should be stored
     * outside this class.
     * @param index1
     *            the first index of the edge
     * @param index2
     *            the second index of the edge
     */
    private Edge(int index1, int index2) {
        this.index1 = index1;
        this.index2 = index2;
    }

    /**
     * This constructor stores both indexes and vertices list. The list should contain ALL verts and not
     * only those belonging to the edge.
     * @param index1
     *            the first index of the edge
     * @param index2
     *            the second index of the edge
     * @param vertices
     *            the vertices of the mesh
     */
    public Edge(int index1, int index2, List<Vector3f> vertices) {
        this(index1, index2);
        this.set(vertices.get(index1), vertices.get(index2));
    }

    @Override
    public Edge clone() {
        Edge result = new Edge(index1, index2);
        result.setOrigin(this.getOrigin());
        result.setDirection(this.getDirection());
        return result;
    }

    /**
     * @return the first index of the edge
     */
    public int getFirstIndex() {
        return index1;
    }

    /**
     * @return the second index of the edge
     */
    public int getSecondIndex() {
        return index2;
    }

    /**
     * Shifts indexes by a given amount.
     * @param shift
     *            how much the indexes should be shifted
     * @param predicate
     *            the predicate that verifies which indexes should be shifted; if null then all will be shifted
     */
    public void shiftIndexes(int shift, IndexPredicate predicate) {
        if (predicate == null) {
            index1 += shift;
            index2 += shift;
        } else {
            index1 += predicate.execute(index1) ? shift : 0;
            index2 += predicate.execute(index2) ? shift : 0;
        }
    }

    /**
     * Flips the order of the indexes.
     */
    public void flipIndexes() {
        int temp = index1;
        index1 = index2;
        index2 = temp;
    }

    /**
     * The method sets the vertices for the first and second index.
     * @param v1
     *            the first vertex
     * @param v2
     *            the second vertex
     */
    public void set(Vector3f v1, Vector3f v2) {
        this.setOrigin(v1);
        this.setDirection(v2.subtract(v1));
    }

    /**
     * The crossing method first computes the points on both lines (that contain the edges)
     * who are closest in distance. If the distance between points is smaller than FastMath.FLT_EPSILON
     * the we consider them to be the same point (the lines cross).
     * The second step is to check if both points are contained within the edges.
     * 
     * The method of computing the crossing point is as follows:
     * Let's assume that:
     * (P0, P1) are the points of the first edge
     * (Q0, Q1) are the points of the second edge
     * 
     * u = P1 - P0
     * v = Q1 - Q0
     * 
     * This gives us the equations of two lines:
     * L1: (x = P1x + ux*t1; y = P1y + uy*t1; z = P1z + uz*t1)
     * L2: (x = P2x + vx*t2; y = P2y + vy*t2; z = P2z + vz*t2)
     * 
     * Comparing the x and y of the first two equations for each line will allow us to compute t1 and t2
     * (which is implemented below).
     * Using t1 and t2 we can compute (x, y, z) of each line and that will give us two points that we need to compare.
     * 
     * @param edge
     *            the edge we check against crossing
     * @return <b>true</b> if the edges cross and false otherwise
     */
    public boolean cross(Edge edge) {
        Vector3f P1 = this.getOrigin(), P2 = edge.getOrigin();
        Vector3f u = this.getDirection();
        Vector3f v = edge.getDirection();
        float t2 = (u.x * (P2.y - P1.y) - u.y * (P2.x - P1.x)) / (u.y * v.x - u.x * v.y);
        float t1 = (P2.x - P1.x + v.x * t2) / u.x;
        Vector3f p1 = P1.add(u.mult(t1));
        Vector3f p2 = P2.add(v.mult(t2));

        if (p1.distance(p2) <= FastMath.FLT_EPSILON) {
            // the lines cross, check if p1 and p2 are within the edges
            Vector3f p = p1.subtract(P1);
            float cos = p.dot(u) / (p.length() * u.length());
            if (cos > 0 && p.length() <= u.length()) {
                // p1 is inside the first edge, lets check the other edge now
                p = p2.subtract(P2);
                cos = p.dot(v) / (p.length() * v.length());
                return cos > 0 && p.length() <= u.length();
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String result = "Edge [" + index1 + ", " + index2 + "]";
        if (this.getOrigin() != null && this.getDirection() != null) {
            result += " -> {" + this.getOrigin() + ", " + this.getOrigin().add(this.getDirection()) + "}";
        }
        return result;
    }

    @Override
    public int hashCode() {
        // The hash code must be identical for the same two indexes, no matter their order.
        final int prime = 31;
        int result = 1;
        int lowerIndex = Math.min(index1, index2);
        int higherIndex = Math.max(index1, index2);
        result = prime * result + lowerIndex;
        result = prime * result + higherIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Edge)) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        Edge other = (Edge) obj;
        return Math.min(index1, index2) == Math.min(other.index1, other.index2) && Math.max(index1, index2) == Math.max(other.index1, other.index2);
    }

    /**
     * The method loads all edges from the given mesh structure that does not belong to any face.
     * @param meshStructure
     *            the mesh structure
     * @return all edges without faces
     * @throws BlenderFileException
     *             an exception is thrown when problems with file reading occur
     */
    public static List<Edge> loadAll(Structure meshStructure) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading all edges that do not belong to any face from mesh: {0}", meshStructure.getName());
        List<Edge> result = new ArrayList<Edge>();

        Pointer pMEdge = (Pointer) meshStructure.getFieldValue("medge");

        if (pMEdge.isNotNull()) {
            List<Structure> edges = pMEdge.fetchData();
            for (Structure edge : edges) {
                int flag = ((Number) edge.getFieldValue("flag")).intValue();
                if ((flag & MeshHelper.EDGE_NOT_IN_FACE_FLAG) != 0) {
                    int v1 = ((Number) edge.getFieldValue("v1")).intValue();
                    int v2 = ((Number) edge.getFieldValue("v2")).intValue();
                    result.add(new Edge(v1, v2));
                }
            }
        }
        LOGGER.log(Level.FINE, "Loaded {0} edges.", result.size());
        return result;
    }
}
