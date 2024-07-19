/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
 * Downsamples from the input texture to an output texture 1/4 the size.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Input: the input color texture.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Output: the texture downsampled to. Is 1/4 the size of the input texture,
 * and matches the format of the input texture.</li>
 * </ul>
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
