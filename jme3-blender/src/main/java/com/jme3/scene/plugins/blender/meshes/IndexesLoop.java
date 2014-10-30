package com.jme3.scene.plugins.blender.meshes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.scene.plugins.blender.file.BlenderFileException;

/**
 * This class represents the Face's indexes loop. It is a simplified implementation of directed graph.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class IndexesLoop implements Comparator<Integer>, Iterable<Integer> {
    public static final IndexPredicate  INDEX_PREDICATE_USE_ALL = new IndexPredicate() {
                                                                    @Override
                                                                    public boolean execute(Integer index) {
                                                                        return true;
                                                                    }
                                                                };

    /** The indexes. */
    private List<Integer>               nodes;
    /** The edges of the indexes graph. The key is the 'from' index and 'value' is - 'to' index. */
    private Map<Integer, List<Integer>> edges                   = new HashMap<Integer, List<Integer>>();

    /**
     * The constructor uses the given nodes in their give order. Each neighbour indexes will form an edge.
     * @param nodes
     *            the nodes for the loop
     */
    public IndexesLoop(Integer[] nodes) {
        this.nodes = new ArrayList<Integer>(Arrays.asList(nodes));
        this.prepareEdges(this.nodes);
    }

    @Override
    public IndexesLoop clone() {
        return new IndexesLoop(nodes.toArray(new Integer[nodes.size()]));
    }

    /**
     * The method prepares edges for the given indexes.
     * @param nodes
     *            the indexes
     */
    private void prepareEdges(List<Integer> nodes) {
        for (int i = 0; i < nodes.size() - 1; ++i) {
            if (edges.containsKey(nodes.get(i))) {
                edges.get(nodes.get(i)).add(nodes.get(i + 1));
            } else {
                edges.put(nodes.get(i), new ArrayList<Integer>(Arrays.asList(nodes.get(i + 1))));
            }
        }
        edges.put(nodes.get(nodes.size() - 1), new ArrayList<Integer>(Arrays.asList(nodes.get(0))));
    }

    /**
     * @return the count of indexes
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Adds edge to the loop.
     * @param from
     *            the start index
     * @param to
     *            the end index
     */
    public void addEdge(Integer from, Integer to) {
        if (nodes.contains(from) && nodes.contains(to)) {
            if (edges.containsKey(from) && !edges.get(from).contains(to)) {
                edges.get(from).add(to);
            }
        }
    }

    /**
     * Removes edge from the face. The edge is removed if it already exists in the face.
     * @param node1
     *            the first index of the edge to be removed
     * @param node2
     *            the second index of the edge to be removed
     * @return <b>true</b> if the edge was removed and <b>false</b> otherwise
     */
    public boolean removeEdge(Integer node1, Integer node2) {
        boolean edgeRemoved = false;
        if (nodes.contains(node1) && nodes.contains(node2)) {
            if (edges.containsKey(node1)) {
                edgeRemoved |= edges.get(node1).remove(node2);
            }
            if (edges.containsKey(node2)) {
                edgeRemoved |= edges.get(node2).remove(node1);
            }
            if (edgeRemoved) {
                if (this.getNeighbourCount(node1) == 0) {
                    this.removeIndexes(node1);
                }
                if (this.getNeighbourCount(node2) == 0) {
                    this.removeIndexes(node2);
                }
            }
        }
        return edgeRemoved;
    }

    /**
     * Tells if the given indexes are neighbours.
     * @param index1
     *            the first index
     * @param index2
     *            the second index
     * @return <b>true</b> if the given indexes are neighbours and <b>false</b> otherwise
     */
    public boolean areNeighbours(Integer index1, Integer index2) {
        if (index1.equals(index2) || !edges.containsKey(index1) || !edges.containsKey(index2)) {
            return false;
        }
        return edges.get(index1).contains(index2) || edges.get(index2).contains(index1);
    }

    /**
     * Returns the value of the index located after the given one. Pointint the last index will return the first one.
     * @param index
     *            the index value
     * @return the value of 'next' index
     */
    public Integer getNextIndex(Integer index) {
        int i = nodes.indexOf(index);
        return i == nodes.size() - 1 ? nodes.get(0) : nodes.get(i + 1);
    }

    /**
     * Returns the value of the index located before the given one. Pointint the first index will return the last one.
     * @param index
     *            the index value
     * @return the value of 'previous' index
     */
    public Integer getPreviousIndex(Integer index) {
        int i = nodes.indexOf(index);
        return i == 0 ? nodes.get(nodes.size() - 1) : nodes.get(i - 1);
    }

    /**
     * The method shifts all indexes by a given value.
     * @param shift
     *            the value to shift all indexes
     * @param predicate
     *            the predicate that verifies which indexes should be shifted; if null then all will be shifted
     */
    public void shiftIndexes(int shift, IndexPredicate predicate) {
        if (predicate == null) {
            predicate = INDEX_PREDICATE_USE_ALL;
        }
        List<Integer> nodes = new ArrayList<Integer>(this.nodes.size());
        for (Integer node : this.nodes) {
            nodes.add(node + (predicate.execute(node) ? shift : 0));
        }

        Map<Integer, List<Integer>> edges = new HashMap<Integer, List<Integer>>();
        for (Entry<Integer, List<Integer>> entry : this.edges.entrySet()) {
            List<Integer> neighbours = new ArrayList<Integer>(entry.getValue().size());
            for (Integer neighbour : entry.getValue()) {
                neighbours.add(neighbour + (predicate.execute(neighbour) ? shift : 0));
            }
            edges.put(entry.getKey() + shift, neighbours);
        }

        this.nodes = nodes;
        this.edges = edges;
    }

    /**
     * Reverses the order of the indexes.
     */
    public void reverse() {
        Collections.reverse(nodes);
        edges.clear();
        this.prepareEdges(nodes);
    }

    /**
     * Returns the neighbour count of the given index.
     * @param index
     *            the index whose neighbour count will be checked
     * @return the count of neighbours of the given index
     */
    private int getNeighbourCount(Integer index) {
        int result = 0;
        if (edges.containsKey(index)) {
            result = edges.get(index).size();
            for (List<Integer> neighbours : edges.values()) {
                if (neighbours.contains(index)) {
                    ++result;
                }
            }
        }
        return result;
    }

    /**
     * Returns the position of the given index in the loop.
     * @param index
     *            the index of the face
     * @return the indexe's position in the loop
     */
    public int indexOf(Integer index) {
        return nodes.indexOf(index);
    }

    /**
     * Returns the index at the given position.
     * @param indexPosition
     *            the position of the index
     * @return the index at a given position
     */
    public Integer get(int indexPosition) {
        return nodes.get(indexPosition);
    }

    /**
     * @return all indexes of the face
     */
    public List<Integer> getAll() {
        return new ArrayList<Integer>(nodes);
    }

    /**
     * The method removes all given indexes.
     * @param indexes
     *            the indexes to be removed
     */
    public void removeIndexes(Integer... indexes) {
        for (Integer index : indexes) {
            nodes.remove(index);
            edges.remove(index);
            for (List<Integer> neighbours : edges.values()) {
                neighbours.remove(index);
            }
        }
    }

    /**
     * The method finds the path between the given indexes.
     * @param start
     *            the start index
     * @param end
     *            the end index
     * @param result
     *            a list containing indexes on the path from start to end (inclusive)
     * @throws IllegalStateException
     *             an exception is thrown when the loop is not normalized (at least one
     *             index has more than 2 neighbours)
     * @throws BlenderFileException
     *             an exception is thrown if the vertices of a face create more than one loop; this is thrown
     *             to prevent lack of memory errors during triangulation
     */
    public void findPath(Integer start, Integer end, List<Integer> result) throws BlenderFileException {
        result.clear();
        Integer node = start;
        while (!node.equals(end)) {
            if (result.contains(node)) {
                throw new BlenderFileException("Indexes of face have infinite loops!");
            }
            result.add(node);
            List<Integer> nextSteps = edges.get(node);
            if (nextSteps == null || nextSteps.size() == 0) {
                result.clear();// no directed path from start to end
                return;
            } else if (nextSteps.size() == 1) {
                node = nextSteps.get(0);
            } else {
                throw new BlenderFileException("Triangulation failed. Face has ambiguous indexes loop. Please triangulate your model in Blender as a workaround.");
            }
        }
        result.add(end);
    }

    @Override
    public String toString() {
        return "IndexesLoop " + nodes.toString();
    }

    @Override
    public int compare(Integer i1, Integer i2) {
        return nodes.indexOf(i1) - nodes.indexOf(i2);
    }

    @Override
    public Iterator<Integer> iterator() {
        return nodes.iterator();
    }

    public static interface IndexPredicate {
        boolean execute(Integer index);
    }
}
