package com.jme3.vulkan.frames;

/**
 * Immutably maps frame indices to resources.
 *
 * @param <T> resource type
 */
public interface VersionedResource<T> {

    /**
     * Gets the resource version for the current frame.
     *
     * <p>This is functionally identical to</p>
     * <pre><code>
     * int currentFrameIndex = ...
     * T version = getVersion(currentFrameIndex);
     * </code></pre>
     * <p>Where {@code currentFrameIndex} is the index of the current frame.</p>
     */
    T getVersion();

    /**
     * Gets the resource version for the specified frame index. The index
     * may be between 0 (inclusive) and the number of frames-in-flight
     * (exclusive), regardless of the number of versions this resource
     * contains.
     *
     * <p>This method is deterministic. Calls with the same arguments return
     * the same resource.</p>
     *
     * @param i frame index
     * @return resource version mapped to the frame index
     */
    T getVersion(int i);

    /**
     * Gets the number of unique versions this resource maintains.
     */
    int getNumVersions();

    /**
     * Gets the index of the {@link #getVersion() current version}, which
     * is not necessarily the current frame index, but is determined
     * by the current frame index. This should always return the same
     * value on the same frame.
     */
    int getCurrentVersionIndex();

}
