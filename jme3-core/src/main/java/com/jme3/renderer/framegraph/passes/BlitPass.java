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

import com.jme3.material.Material;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 * 
 * @author codex
 */
public class BlitPass extends RenderPass {

    private ResourceTicket<FrameBuffer> source;
    private ResourceTicket<Texture2D> color, depth;
    private final TextureDef<Texture2D> colorDef = TextureDef.texture2D();
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D();
    private Material nullMat;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        source = addInput("Source");
        color = addOutput("Color");
        depth = addOutput("Depth");
        colorDef.setFormatFlexible(true);
        depthDef.setFormat(Image.Format.Depth);
        depthDef.setFormatFlexible(true);
        nullMat = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Misc/Null.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(colorDef, color);
        declare(depthDef, depth);
        reserve(color, depth);
        referenceOptional(source);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer sourceBuf = resources.acquireOrElse(source, null);
        FrameBuffer targetBuf;
        if (sourceBuf != null) {
            targetBuf = getFrameBuffer(sourceBuf.getWidth(), sourceBuf.getHeight(), sourceBuf.getSamples());
            colorDef.setSize(sourceBuf.getWidth(), sourceBuf.getHeight());
            depthDef.setSize(sourceBuf.getWidth(), sourceBuf.getHeight());
        } else {
            targetBuf = getFrameBuffer(context, 1);
            colorDef.setSize(context.getWidth(), context.getHeight());
            depthDef.setSize(context.getWidth(), context.getHeight());
        }
        context.getRenderer().copyFrameBuffer(sourceBuf, targetBuf, true, true);
        resources.acquireColorTarget(targetBuf, color);
        resources.acquireDepthTarget(targetBuf, depth);
        context.getRenderer().setFrameBuffer(targetBuf);
        context.renderFullscreen(nullMat);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
