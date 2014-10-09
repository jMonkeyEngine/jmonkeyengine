package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.file.BlenderFileException;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.meshes.IndexesLoop.IndexPredicate;

/**
 * A class that represents a single point on the scene that is not a part of an edge.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class Point {
    private static final Logger LOGGER = Logger.getLogger(Point.class.getName());

    /** The point's index. */
    private int                 index;

    /**
     * Constructs a point for a given index.
     * @param index
     *            the index of the point
     */
    public Point(int index) {
        this.index = index;
    }

    @Override
    public Point clone() {
        return new Point(index);
    }

    /**
     * @return the index of the point
     */
    public int getIndex() {
        return index;
    }

    /**
     * The method shifts the index by a given value.
     * @param shift
     *            the value to shift the index
     * @param predicate
     *            the predicate that verifies which indexes should be shifted; if null then all will be shifted
     */
    public void shiftIndexes(int shift, IndexPredicate predicate) {
        if (predicate == null || predicate.execute(index)) {
            index += shift;
        }
    }

    /**
     * Loads all points of the mesh that do not belong to any edge.
     * @param meshStructure
     *            the mesh structure
     * @return a list of points
     * @throws BlenderFileException
     *             an exception is thrown when problems with file reading occur
     */
    public static List<Point> loadAll(Structure meshStructure) throws BlenderFileException {
        LOGGER.log(Level.FINE, "Loading all points that do not belong to any edge from mesh: {0}", meshStructure.getName());
        List<Point> result = new ArrayList<Point>();

        Pointer pMEdge = (Pointer) meshStructure.getFieldValue("medge");
        if (pMEdge.isNotNull()) {
            int count = ((Number) meshStructure.getFieldValue("totvert")).intValue();
            Set<Integer> unusedVertices = new HashSet<Integer>(count);
            for (int i = 0; i < count; ++i) {
                unusedVertices.add(i);
            }

            List<Structure> edges = pMEdge.fetchData();
            for (Structure edge : edges) {
                unusedVertices.remove(((Number) edge.getFieldValue("v1")).intValue());
                unusedVertices.remove(((Number) edge.getFieldValue("v2")).intValue());
            }

            for (Integer unusedIndex : unusedVertices) {
                result.add(new Point(unusedIndex));
            }
        }
        LOGGER.log(Level.FINE, "Loaded {0} points.", result.size());
        return result;
    }
}
