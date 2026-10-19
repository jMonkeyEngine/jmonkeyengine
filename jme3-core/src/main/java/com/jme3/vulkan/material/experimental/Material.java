package com.jme3.vulkan.material.experimental;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface Material {

    /**
     * Writes {@code component} to this material.
     *
     * @param component component to write
     */
    void writeComponent(@NonNull Component component);

    /**
     * Resets the data related to {@code type} to its default state. Implementations
     * are not required to do anything.
     *
     * @param type type to reset
     */
    void resetComponent(Class<? extends Component> type);

}
