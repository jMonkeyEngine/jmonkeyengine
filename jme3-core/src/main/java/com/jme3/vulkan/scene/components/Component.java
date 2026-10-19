package com.jme3.vulkan.scene.components;

/**
 * Handles a type of component for a {@link com.jme3.vulkan.scene.Scene}.
 *
 * <p>Component types can be configured to observe other types and receive notifications
 * of particular events from them. This is done by adding the {@link Observes} annotation
 * to the class type. Circular type observations should be avoided.</p>
 */
public interface Component {

    default void onNodeDestroyed(int node) {}

}
