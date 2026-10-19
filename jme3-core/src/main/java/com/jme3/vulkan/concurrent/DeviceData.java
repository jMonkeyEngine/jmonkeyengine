package com.jme3.vulkan.concurrent;

/**
 * Exposes the {@link CommandDataAccess} tracking the most recent GPU accesses made to this
 * resource (a buffer, an image, etc.), so that generic utility methods that take the
 * resource as a parameter, such as the {@code cmd*} methods on
 * {@link com.jme3.vulkan.commands.CommandBuffer}, can call
 * {@link com.jme3.vulkan.commands.CommandBuffer#useResource} on its behalf instead of
 * every caller having to do so manually.
 */
@Deprecated
public interface DeviceData {

    /**
     * Gets the {@link CommandDataAccess} tracking this resource's most recent GPU accesses.
     *
     * @return resource access tracker
     */
    CommandDataAccess getDataAccess();

}
