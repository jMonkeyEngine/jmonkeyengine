package com.jme3.system;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.FrameBuffer.FrameBufferTarget;

public class AWTUtils {
    /**
     * Returns a Frame buffer.
     * 
     * @param width
     * @param height
     * @param samples
     * @return
     */
    public static FrameBuffer getFrameBuffer(int width, int height, int samples) {
        FrameBuffer frameBuffer = new FrameBuffer(width, height, samples);

        frameBuffer.addColorTarget(FrameBufferTarget.newTarget(Image.Format.RGBA8));
        frameBuffer.setDepthTarget(FrameBufferTarget.newTarget(Image.Format.Depth));
        frameBuffer.setSrgb(true);

        return frameBuffer;
    }

}
