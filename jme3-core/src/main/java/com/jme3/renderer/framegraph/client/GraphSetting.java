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
package com.jme3.renderer.framegraph.client;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.SavableObject;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FrameGraph;
import java.io.IOException;

/**
 * Shares settings from the FrameGraph settings map with the FrameGraph.
 * 
 * @author codex
 * @param <T>
 */
public class GraphSetting <T> implements GraphSource<T>, GraphTarget<T>, Savable {
    
    private String name;
    private ViewPortFilter filter;
    private T defValue;
    
    /**
     * Serialization only.
     */
    public GraphSetting() {
        this("", null);
    }
    /**
     * Graph setting that interacts with the named setting
     * stored in the FrameGraph.
     * 
     * @param name 
     */
    public GraphSetting(String name) {
        this(name, null);
    }
    /**
     * Graph setting that interacts with the named setting
     * stored in the FrameGraph.
     * 
     * @param name
     * @param defValue 
     */
    public GraphSetting(String name, T defValue) {
        this.name = name;
        this.defValue = defValue;
    }
    
    @Override
    public T getGraphValue(FrameGraph frameGraph, ViewPort viewPort) {
        assert name != null : "Setting name cannot be null.";
        if (filter == null || filter.accept(viewPort)) {
            T value = frameGraph.getSetting(name);
            if (value != null) {
                return value;
            } else {
                return defValue;
            }
        }
        return null;
    }
    @Override
    public boolean setGraphValue(FrameGraph frameGraph, ViewPort viewPort, T value) {
        assert name != null : "Setting name cannot be null.";
        if (filter == null || filter.accept(viewPort)) {
            frameGraph.setSetting(name, value);
            return true;
        }
        return false;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", null);
        if (filter != null && filter instanceof Savable) {
            out.write((Savable)filter, "filter", new DefaultSavableFilter());
        }
        out.write(new SavableObject(defValue), "defValue", SavableObject.NULL);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", null);
        filter = (ViewPortFilter)in.readSavable("filter", new DefaultSavableFilter());
        defValue = (T)in.readSavableObject("defValue", SavableObject.NULL).getObject();
    }
    
    /**
     * Sets the viewport filter.
     * <p>
     * Rejected viewports are not able to change or read from the settings map via
     * this object.
     * 
     * @param filter 
     */
    public void setFilter(ViewPortFilter filter) {
        this.filter = filter;
    }
    
    /**
     * Sets the default value used if no value is available in
     * the FrameGraph settings.
     * <p>
     * default=null
     * 
     * @param defValue
     */
    public void setDefaultValue(T defValue) {
        this.defValue = defValue;
    }
    
    /**
     * Gets the name of the setting this interacts with.
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the viewport filter.
     * 
     * @return 
     */
    public ViewPortFilter getFilter() {
        return filter;
    }
    
    /**
     * 
     * @return 
     */
    public T getDefaultValue() {
        return defValue;
    }
    
    public static class DefaultSavableFilter implements ViewPortFilter, Savable {

        @Override
        public boolean accept(ViewPort vp) {
            return true;
        }
        @Override
        public void write(JmeExporter ex) throws IOException {}
        @Override
        public void read(JmeImporter im) throws IOException {}
        
    }
    
}
