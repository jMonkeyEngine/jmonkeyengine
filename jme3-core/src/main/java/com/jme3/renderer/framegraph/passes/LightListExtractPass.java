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

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.light.LightProbe;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import java.util.LinkedList;
import java.util.List;

/**
 * Extracts ambient light and light probes from a LightList.
 * 
 * @author codex
 */
public class LightListExtractPass extends RenderPass {

    private ResourceTicket<LightList> inLights, outLights;
    private ResourceTicket<ColorRGBA> ambient;
    private ResourceTicket<List<LightProbe>> probes;
    private final LightList outLightList = new LightList(null);
    private final ColorRGBA ambColor = new ColorRGBA(0, 0, 0, 1);
    private final List<LightProbe> probeList = new LinkedList<>();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inLights = addInput("Lights");
        outLights = addOutput("Lights");
        ambient = addOutput("Ambient");
        probes = addOutput("Probes");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(inLights);
        declare(null, outLights);
        declare(null, ambient);
        declare(null, probes);
    }
    @Override
    protected void execute(FGRenderContext context) {
        LightList list = resources.acquireOrElse(inLights, null);
        if (list != null) {
            ambColor.set(0, 0, 0, 1);
            for (Light l : list) switch (l.getType()) {
                case Ambient:
                    ambColor.addLocal(l.getColor());
                    break;
                case Probe:
                    probeList.add((LightProbe)l);
                    break;
                default:
                    outLightList.add(l);
            }
            resources.setPrimitive(outLights, outLightList);
            resources.setPrimitive(ambient, ambColor);
            resources.setPrimitive(probes, probeList);
        } else {
            resources.setUndefined(outLights);
            resources.setUndefined(ambient);
            resources.setUndefined(probes);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {
        outLightList.clear();
        probeList.clear();
    }
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
