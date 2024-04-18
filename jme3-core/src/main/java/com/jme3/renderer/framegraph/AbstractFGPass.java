/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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

import java.util.ArrayList;

/**
 * FGPass.
 * @author JohnKkk
 */
public abstract class AbstractFGPass implements FGPass {
    
    private final String name;
    private final ArrayList<FGSink> sinks;
    private final ArrayList<FGSource> sources;
    protected boolean resetResources = false;
    
    public AbstractFGPass(String name){
        this.name = name;
        this.sinks = new ArrayList<>();
        this.sources = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ArrayList<FGSink> getSinks() {
        return sinks;
    }

    @Override
    public ArrayList<FGSource> getSources() {
        return sources;
    }
    
    @Override
    public boolean isResetResources() {
        return resetResources;
    }
    
    @Override
    public FGSource getSource(String name){
        for(FGSource src : getSources()){
            if(src.getName().equals(name)){
                return src;
            }
        }
        
        System.err.println("Output name [" + name + "] not fount in pass:" + getName());
        return null;
    }
    
    @Override
    public FGSink getSink(String registeredName) {
        for(FGSink sink : sinks){
            if(sink.getRegisteredName().equals(registeredName)){
                return sink;
            }
        }
        return null;
    }
    
    @Override
    public void setSinkLinkage(String registeredName, String targetPass, String targetResource) {
        getSink(registeredName).setTarget(targetPass, targetResource);
    }
    
    @Override
    public void registerSink(AbstractFGSink sink){
        // check for overlap of input names
        for(FGSink si : sinks){
            if(si.getRegisteredName().equals(sink.getRegisteredName())){
                System.err.println("Registered input overlaps with existing: " + sink.getRegisteredName());
                return;
            }
        }
        getSinks().add(sink);
    }
    
    @Override
    public void registerSource(FGSource source) {
        // check for overlap of output names
        for(FGSource src : sources){
            if(src.getName().equals(source.getName())){
                System.err.println("Registered input overlaps with existing: " + source.getName());
                return;
            }
        }
        getSources().add(source);
    }
    
    @Override
    public void resetPass() {
        if(isResetResources()){
            getSources().clear();
            getSinks().clear();
        }
    }
    
    @Override
    public void finalizePass() {
        for(FGSink sink : sinks){
            sink.postLinkValidate();
        }
        for(FGSource src : sources){
            src.postLinkValidate();
        }
    }
    
}
