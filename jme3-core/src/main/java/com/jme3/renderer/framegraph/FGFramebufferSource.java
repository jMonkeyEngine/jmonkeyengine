package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;

/**
 * @author JohnKkk
 */
public class FGFramebufferSource extends FGSource{
    private FrameBufferSourceProxy frameBufferSourceProxy;
    public final static class FrameBufferSourceProxy extends FGBindable{
        private FrameBuffer frameBuffer;

        public FrameBufferSourceProxy(FrameBuffer frameBuffer) {
            this.frameBuffer = frameBuffer;
        }

        public FrameBuffer getFrameBuffer() {
            return frameBuffer;
        }
    }
    public FGFramebufferSource(String name, FrameBuffer frameBuffer) {
        super(name);
        frameBufferSourceProxy = new FrameBufferSourceProxy(frameBuffer);
    }

    @Override
    public void postLinkValidate() {

    }

    @Override
    public FGBindable yieldBindable() {
        return frameBufferSourceProxy;
    }
}
