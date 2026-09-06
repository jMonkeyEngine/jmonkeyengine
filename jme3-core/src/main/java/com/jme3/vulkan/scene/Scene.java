package com.jme3.vulkan.scene;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.util.IntList;
import com.jme3.util.TempVars;
import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.ConcurrentStructArray;
import com.jme3.vulkan.alloc.SlicePointer;
import com.jme3.vulkan.buffer.AutoBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.compile.Final;
import com.jme3.vulkan.compile.FinalWriter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * A scene of spatials trees optimized for traversal and data retrieval.
 */
public class Scene {

    // fast-access node properties
    private static final int ORDER_INDEX = 0;   // type: hierarchy order index
    private static final int PARENT = 1;        // type: node index
    private static final int SIBLING = 2;       // type: node index
    private static final int FIRST_CHILD = 3;   // type: node index
    private static final int FLAGS = 4;
    private static final int HINTS = 5;         // local cull hint, world cull hint, local bucket hint, world bucket hint (1 byte each)
    private static final int UNUSED_NODE_INFO_1 = 6;
    private static final int UNUSED_NODE_INFO_2 = 7;

    private static final int LOCAL_TRANSLATION_X = 0;
    private static final int LOCAL_TRANSLATION_Y = 1;
    private static final int LOCAL_TRANSLATION_Z = 2;
    private static final int LOCAL_ROTATION_X = 3;
    private static final int LOCAL_ROTATION_Y = 4;
    private static final int LOCAL_ROTATION_Z = 5;
    private static final int LOCAL_ROTATION_W = 6;
    private static final int LOCAL_SCALE_X = 7;
    private static final int LOCAL_SCALE_Y = 8;
    private static final int LOCAL_SCALE_Z = 9;
    private static final int WORLD_TRANSFORM = 10;
    private static final int WORLD_MATRIX = 20;
    private static final int BOUNDS_CENTER_X = 36;
    private static final int BOUNDS_CENTER_Y = 37;
    private static final int BOUNDS_CENTER_Z = 38;
    private static final int BOUNDS_EXTENT_X = 39;
    private static final int BOUNDS_EXTENT_Y = 40;
    private static final int BOUNDS_EXTENT_Z = 41;

    // refresh flags
    public static final int TRANSFORM_REFRESH_BIT = 1;
    public static final int BOUNDS_REFRESH_BIT = 1 << 1;
    public static final int LIGHT_REFRESH_BIT = 1 << 2;         // only refresh lights with flag set
    public static final int LIGHT_REFRESH_ALL_BIT = 1 << 3;     // refresh all lights
    public static final int IGNORE_PARENT_TRANSFORM_BIT = 1 << 4;
    public static final int IGNORE_NONLOCAL_LIGHTS_BIT = 1 << 5;
    private static final int CHILD_TRANSFORM_REFRESH_BIT = 1 << 6;
    private static final int CHILD_LIGHT_REFRESH_BIT = 1 << 7;
    private static final int IGNORE_PARENT_COMPONENTS_BIT = 1 << 8;

    private static final int INFO_SIZE = 8;
    private static final int TRANSFORMS_SIZE = 42;

    private static final int NULL = -1000;
    private static final int INHERIT = 0;

    private static final int HINT_MASK = 0xFF;
    private static final int LOCAL_CULL_HINT = 0;
    private static final int WORLD_CULL_HINT = 8;
    private static final int LOCAL_BUCKET_HINT = 16;
    private static final int WORLD_BUCKET_HINT = 24;

    private static int info(int node) {
        return node * INFO_SIZE;
    }

    private static int transforms(int node) {
        return node * TRANSFORMS_SIZE;
    }

    private static int localTransform(int node) {
        return transforms(node);
    }

    private static int worldTransform(int node) {
        return node * TRANSFORMS_SIZE + WORLD_TRANSFORM;
    }

    private static int worldMatrix(int node) {
        return node * TRANSFORMS_SIZE + WORLD_MATRIX;
    }

    private static int localTranslation(int node) {
        return transforms(node);
    }

    private static int localRotation(int node) {
        return node * TRANSFORMS_SIZE + LOCAL_ROTATION_X;
    }

    private static int localScale(int node) {
        return node * TRANSFORMS_SIZE + LOCAL_SCALE_X;
    }

    private static int worldTranslation(int node) {
        return node * TRANSFORMS_SIZE + WORLD_TRANSFORM + Transform.TRANSLATION_X;
    }

    private static int worldRotation(int node) {
        return node * TRANSFORMS_SIZE + WORLD_TRANSFORM + Transform.ROTATION_X;
    }

    private static int worldScale(int node) {
        return node * TRANSFORMS_SIZE + WORLD_TRANSFORM + Transform.SCALE_X;
    }

    private static int worldBounds(int node) {
        return node * TRANSFORMS_SIZE + BOUNDS_CENTER_X;
    }

    private static final AtomicInteger nextUserDataId = new AtomicInteger();

    public static int generateUserDataId() {
        return nextUserDataId.getAndIncrement();
    }

    private int[] nodeInfo, hierarchyOrder;
    private final float[] transforms;     // local transform, world transform, world bounds
    private long[] nodeLights;      // manual bitset for local and world lights per node
    private long[] globalLights;    // manual bitset for global lights virtually attached to every root node
    private final Map<Class<? extends SceneComponent>, ComponentArray> components = new HashMap<>();
    private final Map<String, UserDataArray> userData = new HashMap<>();
    private final AutoBuffer<ConcurrentStructArray<Light>> lights;
    private final AutoBuffer<SlicePointer> geometricLightMasks;

    private final BitSet usedOrderSlots = new BitSet();
    private final BitSet usedNodeSlots = new BitSet();
    private final BitSet usedGeometrySlots = new BitSet();
    private final BitSet usedLightSlots = new BitSet();
    private boolean refreshGlobalLights = false;

    public Scene(MemoryAllocator alloc) {
        this(alloc, 512, 256, 64);
    }

    public Scene(MemoryAllocator alloc, int nodeCapacity, int geometryCapacity, int lightCapacity) {
        assert nodeCapacity > 0 && geometryCapacity > 0 && lightCapacity > 0 : "Capacity must be positive.";
        nodeInfo = new int[nodeCapacity * INFO_SIZE];
        transforms = new float[nodeCapacity * TRANSFORMS_SIZE];
        hierarchyOrder = new int[nodeCapacity];
        lights = new AutoBuffer<>(alloc,
                new ConcurrentStructArray<>(lightCapacity, Light::new),
                BufferType.Dynamic, EngineBuffer.Role.Storage);
        geometricLightMasks = new AutoBuffer<>(alloc,
                new SlicePointer(0, 1028), // i'm too tired to figure out the math for size here again
                BufferType.Streaming, EngineBuffer.Role.Storage);
    }

    private static int[] grow(int[] array) {
        int[] temp = new int[array.length << 1];
        System.arraycopy(array, 0, temp, 0, array.length);
        return temp;
    }

    private int getNumLightMaskWords() {
        return 1 + (lights.getStructure().getLength() - 1) / Long.SIZE;
    }

    /**
     * Gets the index of {@code node} within the scene. Children always have a higher
     * index than their parent. Nodes that are not attached to the scene have a null
     * scene order index.
     *
     * @param nodeId node id
     * @return scene order index
     */
    public int getHierarchyOrderIndex(int nodeId) {
        return nodeInfo[nodeId * INFO_SIZE];
    }

    /**
     * Gets the node described by {@code orderIndex} as a scene order index, or null.
     *
     * @param index scene order index
     * @return node at {@code orderIndex}, or null if no node exists at that point
     */
    public int getNodeAtHierarchyOrderIndex(int index) {
        if (usedOrderSlots.get(index)) return hierarchyOrder[index * INFO_SIZE];
        else return NULL;
    }

    /**
     * Gets the parent of {@code node}, or a null id.
     *
     * @param node node id
     * @return parent id, or null if {@code node} has no parent
     */
    public int getParentId(int node) {
        return nodeInfo[node * INFO_SIZE + PARENT];
    }

    /**
     * Gets the id of the next sibling in line from {@code node}, or a null id.
     *
     * @param node node id
     * @return next sibling id, or null if there is no sibling next
     */
    public int getSiblingId(int node) {
        return nodeInfo[node * INFO_SIZE + SIBLING];
    }

    /**
     * Gets the first child of {@code node}, or a null id.
     *
     * @param node node id
     * @return first child id, or null if {@code node} has no children
     */
    public int getFirstChildId(int node) {
        return nodeInfo[node * INFO_SIZE + FIRST_CHILD];
    }

    /**
     * Gets the flag bit mask of {@code node}.
     *
     * @param node node
     * @return flags
     */
    public int getFlags(int node) {
        return nodeInfo[node * INFO_SIZE + FLAGS];
    }

    public int getLocalCullHint(int node) {
        return nodeInfo[node * INFO_SIZE + HINTS] & HINT_MASK;
    }

    public int getWorldCullHint(int node) {
        return (nodeInfo[node * INFO_SIZE + HINTS] >> WORLD_CULL_HINT) & HINT_MASK;
    }

    public int getLocalBucketHint(int node) {
        return (nodeInfo[node * INFO_SIZE + HINTS] >> LOCAL_BUCKET_HINT) & HINT_MASK;
    }

    public int getWorldBucketHint(int node) {
        return (nodeInfo[node * INFO_SIZE + HINTS] >> WORLD_BUCKET_HINT) & HINT_MASK;
    }

    public boolean isIgnoreParentTransform(int node) {
        return (nodeInfo[node * INFO_SIZE + FLAGS] & IGNORE_PARENT_TRANSFORM_BIT) != 0;
    }

    public boolean isIgnoreNonLocalLights(int node) {
        return (nodeInfo[node * INFO_SIZE + FLAGS] & IGNORE_NONLOCAL_LIGHTS_BIT) != 0;
    }

    /**
     * Returns true if {@code node} is attached to the scene. A node is
     * attached if it is a scene root or is a descendent of a scene root.
     *
     * @param node node id
     * @return true if {@code node} is attached
     */
    public boolean isAttached(int node) {
        return nodeInfo[node * INFO_SIZE] >= 0;
    }

    /**
     * Returns true if {@code node} is a scene root.
     *
     * @param node node id
     * @return true if {@code node} is a scene root
     */
    public boolean isRoot(int node) {
        return nodeInfo[node * INFO_SIZE] >= 0 && nodeInfo[node * INFO_SIZE + PARENT] < 0;
    }

    /**
     * Returns true if {@code node} is a child of another node.
     *
     * @param node node id
     * @return true if {@code node} is a child
     */
    public boolean isChild(int node) {
        return nodeInfo[node * INFO_SIZE + PARENT] >= 0;
    }

    /**
     * Returns true if {@code node} has at least one child.
     *
     * @param node node id
     * @return true if {@code node} has a child
     */
    public boolean hasChildren(int node) {
        return nodeInfo[node * INFO_SIZE + FIRST_CHILD] >= 0;
    }

    public boolean hasParent(int node) {
        return nodeInfo[node * INFO_SIZE + PARENT] >= 0;
    }

    public void requireValidNodeId(int node) {
        assert node >= 0 && usedNodeSlots.get(node) : "Invalid node ID: " + node;
    }

    public Vector3f getLocalTranslation(int node, @Nullable Vector3f store) {
        return Vector3f.extract(transforms, localTranslation(node), store);
    }

    public Quaternion getLocalRotation(int node, @Nullable Quaternion store) {
        return Quaternion.extract(transforms, localRotation(node), store);
    }

    public Vector3f getLocalScale(int node, @Nullable Vector3f store) {
        return Vector3f.extract(transforms, localScale(node), store);
    }

    public Transform getLocalTransform(int node, @Nullable Transform store) {
        return Transform.extract(transforms, localTransform(node), store);
    }

    public Vector3f getWorldTranslation(int node, @Nullable Vector3f store) {
        return Vector3f.extract(transforms, worldTranslation(node), store);
    }

    public Quaternion getWorldRotation(int node, @Nullable Quaternion store) {
        return Quaternion.extract(transforms, worldRotation(node), store);
    }

    public Vector3f getWorldScale(int node, @Nullable Vector3f store) {
        return Vector3f.extract(transforms, worldScale(node), store);
    }

    public Transform getWorldTransform(int node, @Nullable Transform store) {
        return Transform.extract(transforms, worldTransform(node), store);
    }

    public BoundingBox getWorldBounds(int node, @Nullable BoundingBox store) {
        return BoundingBox.extractLiteral(transforms, worldBounds(node), store);
    }

    /**
     * Returns true if {@code id} is a null id or index.
     *
     * @param id id to test
     * @return true if {@code id} is null
     */
    public static boolean isNull(int id) {
        return id < 0;
    }

    private void setHierarchyOrderIndex(int node, int index) {
        nodeInfo[node * INFO_SIZE] = index;
    }

    private void setParentId(int node, int parent) {
        nodeInfo[node * INFO_SIZE + PARENT] = parent;
    }

    private void setSiblingId(int node, int sibling) {
        nodeInfo[node * INFO_SIZE + SIBLING] = sibling;
    }

    private void setFirstChildId(int node, int firstChild) {
        nodeInfo[node * INFO_SIZE + FIRST_CHILD] = firstChild;
    }

    public void setFlags(int node, int flags) {
        nodeInfo[node * INFO_SIZE + FLAGS] = flags;
    }

    public void addFlags(int node, int flags) {
        nodeInfo[node * INFO_SIZE + FLAGS] |= flags;
    }

    /**
     * Creates a new blank node that is not attached to the scene.
     *
     * @return id of created node
     */
    public Node createNode() {
        int id = usedNodeSlots.nextClearBit(0);
        usedNodeSlots.set(id);
        int index = id * INFO_SIZE;
        if (index + INFO_SIZE > nodeInfo.length) {
            nodeInfo = grow(nodeInfo);
        }
        setHierarchyOrderIndex(id, NULL); // not attached to a root node
        nodeInfo[index + PARENT] = NULL;
        nodeInfo[index + SIBLING] = NULL;
        nodeInfo[index + FIRST_CHILD] = NULL;
        nodeInfo[index + FLAGS] = TRANSFORM_REFRESH_BIT | BOUNDS_REFRESH_BIT | LIGHT_REFRESH_ALL_BIT;
        nodeInfo[index + HINTS] = 0;
        for (ComponentArray c : components.values()) {
            c.set(id, null);
        }
        for (UserDataArray d : userData.values()) {
            d.set(id, null);
        }
        return new Node(this, id);
    }

    public Node getNode(int id) {
        return new Node(this, id);
    }

    /**
     * Destroys {@code node}. All children of {@code node} are detached from {@code node}
     * and the scene but are not destroyed.
     *
     * @param node id of node to destroy
     */
    public void destroyNode(int node) {
        detach(node);
        for (int i = nodeInfo[node * INFO_SIZE + FIRST_CHILD] * INFO_SIZE; i >= 0;) {
            int next = nodeInfo[i + SIBLING] * INFO_SIZE;
            nodeInfo[i + PARENT] = NULL;
            nodeInfo[i + SIBLING] = NULL;
            nodeInfo[i + FLAGS] = TRANSFORM_REFRESH_BIT | BOUNDS_REFRESH_BIT | LIGHT_REFRESH_ALL_BIT;
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
     * Forms a parent-child relationship between {@code parent} and {@code child}, if
     * it does not already exist.
     *
     * @param parent id of node to attach to
     * @param child id of node to be attached
     */
    public void attachChild(int parent, int child) {
        if (getParentId(child) == parent) {
            return;
        }
        detach(child);
        int firstChild = getFirstChildId(parent);
        if (firstChild >= 0) {
            setSiblingId(child, firstChild);
        }
        setFirstChildId(parent, child);
        setParentId(child, parent);
        if (isAttached(parent)) for (int i : depthFirst(child)) {
            int index = nodeInfo[i * INFO_SIZE] = usedOrderSlots.nextClearBit(nodeInfo[getParentId(i)] + 1);
            usedOrderSlots.set(index);
            while (index >= hierarchyOrder.length) {
                hierarchyOrder = grow(hierarchyOrder);
            }
            hierarchyOrder[index] = i;
        }
        nodeInfo[child * INFO_SIZE + FLAGS] |= TRANSFORM_REFRESH_BIT | LIGHT_REFRESH_ALL_BIT;
        nodeInfo[parent * INFO_SIZE + FLAGS] |= BOUNDS_REFRESH_BIT;
    }

    /**
     * Attaches {@code node} to the scene as a root.
     *
     * @param node id of node to attach
     */
    public void makeRoot(int node) {
        if (hasParent(node)) {
            detachChildToRoot(node);
            return;
        }
        if (isAttached(node)) {
            return;
        }
        for (int i : depthFirst(node)) {
            int order = nodeInfo[i] = usedOrderSlots.nextClearBit(nodeInfo[nodeInfo[i + PARENT]] + 1);
            usedOrderSlots.set(order);
            while (order >= hierarchyOrder.length) {
                hierarchyOrder = grow(hierarchyOrder);
            }
            hierarchyOrder[order] = i;
        }
        nodeInfo[node + FLAGS] |= LIGHT_REFRESH_BIT;
    }

    /**
     * Removes the parent-child connection of {@code child}. If {@code child} was a child
     * of another node, {@code child} becomes a scene root. If {@code child} was not a child
     * of another node, this method does not make {@code child} a scene root.
     *
     * @param child id of node to detach
     * @return true if {@code child} was made a scene root
     */
    public boolean detachChildToRoot(int child) {
        int childInfo = child * INFO_SIZE;
        int parent = nodeInfo[childInfo + PARENT];
        if (parent < 0) {
            return false;
        }
        parent *= INFO_SIZE;
        int prev = nodeInfo[parent + FIRST_CHILD] * INFO_SIZE;
        if (childInfo == prev) {
            nodeInfo[parent + FIRST_CHILD] = nodeInfo[prev + SIBLING];
        } else for (int i = nodeInfo[prev + SIBLING] * INFO_SIZE; i >= 0; prev = i, i = nodeInfo[i + SIBLING] * INFO_SIZE) {
            if (childInfo == i) {
                nodeInfo[prev + SIBLING] = nodeInfo[i + SIBLING];
                break;
            }
        }
        nodeInfo[childInfo + PARENT] = NULL;
        nodeInfo[childInfo + SIBLING] = NULL;
        nodeInfo[childInfo + FLAGS] |= TRANSFORM_REFRESH_BIT | LIGHT_REFRESH_BIT;
        nodeInfo[parent + FLAGS] |= BOUNDS_REFRESH_BIT;
        return true;
    }

    /**
     * Detaches {@code node} from its parent and the scene.
     *
     * @param node id of node to detach
     */
    public void detach(int node) {
        detachChildToRoot(node);
        if (isAttached(node)) for (int i : depthFirst(node)) {
            i *= INFO_SIZE;
            usedOrderSlots.clear(nodeInfo[i]);
            nodeInfo[i] = NULL; // not attached to a root node
        }
    }

    /**
     * Detaches all children of {@code parent} from {@code parent} and the scene.
     *
     * @param parent id of parent node
     */
    public void detachAllChildren(int parent) {
        parent *= INFO_SIZE;
        for (int i = nodeInfo[parent + FIRST_CHILD]; i >= 0;) {
            int next = getSiblingId(i);
            setParentId(i, NULL);
            setSiblingId(i, NULL);
            detach(i);
            i = next;
        }
        nodeInfo[parent + FIRST_CHILD] = NULL;
    }

    /**
     * Forces {@code node} to recompute transforms.
     *
     * @param node node to force
     */
    public void forceTransformRefresh(int node) {
        nodeInfo[node * INFO_SIZE + FLAGS] |= TRANSFORM_REFRESH_BIT;
    }

    /**
     * Forces {@code node} to recompute bounds.
     *
     * @param node node to force
     */
    public void forceBoundsRefresh(int node) {
        nodeInfo[node * INFO_SIZE + FLAGS] |= BOUNDS_REFRESH_BIT;
    }

    /**
     * Forces {@code node} to recompute lights.
     *
     * @param node nodes
     */
    public void forceLightRefresh(int node) {
        nodeInfo[node * INFO_SIZE + FLAGS] |= LIGHT_REFRESH_BIT;
    }

    public void setCullHint(int node, Spatial.CullHint hint) {
        setCullHint(node, hint.ordinal());
    }

    private void setCullHint(int node, int hint) {
        nodeInfo[node * INFO_SIZE + HINTS] = (nodeInfo[node * INFO_SIZE + HINTS] & HINT_MASK) | hint;
    }

    private void setWorldCullHint(int node, int hint) {
        nodeInfo[node * INFO_SIZE + HINTS] = (nodeInfo[node * INFO_SIZE + HINTS] & (HINT_MASK << WORLD_CULL_HINT)) | (hint << WORLD_CULL_HINT);
    }

    public void setBucketHint(int node, int hint) {
        nodeInfo[node * INFO_SIZE + HINTS] = (nodeInfo[node * INFO_SIZE + HINTS] & (HINT_MASK << LOCAL_BUCKET_HINT)) | (hint << LOCAL_BUCKET_HINT);
    }

    private void setWorldBucketHint(int node, int hint) {
        nodeInfo[node * INFO_SIZE + HINTS] = (nodeInfo[node * INFO_SIZE + HINTS] & (HINT_MASK << WORLD_BUCKET_HINT)) | (hint << WORLD_BUCKET_HINT);
    }

    public void setTranslation(int node, Vector3f translation) {
        Vector3f.inject(transforms, localTranslation(node), translation);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void setTranslation(int node, float x, float y, float z) {
        Vector3f.inject(transforms, localTranslation(node), x, y, z);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void move(int node, Vector3f translation) {
        Vector3f.add(transforms, localTranslation(node), transforms, localTranslation(node), translation);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void move(int node, float x, float y, float z) {
        Vector3f.add(transforms, localTranslation(node), transforms, localTranslation(node), x, y, z);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void setRotation(int node, Quaternion rotation) {
        Quaternion.inject(transforms, localRotation(node), rotation);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void rotate(int node, Quaternion rotation) {
        Quaternion.multQuat(transforms, localRotation(node), transforms, localRotation(node), rotation);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void setScale(int node, Vector3f scale) {
        Vector3f.inject(transforms, localScale(node), scale);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void setScale(int node, float x, float y, float z) {
        Vector3f.inject(transforms, localScale(node), x, y, z);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void setScale(int node, float scale) {
        Vector3f.inject(transforms, localScale(node), scale, scale, scale);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void scale(int node, Vector3f scale) {
        Vector3f.mult(transforms, localScale(node), transforms, localScale(node), scale);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void scale(int node, float x, float y, float z) {
        Vector3f.mult(transforms, localScale(node), transforms, localScale(node), x, y, z);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    public void scale(int node, float scalar) {
        Vector3f.mult(transforms, localScale(node), transforms, localScale(node), scalar);
        addFlags(node, TRANSFORM_REFRESH_BIT);
    }

    /**
     * Sets {@code node} to ignore the transform of its parent when computing transforms.
     *
     * @param node node to set
     * @param ignore true to ignore, false to incorporate
     */
    public void setIgnoreParentTransform(int node, boolean ignore) {
        int flags = getFlags(node);
        if (ignore) {
            flags |= IGNORE_PARENT_TRANSFORM_BIT;
        } else {
            flags &= ~IGNORE_PARENT_TRANSFORM_BIT;
        }
        setFlags(node, flags | TRANSFORM_REFRESH_BIT);
    }

    /**
     * Sets {@code node} to ignore global lights and lights from its parent.
     *
     * @param node node to set
     * @param ignore true to ignore, false to incorporate
     */
    public void setIgnoreNonLocalLights(int node, boolean ignore) {
        int flags = getFlags(node);
        if (ignore) {
            flags |= IGNORE_NONLOCAL_LIGHTS_BIT;
        } else {
            flags &= ~IGNORE_NONLOCAL_LIGHTS_BIT;
        }
        setFlags(node, flags | LIGHT_REFRESH_ALL_BIT);
    }

    /**
     * Updates node states that are interdependent on states in other related nodes.
     */
    public void updateStates() {
        for (int i : hierarchyOrdered()) {
            updateTransform(i);
            updateCullHint(i);
            updateBucketHint(i);
        }
        for (int i : hierarchyOrderedReverse()) {
            updateWorldBounds(i);
        }
        for (int i : hierarchyOrdered()) {
            updateLights(i);
        }
        refreshGlobalLights = false;
    }

    private void updateTransform(int id) {
        int flags = getFlags(id);
        int parent = getParentId(id);
        if ((flags & TRANSFORM_REFRESH_BIT) != 0 || (parent >= 0 && (getFlags(parent) & CHILD_TRANSFORM_REFRESH_BIT) != 0)) {
            if (parent >= 0 && (flags & IGNORE_PARENT_TRANSFORM_BIT) == 0) {
                Transform.combineWithParent(
                        transforms, worldTransform(parent),
                        transforms, localTransform(id),
                        transforms, worldTransform(id));
            } else {
                Transform.copy(transforms, localTransform(id), transforms, worldTransform(id));
            }
            Transform.matrix(transforms, worldTransform(id), transforms, worldMatrix(id));
            if (isGeometric(id)) {
                flags |= BOUNDS_REFRESH_BIT;
            }
            flags &= ~TRANSFORM_REFRESH_BIT;
            flags |= CHILD_TRANSFORM_REFRESH_BIT;
        } else {
            flags &= ~CHILD_TRANSFORM_REFRESH_BIT;
        }
        setFlags(id, flags);
    }

    private void updateCullHint(int id) {
        int parent = getParentId(id);
        int localCull = getLocalCullHint(id);
        if (localCull == 0 && parent >= 0) {
            setWorldCullHint(id, getWorldCullHint(parent));
        } else {
            setWorldCullHint(id, localCull);
        }
    }

    private void updateBucketHint(int id) {
        int parent = getParentId(id);
        int localBucket = getLocalBucketHint(id);
        if (localBucket == 0 && parent >= 0) {
            setWorldBucketHint(id, getWorldBucketHint(parent));
        } else {
            setWorldBucketHint(id, localBucket);
        }
    }

    private void updateWorldBounds(int id) {
        int flags = getFlags(id);
        Geometry geom = getComponent(id, Geometry.class);
        if ((flags & BOUNDS_REFRESH_BIT) != 0 || ) {
            BoundingBox.makeNull(transforms, worldBounds(id)); // merging with a null box will just copy the non-null box
            if (isGeometric(id)) {
                //vol = mesh.getBounds().transform(worldTransform, worldBounds);
                BoundingBox.inject(transforms, worldBounds(id), geom.getMesh().getBounds());
                BoundingBox.transform(transforms, worldTransform(id), transforms, worldBounds(id), transforms, worldBounds(id));
            }
            for (int c : children(id)) {
                BoundingBox.merge(transforms, worldBounds(c), transforms, worldBounds(id), transforms, worldBounds(id));
            }
            int parent = nodeInfo[id + PARENT];
            if (parent >= 0) {
                nodeInfo[parent + FLAGS] |= BOUNDS_REFRESH_BIT;
            }
            flags &= ~BOUNDS_REFRESH_BIT;
            flags |= LIGHT_REFRESH_ALL_BIT; // bound changed, recompute culling for all lights
        }
        setFlags(id, flags);
    }

    private void updateLights(int id) {
        int flags = getFlags(id);
        int parent = getParentId(id);
        boolean boundsCheckAll = (flags & LIGHT_REFRESH_ALL_BIT) != 0;
        boolean includeNonLocal = (flags & IGNORE_NONLOCAL_LIGHTS_BIT) == 0;

        // evaluate lights for culling
        if (boundsCheckAll || (flags & LIGHT_REFRESH_BIT) != 0 || (includeNonLocal
                // parent is signaling that one of its world lights requires refresh
                && ((parent >= 0 && (getFlags(parent) & CHILD_LIGHT_REFRESH_BIT) != 0)
                // root only: explicitly check for global light update
                || (parent < 0 && refreshGlobalLights)))) {
            int words = getNumLightMaskWords();
            int localLightIndex = id * words * 2;
            int parentWorldLights = parent * words * 2 + words;
            TempVars vars = TempVars.get();
            for (int i = 0; i < words; i++) {
                long mask = nodeLights[localLightIndex + i];
                if (includeNonLocal) {
                    mask |= (parent >= 0 ? nodeLights[parentWorldLights + i] : globalLights[i]);
                }
                for (int j = Long.numberOfTrailingZeros(mask); j < Long.SIZE; j = Long.numberOfTrailingZeros((mask >> (j + 1)) + j + 1)) {
                    Light l = lights.getStructure().index(j + i * Long.SIZE);
                    if (l.needsBoundsCheck || boundsCheckAll) switch (l.getType()) {
                        case 1: { // point
                            vars.bsphere.setRadius(l.getRadius());
                            vars.bsphere.setCenter(l.getPosition());
                            if (!BoundingBox.intersects(transforms, worldBounds(id), vars.bsphere)) {
                                mask &= ~(1L << j);
                            }
                        } break;
                        case 2: { // spot
                            // fast to compute but often includes nodes that are clearly not influenced
                            vars.plane.setOriginNormal(l.getPosition(), l.getDirection());
                            if (BoundingBox.side(transforms, worldBounds(i), vars.plane) == Plane.Side.Negative) {
                                mask &= ~(1L << j);
                            }
                        } break;
                        // ambient and directional lights are never culled
                    }
                }
                // write world lights to device
                int geom = getGeometry(id);
                if (geom >= 0) {
                    geometricLightMasks.cache().offset(geom * words * Long.BYTES).put(mask);
                }
                nodeLights[localLightIndex + words + i] = mask;
            }
            vars.release();
            flags |= CHILD_LIGHT_REFRESH_BIT;
            flags &= ~(LIGHT_REFRESH_BIT | LIGHT_REFRESH_ALL_BIT);
        } else {
            flags &= ~CHILD_LIGHT_REFRESH_BIT;
        }
        nodeInfo[id + FLAGS] = flags;
    }

    /**
     * Sets the local component of {@code node} according to the type of {@code component}.
     *
     * @param node node to set
     * @param component component to use
     */
    public void setComponent(int node, @NonNull SceneComponent component) {
        components.computeIfAbsent(component.getClass(), k ->
                new ComponentArray(k.getAnnotation(ComponentInheritance.class))).set(node, component);
    }

    /**
     * Clears the local component of {@code type} in {@code node}.
     *
     * @param node node to clear the component of
     * @param type type of component to clear
     */
    public void clearComponent(int node, Class<? extends SceneComponent> type) {
        ComponentArray array = components.get(type);
        if (array != null) {
            array.set(node, null);
        }
    }

    /**
     * Initializes the component array of {@code type} if it has not already been initialized.
     * This can be used to override the presence (or lack) of the {@link ComponentInheritance} annotation
     * on the component type.
     *
     * @param type type to initialize
     * @param allowInheritance true to allow inheritance for the component type
     */
    public void initComponent(Class<? extends SceneComponent> type, boolean allowInheritance) {
        components.computeIfAbsent(type, k -> new ComponentArray(allowInheritance));
    }

    /**
     * Gets the local component of {@code node} set by {@link #setComponent(int, SceneComponent)}.
     *
     * @param node node to get the component of
     * @param type type of component to get
     * @return local component of {@code node}, or null if it does not exist
     * @param <T> component type
     */
    @SuppressWarnings("unchecked")
    public <T extends SceneComponent> T getLocalComponent(int node, Class<T> type) {
        ComponentArray array = components.get(type);
        return array != null ? (T)array.getLocal(node) : null;
    }

    /**
     * Gets the world component of {@code node} computed from the local component of
     * {@code node} and the world component of the node's parent (if any).
     *
     * @param node node to get the component of
     * @param type type of component to get
     * @return world component of {@code node}, or null if it does not exist
     * @param <T> component type
     */
    @SuppressWarnings("unchecked")
    public <T extends SceneComponent> T getComponent(int node, Class<T> type) {
        ComponentArray array = components.get(type);
        return array != null ? (T)array.getWorld(node) : null;
    }

    /**
     * Sets the userdata of {@code node} under {@code name}. Userdata should be reserved
     * for marking a handful of nodes (one node is optimal). If many nodes need to use the same
     * key or inheritance is required, consider using {@link #setComponent(int, SceneComponent)}
     * instead.
     *
     * @param name userdata key
     * @param node node to set the userdata of
     * @param value value to assign
     */
    public void setUserData(String name, int node, @Nullable Object value) {
        userData.computeIfAbsent(name, k -> new UserDataArray()).set(node, value);
    }

    /**
     * Gets the userdata of {@code node} under {@code name}.
     *
     * @param name userdata key
     * @param node node to get the userdata of
     * @return the assigned value, or null
     * @param <T> value type to return
     */
    @SuppressWarnings("unchecked")
    public <T> T getUserData(String name, int node) {
        UserDataArray array = userData.get(name);
        return array != null ? (T)array.get(node) : null;
    }

    /**
     * Creates a subset of this scene where each node in the subset satisfies {@code filter}
     *
     * @param filter filters nodes to be in the subset
     * @return subset of this scene
     */
    public Subset createSubset(Predicate<Integer> filter) {
        Subset subset = new Subset();
        for (int i : unordered()) {
            if (filter.test(i)) {
                subset.members.set(i);
            }
        }
        return subset;
    }

    /**
     * Creates a subset of this scene where each node in the subset satisfies {@code filter}
     * and all its ancestors are also in the subset.
     *
     * @param filter node filter
     * @return subset
     */
    public Subset createHierarchalSubset(Predicate<Integer> filter) {
        Subset subset = new Subset();
        for (int i : hierarchyOrdered()) {
            int parent = getParentId(i);
            if ((parent < 0 || subset.members.get(parent)) && filter.test(i)) {
                subset.members.set(i);
            }
        }
        return subset;
    }

    public Subset createFullSubset() {
        Subset set = new Subset();
        set.members.or(usedNodeSlots);
        return set;
    }

    public Subset createEmptySubset() {
        return new Subset();
    }

    public Light createLight(CommandBuffer cmd) {
        int i = usedLightSlots.nextClearBit(0);
        usedLightSlots.set(i);
        if (i >= lights.getStructure().getLength()) {
            lights.update(cmd, BufferType.Dynamic, EngineBuffer.Role.Storage, OpLocation.PreferHost);
        }
        return lights.getStructure().index(i);
    }

    public void destroyLight(Light light) {
        if (!usedLightSlots.get(light.index)) {
            usedLightSlots.clear(light.getIndex());
            globalLights[light.index / Long.SIZE] &= ~(1L << (light.index & (Long.SIZE - 1)));
            refreshGlobalLights = true;
        }
    }

    public void addGlobalLight(Light light) {
        int word = light.index / Long.SIZE;
        long mask = 1L << (light.index & (Long.SIZE - 1));
        if ((globalLights[word] & mask) == 0) {
            globalLights[word] |= mask;
            refreshGlobalLights = true;
        }
    }

    public void removeGlobalLight(Light light) {
        int word = light.index / Long.SIZE;
        long mask = 1L << (light.index & (Long.SIZE - 1));
        if ((globalLights[word] & mask) != 0) {
            globalLights[word] &= ~mask;
            refreshGlobalLights = true;
        }
    }

    public void clearGlobalLights() {
        boolean notempty = false;
        for (int i = 0; i < globalLights.length; i++) {
            notempty = notempty | globalLights[i] != 0;
            globalLights[i] = 0;
        }
        refreshGlobalLights = refreshGlobalLights | notempty;
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
     * Creates an iterable that iterates over each descendant of {@code origin}
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
    public OrderedSceneIterator hierarchyOrdered() {
        return new OrderedSceneIterator();
    }

    /**
     * Creates an iterable that iterates over each node attached to the scene, where all
     * nodes are guaranteed to be visited after their children.
     *
     * @return ordered scene iterable
     */
    public OrderedSceneIterator hierarchyOrderedReverse() {
        return new OrderedSceneIterator(false);
    }

    /**
     * Creates an iterable that iterates over each node attached to the scene
     * in an arbitrary order.
     *
     * @return unordered scene iterable
     */
    public UnorderedSceneIterator unordered() {
        return new UnorderedSceneIterator();
    }

    protected class ComponentArray {

        private final boolean allowInherit;
        private final InheritMode onNull;
        private final int stride;
        private SceneComponent[] array;

        protected ComponentArray(boolean allowInherit, InheritMode onNull) {
            this.allowInherit = allowInherit;
            this.onNull = onNull;
            this.stride = allowInherit ? 2 : 1;
        }

        protected ComponentArray(ComponentInheritance i) {
            this(i == null || i.allow(), i != null ? i.onNull() : InheritMode.Pull);
        }

        protected void growToContain(int node) {
            if (array == null) {
                array = new SceneComponent[nodeInfo.length * stride / INFO_SIZE];
            } else if (node * stride >= array.length) {
                SceneComponent[] temp = new SceneComponent[nodeInfo.length * stride / INFO_SIZE];
                System.arraycopy(array, 0, temp, 0, array.length);
                array = temp;
            }
        }

        protected void updateWorldValues(int originNode) {
            if (!allowInherit) {
                return;
            }
            DepthFirstIterator it = depthFirst(originNode);
            for (int child : it) {
                SceneComponent newWorldValue = array[child * stride];
                InheritMode mode = newWorldValue != null ? newWorldValue.getInheritMode() : onNull;
                if (!mode.isIgnoreParent() && (getFlags(child) & IGNORE_PARENT_COMPONENTS_BIT) == 0) {
                    SceneComponent parentWorldValue = hasParent(child) ? array[getParentId(child) * stride + 1] : null;
                    InheritMode parentMode = parentWorldValue != null ? parentWorldValue.getInheritMode() : onNull;
                    if (parentMode.isPush() || (mode.isPull() && parentWorldValue != null)) {
                        newWorldValue = parentWorldValue;
                    }
                }
                child = child * stride + 1;
                if (array[child] != newWorldValue) {
                    array[child] = newWorldValue;
                } else {
                    it.skipChildren();
                }
            }
        }

        public void set(int node, SceneComponent value) {
            growToContain(node);
            int i = node * stride;
            if (array[i] != value) {
                array[i] = value;
                updateWorldValues(node);
            }
        }

        public SceneComponent getLocal(int node) {
            return array[node * stride];
        }

        public SceneComponent getWorld(int node) {
            return array[node * stride + (!allowInherit ? 0 : 1)];
        }

    }

    protected static class UserDataArray {

        private Object[] array;
        private int offset;

        protected void growToFit(int node) {
            if (array == null) {
                array = new Object[1];
            } else if (node < offset || node >= offset + array.length) {
                Object[] temp = new Object[Math.max(offset - node + array.length, node - offset)];
                System.arraycopy(array, 0, temp, Math.max(offset - node, 0), array.length);
                array = temp;
                offset = Math.min(offset, node);
            }
        }

        public void set(int node, Object value) {
            if (value != null) {
                growToFit(node);
                array[node] = value;
            } else if (node >= offset && node < offset + array.length) {
                array[node] = null;
            }
        }

        public Object get(int node) {
            return node >= offset && node < offset + array.length ? array[node] : null;
        }

    }

    @ComponentInheritance(allow=false)
    private static class GeometryData implements SceneComponent {

        private Mesh mesh;

        public Mesh getMesh() {
            return mesh;
        }

        public void setMesh(Mesh mesh) {
            this.mesh = mesh;
        }

    }

    public class Subset implements Iterable<Integer> {

        private final BitSet members = new BitSet();

        protected Subset() {}

        @Override
        public SubsetIterator iterator() {
            return new SubsetIterator(this);
        }

        public Subset filter(Predicate<Integer> filter) {
            Subset target = new Subset();
            for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1)) {
                if (filter.test(i)) {
                    target.members.set(i);
                }
            }
            return target;
        }

        public Subset transfer(Predicate<Integer> filter) {
            Subset target = new Subset();
            for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1)) {
                if (!filter.test(i)) {
                    members.clear(i);
                    target.members.set(i);
                }
            }
            return target;
        }

        public int[] toArray() {
            return members.stream().toArray();
        }

        public Scene getScene() {
            return Scene.this;
        }

        public BitSet getMemberIndices() {
            return members;
        }

        public boolean contains(int node) {
            return members.get(node);
        }

    }

    public class Light extends Struct {

        private final Field<Vector3f> color = new Field<>(new Vector3f());
        private final Field<Integer> type = new Field<>(0);
        private final Field<Vector4f> position = new Field<>(new Vector4f());
        private final Field<Vector4f> direction = new Field<>(new Vector4f());

        @Final
        private int index;
        private float radius, innerCos, outerCos;
        private final ColorRGBA colorRGB = new ColorRGBA(0f, 0f, 0f, 1f);
        private final Vector3f positionVec = new Vector3f();
        private final Vector3f directionVec = new Vector3f();

        private boolean needsBoundsCheck = true;
        private final IntList attachedNodes = new IntList(1);
        private final boolean global = false;

        protected Light() {}

        public Light(int index) {
            this.index = index;
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            super.write(ex);
            OutputCapsule out = ex.getCapsule(this);
            out.write(index, "index", 0);
        }

        @Override @FinalWriter
        public void read(JmeImporter im) throws IOException {
            super.read(im);
            InputCapsule in = im.getCapsule(this);
            index = in.readInt("index", 0);
        }

        public void setUpdateNeeded() {
            needsBoundsCheck = true;
            refreshGlobalLights = refreshGlobalLights || global;
            for (int i = 0; i < attachedNodes.size(); i++) {
                nodeInfo[i + FLAGS] |= LIGHT_REFRESH_BIT;
            }
        }

        public void setType(com.jme3.light.Light.Type type) {
            this.type.aliasAndSet(type.getId());
            needsBoundsCheck = true;
        }

        public void setColor(ColorRGBA color) {
            colorRGB.set(color.r, color.g, color.b, 1);
            this.color.alias().set(color.r, color.g, color.b);
            this.color.set();
        }

        public void setColor(float r, float g, float b) {
            colorRGB.set(r, g, b, 1);
            this.color.alias().set(r, g, b);
            this.color.set();
        }

        public void setPosition(Vector3f position) {
            positionVec.set(position);
            Vector4f alias = this.position.alias();
            alias.x = position.x;
            alias.y = position.y;
            alias.z = position.z;
            this.position.set();
            setUpdateNeeded();
        }

        public void setDirection(Vector3f direction) {
            directionVec.set(direction).normalizeLocal();
            Vector4f alias = this.direction.alias();
            alias.x = directionVec.x;
            alias.y = directionVec.y;
            alias.z = directionVec.z;
            this.direction.set();
            setUpdateNeeded();
        }

        public void setRadius(float radius) {
            assert radius >= 0 : "Radius must be positive.";
            this.radius = radius;
            position.alias().w = radius > 0 ? 1f/radius : 0f;
            position.set();
            setUpdateNeeded();
        }

        protected void updatePackedAngleCos() {
            position.alias().w = FastMath.floor(innerCos * 1000) + (outerCos - 0.001f);
            setUpdateNeeded();
        }

        public void setAngles(float innerAngle, float outerAngle) {
            this.innerCos = FastMath.cos(innerAngle);
            this.outerCos = FastMath.cos(outerAngle);
            updatePackedAngleCos();
        }

        public void setInnerAngle(float innerAngle) {
            this.innerCos = FastMath.cos(innerAngle);
            updatePackedAngleCos();
        }

        public void setOuterAngle(float outerAngle) {
            this.outerCos = FastMath.cos(outerAngle);
            updatePackedAngleCos();
        }

        public int getIndex() {
            return index;
        }

        public int getType() {
            return type.alias();
        }

        public float getRadius() {
            return radius;
        }

        public float getInnerCos() {
            return innerCos;
        }

        public float getOuterCos() {
            return outerCos;
        }

        public ColorRGBA getColor() {
            return colorRGB;
        }

        public Vector3f getPosition() {
            return positionVec;
        }

        public Vector3f getDirection() {
            return directionVec;
        }

    }

    public static class SubsetIterator implements Iterable<Integer>, Iterator<Integer> {

        private final Subset set;
        private int nextIndex;
        private int currentIndex = -1;

        protected SubsetIterator(Subset set) {
            this.set = set;
            computeNextIndex();
        }

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
            return currentIndex;
        }

        @Override
        public void remove() {
            set.members.clear(currentIndex);
        }

        private void computeNextIndex() {
            nextIndex = set.members.nextSetBit(currentIndex + 1);
        }

    }

    public class ChildIterator implements Iterable<Integer>, Iterator<Integer> {

        private final int parent;
        private final int[] queue = {NULL, NULL, NULL};

        private ChildIterator(int parent) {
            this.parent = parent;
            queue[2] = nodeInfo[parent + FIRST_CHILD];
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
            queue[2] = nodeInfo[queue[2] + SIBLING];
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
                nodeInfo[parent + FIRST_CHILD] = queue[2];
            } else {
                nodeInfo[queue[0] + SIBLING] = queue[2];
            }
            nodeInfo[queue[1] + PARENT] = NULL;
            nodeInfo[queue[1] + SIBLING] = NULL;
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
            int child = nodeInfo[next + FIRST_CHILD];
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
                int sib = nodeInfo[node + SIBLING];
                if (sib >= 0) return sib;
                node = nodeInfo[node + PARENT];
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

        private final boolean forward;
        private int nextIndex = usedOrderSlots.nextSetBit(0);
        private int currentIndex = NULL;

        private OrderedSceneIterator() {
            this(true);
        }

        private OrderedSceneIterator(boolean forward) {
            this.forward = forward;
        }

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
            return hierarchyOrder[currentIndex];
        }

        @Override
        public void remove() {
            detach(hierarchyOrder[currentIndex]);
            computeNextIndex();
        }

        private void computeNextIndex() {
            nextIndex = forward ? usedOrderSlots.nextSetBit(currentIndex + 1)
                                : usedOrderSlots.previousClearBit(currentIndex - 1);
        }

        public void destroy() {
            destroyNode(hierarchyOrder[currentIndex]);
            computeNextIndex();
        }

        public void destroyTree() {
            Scene.this.destroyTree(hierarchyOrder[currentIndex]);
            computeNextIndex();
        }

    }

    public class UnorderedSceneIterator implements Iterable<Integer>, Iterator<Integer> {

        private int nextIndex;
        private int currentIndex = NULL;

        private UnorderedSceneIterator() {
            computeNextIndex();
        }

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
            return currentIndex;
        }

        @Override
        public void remove() {
            detach(currentIndex);
            computeNextIndex();
        }

        private void computeNextIndex() {
            do {
                nextIndex = usedOrderSlots.nextSetBit(Math.max(0, currentIndex + 1));
            } while (nextIndex >= 0 && nodeInfo[nextIndex] >= 0);
        }

        public void destroy() {
            destroyNode(currentIndex);
            computeNextIndex();
        }

        public void destroyTree() {
            Scene.this.destroyTree(currentIndex);
            computeNextIndex();
        }

    }

}
