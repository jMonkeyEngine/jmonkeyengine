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
package com.jme3.renderer.framegraph;

import com.jme3.asset.AssetManager;
import com.jme3.renderer.framegraph.passes.DeferredPass;
import com.jme3.renderer.framegraph.passes.GBufferPass;
import com.jme3.renderer.framegraph.passes.Junction;
import com.jme3.renderer.framegraph.passes.LightImagePass;
import com.jme3.renderer.framegraph.passes.OutputBucketPass;
import com.jme3.renderer.framegraph.passes.OutputPass;
import com.jme3.renderer.framegraph.passes.PostProcessingPass;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.renderer.framegraph.passes.TileDeferredPass;
import com.jme3.renderer.queue.RenderQueue;

/**
 * Utility class for constructing common framegraphs.
 * 
 * @author codex
 */
public class FrameGraphFactory {
    
    /**
     * Constructs a standard forward framegraph, with no controllable features.
     * 
     * @param assetManager
     * @return forward framegraph
     */
    public static FrameGraph forward(AssetManager assetManager) {
        FrameGraph fg = new FrameGraph(assetManager);
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Opaque));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Sky, DepthRange.REAR));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Transparent));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Gui, DepthRange.FRONT));
        fg.add(new PostProcessingPass());
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Translucent));
        return fg;
    }
    
    /**
     * Constructs a deferred or tiled deferred framegraph.
     * 
     * @param assetManager
     * @param tiled true to construct advanced tiled deferred
     * @return deferred framegraph
     */
    public static FrameGraph deferred(AssetManager assetManager, boolean tiled) {
        FrameGraph fg = new FrameGraph(assetManager);
        GBufferPass gbuf = fg.add(new GBufferPass());
        LightImagePass lightImg = fg.add(new LightImagePass());
        Junction lightJunct = fg.add(new Junction(2, 7));
        RenderPass deferred;
        if (!tiled) {
            deferred = fg.add(new DeferredPass());
        } else {
            deferred = fg.add(new TileDeferredPass());
        }
        OutputPass defOut = fg.add(new OutputPass(0f));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Sky, DepthRange.REAR));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Transparent));
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Gui, DepthRange.FRONT));
        fg.add(new PostProcessingPass());
        fg.add(new OutputBucketPass(RenderQueue.Bucket.Translucent));
        
        lightImg.makeInput(gbuf, "Lights", "Lights");
        
        lightJunct.setName("LightPackMethod");
        lightJunct.makeInput(lightImg, "Textures", "Input[0]", 0, 3);
        lightJunct.makeInput(lightImg, "NumLights", "Input[0][3]");
        lightJunct.makeInput(lightImg, "Ambient", "Input[0][4]");
        lightJunct.makeInput(lightImg, "Probes", "Input[0][5]");
        lightJunct.makeInput(gbuf, "Lights", "Input[1][6]");
        
        deferred.makeInput(lightJunct, "Value", "LightTextures", 0, 3);
        deferred.makeInput(lightJunct, "Value[3]", "NumLights");
        deferred.makeInput(lightJunct, "Value[4]", "Ambient");
        deferred.makeInput(lightJunct, "Value[5]", "Probes");
        deferred.makeInput(lightJunct, "Value[6]", "Lights");
        deferred.makeInput(gbuf, "GBufferData", "GBufferData");
        
        defOut.makeInput(deferred, "Color", "Color");
        defOut.makeInput(gbuf, "GBufferData[4]", "Depth");
        
        return fg;
        
    }
    
}
