package com.jme3.renderer.framegraph;

import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;

public class FGRenderTargetSource extends FGSource{
    RenderTargetSourceProxy renderTargetSourceProxy;
    public final static class RenderTargetSourceProxy extends FGBindable{
        FrameBuffer.FrameBufferTextureTarget renderTarget;

        public RenderTargetSourceProxy(FrameBuffer.FrameBufferTextureTarget renderTarget) {
            this.renderTarget = renderTarget;
        }

        /**
         * return RT.<br/>
         * @return
         */
        public FrameBuffer.FrameBufferTextureTarget getRenderTarget() {
            return renderTarget;
        }

        /**
         * return RT shaderResource.<br/>
         * @return
         */
        public Texture getShaderResource(){
            return renderTarget.getTexture();
        }
    }
    public FGRenderTargetSource(String name, FrameBuffer.FrameBufferTextureTarget renderTarget) {
        super(name);
        renderTargetSourceProxy = new RenderTargetSourceProxy(renderTarget);
    }

    @Override
    public void postLinkValidate() {

    }

    @Override
    public FGBindable yieldBindable() {
        return renderTargetSourceProxy;
    }
}
