package com.jme3.vulkan.frames;

/**
 * Immutably maps frame indices to resources.
 *
 * @param <T> resource type
 */
public interface VersionedResource<T> extends Iterable<T> {

    /**
     * Sets the resource for the current frame.
     *
     * @param resource resource to assign (not null)
     */
    @Deprecated
    default void set(T resource) {}

    /**
     * Sets the resource for the specified frame.
     *
     * @param frame frame to assign to (not negative)
     * @param resource resource to assign (not null)
     */
    @Deprecated
    default void set(int frame, T resource) {}

    /**
     * Gets the resource for the current frame. Must return the same value
     * over the course of a frame.
     */
    T get();

    /**
     * Gets the resource for the specified frame index.
     *
     * @param frame frame index (not negative)
     * @return resource version mapped to the frame index
     */
    T get(int frame);

    /**
     * Gets the number of unique resources this resource maintains.
     */
    int getNumResources();

}
