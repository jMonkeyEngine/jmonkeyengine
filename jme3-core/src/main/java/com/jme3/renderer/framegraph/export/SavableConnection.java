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
package com.jme3.renderer.framegraph.export;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;

/**
 * Represents a connection of tickets between render passes.
 * 
 * @author codex
 */
public class SavableConnection implements Savable {
    
    private int targetId, sourceId;
    private String targetTicket, sourceTicket;
    
    /**
     * Serialization only.
     */
    public SavableConnection() {}
    /**
     * 
     * @param targetId
     * @param sourceId
     * @param targetTicket
     * @param sourceTicket 
     */
    public SavableConnection(int targetId, int sourceId, String targetTicket, String sourceTicket) {
        this.targetId = targetId;
        this.sourceId = sourceId;
        this.targetTicket = targetTicket;
        this.sourceTicket = sourceTicket;
    }
    
    /**
     * 
     * @param targetId 
     */
    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }
    /**
     * 
     * @param sourceId 
     */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }
    /**
     * 
     * @param targetTicket 
     */
    public void setTargetTicket(String targetTicket) {
        this.targetTicket = targetTicket;
    }
    /**
     * 
     * @param sourceTicket 
     */
    public void setSourceTicket(String sourceTicket) {
        this.sourceTicket = sourceTicket;
    }
    
    /**
     * 
     * @return 
     */
    public int getTargetId() {
        return targetId;
    }
    /**
     * 
     * @return 
     */
    public int getSourceId() {
        return sourceId;
    }
    /**
     * 
     * @return 
     */
    public String getTargetTicket() {
        return targetTicket;
    }
    /**
     * 
     * @return 
     */
    public String getSourceTicket() {
        return sourceTicket;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(targetId, "inputId", -1);
        out.write(sourceId, "outputId", -1);
        out.write(targetTicket, "inputTicket", "");
        out.write(sourceTicket, "outputTicket", "");
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        targetId = in.readInt("inputId", -1);
        sourceId = in.readInt("outputId", -1);
        targetTicket = in.readString("inputTicket", "");
        sourceTicket = in.readString("outputTicket", "");
    }
    
}
