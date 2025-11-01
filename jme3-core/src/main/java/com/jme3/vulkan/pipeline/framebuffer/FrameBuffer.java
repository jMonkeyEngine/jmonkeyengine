package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.texture.ImageView;

import java.util.List;

public interface FrameBuffer <T extends ImageView> {

    void addColorTarget(T image);

    void setColorTarget(int i, T image);

    void removeColorTarget(int i);

    void removeColorTarget(T image);

    void clearColorTargets();

    void setDepthTarget(T image);

    List<T> getColorTargets();

    T getColorTarget(int i);

    T getDepthTarget();

}
