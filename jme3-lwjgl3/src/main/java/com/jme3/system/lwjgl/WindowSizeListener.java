package com.jme3.system.lwjgl;

/**
 * Listen to window size changes. Note, GLFW does not support registering multiple callbacks
 * in {@link org.lwjgl.glfw.GLFW#glfwSetWindowSizeCallback(long, org.lwjgl.glfw.GLFWWindowSizeCallbackI)},
 * registering a new one will remove the previous one. Using this interface one can register
 * multiple listeners.
 *
 * @author Ali-RS
 */
public interface WindowSizeListener {

    public void onWindowSizeChanged(final int width, final int height);

}
