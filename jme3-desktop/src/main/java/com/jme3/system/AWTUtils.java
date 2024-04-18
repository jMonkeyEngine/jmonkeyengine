package com.jme3.system;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;

public class AWTUtils {
    /**
     * This should be a Temporary solution. FrameBuffer functions and Image-Formats are deprecated.
     * 
     * @param width
     * @param height
     * @param samples
     * @return
     */
    @SuppressWarnings("deprecation")
    public static FrameBuffer getFrameBuffer(int width, int height, int samples) {
        FrameBuffer frameBuffer = new FrameBuffer(width, height, samples);
        frameBuffer.setDepthBuffer(Image.Format.Depth);
        frameBuffer.setColorBuffer(Image.Format.RGBA8);
        frameBuffer.setSrgb(true);

        return frameBuffer;
    }

}
