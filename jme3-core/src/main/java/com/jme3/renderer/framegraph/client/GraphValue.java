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
 * Implementation of GraphSource and GraphTarget that manages a value.
 * 
 * @author codex
 * @param <T>
 */
public class GraphValue <T> implements GraphSource<T>, GraphTarget<T>, Savable {
    
    private T value;
    
    public GraphValue() {}
    public GraphValue(T value) {
        this.value = value;
    }
    
    @Override
    public T getGraphValue(FrameGraph frameGraph, ViewPort viewPort) {
        return value;
    }
    @Override
    public boolean setGraphValue(FrameGraph frameGraph, ViewPort viewPort, T value) {
        this.value = value;
        return true;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(new SavableObject(value), "value", SavableObject.NULL);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        value = (T)in.readSavableObject("value", SavableObject.NULL).getObject();
    }
    
    /**
     * Sets the held value.
     * <p>
     * Subject to change if values are being recieved from the FrameGraph.
     * 
     * @param value 
     */
    public void setValue(T value) {
        this.value = value;
    }
    
    /**
     * Gets the held value.
     * 
     * @return 
     */
    public T getValue() {
        return value;
    }
    
}
