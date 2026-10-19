package com.jme3.vulkan.material.experimental;

import com.jme3.vulkan.scene.Scene;
import com.jme3.vulkan.scene.components.ComponentPipe;
import com.jme3.vulkan.scene.components.Component;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks, per node, whether every one of a fixed set of {@link Component}s is
 * present, and builds (or discards) a caller-defined artifact exactly when that
 * stops being true or starts being true — the kind of "tag" a
 * {@link ShadingTechnique} looks for (e.g. "has a PBR tag") is just the one-type case
 * of this; {@code Archetype} generalizes it to "has all of these component types."
 * <p>
 * An {@code Archetype} is itself a {@link ComponentPipe}. It is driven the same way any
 * other {@link ComponentPipe} is: register it once per {@link Scene} with
 * {@link Scene#getPipe}, which links it to every type in {@link #getObservedTypes()} so
 * it receives {@link #onComponentChanged} automatically whenever one of those types
 * changes for some node — there is no per-frame scan over the scene involved.
 * {@link #activate(int)} seeds a node's presence bits from its current component
 * values, the same way {@code StructPipe}/{@code DescriptorPipe} seed themselves;
 * {@link #deactivate(int)} drops them and discards the artifact, if any.
 * <p>
 * {@code build} and {@code discard} both receive the node id, so a
 * {@link ShadingTechnique} can use them to drive the GPU-memory-backed
 * {@link ComponentPipe}s it actually renders with — e.g. {@code build} calling
 * {@code activate} on its own pipes once a node has everything the technique needs,
 * {@code discard} calling {@code deactivate} on them the moment that stops being true.
 * That is what makes activation sticky and leak-free: it happens exactly on the
 * transition, driven by the same change notification every other component consumer
 * already relies on, not by re-deriving membership from scratch every pass.
 *
 * @param <T> artifact type built once every observed component type is present
 */
public class Archetype <T> implements ComponentPipe {

    private final Scene scene;
    private final Class<? extends Component>[] types;
    private final ArchetypeListener<T> listener;
    private final BitSet presentBits = new BitSet();
    private final Map<Integer, T> artifacts = new HashMap<>();

    @SafeVarargs
    public Archetype(Scene scene, ArchetypeListener<T> listener, Class<? extends Component>... types) {
        this.scene = scene;
        this.listener = listener;
        this.types = types;
    }

    private int indexOf(Class<?> type) {
        for (int i = 0; i < types.length; i++) {
            if (types[i] == type) {
                return i;
            }
        }
        return -1;
    }

    private boolean isComplete(int node) {
        int origin = node * types.length;
        return presentBits.nextClearBit(origin) >= origin + types.length;
    }

    private void setBit(int node, int i) {
        if (isComplete(node)) {
            return;
        }
        presentBits.set(node * types.length + i);
        if (isComplete(node)) {
            artifacts.put(node, listener.build(node));
        }
    }

    private void clearBit(int node, int i) {
        if (isComplete(node)) {
            T artifact = artifacts.remove(node);
            if (artifact != null) {
                listener.discard(artifact, node);
            }
        }
        presentBits.clear(node * types.length + i);
    }

    @Override
    public Class<? extends Component>[] getObservedTypes() {
        return types;
    }

    @Override
    public void onComponentChanged(Component type, int node) {
        int i = indexOf(type.getClass());
        if (i < 0) {
            return;
        }
        if (type.isNull(node)) {
            clearBit(node, i);
        } else {
            setBit(node, i);
        }
    }

    @Override
    public void activate(int node) {
        for (Class<? extends Component> t : types) {
            onComponentChanged(scene.getComponent(t), node);
        }
    }

    @Override
    public void deactivate(int node) {
        T artifact = artifacts.remove(node);
        if (artifact != null) {
            listener.discard(artifact, node);
        }
        int origin = node * types.length;
        presentBits.clear(origin, origin + types.length);
    }

    /**
     * Gets the artifact currently built for {@code node}.
     *
     * @param node node id
     * @return the artifact built for {@code node}, or null if {@code node} does not
     *         currently have every observed component type present
     */
    public T get(int node) {
        return artifacts.get(node);
    }

}
