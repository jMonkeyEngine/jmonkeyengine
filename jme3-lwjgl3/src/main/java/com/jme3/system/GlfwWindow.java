package com.jme3.system;

import com.jme3.math.Vector2f;

public interface GlfwWindow {

    long getWindowHandle();

    Vector2f getWindowContentScale(Vector2f store);

    boolean isRenderable();

    void registerWindowSizeListener(WindowSizeListener listener);

    void removeWindowSizeListener(WindowSizeListener listener);

}
