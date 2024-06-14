/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
