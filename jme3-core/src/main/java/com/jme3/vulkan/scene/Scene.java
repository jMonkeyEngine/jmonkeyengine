package com.jme3.vulkan.scene;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.*;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.util.collections.IntList;
import com.jme3.util.struct.Struct;
import com.jme3.vulkan.alloc.ConcurrentStructArray;
import com.jme3.vulkan.alloc.SlicePointer;
import com.jme3.vulkan.buffer.AutoBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.buffer.alloc.BufferType;
import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.commands.OpLocation;
import com.jme3.vulkan.compile.Final;
import com.jme3.vulkan.compile.FinalWriter;
import com.jme3.vulkan.material.experimental.ShadingTechnique;
import com.jme3.vulkan.material.experimental.TriggeredOn;
import com.jme3.vulkan.scene.components.Property;
import com.jme3.vulkan.scene.components.Component;
import com.jme3.vulkan.scene.components.ComponentPipe;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.*;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * A scene of spatials trees optimized for traversal and data retrieval.
 */
public class Scene {

    private static final Logger log = Logger.getLogger(Scene.class.getName());

    // fast-access node properties
    private static final int ORDER_INDEX = 0;   // type: hierarchy order index
    private static final int PARENT = 1;        // type: node index
    private static final int SIBLING = 2;       // type: node index
    private static final int FIRST_CHILD = 3;   // type: node index
    private static final int FLAGS = 4;         // misc boolean fields
    private static final int HINTS = 5;         // local cull hint, world cull hint, local bucket hint, world bucket hint (1 byte each)
    private static final int GEOMETRY = 6;      // type: geometric index
    private static final int UNUSED_NODE_INFO = 7;

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
    public static final int IGNORE_PARENT_TRANSFORM_BIT = 1;
    public static final int IGNORE_NONLOCAL_LIGHTS_BIT = 1 << 1;
    public static final int IGNORE_PARENT_COMPONENTS_BIT = 1 << 2;
    public static final int TRANSFORM_REFRESH_BIT = 1 << 3;
    public static final int BOUNDS_REFRESH_BIT = 1 << 4;
    public static final int LIGHT_REFRESH_BIT = 1 << 5;         // only refresh lights with flag set
    public static final int LIGHT_REFRESH_ALL_BIT = 1 << 6;     // refresh all lights
    public static final int COMPONENT_REFRESH_BIT = 1 << 7;
    private static final int CHILD_TRANSFORM_REFRESH_BIT = 1 << 8;
    private static final int CHILD_LIGHT_REFRESH_BIT = 1 << 9;
    private static final int CHILD_COMPONENT_REFRESH_BIT = 1 << 10;

    private static final int INFO_SIZE = 8;
    private static final int TRANSFORMS_SIZE = 42;

    private static final int NULL = -1;
    private static final int INHERIT = 0;

    private static final int HINT_MASK = 0xFF;
    private static final int LOCAL_CULL_HINT = 0;
    private static final int WORLD_CULL_HINT = 8;
    private static final int LOCAL_BUCKET_HINT = 16;
    private static final int WORLD_BUCKET_HINT = 24;

    private static final byte INHERIT_OFF = 0;
    private static final byte INHERIT_PULL = 1;
    private static final byte INHERIT_PUSH = 2;
    private static final byte INHERIT_OFF_IGNORE_PARENT = 3;
    private static final byte INHERIT_PUSH_IGNORE_PARENT = 4;

    private static final Map<Class<? extends Component>, Function<Scene, Component>> componentFactories = new HashMap<>();

    public static void addComponentFactory(Class<? extends Component> type, Function<Scene, Component> factory) {
        componentFactories.put(type, factory);
    }

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

    private int[] nodeInfo, hierarchyOrder;
    private final float[] transforms;     // local transform, world transform, world bounds
    private long[] nodeLights;      // manual bitset for local and world lights per node
    private long[] globalLights;    // manual bitset for global lights virtually attached to every root node
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();
    private final Map<Class<? extends ComponentPipe>, ComponentPipe> pipes = new HashMap<>();
    private final Map<Class<? extends ShadingTechnique>, ShadingTechnique> techniques = new HashMap<>();
    private final Map<String, Tag> tags = new HashMap<>();

    private final Collection<IntConsumer> transformListeners = new ArrayList<>();
    private final Collection<IntConsumer> boundsListeners = new ArrayList<>();

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
                BufferType.Dynamic, GpuBuffer.Role.Storage);
        geometricLightMasks = new AutoBuffer<>(alloc,
                new SlicePointer(0, 1028), // i'm too tired to figure out the math for size here again
                BufferType.Streaming, GpuBuffer.Role.Storage);
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

    public int getGeometricId(int node) {
        return nodeInfo[node * INFO_SIZE + GEOMETRY];
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

    public boolean isGeometric(int node) {
        return nodeInfo[node * INFO_SIZE + GEOMETRY] >= 0;
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

    public Matrix4f getWorldMatrix(int node, @Nullable Matrix4f store) {
        return Matrix4f.extract(transforms, worldMatrix(node), store);
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

    private void setGeometricId(int node, int geometricId) {
        nodeInfo[node * INFO_SIZE + GEOMETRY] = geometricId;
    }

    private void setFlags(int node, int flags) {
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
        nodeInfo[index + GEOMETRY] = NULL;
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
        cleanDeadNode(node);
    }

    /**
     * Destroys {@code node} and all descendents of {@code node}.
     *
     * @param node id of tree source node
     */
    public void destroyTree(int node) {
        detach(node);
        for (int i : depthFirst(node)) {
            cleanDeadNode(node);
        }
    }

    private void cleanDeadNode(int node) {
        usedNodeSlots.clear(node);
        for (Component c : components.values()) {
            c.onNodeDestroyed(node);
        }
        clearAllTags(node);
    }

    /**
     * Forms a parent-child relationship between {@code parent} and {@code child}, if
     * it does not already exist.
     *
     * @param parent id of node to attach to
     * @param child id of node to be attached
     */
    public boolean attachChild(int parent, int child) {
        if (getParentId(child) == parent) {
            return false;
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
        return true;
    }

    /**
     * Attaches {@code node} to the scene as a root.
     *
     * @param node id of node to attach
     */
    public boolean makeRoot(int node) {
        if (hasParent(node)) {
            return detachChildToRoot(node);
        }
        if (isAttached(node)) {
            return false;
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
        return true;
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
            for (IntConsumer l : transformListeners) {
                l.accept(id);
            }
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
        if ((flags & BOUNDS_REFRESH_BIT) != 0) {
            BoundingBox.makeNull(transforms, worldBounds(id)); // merging with a null box will just copy the non-null box
            if (isGeometric(id)) {
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
            for (IntConsumer l : boundsListeners) {
                l.accept(id);
            }
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

    public <T extends Component> T getComponent(Class<T> type) {
        return getComponent(type, null);
    }

    /**
     * Gets {@code node}'s value for the property type {@code type}, or {@code defaultValue}
     * if {@code node} has no value for it (it was never set, or was cleared).
     *
     * @param type property component type
     * @param node node id
     * @param defaultValue value to return if {@code node} has no value for {@code type}
     * @return {@code node}'s value for {@code type}, or {@code defaultValue}
     * @param <T> property value type
     */
    public <T> T getProperty(Class<? extends Property<T>> type, int node, T defaultValue) {
        Property<T> property = getComponent(type);
        return property.isNull(node) ? defaultValue : property.get(node);
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> type, @Nullable Function<Scene, Component> factory) {
        Component t = components.get(type);
        if (t == null) {
            if (factory == null) {
                factory = componentFactories.get(type);
            }
            if (factory != null) {
                t = factory.apply(this);
            } else try {
                Constructor c = type.getDeclaredConstructor(Scene.class);
                c.setAccessible(true);
                t = (Component)c.newInstance(this);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Failed to invoke constructor for " + type, e);
            } catch (NoSuchMethodException e) {
                try {
                    Constructor c = type.getDeclaredConstructor();
                    c.setAccessible(true);
                    t = (Component)c.newInstance();
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException("Failed to invoke parameterless constructor for " + type, ex);
                } catch (NoSuchMethodException ex) {
                    throw new RuntimeException(type + " does not have an parameterless constructor or a constructor accepting "
                            + Scene.class + ". Register a builder directly or add a usable constructor.", e);
                }
            }
            if (t == null) {
                throw new NullPointerException("Produced component type for " + type + " is null.");
            }
            components.put(type, t);
        }
        return (T)t;
    }

    @SuppressWarnings("unchecked")
    public <T extends ComponentPipe> T getPipe(Class<T> type, Function<Scene, T> compute) {
        T l = (T)pipes.get(type);
        if (l == null) {
            pipes.put(type, l = compute.apply(this));
            for (Class t : l.getObservedTypes()) {
                getComponent(t).addPipe(l);
            }
        }
        return l;
    }

    /**
     * Gets this scene's instance of {@code type}, creating it with {@code compute} if this
     * is the first time it has been requested. Unlike {@link #getPipe} and
     * {@link #getComponent}, there is no reflective fallback: a {@link ShadingTechnique}
     * is always built by {@code compute}, since it is otherwise just as likely to need
     * scene-specific construction arguments as not. Every {@link Scene} gets its own
     * instance even for the same {@code type}, so a technique never ends up resolving node
     * ids against more than one scene.
     *
     * @param type concrete technique type, used as the registry key
     * @param compute builds the technique for this scene if it does not already exist
     * @return this scene's instance of {@code type}
     */
    @SuppressWarnings("unchecked")
    public <T extends ShadingTechnique> T getOrCreateTechnique(Class<T> type, Function<Scene, T> compute) {
        T technique = (T)techniques.get(type);
        if (technique == null) {
            technique = compute.apply(this);
            if (technique.getClass() != type) {
                throw new IllegalStateException("Created technique type must match the queried type.");
            }
            TriggeredOn triggered = technique.getClass().getAnnotation(TriggeredOn.class);
            if (triggered != null) {
                for (String tag : triggered.value()) {
                    addTagListener(tag, technique);
                }
            }
            techniques.put(type, technique);
        }
        return technique;
    }

    @SuppressWarnings("unchecked")
    public <T extends ShadingTechnique> T getTechnique(Class<T> type) {
        return (T)techniques.get(type);
    }

    /**
     * Returns true if {@code node} currently has {@code tag}.
     *
     * @param tag tag name
     * @param node node id
     * @return true if tagged
     */
    public boolean hasTag(String tag, int node) {
        Tag t = tags.get(tag);
        return t != null && t.taggedNodes.get(node);
    }

    /**
     * Tags {@code node} with {@code tag}, notifying every {@link TagListener} registered
     * for {@code tag} via {@link TagListener#tagAdded} if {@code node} was not already
     * tagged. Does nothing if {@code node} already has {@code tag}.
     *
     * @param tag tag name
     * @param node node id
     */
    public void setTag(String tag, int node) {
        Tag t = tags.computeIfAbsent(tag, k -> new Tag());
        if (!t.taggedNodes.get(node)) {
            t.taggedNodes.set(node);
            for (TagListener l : t.listeners) {
                l.tagAdded(this, node, tag);
            }
        }
    }

    /**
     * Removes {@code tag} from {@code node}, notifying every {@link TagListener} registered
     * for {@code tag} via {@link TagListener#tagRemoved} if {@code node} was tagged. Does
     * nothing if {@code node} does not have {@code tag}.
     *
     * @param tag tag name
     * @param node node id
     */
    public void clearTag(String tag, int node) {
        Tag t = tags.get(tag);
        if (t != null && t.taggedNodes.get(node)) {
            t.taggedNodes.clear(node);
            for (TagListener l : t.listeners) {
                l.tagRemoved(this, node, tag);
            }
        }
    }

    /**
     * Registers {@code listener} to be notified when {@code tag} is added to or removed
     * from any node.
     *
     * @param tag tag name
     * @param listener listener to notify
     */
    public void addTagListener(String tag, TagListener listener) {
        tags.computeIfAbsent(tag, k -> new Tag()).listeners.add(listener);
    }

    /**
     * Unregisters {@code listener} from {@code tag}. Does nothing if {@code listener} was
     * not registered for {@code tag}.
     *
     * @param tag tag name
     * @param listener listener to unregister
     */
    public void removeTagListener(String tag, TagListener listener) {
        Tag t = tags.get(tag);
        if (t != null) {
            t.listeners.remove(listener);
        }
    }

    /**
     * Removes every tag currently on {@code node}, notifying listeners for each one that
     * was actually present. Node ids are recycled by {@link #createNode()}, so this must
     * run before a destroyed node's id can be reused - otherwise a brand new node could
     * appear to already carry whatever tags the previous occupant of its id had.
     *
     * @param node node id
     */
    private void clearAllTags(int node) {
        for (Map.Entry<String, Tag> e : tags.entrySet()) {
            Tag t = e.getValue();
            if (t.taggedNodes.get(node)) {
                t.taggedNodes.clear(node);
                for (TagListener l : t.listeners) {
                    l.tagRemoved(this, node, e.getKey());
                }
            }
        }
    }

    public void addTransformListener(IntConsumer listener) {
        transformListeners.add(listener);
    }

    public void addBoundsListener(IntConsumer listener) {
        boundsListeners.add(listener);
    }

    /**
     * Creates a group of nodes from this scene where each node in the group satisfies {@code filter}
     *
     * @param filter filters nodes to be in the group
     * @return node group
     */
    public Group createGroup(Predicate<Integer> filter) {
        Group subset = new Group();
        for (int i : unordered()) {
            if (filter.test(i)) {
                subset.members.set(i);
            }
        }
        return subset;
    }

    /**
     * Creates a group containing all nodes that exist in the scene (nodes do not have to be attached).
     *
     * @return node group
     */
    public Group createGroup() {
        Group set = new Group();
        set.members.or(usedNodeSlots);
        return set;
    }

    /**
     * Creates a group of nodes from this scene where each node in the group satisfies {@code filter}
     * and all its ancestors are also in the group.
     *
     * @param filter node filter
     * @return group
     */
    public Group createHierarchalGroup(Predicate<Integer> filter) {
        Group subset = new Group();
        for (int i : hierarchyOrdered()) {
            int parent = getParentId(i);
            if ((parent < 0 || subset.members.get(parent)) && filter.test(i)) {
                subset.members.set(i);
            }
        }
        return subset;
    }

    /**
     * Creates a new group containing no nodes.
     *
     * @return enpty node group
     */
    public Group createEmptyGroup() {
        return new Group();
    }

    public Light createLight(CommandBuffer cmd) {
        int i = usedLightSlots.nextClearBit(0);
        usedLightSlots.set(i);
        if (i >= lights.getStructure().getLength()) {
            lights.update(cmd, BufferType.Dynamic, GpuBuffer.Role.Storage, OpLocation.PreferHost);
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

    public int getAvailableNodeSlots() {
        return nodeInfo.length / INFO_SIZE;
    }

    /**
     * Gets the set of child nodes of {@code parent}. Modifying the set modifies the scene
     * graph and vise versa.
     *
     * @param parent id of parent node
     * @return child set
     */
    public Set<Integer> children(int parent) {
        return new Children(parent);
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

    private static class Tag {

        private final BitSet taggedNodes = new BitSet();
        private final Collection<TagListener> listeners = new ArrayList<>(2);

    }

    public class Group implements Set<Integer> {

        private final BitSet members = new BitSet();

        protected Group() {}

        @Override
        public int size() {
            return members.cardinality();
        }

        @Override
        public boolean isEmpty() {
            return members.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (o == null || o.getClass() == Integer.class) {
                return false;
            }
            return members.get((Integer)o);
        }

        @Override
        public GroupIterator iterator() {
            return new GroupIterator(this);
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(Integer integer) {
            if (!members.get(integer)) {
                members.set(integer);
                return true;
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (o == null || o.getClass() != Integer.class) {
                return false;
            }
            int i = (Integer)o;
            boolean present = members.get(i);
            members.clear(i);
            return present;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!contains(o)) return false;
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            boolean changed = false;
            for (int i : c) {
                changed = changed || !members.get(i);
                members.set(i);
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean changed = false;
            for (Object o : c) {
                changed = remove(o) | changed;
            }
            return changed;
        }

        @Override
        public void clear() {
            members.clear();
        }

        public void render(CommandBuffer cmd, Queue<Class<? extends ShadingTechnique>> techniques) {
            for (Class<? extends ShadingTechnique> type : techniques) {
                if (members.isEmpty()) break;
                ShadingTechnique tech = getTechnique(type);
                if (tech == null) continue;
                Group g = new Group();
                for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1)) {
                    if (tech.acceptNode(i)) {
                        members.clear(i);
                        g.members.set(i);
                    }
                }
                tech.render(cmd, g);
            }
        }

        /**
         * Returns an IntStream of all nodes in this group.
         *
         * @return int stream
         */
        public IntStream intStream() {
            return members.stream();
        }

        /**
         * Adds filtered nodes from this group to a new group.
         *
         * @param filter filters nodes
         * @return new group containing the filtered nodes
         */
        public Group filter(IntPredicate filter) {
            Group target = new Group();
            for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1)) {
                if (filter.test(i)) {
                    target.members.set(i);
                }
            }
            return target;
        }

        /**
         * Moves filtered nodes from this group to a new group.
         *
         * @param filter filters nodes
         * @return group containing the filtered nodes
         */
        public Group transfer(IntPredicate filter) {
            Group target = new Group();
            for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1)) {
                if (!filter.test(i)) {
                    members.clear(i);
                    target.members.set(i);
                }
            }
            return target;
        }

        public boolean removeIf(IntPredicate filter) {
            boolean changed = false;
            for (int i = members.nextSetBit(0); i >= 0; i = members.nextSetBit(i + 1)) {
                if (!filter.test(i)) {
                    members.clear(i);
                    changed = true;
                }
            }
            return changed;
        }

        /**
         * Creates a copy of this group.
         *
         * @return group copy
         */
        public Group copy() {
            Group g = new Group();
            g.members.or(members);
            return g;
        }

        public Scene getScene() {
            return Scene.this;
        }

    }

    public static class MeshType extends Property<Mesh> {

        protected MeshType(Scene scene) {
            super(scene, () -> new DynamicObjectArray<>(128));
        }

    }

    public class Light extends Struct {

        private final Field<Vector3f> color = new Field<>(new Vector3f());
        private final Field<Integer> type = new Field<>(0);
        private final Field<Vector4f> position = new Field<>(new Vector4f());
        private final Field<Vector4f> direction = new Field<>(new Vector4f());

        @Final private int index;
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

    public static class GroupIterator implements Iterable<Integer>, Iterator<Integer> {

        private final Group set;
        private int nextIndex;
        private int currentIndex = -1;

        protected GroupIterator(Group set) {
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

    private class Children implements Set<Integer> {

        private final int parent;

        private Children(int parent) {
            this.parent = parent;
        }

        @Override
        public int size() {
            int size = 0;
            for (int node = getFirstChildId(parent); node >= 0; node = getSiblingId(node)) {
                size++;
            }
            return size;
        }

        @Override
        public boolean isEmpty() {
            return !hasChildren(parent);
        }

        @Override
        public boolean contains(Object o) {
            if (o == null || o.getClass() != Integer.class) {
                return false;
            }
            int n = (int)o;
            for (int node = getFirstChildId(parent); node >= 0; node = getSiblingId(node)) {
                if (n == node) return true;
            }
            return false;
        }

        @Override
        public Iterator<Integer> iterator() {
            return new IteratorImpl();
        }

        @Override
        public Integer[] toArray() {
            Integer[] array = new Integer[size()];
            for (int node = getFirstChildId(parent), i = 0; node >= 0; node = getSiblingId(node), i++) {
                array[i] = node;
            }
            return array;
        }

        @Override @SuppressWarnings({"unchecked", "WrapperTypeMayBePrimitive"})
        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size) {
                a = (T[])Array.newInstance(a.getClass().getComponentType(), size);
            }
            for (Integer node = getFirstChildId(parent), i = 0; node >= 0; node = getSiblingId(node), i++) {
                a[i] = (T)node;
            }
            return a;
        }

        @Override
        public boolean add(Integer integer) {
            return attachChild(parent, integer);
        }

        @Override
        public boolean remove(Object o) {
            if (o == null || o.getClass() != Integer.class) {
                return false;
            }
            int node = (int)o;
            if (getParentId(node) != parent) {
                return false;
            }
            detach(node);
            return true;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            for (int node = getFirstChildId(parent); node >= 0; node = getSiblingId(node)) {
                if (!c.contains(node)) return false;
            }
            return true;
        }

        @Override
        public boolean addAll(Collection<? extends Integer> c) {
            boolean changed = false;
            for (int node : c) {
                changed = attachChild(parent, node) | changed;
            }
            return changed;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            boolean changed = false;
            for (int node = getFirstChildId(parent); node >= 0;) {
                int next = getSiblingId(node);
                if (!c.contains(node)) {
                    detach(node);
                    changed = true;
                }
                node = next;
            }
            return changed;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            boolean changed = false;
            for (int node = getFirstChildId(parent); node >= 0;) {
                int next = getSiblingId(node);
                if (c.contains(node)) {
                    detach(node);
                    changed = true;
                }
                node = next;
            }
            return changed;
        }

        @Override
        public void clear() {
            detachAllChildren(parent);
        }

        public class IteratorImpl implements Iterator<Integer> {

            private int next = getFirstChildId(parent);
            private int current = NULL;

            @Override
            public boolean hasNext() {
                return next >= 0;
            }

            @Override
            public Integer next() {
                current = next;
                next = getSiblingId(next);
                return current;
            }

            @Override
            public void remove() {
                detach(current);
            }

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
