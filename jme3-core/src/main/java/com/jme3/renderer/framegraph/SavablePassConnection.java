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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * Represents an abstract connection between render passes.
 * 
 * @author codex
 */
public class SavablePassConnection implements Savable {
    
    private int inputId, outputId;
    private String inputTicket, outputTicket;
    
    /**
     * 
     */
    public SavablePassConnection() {}
    /**
     * 
     * @param inputId
     * @param outputId
     * @param inputTicket
     * @param outputTicket 
     */
    public SavablePassConnection(int inputId, int outputId, String inputTicket, String outputTicket) {
        this.inputId = inputId;
        this.outputId = outputId;
        this.inputTicket = inputTicket;
        this.outputTicket = outputTicket;
    }
    
    /**
     * 
     * @param shift 
     */
    public void shiftIds(long shift) {
        inputId += shift;
        outputId += shift;
    }
    
    /**
     * 
     * @param inputId 
     */
    public void setInputId(int inputId) {
        this.inputId = inputId;
    }
    /**
     * 
     * @param outputId 
     */
    public void setOutputId(int outputId) {
        this.outputId = outputId;
    }
    /**
     * 
     * @param inputTicket 
     */
    public void setInputTicket(String inputTicket) {
        this.inputTicket = inputTicket;
    }
    /**
     * 
     * @param outputTicket 
     */
    public void setOutputTicket(String outputTicket) {
        this.outputTicket = outputTicket;
    }
    
    /**
     * 
     * @return 
     */
    public int getInputId() {
        return inputId;
    }
    /**
     * 
     * @return 
     */
    public int getOutputId() {
        return outputId;
    }
    /**
     * 
     * @return 
     */
    public String getInputTicket() {
        return inputTicket;
    }
    /**
     * 
     * @return 
     */
    public String getOutputTicket() {
        return outputTicket;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(inputId, "inputId", -1);
        out.write(outputId, "outputId", -1);
        out.write(inputTicket, "inputTicket", "");
        out.write(outputTicket, "outputTicket", "");
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        inputId = in.readInt("inputId", -1);
        outputId = in.readInt("outputId", -1);
        inputTicket = in.readString("inputTicket", "");
        outputTicket = in.readString("outputTicket", "");
    }
    
}
