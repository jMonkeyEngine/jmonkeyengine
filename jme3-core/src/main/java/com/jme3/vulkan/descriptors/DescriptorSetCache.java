package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.frames.Frame;

import java.util.*;

/**
 * Manages the {@link DescriptorSet descriptor sets} a {@link com.jme3.vulkan.material.experimental.ShadingTechnique
 * ShadingTechnique} uses for each node (or other key) it renders, all of a single {@link DescriptorSetLayout}.
 * <p>
 * A technique assigns a node's values directly to the node's {@link #get(Object) current set}. Assigning
 * values only touches CPU-side state, so this is allowed even while the device is reading the set;
 * only {@link DescriptorSet#update() writing} them to the device is restricted, and that is left to
 * {@link #bind(Object)}. When the current set may still be read by a frame in flight and
 * {@link DescriptorSet#updateNeeded()} reports its descriptors would change, its resources are
 * transferred to another set, which becomes the node's current set, and the old one is kept aside
 * until the device is done with it. A node whose descriptors change every frame therefore cycles
 * through at most {@code frames + 1} sets, and preferably reuses its own older sets, which already
 * hold most of its descriptors so an update writes little.
 * <p>
 * Sets no longer needed (because a node is {@link #release(Object) released} or stopped changing)
 * are recycled through a shared queue instead of being freed back to the {@link DescriptorPool}:
 * reusing a set costs nothing on the device, whereas freeing and reallocating takes two driver
 * calls, requires {@link DescriptorPool.Create#FreeDescriptorSets} and can fragment the pool.
 * {@link #trim()} can still return idle sets to the pool after a spike.
 *
 * @param <K> key type, typically a node id
 */
public class DescriptorSetCache <K> {

    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final Frame frames;
    private final Map<K, Entry> entries = new HashMap<>();
    private final List<Version> pending = new ArrayList<>();
    private final Queue<DescriptorSet> free = new ArrayDeque<>();

    /**
     * @param pool pool to allocate sets from
     * @param layout layout of every set
     * @param frames frame tracker, advanced once per frame after the upcoming slot's fence is waited on
     */
    public DescriptorSetCache(DescriptorPool pool, DescriptorSetLayout layout, Frame frames) {
        this.pool = pool;
        this.layout = layout;
        this.frames = frames;
    }

    /**
     * Gets {@code key}'s current set, acquiring one if {@code key} has none. Resources may be
     * {@link DescriptorSet#set assigned} to the returned set at any time, but it may be in use by
     * the device, so it must not be {@link DescriptorSet#update() updated} directly; {@link #bind(Object)}
     * does that. A newly acquired set may hold another key's resources, so every binding the
     * technique relies on should be assigned.
     *
     * @param key key
     * @return current set of {@code key}
     * @throws IllegalStateException if a set must be allocated and the pool is exhausted
     */
    public DescriptorSet get(K key) {
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        if (e.current == null) {
            e.current = obtain(e);
        }
        return e.current.set;
    }

    /**
     * Gets the set {@code key} should be rendered with this frame, and marks it as in use by this
     * frame. Changed descriptors of the current set are written first: to the current set itself if
     * no frame in flight uses it, otherwise its resources are transferred to another set, which
     * becomes {@code key}'s current set.
     *
     * @param key key to render
     * @return set to bind for {@code key}
     * @throws IllegalStateException if a set must be allocated and the pool is exhausted
     */
    public DescriptorSet update(K key) {
        Entry e = entries.computeIfAbsent(key, k -> new Entry());
        if (e.current == null) {
            e.current = obtain(e);
            e.current.set.update();
        } else if (!inFlight(e.current)) {
            // writes only the descriptors that changed, if any
            releaseIdleSpares(e);
            e.current.set.update();
        } else if (e.current.set.updateNeeded()) {
            // the device may be reading the current set, so the changes go to another set
            Version next = obtain(e);
            e.current.set.copyResourcesTo(next.set);
            e.spares.add(e.current);
            e.current = next;
            e.current.set.update();
        } else {
            releaseIdleSpares(e);
        }
        e.current.lastUsed = frames.frame();
        return e.current.set;
    }

    /**
     * Releases every set of {@code key}, e.g. when the node is no longer rendered by the
     * technique. The sets become reusable once no frame in flight uses them.
     *
     * @param key key to release
     * @return true if {@code key} had any sets
     */
    public boolean release(K key) {
        Entry e = entries.remove(key);
        if (e == null) {
            return false;
        }
        retire(e);
        return true;
    }

    /**
     * Releases the sets of every key.
     */
    public void releaseAll() {
        for (Entry e : entries.values()) {
            retire(e);
        }
        entries.clear();
    }

    /**
     * Frees every idle set back to the pool, e.g. after a spike in rendered nodes. Does nothing
     * unless the pool was created with {@link DescriptorPool.Create#FreeDescriptorSets}. Sets that
     * may still be in use by a frame in flight, and spares still held by nodes, are not freed.
     */
    public void trim() {
        reclaim();
        if (!free.isEmpty() && pool.getFlags().contains(DescriptorPool.Create.FreeDescriptorSets)) {
            pool.free(free);
            free.clear();
        }
    }

    public boolean contains(K key) {
        return entries.containsKey(key);
    }

    public DescriptorSetLayout getLayout() {
        return layout;
    }

    public DescriptorPool getPool() {
        return pool;
    }

    /**
     * A set may be read by the device until the last frame it was used by has completed.
     */
    private boolean inFlight(Version v) {
        return v.lastUsed > frames.completed();
    }

    /**
     * Hands spares of a node whose descriptors are not changing to other nodes.
     */
    private void releaseIdleSpares(Entry e) {
        while (!e.spares.isEmpty() && !inFlight(e.spares.peek())) {
            //noinspection DataFlowIssue
            free.add(e.spares.poll().set);
        }
    }

    private Version obtain(Entry e) {
        // the node's own oldest spare is preferred, since it already holds most of the node's values
        if (!e.spares.isEmpty() && !inFlight(e.spares.peek())) {
            return e.spares.poll();
        }
        if (free.isEmpty()) {
            reclaim();
        }
        DescriptorSet set = free.poll();
        if (set == null) {
            set = pool.allocateSet(layout).orElseThrow(() -> new IllegalStateException(
                    "Descriptor pool exhausted (" + entries.size() + " nodes cached)."));
        }
        return new Version(set);
    }

    private void retire(Entry e) {
        if (e.current != null) {
            pending.add(e.current);
        }
        pending.addAll(e.spares);
    }

    private void reclaim() {
        for (Iterator<Version> it = pending.iterator(); it.hasNext();) {
            Version v = it.next();
            if (!inFlight(v)) {
                free.add(v.set);
                it.remove();
            }
        }
    }

    /**
     * Sets belonging to one key.
     */
    private static class Entry {

        private Version current;
        private final Queue<Version> spares = new ArrayDeque<>(); // oldest use first

    }

    /**
     * A set and the last frame it was used by.
     */
    private static class Version {

        private final DescriptorSet set;
        private long lastUsed = Long.MIN_VALUE / 2;

        private Version(DescriptorSet set) {
            this.set = set;
        }

    }

}
