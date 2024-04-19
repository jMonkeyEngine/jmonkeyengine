/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.light.LightList;
import com.jme3.renderer.framegraph.FGBindable;
import com.jme3.renderer.framegraph.RenderContext;

/**
 *
 * @author codex
 */
public class DeferredLightDataProxy implements FGBindable {
    
    private final LightList lightData;

    public DeferredLightDataProxy(LightList lightData) {
        this.lightData = lightData;
    }

    public LightList getLightData() {
        return lightData;
    }

    @Override
    public void bind(RenderContext renderContext) {}
    
}
