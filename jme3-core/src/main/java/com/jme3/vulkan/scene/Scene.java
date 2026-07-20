package com.jme3.vulkan.scene;

import java.util.BitSet;
import java.util.Iterator;

/**
 * A scene of spatials trees optimized for traversal and data retrieval.
 */
public class Scene {

    private static final int INDEX = 0;
    private static final int PARENT = 1;
    private static final int SIBLING = 2;
    private static final int FIRST_CHILD = 3;
    private static final int NODE_SIZE = 4;
    private static final int NULL = -NODE_SIZE;

    private int[] nodes, order;
    private Node[] nodeData;
    private final BitSet usedOrderSlots = new BitSet();
    private final BitSet usedNodeSlots = new BitSet();

    public Scene() {
        this(64);
    }

    public Scene(int initialArraySize) {
        nodes = new int[initialArraySize * NODE_SIZE];
        order = new int[initialArraySize];
        nodeData = new Node[initialArraySize];
    }

    private static int[] grow(int[] array) {
        int[] temp = new int[array.length << 1];
        System.arraycopy(array, 0, temp, 0, array.length);
        return temp;
    }

    /**
     * Gets the data array index of {@code node}. The data array index never
     * changes for a node while it exists.
     *
     * @param node node id
     * @return data array index
     */
    public int getNodeDataIndex(int node) {
        return node / NODE_SIZE;
    }

    /**
     * Gets the index of {@code node} within the scene. Children always have a higher
     * index than their parent. Nodes that are not attached to the scene have a null
     * scene order index.
     *
     * @param node node id
     * @return scene order index
     */
    public int getSceneOrderIndex(int node) {
        return nodes[node];
    }

    /**
     * Gets the node described by {@code orderIndex} as a scene order index, or null.
     *
     * @param orderIndex scene order index
     * @return node at {@code orderIndex}, or null if no node exists at that point
     */
    public int getNodeAtSceneOrderIndex(int orderIndex) {
        if (usedOrderSlots.get(orderIndex)) return order[orderIndex];
        else return NULL;
    }

    /**
     * Gets the parent of {@code node}, or a null id.
     *
     * @param node node id
     * @return parent id, or null if {@code node} has no parent
     */
    public int getParentId(int node) {
        return nodes[node + PARENT];
    }

    /**
     * Gets the id of the next sibling in line from {@code node}, or a null id.
     *
     * @param node node id
     * @return next sibling id, or null if there is no sibling next
     */
    public int getSiblingId(int node) {
        return nodes[node + SIBLING];
    }

    /**
     * Gets the first child of {@code node}, or a null id.
     *
     * @param node node id
     * @return first child id, or null if {@code node} has no children
     */
    public int getFirstChildId(int node) {
        return nodes[node + FIRST_CHILD];
    }

    /**
     * Returns true if {@code node} is attached to the scene. A node is
     * attached if it is a scene root or is a descendent of a scene root.
     *
     * @param node node id
     * @return true if {@code node} is attached
     */
    public boolean isAttached(int node) {
        return nodes[node] >= 0;
    }

    /**
     * Returns true if {@code node} is a scene root.
     *
     * @param node node id
     * @return true if {@code node} is a scene root
     */
    public boolean isRoot(int node) {
        return nodes[node] >= 0 && nodes[node + PARENT] < 0;
    }

    /**
     * Returns true if {@code node} is a child of another node.
     *
     * @param node node id
     * @return true if {@code node} is a child
     */
    public boolean isChild(int node) {
        return nodes[node + PARENT] >= 0;
    }

    /**
     * Returns true if {@code node} has at least one child.
     *
     * @param node node id
     * @return true if {@code node} has a child
     */
    public boolean hasChildren(int node) {
        return nodes[node + FIRST_CHILD] >= 0;
    }

    /**
     * Returns true if {@code id} is a null id or index.
     *
     * @param id id to test
     * @return true if {@code id} is null
     */
    public static boolean isNullId(int id) {
        return id < 0;
    }

    /**
     * Creates a new blank node that is not attached to the scene.
     *
     * @return id of created node
     */
    public int createNode() {
        int id = usedNodeSlots.nextClearBit(0);
        usedNodeSlots.set(id);
        id *= NODE_SIZE;
        if (id + NODE_SIZE >= nodes.length) {
            nodes = grow(nodes);
        }
        nodes[id] = NULL; // not attached to a root node
        nodes[id + PARENT] = NULL;
        nodes[id + SIBLING] = NULL;
        nodes[id + FIRST_CHILD] = NULL;
        return id;
    }

    /**
     * Destroys {@code node}. All children of {@code node} are detached from {@code node}
     * and the scene but are not destroyed.
     *
     * @param node id of node to destroy
     */
    public void destroyNode(int node) {
        detach(node);
        for (int i = nodes[node + FIRST_CHILD]; i >= 0;) {
            int next = nodes[i + SIBLING];
            nodes[i + PARENT] = NULL;
            nodes[i + SIBLING] = NULL;
            i = next;
        }
        usedNodeSlots.clear(node);
    }

    /**
     * Destroys {@code node} and all descendents of {@code node}.
     *
     * @param node id of tree source node
     */
    public void destroyTree(int node) {
        detach(node);
        for (int i : depthFirst(node)) {
            usedNodeSlots.clear(i);
        }
    }

    /**
     * Froms a parent-child relationship between {@code parent} and {@code child}, if
     * it does not already exist.
     *
     * @param parent id of node to attach to
     * @param child id of node to be attached
     */
    public void attachChild(int parent, int child) {
        if (nodes[child + PARENT] == parent) {
            return;
        }
        detach(child);
        int firstChild = nodes[parent + FIRST_CHILD];
        if (firstChild >= 0) {
            nodes[child + SIBLING] = firstChild;
        }
        nodes[parent + FIRST_CHILD] = child;
        nodes[child + PARENT] = parent;
        if (nodes[parent] >= 0) for (int i : depthFirst(child)) {
            nodes[i] = usedOrderSlots.nextClearBit(nodes[nodes[i + PARENT]] + 1);
            usedOrderSlots.set(nodes[i]);
            while (nodes[i] >= order.length) {
                order = grow(order);
            }
            order[nodes[i]] = i;
        }
    }

    /**
     * Attaches {@code node} to the scene as a root.
     *
     * @param node id of node to attach
     */
    public void attachRoot(int node) {
        if (nodes[node + PARENT] >= 0) {
            detachChildToRoot(node);
            return;
        }
        if (nodes[node] >= 0) {
            return;
        }
        for (int i : depthFirst(node)) {
            int index = nodes[i] = usedOrderSlots.nextClearBit(nodes[nodes[i + PARENT]] + 1);
            usedOrderSlots.set(index);
            while (index >= order.length) {
                order = grow(order);
            }
            order[index] = i;
        }
    }

    /**
     * Removes the parent-child connection of {@code child}. If {@code child} was a child
     * of another node, {@code child} becomes a scene root.
     *
     * @param child id of node to detach
     */
    public void detachChildToRoot(int child) {
        int parent = nodes[child + PARENT];
        if (parent < 0) {
            return;
        }
        int prev = nodes[parent + FIRST_CHILD];
        if (child == prev) {
            nodes[parent + FIRST_CHILD] = nodes[prev + SIBLING];
        } else for (int i = nodes[prev + SIBLING]; i >= 0; prev = i, i = nodes[i + SIBLING]) {
            if (child == i) {
                nodes[prev + SIBLING] = nodes[i + SIBLING];
                break;
            }
        }
        nodes[child + PARENT] = NULL;
        nodes[child + SIBLING] = NULL;
    }

    /**
     * Detaches {@code node} from its parent and the scene.
     *
     * @param node id of node to detach
     */
    public void detach(int node) {
        detachChildToRoot(node);
        if (nodes[node] >= 0) for (int i : depthFirst(node)) {
            usedOrderSlots.clear(nodes[i]);
            nodes[i] = NULL; // not attached to a root node
        }
    }

    /**
     * Detaches all children of {@code parent} from {@code parent} and the scene.
     *
     * @param parent id of parent node
     */
    public void detachAllChildren(int parent) {
        for (int i = nodes[parent + FIRST_CHILD]; i >= 0;) {
            int next = nodes[i + SIBLING];
            nodes[i + PARENT] = NULL;
            nodes[i + SIBLING] = NULL;
            detach(i);
            i = next;
        }
        nodes[parent + FIRST_CHILD] = NULL;
    }

    /**
     * Creates an iterable that iterates over each child of {@code parent}.
     *
     * @param parent id of parent node
     * @return child iterable
     */
    public ChildIterator children(int parent) {
        return new ChildIterator(parent);
    }

    /**
     * Creates an iterable that iterates over each descendent of {@code origin}
     * in a depth-first visitation pattern.
     *
     * @param origin id of origin node
     * @return depth-first iterable
     */
    public DepthFirstIterator depthFirst(int origin) {
        return new DepthFirstIterator(origin, false);
    }

    /**
     * Creates an iterable that iterates over each descendent of {@code origin}
     * in a depth-first visitation pattern.
     *
     * @param origin id of origin node
     * @param skipOrigin true to have the iterator skip the origin node
     * @return depth-first iterable
     */
    public DepthFirstIterator depthFirst(int origin, boolean skipOrigin) {
        return new DepthFirstIterator(origin, skipOrigin);
    }

    /**
     * Creates an iterable that iterates over each node attached to the scene, where all
     * nodes are guaranteed to be visited after their parent.
     *
     * @return ordered scene iterable
     */
    public OrderedSceneIterator orderedScene() {
        return new OrderedSceneIterator();
    }

    public class ChildIterator implements Iterable<Integer>, Iterator<Integer> {

        private final int parent;
        private final int[] queue = {NULL, NULL, NULL};

        private ChildIterator(int parent) {
            this.parent = parent;
            queue[2] = nodes[parent + FIRST_CHILD];
        }

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return queue[2] >= 0;
        }

        @Override
        public Integer next() {
            queue[0] = queue[1];
            queue[1] = queue[2];
            queue[2] = nodes[queue[2] + SIBLING];
            return queue[1];
        }

        @Override
        public void remove() {
            removeCurrentFromChain();
            detach(queue[1]);
            queue[1] = queue[0];
        }

        private void removeCurrentFromChain() {
            if (queue[0] < 0) {
                nodes[parent + FIRST_CHILD] = queue[2];
            } else {
                nodes[queue[0] + SIBLING] = queue[2];
            }
            nodes[queue[1] + PARENT] = NULL;
            nodes[queue[1] + SIBLING] = NULL;
        }

        public void destroy() {
            removeCurrentFromChain();
            destroyNode(queue[1]);
            queue[1] = queue[0];
        }

        public void destroyTree() {
            removeCurrentFromChain();
            Scene.this.destroyTree(queue[1]);
            queue[1] = queue[0];
        }

    }

    public class DepthFirstIterator implements Iterable<Integer>, Iterator<Integer> {

        private final int origin;
        private int next;
        private int current = NULL;
        private int nextDepth = 0;
        private int currentDepth = -1;

        private DepthFirstIterator(int origin, boolean skipOrigin) {
            this.origin = origin;
            this.next = origin;
            if (skipOrigin && hasNext()) {
                next();
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return next >= 0;
        }

        @Override
        public Integer next() {
            current = next;
            currentDepth = nextDepth;
            int child = nodes[next + FIRST_CHILD];
            if (child >= 0) {
                next = child;
                nextDepth++;
            } else {
                next = nextSibling(next);
            }
            return current;
        }

        @Override
        public void remove() {
            skipChildren();
            detach(current);
        }

        private int nextSibling(int node) {
            nextDepth = currentDepth;
            while (node != origin && node >= 0) {
                int sib = nodes[node + SIBLING];
                if (sib >= 0) return sib;
                node = nodes[node + PARENT];
                nextDepth--;
            }
            return NULL;
        }

        public void skipChildren() {
            next = nextSibling(current);
        }

        public void destroy() {
            skipChildren();
            destroyNode(current);
        }

        public void destroyTree() {
            skipChildren();
            Scene.this.destroyTree(current);
        }

        public int getDepth() {
            return currentDepth;
        }

    }

    public class OrderedSceneIterator implements Iterable<Integer>, Iterator<Integer> {

        private int nextIndex = usedOrderSlots.nextSetBit(0);
        private int currentIndex = NULL;

        private OrderedSceneIterator() {}

        @Override
        public Iterator<Integer> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return nextIndex >= 0;
        }

        @Override
        public Integer next() {
            currentIndex = nextIndex;
            computeNextIndex();
            return order[currentIndex];
        }

        @Override
        public void remove() {
            detach(order[currentIndex]);
            computeNextIndex();
        }

        public void computeNextIndex() {
            nextIndex = usedOrderSlots.nextSetBit(currentIndex + 1);
        }

        public void destroy() {
            destroyNode(order[currentIndex]);
            computeNextIndex();
        }

        public void destroyTree() {
            Scene.this.destroyTree(order[currentIndex]);
            computeNextIndex();
        }

    }

}
