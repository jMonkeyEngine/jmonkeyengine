/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.client;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FrameGraph;
import java.io.IOException;

/**
 *
 * @author codex
 * @param <T>
 */
public class GraphSetting <T> implements GraphSource<T>, GraphTarget<T>, Savable {
    
    private String name;
    private ViewPortFilter filter;
    
    public GraphSetting() {
        this("");
    }
    public GraphSetting(String name) {
        this.name = name;
    }
    
    @Override
    public T getGraphValue(FrameGraph frameGraph, ViewPort viewPort) {
        if (filter == null || filter.confirm(viewPort)) {
            return frameGraph.getSetting(name);
        }
        return null;
    }
    @Override
    public boolean setGraphValue(FrameGraph frameGraph, ViewPort viewPort, T value) {
        if (filter == null || filter.confirm(viewPort)) {
            frameGraph.setSetting(name, value);
            return true;
        }
        return false;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", "");
        if (filter != null && filter instanceof Savable) {
            out.write((Savable)filter, "filter", new DefaultSavableFilter());
        }
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "");
        filter = (ViewPortFilter)in.readSavable("filter", new DefaultSavableFilter());
    }
    
    public void setFilter(ViewPortFilter filter) {
        this.filter = filter;
    }
    
    public String getName() {
        return name;
    }
    public ViewPortFilter getFilter() {
        return filter;
    }
    
    public static class DefaultSavableFilter implements ViewPortFilter, Savable {

        @Override
        public boolean confirm(ViewPort vp) {
            return true;
        }
        @Override
        public void write(JmeExporter ex) throws IOException {}
        @Override
        public void read(JmeImporter im) throws IOException {}
        
    }
    
}
