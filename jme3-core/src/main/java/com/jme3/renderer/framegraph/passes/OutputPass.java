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
package com.jme3.renderer.framegraph.passes;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * Renders a set of color and depth textures on a fullscreen quad to the
 * viewport's output framebuffer.
 * 
 * @author codex
 */
public class OutputPass extends RenderPass {
    
    private ResourceTicket<Texture2D> color, depth;
    private Float alphaDiscard;

    public OutputPass() {
        this(null);
    }
    public OutputPass(Float alphaDiscard) {
        this.alphaDiscard = alphaDiscard;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("Color");
        depth = addInput("Depth");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(color, depth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        context.popFrameBuffer();
        Texture2D colorTex = resources.acquireOrElse(color, null);
        Texture2D depthTex = resources.acquireOrElse(depth, null);
        if (alphaDiscard != null) {
            context.getScreen().setAlphaDiscard(alphaDiscard);
        }
        //System.out.println("camera: "+context.getWidth()+" "+context.getHeight());
        //System.out.println("texture: "+colorTex.getImage().getWidth()+" "+colorTex.getImage().getHeight());
        //context.resizeCamera(context.getWidth(), context.getHeight(), true, false, true);
        context.renderTextures(colorTex, depthTex);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return color.hasSource() || depth.hasSource();
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(alphaDiscard, "AlphaDiscard", -1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        alphaDiscard = in.readFloat("AlphaDiscard", -1);
    }
    
}
