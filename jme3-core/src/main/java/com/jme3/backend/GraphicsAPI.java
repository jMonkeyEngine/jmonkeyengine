package com.jme3.backend;

import java.util.Objects;

/**
 * Enum indicating possible backends.
 */
@Deprecated
public enum GraphicsAPI {

    // Note: order matters, add new backends to the rear

    OpenGL, Vulkan;

    private static GraphicsAPI api = GraphicsAPI.OpenGL;

    public static void setActiveAPI(GraphicsAPI api) {
        GraphicsAPI.api = Objects.requireNonNull(api, "Graphics API cannot be null.");
    }

    public static GraphicsAPI getActiveAPI() {
        return api;
    }

}
