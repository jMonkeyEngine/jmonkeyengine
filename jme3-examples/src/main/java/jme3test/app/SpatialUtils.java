package jme3test.app;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Utility class for working with spatial hierarchies.
 * Provides helper methods for searching and manipulating spatial scene graphs.
 */
public class SpatialUtils {

    /**
     * Recursively finds the first Geometry in a spatial hierarchy.
     * This is useful for handling both simple flat structures (Ogre models) and
     * complex nested node trees (glTF models).
     *
     * @param spatial The root spatial to search from
     * @return The first Geometry found, or null if none exists
     */
    public static Geometry findFirstGeometry(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return (Geometry) spatial;
        }
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                Geometry geom = findFirstGeometry(child);
                if (geom != null) {
                    return geom;
                }
            }
        }
        return null;
    }

    /**
     * Recursively finds a Geometry by name in a spatial hierarchy.
     * Searches depth-first through the entire spatial tree.
     *
     * @param spatial The root spatial to search from
     * @param name The name of the geometry to find
     * @return The Geometry with the matching name, or null if not found
     */
    public static Geometry findGeometryByName(Spatial spatial, String name) {
        if (spatial.getName() != null && spatial.getName().equals(name) && spatial instanceof Geometry) {
            return (Geometry) spatial;
        }
        if (spatial instanceof Node) {
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                Geometry geom = findGeometryByName(child, name);
                if (geom != null) {
                    return geom;
                }
            }
        }
        return null;
    }

    /**
     * Counts the total number of Geometries in a spatial hierarchy.
     * Useful for debugging and understanding scene structure.
     *
     * @param spatial The root spatial to count from
     * @return The total number of geometries in the hierarchy
     */
    public static int countGeometries(Spatial spatial) {
        if (spatial instanceof Geometry) {
            return 1;
        }
        if (spatial instanceof Node) {
            int count = 0;
            Node node = (Node) spatial;
            for (Spatial child : node.getChildren()) {
                count += countGeometries(child);
            }
            return count;
        }
        return 0;
    }

    private SpatialUtils() {
        // Utility class, no instantiation
    }
}
