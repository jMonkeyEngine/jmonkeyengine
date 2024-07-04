/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jme3test.framegraph.examples;

import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class DownsamplingPass extends RenderPass {
    
    private ResourceTicket<Texture2D> in;
    private ResourceTicket<Texture2D> out;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        in = addInput("Input");
        out = addOutput("Output");
        texDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texDef.setMagFilter(Texture.MagFilter.Nearest);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, out);
        reserve(out);
        reference(in);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        Texture2D inTex = resources.acquire(in);
        Image img = inTex.getImage();
        
        int w = img.getWidth() / 2;
        int h = img.getHeight() / 2;
        texDef.setSize(w, h);
        
        texDef.setFormat(img.getFormat());
        
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireColorTarget(fb, out);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        
        context.resizeCamera(w, h, false, false, false);
        context.renderTextures(inTex, null);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
