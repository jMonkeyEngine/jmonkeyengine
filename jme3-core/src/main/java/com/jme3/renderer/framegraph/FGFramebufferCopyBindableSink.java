package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;

import java.util.ArrayList;

/**
 * @author JohnKkk
 * @param <T>
 */
public class FGFramebufferCopyBindableSink<T extends FGFramebufferSource.FrameBufferSourceProxy> extends FGContainerBindableSink<T>{
    FramebufferCopyBindableProxy framebufferCopyBindableProxy;
    public final void setDistFrameBuffer(FrameBuffer distFrameBuffer){
        framebufferCopyBindableProxy.distFramebuffer = distFrameBuffer;
    }
    public FGFramebufferCopyBindableSink(String registeredName, FrameBuffer distFrameBuffer, boolean copyColor, boolean copyDepth, boolean copyStencil, ArrayList<FGBindable> container, int index) {
        super(registeredName, container, index);
        framebufferCopyBindableProxy = new FramebufferCopyBindableProxy(distFrameBuffer, copyColor, copyDepth, copyStencil);
    }

    private final static class FramebufferCopyBindableProxy extends FGBindable{
        FrameBuffer sourceFramebuffer;
        FrameBuffer distFramebuffer;
        boolean bCopyColor;
        boolean bCopyDepth;
        boolean bCopyStencil;

        public FramebufferCopyBindableProxy(FrameBuffer distFramebuffer, boolean bCopyColor, boolean bCopyDepth, boolean bCopyStencil) {
            this.distFramebuffer = distFramebuffer;
            this.bCopyColor = bCopyColor;
            this.bCopyDepth = bCopyDepth;
            this.bCopyStencil = bCopyStencil;
        }

        public void setSourceFramebuffer(FrameBuffer sourceFramebuffer) {
            this.sourceFramebuffer = sourceFramebuffer;
        }

        @Override
        public void bind(FGRenderContext renderContext) {
            if(this.distFramebuffer != null || this.sourceFramebuffer != null){
                renderContext.renderManager.getRenderer().copyFrameBuffer(this.sourceFramebuffer, this.distFramebuffer != null ? this.distFramebuffer : renderContext.viewPort.getOutputFrameBuffer(), bCopyColor, bCopyDepth || bCopyStencil);
            }
        }
    }

    @Override
    public void bind(FGSource fgSource) {
        T p = (T)fgSource.yieldBindable();
        if(p == null){
            System.err.println("Binding input [" + getRegisteredName() + "] to output [" + getLinkPassName() + "." + getLinkPassResName() + "] " + " { " + fgSource.getName() + " } ");
            return;
        }
        if(fgSource instanceof FGFramebufferSource){
            linked = true;
            FGFramebufferSource framebufferSource = (FGFramebufferSource)fgSource;
            framebufferCopyBindableProxy.setSourceFramebuffer(((FGFramebufferSource.FrameBufferSourceProxy)framebufferSource.yieldBindable()).getFrameBuffer());
            bindableProxy.targetBindable = framebufferCopyBindableProxy;
        }
        else{
            System.err.println(getRegisteredName() + " needs a FGFramebufferSource");
        }
    }

    @Override
    public void postLinkValidate() {

    }
}
